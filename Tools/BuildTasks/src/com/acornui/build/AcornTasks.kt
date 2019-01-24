/*
 * Copyright 2019 PolyForest
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

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

open class AcornTasks(val config: AcornUiBuildConfig) {

	@Task(description = "Says hello")
	open fun hello(@TaskArgument(description = "The language to use", alias = "lang") language: String = "en_US") {
		val greeting = when (language) {
			"en_US" -> "Hello Acorn"
			"de_DE" -> "Hallo Acorn"
			"fr_FR" -> "Bonjour Acorn"
			else -> throw Exception("Unknown language")
		}
		println(greeting)
	}

	@Task(description = "Says hello")
	open fun hello(@TaskArgument(description = "The language to use", alias = "lang") language: String, title: Int) {
		val greeting = when (language) {
			"en_US" -> "Hello Acorn"
			"de_DE" -> "Hallo Acorn"
			"fr_FR" -> "Bonjour Acorn"
			else -> throw Exception("Unknown language")
		}
		println(greeting)
	}


}

@ConfigObject("Global settings for the Acorn UI tasks.")
object AcornUiBuildConfig {

	@ConfigProp("If true, out of date checks will not be used.")
	var force = false

	@ConfigProp("If true, work will not be done, only log statements.")
	var dry = false

}