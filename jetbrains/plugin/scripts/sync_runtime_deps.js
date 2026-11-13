import fs from "node:fs"
import path from "node:path"
import { builtinModules, createRequire } from "node:module"
import { fileURLToPath } from "node:url"

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const pluginDir = path.resolve(__dirname, "..")
const repoRoot = path.resolve(pluginDir, "../..")
const hostDir = path.resolve(pluginDir, "../host")
const resourcesDir = path.resolve(pluginDir, "../resources")
const resourcesNodeModules = path.join(resourcesDir, "node_modules")

const builtins = new Set(builtinModules.flatMap((m) => [m, `node:${m}`]))

function getPackageName(specifier) {
	if (specifier.startsWith("@")) {
		const [scope, name] = specifier.split("/")
		return scope && name ? `${scope}/${name}` : specifier
	}
	return specifier.split("/")[0]
}

function getExternalImportsFromHostBundle() {
	const bundlePath = path.join(hostDir, "dist", "extension.js")
	const roots = new Set(["minimist", "@vscode/ripgrep", "vscode-regexpp", "@vscode/proxy-agent"])
	if (!fs.existsSync(bundlePath)) {
		return [...roots]
	}
	const code = fs.readFileSync(bundlePath, "utf8")
	for (const match of code.matchAll(/^import\s+.*?\s+from\s+["']([^"']+)["'];?$/gm)) {
		const spec = match[1]
		if (!spec.startsWith(".") && !spec.startsWith("/") && !builtins.has(spec)) {
			roots.add(getPackageName(spec))
		}
	}
	return [...roots]
}

function resolvePackageJsonPath(pkgName, searchFromDir) {
	const directPkgJson = path.join(searchFromDir, "node_modules", ...pkgName.split("/"), "package.json")
	if (fs.existsSync(directPkgJson)) {
		return directPkgJson
	}

	const req = createRequire(path.join(searchFromDir, "__resolve__.js"))
	try {
		return req.resolve(`${pkgName}/package.json`)
	} catch {
		try {
			let dir = path.dirname(req.resolve(pkgName))
			while (dir !== path.dirname(dir)) {
				const candidate = path.join(dir, "package.json")
				if (fs.existsSync(candidate)) {
					const parsed = JSON.parse(fs.readFileSync(candidate, "utf8"))
					if (parsed.name === pkgName) {
						return candidate
					}
				}
				dir = path.dirname(dir)
			}
		} catch {
			// Fallback below checks pnpm store when package is externalized without a direct dependency entry
		}
	}

	const pnpmStoreDir = path.join(repoRoot, "node_modules", ".pnpm")
	if (fs.existsSync(pnpmStoreDir)) {
		const escapedName = pkgName.replace("/", "+")
		const prefix = `${escapedName}@`
		for (const entry of fs.readdirSync(pnpmStoreDir, { withFileTypes: true })) {
			if (entry.isDirectory() && entry.name.startsWith(prefix)) {
				const candidate = path.join(pnpmStoreDir, entry.name, "node_modules", ...pkgName.split("/"), "package.json")
				if (fs.existsSync(candidate)) {
					return candidate
				}
			}
		}
	}

	return null
}

function copyPackageTree(pkgName, searchFromDir, destNodeModulesDir, topVersions) {
	const pkgJsonPath = resolvePackageJsonPath(pkgName, searchFromDir)
	if (!pkgJsonPath) {
		console.warn(`[sync_runtime_deps] Warning: could not resolve ${pkgName} from ${searchFromDir}`)
		return
	}

	const realPkgDir = fs.realpathSync(path.dirname(pkgJsonPath))
	const pkgJson = JSON.parse(fs.readFileSync(pkgJsonPath, "utf8"))
	const pkgVersion = pkgJson.version || "0.0.0"

	let targetDir
	if (destNodeModulesDir === resourcesNodeModules) {
		const existingVersion = topVersions.get(pkgName)
		if (existingVersion) {
			return
		}
		topVersions.set(pkgName, pkgVersion)
		targetDir = path.join(resourcesNodeModules, pkgName)
	} else {
		targetDir = path.join(destNodeModulesDir, pkgName)
	}

	fs.mkdirSync(path.dirname(targetDir), { recursive: true })
	fs.cpSync(realPkgDir, targetDir, {
		recursive: true,
		dereference: true,
		filter: (src) => path.basename(src) !== "node_modules" || src === realPkgDir,
	})

	const deps = [...new Set([...Object.keys(pkgJson.dependencies || {}), ...Object.keys(pkgJson.optionalDependencies || {})])]
	for (const dep of deps) {
		const depPkgJsonPath = resolvePackageJsonPath(dep, realPkgDir)
		if (!depPkgJsonPath) {
			continue
		}
		const depPkgJson = JSON.parse(fs.readFileSync(depPkgJsonPath, "utf8"))
		const depVersion = depPkgJson.version || "0.0.0"
		const topVersion = topVersions.get(dep)

		if (!topVersion || topVersion === depVersion) {
			copyPackageTree(dep, realPkgDir, resourcesNodeModules, topVersions)
		} else {
			copyPackageTree(dep, realPkgDir, path.join(targetDir, "node_modules"), topVersions)
		}
	}
}

function copyRuntimeDeps() {
	fs.mkdirSync(resourcesDir, { recursive: true })
	fs.copyFileSync(path.join(hostDir, "package.json"), path.join(resourcesDir, "package.json"))
	fs.rmSync(resourcesNodeModules, { recursive: true, force: true })
	fs.mkdirSync(resourcesNodeModules, { recursive: true })

	const topVersions = new Map()
	const roots = getExternalImportsFromHostBundle()
	for (const pkg of roots) {
		copyPackageTree(pkg, hostDir, resourcesNodeModules, topVersions)
	}
	console.log(`[sync_runtime_deps] Copied ${topVersions.size} runtime packages to ${resourcesNodeModules}`)
}

function writeProdDepList() {
	const entries = []
	if (fs.existsSync(resourcesNodeModules)) {
		for (const item of fs.readdirSync(resourcesNodeModules, { withFileTypes: true })) {
			if (!item.isDirectory() || item.name.startsWith(".")) continue
			const itemPath = path.join(resourcesNodeModules, item.name)
			if (item.name.startsWith("@")) {
				for (const scoped of fs.readdirSync(itemPath, { withFileTypes: true })) {
					if (scoped.isDirectory()) {
						entries.push(path.join(itemPath, scoped.name).replace(/\\/g, "/"))
					}
				}
			} else {
				entries.push(itemPath.replace(/\\/g, "/"))
			}
		}
	}
	fs.writeFileSync(path.join(pluginDir, "prodDep.txt"), entries.join("\n") + (entries.length ? "\n" : ""), "utf8")
	console.log(`[sync_runtime_deps] Wrote ${entries.length} entries to prodDep.txt`)
}

const mode = process.argv[2]
if (mode === "--copy") {
	copyRuntimeDeps()
} else if (mode === "--list") {
	writeProdDepList()
} else {
	console.error("Usage: node scripts/sync_runtime_deps.js <--copy|--list>")
	process.exit(1)
}
