import { FunctionCallingConfigMode } from '@google/genai';
import { geminiCliDefaultModelId, geminiCliModels } from '@roo-code/types';
import { readFile, writeFile } from 'fs/promises';
import { OAuth2Client } from 'google-auth-library';
import { homedir } from 'node:os';
import { dirname, join } from 'node:path';
import { createInterface } from 'node:readline';
import { Readable } from 'node:stream';
import { convertAnthropicMessageToGemini } from '../transform/gemini-format';
import { getModelParams } from '../transform/model-params';
import { BaseProvider } from './base-provider';

import type { Anthropic } from '@anthropic-ai/sdk';
import type { GeminiCliModelId } from '@roo-code/types';
import type { ApiHandlerOptions } from '../../shared/api';
import type { ApiHandlerCreateMessageMetadata, SingleCompletionHandler } from '../index';
import type { ApiStream } from '../transform/stream';

const CODE_ASSIST_ENDPOINT = 'https://cloudcode-pa.googleapis.com/v1internal';
const OAUTH_CLIENT_ID = '681255809395-oo8ft2oprdrnp9e3aqf6av3hmdib135j.apps.googleusercontent.com';
const OAUTH_CLIENT_SECRET = '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';
const OAUTH_REDIRECT_URI = 'http://localhost:45289';
const OAUTH_DEFAULT_PATH = join(homedir(), '.gemini', 'oauth_creds.json');

interface OAuthCredentials {
  readonly access_token: string;
  readonly refresh_token: string;
  readonly token_type: string;
  readonly expiry_date: number;
}

export class GeminiCliHandler extends BaseProvider implements SingleCompletionHandler {
  private readonly options: ApiHandlerOptions;
  private readonly authClient: OAuth2Client;

  private credPath: string = OAUTH_DEFAULT_PATH;
  private credentials?: OAuthCredentials;
  private projectId?: string;
  private lastResponseId?: string;
  private lastThoughtSignature?: string;

  public constructor(options: ApiHandlerOptions) {
    super();
    this.options = options;
    this.authClient = new OAuth2Client(OAUTH_CLIENT_ID, OAUTH_CLIENT_SECRET, OAUTH_REDIRECT_URI);
  }

