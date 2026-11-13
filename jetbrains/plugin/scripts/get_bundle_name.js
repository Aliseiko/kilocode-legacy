#!/usr/bin/env node

import { readFileSync } from "fs"
import { join, dirname } from "path"
import { fileURLToPath, pathToFileURL } from "url"

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

/**
 * Get the bundle zip file name based on version from gradle.properties
 */
function getBundleName() {
	try {
		// Read version from gradle.properties
		const gradlePropertiesPath = join(__dirname, "../gradle.properties")
		const gradlePropertiesContent = readFileSync(gradlePropertiesPath, "utf8")

		const gradleVersionMatch = gradlePropertiesContent.match(/^pluginVersion=(.+)$/m)
		if (!gradleVersionMatch) {
			throw new Error("pluginVersion not found in gradle.properties")
		}

		const version = gradleVersionMatch[1].trim()

		// Generate the bundle name following the pattern: Kilo Code v5-{version}.zip
		const bundleName = `Kilo Code v5-${version}.zip`

		// Output just the filename for CI usage
		process.stdout.write(bundleName)

		return bundleName
	} catch (error) {
		console.error("❌ Error getting bundle name:", error.message)
		process.exit(1)
	}
}

// Run the function if this script is executed directly
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
	getBundleName()
}

export default getBundleName
