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

	fun editKotlinJs(kotlinJs: File, dest: File) {
		var src = kotlinJs.readText()
		dest.parentFile.mkdirs()
//		src = addCachedBind(src)
//		src = patchKotlinCode(src)
		dest.writeText(src)
	}

	private fun addCachedBind(src: String): String {
		val cachedBind = """
/**
 * Creates a method that wraps a function which when called, will have its `this` keyword match the given receiver.
 * This is similar to bind, except that it doesn't take a list of arguments to prepend, and the wrapped method is cached.
 * The caching is done so that calling cachedBind on the same method with the same receiver will return the same wrapped method.
 * This is useful for when the binding needs to be referenced again, such as removing an event handler.
 */
Function.prototype.cachedBind = function(receiver) {
	if (receiver.__bindingCache == null) receiver.__bindingCache = {};
	var existing = receiver.__bindingCache[this];
	if (existing != null) {
		return existing;
	} else {
		var newBinding = this.bind(receiver);
		receiver.__bindingCache[this] = newBinding;
		return newBinding;
	}
};
"""
		return cachedBind + src
	}


	/**
	 * Patches kotlin code to fix some inconsistencies between the JVM and JS versions.
	 */
	fun patchKotlinCode(src: String, file: File? = null): String {
		var result = src
		result = fixMemberReferences(result)
		return result
	}

	private val memberRefs = Regex("""Kotlin\.getCallableRef\('[\w]+',\s*function\s*\([^\)]+\)\s*\{\s*return\s*[\${'$'}\w\d]+\.([\w\${'$'}]+)\([^\)]*\);\s*}\.bind\(null,\s*([\w\${'$'}]+)\)\)""")
	private fun fixMemberReferences(src: String): String {
		return memberRefs.replace(src) {
			val oldValue = it.groups[0]!!.value
			val receiver = it.groups[2]!!.value
			val method = it.groups[1]!!.value
			val toStr = "$receiver.$method.cachedBind($receiver)"
			val oldNumLines = oldValue.count { it == '\n' }
			val oldSize = oldValue.length

			toStr + " ".repeat(oldSize - toStr.length - oldNumLines) + "\n".repeat(oldNumLines)
		}
	}

	/**
	 * Makes it all go weeeeee!
	 */
	fun optimizeProductionCode(src: String, file: File? = null): String {
		var result = src
		result = simplifyArrayListGet(result)
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
		return Regex("""ArrayList\.prototype\.get_za3lpa\$ = function\(index\) \{([^}]+)};""").replace(src, {
			"""ArrayList.prototype.get_za3lpa$ = function(index) { return this.array_9xgyxj${'$'}_0[index] };"""
		})
	}
}