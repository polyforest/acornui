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

import com.acornui.build.util.PATH_SEPARATOR


data class KotlinJsArguments(

		/**
		 * Output file path
		 */
		val output: String? = null,

		/**
		 * Don't use bundled Kotlin stdlib
		 */
		val noStdlib: Boolean = false,

		/**
		 * Path to zipped library sources or kotlin files separated by commas
		 */
		val libraries: MutableList<String>? = null,

		/**
		 * Generate source map
		 */
		val sourceMap: Boolean = false,

		/**
		 * Generate metadata
		 */
		val metaInfo: Boolean = false,

		/**
		 * Generate JS files for specific ECMA version (only ECMA 5 is supported)
		 */
		val target: String? = null,

		/**
		 * Whether a main function should be called
		 */
		val main: Boolean = true,

		/**
		 * Path to file which will be added to the beginning of output file
		 */
		val outputPrefix: String? = null,

		/**
		 * Path to file which will be added to the end of output file
		 */
		val outputPostfix: String? = null,

		val common: KotlinCommonArguments = KotlinCommonArguments()


) {

	fun populate(args: MutableList<String>) {
		if (output != null) args.add("-output", output)
		if (noStdlib) args.add("-no-stdlib")
		if (libraries != null && libraries.isNotEmpty()) args.add("-libraries", libraries.joinToString(PATH_SEPARATOR))
		if (sourceMap) args.add("-source-map")
		if (metaInfo) args.add("-meta-info")
		if (target != null) args.add("-target", target)
		if (!main) args.add("-main", "noCall")
		if (outputPrefix != null) args.add("-output-prefix", outputPrefix)
		if (outputPostfix != null) args.add("-output-postfix", outputPostfix)
		common.populate(args)
	}

	private fun <T> MutableList<T>.add(first: T, second: T) {
		add(first)
		add(second)
	}
}

