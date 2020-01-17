/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.build.plugins.tasks.fileprocessor

class TokenReplacementFileProcessor(val tokenStart: String = "@", val tokenEnd: String = "@") : TextFileContentsProcessor {

	private val regex = Regex("""@([a-zA-Z]+)@""")

	override fun process(path: String, input: String, properties: Map<String, String>): String {
		return input.replace(regex) {
			val key = it.groups[1]!!.value
			properties[key] ?: it.groups[0]!!.value
		}
	}
}