  public async *createMessage(
    systemInstruction: string,
    messages: Anthropic.Messages.MessageParam[],
    metadata?: ApiHandlerCreateMessageMetadata,
  ): ApiStream {
    const { id: model, info, reasoning: thinkingConfig, maxTokens } = this.getModel();
    this.lastThoughtSignature = undefined;
    this.lastResponseId = undefined;

    const isHybridReasoningModel = (info as any).supportsReasoningBudget || (info as any).requiredReasoningBudget;
    const maxOutputTokens = isHybridReasoningModel ? (this.options.modelMaxTokens ?? maxTokens ?? undefined) : (maxTokens ?? undefined);

    const usingNativeTools = Boolean(metadata?.tools && metadata.tools.length > 0);
    const includeThoughtSignatures = Boolean(thinkingConfig) || usingNativeTools;

    const toolIdToName = new Map<string, string>();
    for (const message of messages) {
      if (Array.isArray(message.content)) {
        for (const block of message.content) {
          if (block.type === 'tool_use') {
            toolIdToName.set(block.id, block.name);
          }
        }
      }
    }

    const contents = messages.map((message) => convertAnthropicMessageToGemini(message, { includeThoughtSignatures, toolIdToName })).flat();

    const tools: any[] = [];
    if (metadata?.tools && metadata.tools.length > 0) {
      tools.push({
        functionDeclarations: metadata.tools.map((tool) => ({
          name: (tool as any).function.name,
          description: (tool as any).function.description,
          parametersJsonSchema: (tool as any).function.parameters,
        })),
      });
    }

    const supportsTemperature = (info as any).supportsTemperature !== false;
    const temperatureConfig: number | undefined = supportsTemperature
      ? (this.options.modelTemperature ?? (info as any).defaultTemperature ?? 1)
      : (info as any).defaultTemperature;

    await this.ensureAuthenticated();
    await this.discoverProjectId();

    const config: any = {
      generationConfig: {
        temperature: temperatureConfig,
        topP: 0.95,
        topK: 64,
        maxOutputTokens,
        ...(thinkingConfig ? { thinkingConfig } : {}),
      },
    };

    if (metadata?.allowedFunctionNames && metadata.allowedFunctionNames.length > 0) {
      config.toolConfig = {
        functionCallingConfig: {
          mode: FunctionCallingConfigMode.ANY,
          allowedFunctionNames: metadata.allowedFunctionNames,
        },
      };
    } else if (metadata?.tool_choice) {
      const choice = metadata.tool_choice;
      let mode: FunctionCallingConfigMode;
      let allowedFunctionNames: string[] | undefined;

      if (choice === 'auto') {
        mode = FunctionCallingConfigMode.AUTO;
      } else if (choice === 'none') {
        mode = FunctionCallingConfigMode.NONE;
      } else if (choice === 'required') {
        mode = FunctionCallingConfigMode.ANY;
      } else if (typeof choice === 'object' && 'function' in choice && choice.type === 'function') {
        mode = FunctionCallingConfigMode.ANY;
        allowedFunctionNames = [choice.function.name];
      } else {
        mode = FunctionCallingConfigMode.AUTO;
      }

      config.toolConfig = {
        functionCallingConfig: {
          mode,
          ...(allowedFunctionNames ? { allowedFunctionNames } : {}),
        },
      };
    }

    const requestBody = {
      model,
      project: this.projectId,
      request: {
        systemInstruction: {
          role: 'user',
          parts: [{ text: systemInstruction }],
        },
        contents,
        ...config,
        ...(tools.length > 0 ? { tools } : {}),
      },
    };

    const controller = new AbortController();
    const result = await this.authClient.request({
      method: 'POST',
      url: this.getApiUrl('streamGenerateContent'),
      headers: { 'Content-Type': 'application/json' },
      params: { alt: 'sse' },
      responseType: 'stream',
      data: requestBody,
      signal: controller.signal,
    });

    let lastUsage: any;
    let toolCallCounter = 0;

    try {
      const stream = this.parseSSEStream(result.data as NodeJS.ReadableStream);
      for await (const jsonData of stream) {
        const response = jsonData.response;
        if (response?.responseId) {
          this.lastResponseId = response.responseId;
        }

        const candidate = response.candidates?.[0];

        if (candidate?.content?.parts) {
          for (const part of candidate.content.parts) {
            if (part.thoughtSignature && includeThoughtSignatures) {
              this.lastThoughtSignature = part.thoughtSignature;
            }

            if (part.text) {
              if (part.thought) {
                yield { type: 'reasoning', text: part.text };
              } else {
                yield { type: 'text', text: part.text };
              }
            } else if (part.functionCall) {
              const callId = `${part.functionCall.name}-${toolCallCounter}`;
              const args = JSON.stringify(part.functionCall.args);

              yield {
                type: 'tool_call_partial',
                index: toolCallCounter,
                id: callId,
                name: part.functionCall.name,
                arguments: undefined,
              };

              yield {
                type: 'tool_call_partial',
                index: toolCallCounter,
                id: callId,
                name: undefined,
                arguments: args,
              };

              toolCallCounter++;
            }
          }
        }
        if (response.usageMetadata) lastUsage = response.usageMetadata;
        if (candidate?.finishReason) {
          if (candidate.finishMessage) yield { type: 'text', text: candidate.finishMessage };
          break;
        }
      }
    } finally {
      controller.abort();
    }

    if (lastUsage) {
      yield {
        type: 'usage',
        inputTokens: lastUsage.promptTokenCount ?? 0,
        outputTokens: lastUsage.candidatesTokenCount ?? 0,
        cacheReadTokens: lastUsage.cachedContentTokenCount,
        reasoningTokens: lastUsage.thoughtsTokenCount,
        totalCost: 0,
      };
    }
  }

  public async completePrompt(prompt: string): Promise<string> {
    const { id: model, info } = this.getModel();

    const supportsTemperature = (info as any).supportsTemperature !== false;
    const temperatureConfig: number | undefined = supportsTemperature
      ? (this.options.modelTemperature ?? (info as any).defaultTemperature ?? 1)
      : (info as any).defaultTemperature;

    await this.ensureAuthenticated();
    await this.discoverProjectId();

    const requestBody = {
      model,
      project: this.projectId,
      request: {
        contents: [{ role: 'user', parts: [{ text: prompt }] }],
        generationConfig: { temperature: temperatureConfig },
      },
    };

    const result = await this.callEndpoint('generateContent', requestBody);
    const response = result.response;
    const candidate = response.candidates?.[0];

    return (
      candidate?.content?.parts
        ?.filter((p: any) => p.text && !p.thought)
        ?.map((p: any) => p.text)
        ?.join('') || ''
    );
  }

  public override getModel() {
    const modelId = this.options.apiModelId?.replace(':thinking', '') || geminiCliDefaultModelId;
    const id = (modelId in geminiCliModels ? modelId : geminiCliDefaultModelId) as GeminiCliModelId;
    const info = geminiCliModels[id];
    return {
      id,
      info,
      ...getModelParams({
        format: 'gemini',
        modelId: id,
        model: info,
        settings: this.options,
        defaultTemperature: (info as any).defaultTemperature ?? 1,
      }),
    };
  }

  public getThoughtSignature(): string | undefined {
    return this.lastThoughtSignature;
  }

