import { createRequire } from "node:module"
import { FileRPCProtocolLogger } from "../deps/vscode/vs/workbench/services/extensions/common/fileRPCProtocolLogger.js"

const require = createRequire(import.meta.url)
const g = globalThis as Record<string, unknown>

if (!g._VSCODE_NODE_MODULES) {
	const cache: Record<string, unknown> = Object.create(null)
	g._VSCODE_NODE_MODULES = new Proxy(cache, {
		get(target, prop: string | symbol) {
			if (typeof prop !== "string") {
				return undefined
			}
			if (prop in target) {
				return target[prop]
			}
			try {
				const mod = require(prop)
				target[prop] = mod
				return mod
			} catch (err) {
				if (prop !== "native-watchdog") {
					throw err
				}
				const stub = { start() {}, stop() {}, exit() {} }
				target[prop] = stub
				return stub
			}
		},
	})
}

if (!g.FileRPCProtocolLogger) {
	g.FileRPCProtocolLogger = FileRPCProtocolLogger
}
if (!g.fileLoggerGlobal) {
	g.fileLoggerGlobal = new FileRPCProtocolLogger("extension")
}
if (!g.RequestInitiator) {
	g.RequestInitiator = { LocalSide: 0, OtherSide: 1 }
}
