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

package com.acornui.text

import com.acornui.collection.stringMapOf
import com.acornui.removeBackslashes
import com.acornui.string.StringReader

object PropertiesParser {

	fun parse(target: String): Map<String, String> {
		val map = stringMapOf<String>()
		val parser = StringReader(target)
		while (parser.hasNext) {
			val line = parser.readLine().trimStart()
			if (line.startsWith('#') || line.startsWith('!')) {
				continue // Comment
			}
			val separatorIndex = line.indexOf('=')
			if (separatorIndex == -1) continue
			val key = line.substring(0, separatorIndex).trim()
			var value = line.substring(separatorIndex + 1)

			while (value.endsWith('\\')) {
				value = value.substring(0, value.length - 1) + '\n' + parser.readLine()
			}
			map[key] = removeBackslashes(value)
		}
		return map
	}
}