  public getResponseId(): string | undefined {
    return this.lastResponseId;
  }

  private getApiUrl(method: string): string {
    return `${CODE_ASSIST_ENDPOINT}:${method}`;
  }

  private async ensureAuthenticated(): Promise<void> {
    if (!this.credentials) {
      let geminiCliOAuthPath = this.options.geminiCliOAuthPath || process.env.GOOGLE_OAUTH_PATH;
      if (!geminiCliOAuthPath) {
        try {
          const content = await readFile(join(dirname(OAUTH_DEFAULT_PATH), '.env'), 'utf-8');
          const match = content.match(/GOOGLE_OAUTH_PATH=["']?([^"'\r\n\s;]+)["']?/);
          const oauthPath = match ? match[1].trim() : '';
          if (oauthPath) geminiCliOAuthPath = oauthPath;
        } catch {}
      }

      this.credPath = geminiCliOAuthPath || OAUTH_DEFAULT_PATH;

      const data = await readFile(this.credPath, 'utf-8');
      this.credentials = JSON.parse(data);
      this.authClient.setCredentials(this.credentials!);
    }

    if (this.credentials!.expiry_date - 60_000 < Date.now()) {
      const { credentials } = await this.authClient.refreshAccessToken();
      this.credentials = {
        access_token: credentials.access_token!,
        refresh_token: credentials.refresh_token || this.credentials!.refresh_token,
        token_type: credentials.token_type || 'Bearer',
        expiry_date: credentials.expiry_date || Date.now() + 3600000,
      };
      await writeFile(this.credPath, JSON.stringify(this.credentials, null, 2));
      this.authClient.setCredentials(this.credentials);
    }
  }

  private async discoverProjectId(): Promise<void> {
    if (this.projectId) return;

    const geminiCliProjectId = this.options.geminiCliProjectId?.trim();
    if (geminiCliProjectId) {
      this.projectId = geminiCliProjectId;
      return;
    }

    if (process.env.GOOGLE_CLOUD_PROJECT) {
      this.projectId = process.env.GOOGLE_CLOUD_PROJECT;
      return;
    }

    try {
      const content = await readFile(join(dirname(this.credPath), '.env'), 'utf-8');
      const match = content.match(/GOOGLE_CLOUD_PROJECT=["']?([^"'\r\n\s;]+)["']?/);
      const projectId = match ? match[1].trim() : '';
      if (projectId) {
        this.projectId = projectId;
        return;
      }
    } catch {}

    const metadata = {
      ideType: 'IDE_UNSPECIFIED',
      platform: 'PLATFORM_UNSPECIFIED',
      pluginType: 'GEMINI',
    };

    const load = await this.callEndpoint('loadCodeAssist', {
      cloudaicompanionProject: this.projectId,
      metadata: { ...metadata, duetProject: this.projectId },
    });
    if (load.cloudaicompanionProject) {
      this.projectId = load.cloudaicompanionProject;
      return;
    }

    const tierId = load.allowedTiers?.find((t: any) => t.isDefault)?.id || 'free-tier';
    let onboard = await this.callEndpoint('onboardUser', {
      tierId,
      cloudaicompanionProject: undefined,
      metadata,
    });

    if (!onboard.done && onboard.name) {
      const operationName = onboard.name;
      while (!onboard.done) {
        await new Promise((f) => setTimeout(f, 5000));
        onboard = (
          await this.authClient.request({
            method: 'GET',
            url: `${CODE_ASSIST_ENDPOINT}/${operationName}`,
            headers: { 'Content-Type': 'application/json' },
          })
        ).data;
      }
    }

    this.projectId = onboard.response?.cloudaicompanionProject?.id;
  }

  private async callEndpoint(method: string, data: any, retry = true): Promise<any> {
    try {
      const result = await this.authClient.request({
        method: 'POST',
        url: this.getApiUrl(method),
        headers: { 'Content-Type': 'application/json' },
        data,
      });
      return result.data;
    } catch (error: any) {
      if (error.response?.status === 401 && retry) {
        await this.ensureAuthenticated();
        return this.callEndpoint(method, data, false);
      }
      throw error;
    }
  }

  private async *parseSSEStream(stream: NodeJS.ReadableStream): AsyncGenerator<any> {
    const rl = createInterface({ input: Readable.from(stream), crlfDelay: Infinity });

    const fullResponse: unknown[] = [];
    let bufferedLines: string[] = [];

    for await (const line of rl) {
      if (line.startsWith('data: ')) {
        bufferedLines.push(line.slice(6).trim());
      } else if (line === '') {
        if (bufferedLines.length === 0) continue;

        const data = JSON.parse(bufferedLines.join('\n'));
        fullResponse.push(data);
        yield data;
        bufferedLines = [];
      }
    }
  }
}