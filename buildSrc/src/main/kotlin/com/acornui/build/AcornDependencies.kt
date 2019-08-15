package com.acornui.build

import org.gradle.api.plugins.ExtraPropertiesExtension

object AcornDependencies {
	val versionDefaults = mapOf(
			"kotlinVersion" to "1.3.31",
			"gdxVersion" to "1.9.8",
			"lwjglVersion" to "3.2.2",
			"jorbisVersion" to "0.0.17",
			"jlayerVersion" to "1.0.2",

			"kotlinLanguageVersion" to "1.3",
			"kotlinJvmTarget" to "1.8",
			"kotlinSerializationVersion" to "0.11.1",
			"kotlinCoroutinesVersion" to "1.3.0-RC2"
	)

	fun addVersionProperties(extra: ExtraPropertiesExtension) {
		for (version in versionDefaults) {
			if (!extra.has(version.key))
				extra[version.key] = version.value
		}
	}
}