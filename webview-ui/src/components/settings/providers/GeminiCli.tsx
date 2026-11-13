import { VSCodeTextField } from "@vscode/webview-ui-toolkit/react"
import { useCallback } from "react"
import { inputEventTransform } from "../transforms"

import type { ProviderSettings } from "@roo-code/types"

interface GeminiCliProps {
	readonly apiConfiguration: ProviderSettings
	readonly setApiConfigurationField: (field: keyof ProviderSettings, value: ProviderSettings[keyof ProviderSettings]) => void
}

export const GeminiCli = ({ apiConfiguration, setApiConfigurationField }: GeminiCliProps) => {
	const handleInputChange = useCallback(
		<K extends keyof ProviderSettings, E>(
			field: K,
			transform: (event: E) => ProviderSettings[K] = inputEventTransform,
		) =>
			(event: E | Event) => {
				setApiConfigurationField(field, transform(event as E))
			},
		[setApiConfigurationField],
	)

	return (
    <>
      <VSCodeTextField
        value={apiConfiguration?.geminiCliOAuthPath || ""}
        onInput={handleInputChange("geminiCliOAuthPath")}
        placeholder="~/.gemini/oauth_creds.json"
        className="w-full">
        <label className="block font-medium mb-1">
          OAuth Credentials Path (optional)
        </label>
      </VSCodeTextField>
      <VSCodeTextField
        value={apiConfiguration?.geminiCliProjectId || ""}
        onInput={handleInputChange("geminiCliProjectId")}
        placeholder="gen-lang-client-0123456789"
        className="w-full">
        <label className="block font-medium mb-1">
          Cloud Project ID (optional)
        </label>
      </VSCodeTextField>
    </>
	)
}