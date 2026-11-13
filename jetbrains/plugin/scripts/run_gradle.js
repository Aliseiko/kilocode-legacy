import { spawnSync } from "node:child_process"
import path from "node:path"
import { fileURLToPath } from "node:url"

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const pluginDir = path.resolve(__dirname, "..")
const gradleWrapper = process.platform === "win32" ? path.join(pluginDir, "gradlew.bat") : "./gradlew"
const args = process.argv.slice(2)

const result = spawnSync(gradleWrapper, args, {
	cwd: pluginDir,
	stdio: "inherit",
	shell: process.platform === "win32",
})

process.exit(result.status ?? 1)
