/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.build.util

import java.io.File

object KotlinMonkeyPatcher {

	/**
	 * Makes it all go weeeeee!
	 */
	fun optimizeProductionCode(src: String, file: File? = null): String {
		var result = src
		result = simplifyArrayListGet(result)
		result = simplifyArrayListSet(result)
		result = stripCce(result)
		result = stripRangeCheck(result)
		return result
	}

	/**
	 * Strips type checking that only results in a class cast exception.
	 */
	private fun stripCce(src: String): String {
		return Regex("""Kotlin\.isType\(([^,(]+),\s*[^)]+\)\s*\?\s*([^:]+)\s*:\s*Kotlin\.throwCCE\(\)""").replace(src, {
			val one = it.groups[1]!!.value.trim()
			val two = it.groups[2]!!.value.trim()
			if (one == two) {
				"true?$one:null"
			} else {
				"true?($one,$two):null"
			}
		})
	}

	private fun stripRangeCheck(src: String): String {
		return src.replace("this.rangeCheck_2lys7f${'$'}_0(index)", "index")
	}

	private fun simplifyArrayListGet(src: String): String {
		return Regex("""ArrayList\.prototype\.get_za3lpa\$[\s]*=[\s]*function[\s]*\(index\)[\s]*\{([^}]+)};""").replace(src, {
			"""ArrayList.prototype.get_za3lpa$ = function(index) { return this.array_hd7ov6${'$'}_0[index] };"""
		})
	}

	private fun simplifyArrayListSet(src: String): String {
		return Regex("""ArrayList\.prototype\.set_wxm5ur\$[\s]*=[\s]*function[\s]*\(index, element\)[\s]*\{([^}]+)};""").replace(src, {
			"""ArrayList.prototype.set_wxm5ur${'$'} = function (index, element) {
			  var previous = this.array_hd7ov6${'$'}_0[index];
			  this.array_hd7ov6${'$'}_0[index] = element;
			  return previous;
			};"""
		})
	}
}