// tsup.config.ts
import fs from "fs"
import { defineConfig } from "tsup"
import path from "path"

function resolvePnpmGracefulFs(): string {
	const pnpmStoreDir = path.resolve(__dirname, "../../node_modules/.pnpm")
	const entry = fs.readdirSync(pnpmStoreDir).find((name) => name.startsWith("graceful-fs@"))
	if (!entry) {
		throw new Error(`Could not locate graceful-fs in ${pnpmStoreDir}`)
	}
	return path.join(pnpmStoreDir, entry, "node_modules/graceful-fs/graceful-fs.js")
}

const vscodeEsmCompatPlugin = {
	name: "vscode-esm-compat",
	setup(build: any) {
		build.onLoad({ filter: /[\\/]deps[\\/]vscode[\\/]vs[\\/]workbench[\\/]api[\\/]node[\\/]extensionHostProcess\.ts$/ }, (args: any) => {
			let source = fs.readFileSync(args.path, "utf8")
			source = source.replace("import * as minimist from 'minimist';", "import minimist from 'minimist';")
			return { contents: source, loader: "ts" }
		})

		build.onLoad({ filter: /[\\/]deps[\\/]vscode[\\/]vs[\\/]workbench[\\/]api[\\/]node[\\/]proxyResolver\.ts$/ }, (args: any) => {
			let source = fs.readFileSync(args.path, "utf8")
			source = source
				.replace("import * as http from 'http';", "const http = require('http');")
				.replace("import * as https from 'https';", "const https = require('https');")
				.replace("import * as tls from 'tls';", "const tls = require('tls');")
				.replace("import * as net from 'net';", "const net = require('net');")
				.replace(
					"addCertificates: [],",
					"addCertificates: [],\n\t\tisUseHostProxyEnabled: () => doUseHostProxy,\n\t\tisAdditionalFetchSupportEnabled: () => false,\n\t\taddCertificatesV1: () => false,\n\t\taddCertificatesV2: () => false,\n\t\tloadAdditionalCertificates: async () => [],",
				)
			return { contents: source, loader: "ts" }
		})

		// Prevent infinite microtask loop in SpdLogLogger.flush() when optional @vscode/spdlog is unavailable
		build.onLoad({ filter: /[\\/]deps[\\/]vscode[\\/]vs[\\/]platform[\\/]log[\\/]node[\\/]spdlogLog\.ts$/ }, (args: any) => {
			let source = fs.readFileSync(args.path, "utf8")
			source = source.replace(
				"this._loggerCreationPromise.then(() => this.flush());",
				"this._loggerCreationPromise.then(() => this._logger?.flush());",
			)
			return { contents: source, loader: "ts" }
		})

		// Normalize Windows file URIs and ignore path casing in extension path index
		build.onLoad({ filter: /[\\/]deps[\\/]vscode[\\/]vs[\\/]workbench[\\/]api[\\/]common[\\/]extHostExtensionService\.ts$/ }, (args: any) => {
			let source = fs.readFileSync(args.path, "utf8")
			source = source
				.replace(
					"return extUriBiasedIgnorePathCase.ignorePathCasing(key);",
					"return process.platform === 'win32' || extUriBiasedIgnorePathCase.ignorePathCasing(key);",
				)
				.replace(
					/if \(uri\.scheme === Schemas\.file && this\._hostUtils\.fsRealpath\) \{\r?\n\s*const realpathValue = await this\._hostUtils\.fsRealpath\(uri\.fsPath\);\r?\n\s*return URI\.file\(realpathValue\);\r?\n\s*\}/,
					"if (uri.scheme === Schemas.file) {\n\t\t\tif (this._hostUtils.fsRealpath) {\n\t\t\t\tconst realpathValue = await this._hostUtils.fsRealpath(uri.fsPath);\n\t\t\t\treturn URI.file(realpathValue);\n\t\t\t}\n\t\t\treturn URI.file(uri.fsPath);\n\t\t}",
				)
			return { contents: source, loader: "ts" }
		})

		// Fall back to the registered extension instead of nullExtensionDescription and ensure fsPath is cached for serialization
		build.onLoad({ filter: /[\\/]deps[\\/]vscode[\\/]vs[\\/]workbench[\\/]api[\\/]common[\\/]extHostRequireInterceptor\.ts$/ }, (args: any) => {
			let source = fs.readFileSync(args.path, "utf8")
			source = source.replace(
				"const ext = this._extensionPaths.findSubstr(parent);",
				"const ext = this._extensionPaths.findSubstr(parent) || this._extensionRegistry.mine.getAllExtensionDescriptions()[0];\n\t\tif (ext?.extensionLocation) { void ext.extensionLocation.fsPath; }",
			)
			return { contents: source, loader: "ts" }
		})

		build.onLoad({ filter: /[\\/]deps[\\/]vscode[\\/]vs[\\/]base[\\/]common[\\/](?:performance|marked[\\/]marked|semver[\\/]semver)\.js$/ }, (args: any) => {
			const source = fs.readFileSync(args.path, "utf8")
			const fakeMod = { exports: {} as any }
			new Function("module", "exports", "define", source)(fakeMod, fakeMod.exports, undefined)
			const exported = fakeMod.exports
			const namedExports =
				exported && (typeof exported === "object" || typeof exported === "function")
					? Object.keys(exported)
							.filter((k) => /^[A-Za-z_$][\w$]*$/.test(k) && k !== "default" && k !== "__esModule")
							.map((k) => `export const ${k} = __vscodeCjsModule.exports[${JSON.stringify(k)}];`)
							.join("\n")
					: ""
			const wrapped = [
				`const __vscodeCjsModule = { exports: {} };`,
				`(function(module, exports, define) {`,
				source,
				`})(__vscodeCjsModule, __vscodeCjsModule.exports, undefined);`,
				`export default __vscodeCjsModule.exports;`,
				namedExports,
			].join("\n")
			return { contents: wrapped, loader: "js" }
		})
	},
}

export default defineConfig({
	entry: ["src/extension.ts"],
	format: ["esm"],
	minify: false,
	clean: true,
	splitting: false,
	platform: "node",
	target: "node18",
	skipNodeModulesBundle: false,
	external: ["chokidar", "fsevents"],
	dts: false,
	esbuildPlugins: [vscodeEsmCompatPlugin],
	esbuildOptions(options) {
		options.alias = {
			"vs": path.resolve(__dirname, "./deps/vscode/vs"),
			"graceful-fs": resolvePnpmGracefulFs(),
		}
		options.resolveExtensions = [".ts", ".js", ".json"]
		options.supported = {
			...options.supported,
			"class-static-blocks": false,
			"class-static-field": false,
		}
		options.banner = {
			js: `import { createRequire as __bundleCreateRequire } from "node:module";\nconst require = globalThis.require || __bundleCreateRequire(import.meta.url);\nif (typeof require.__$__nodeRequire !== "function") { require.__$__nodeRequire = require; }`,
		}
	}
})
