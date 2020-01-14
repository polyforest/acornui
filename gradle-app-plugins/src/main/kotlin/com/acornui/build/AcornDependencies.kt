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
import java.util.*

/**
 * Applies the dependency version properties set in acornDependencies.txt
 */
object AcornDependencies {

	private val props: Properties

	init {
		val iS = AcornDependencies::class.java.classLoader.getResourceAsStream("acornDependencies.txt")
		props = Properties()
		props.load(iS)
	}

	fun putVersionProperties(extra: ExtraPropertiesExtension) {
		for (entry in props.entries) {
			val key = entry.key.toString()
			val value = entry.value.toString()
			if (!extra.has(key))
				extra[key] = value
		}
	}
}