/*
 * Copyright 2015 Nicholas Bilyk
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



/**
 * Arguments used by both js and jvm.
 */
data class KotlinCommonArguments(

		/**
		 * Source files
		 */
		val src: MutableList<String>? = null,

		/**
		 * Generate no warnings
		 */
		val noWarn: Boolean = false,

		/**
		 * Enable verbose logging output
		 */
		val verbose: Boolean = false,

		/**
		 * Display compiler version
		 */
		val version: Boolean = false,

		/**
		 * Print a synopsis of standard options
		 */
		val help: Boolean = false,

		/**
		 * Print a synopsis of advanced options
		 */
		val helpAdvanced: Boolean = false,

		/**
		 * Disable method inlining
		 */
		var noInline: Boolean = false,

		var apiVersion: String? = null

) {

	fun populate(args: MutableList<String>) {
		if (noWarn) args.add("-nowarn")
		if (verbose) args.add("-verbose")
		if (version) args.add("-version")
		if (help) args.add("-help")
		if (helpAdvanced) args.add("-X")
		if (src != null) args.addAll(src)
		if (apiVersion != null) args.add(apiVersion!!)
	}
}