/*
 * Copyright 2019 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.build

import org.gradle.api.plugins.ExtraPropertiesExtension

/**
 *
 */
object AcornDependencies {
	val versionDefaults = mapOf(
			"kotlinVersion" to "1.3.61",
			"gdxVersion" to "1.9.8",
			"lwjglVersion" to "3.2.2",
			"jorbisVersion" to "0.0.17",
			"jlayerVersion" to "1.0.2",

			"dokkaVersion" to "0.9.18",
			"kotlinLanguageVersion" to "1.3",
			"kotlinJvmTarget" to "1.8",
			"kotlinSerializationVersion" to "0.14.0",
			"kotlinCoroutinesVersion" to "1.3.0-RC2"
	)

	fun addVersionProperties(extra: ExtraPropertiesExtension) {
		for (version in versionDefaults) {
			if (!extra.has(version.key))
				extra[version.key] = version.value
		}
	}
}