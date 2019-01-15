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

import com.acornui.build.util.PATH_SEPARATOR


data class KotlinJvmArguments(

		/**
		 * Destination for generated class files.  directory|jar
		 */
		val destination: String? = null,

		/**
		 * Paths where to find user class files
		 */
		val classpath: MutableList<String>? = null,

		/**
		 * Paths to external annotations
		 */
		val annotations: String? = null,

		/**
		 * Include Kotlin runtime in to resulting .jar
		 */
		val includeRuntime: Boolean = false,

		/**
		 * Don't include Java runtime into classpath
		 */
		val noJdk: Boolean = false,

		/**
		 * Don't include Kotlin runtime into classpath
		 */
		val noStdlib: Boolean = false,

		/**
		 * Don't include JDK external annotations into classpath
		 */
		val noJdkAnnotations: Boolean = false,

		/**
		 * Path to the module file to compile
		 */
		val module: String? = null,

		/**
		 * Evaluate the script file
		 */
		val script: Boolean = false,

		/**
		 * Path to Kotlin compiler home directory, used for annotations and runtime libraries discovery
		 */
		val kotlinHome: String? = null,

		/**
		 * Don't generate not-null assertion after each invocation of method returning not-null
		 */
		val noCallAssertions: Boolean = false,

		/**
		 * Don't generate not-null assertions on parameters of methods accessible from Java
		 */
		val noParamAssertions: Boolean = false,

		/**
		 * Disable optimizations
		 */
		val noOptimize: Boolean = false,

		val common: KotlinCommonArguments = KotlinCommonArguments()


) {

	fun populate(args: MutableList<String>) {
		if (destination != null) args.add("-d", destination)
		if (classpath != null && classpath.isNotEmpty()) args.add("-classpath", classpath.joinToString(PATH_SEPARATOR))
		if (annotations != null) args.add("-annotations", annotations)
		if (includeRuntime) args.add("-include-runtime")
		if (noJdk) args.add("-no-jdk")
		if (noStdlib) args.add("-no-stdlib")
		if (noJdkAnnotations) args.add("-no-jdk-annotations")
		if (module != null) args.add("-module", module)
		if (script) args.add("-script")
		if (kotlinHome != null) args.add("-kotlin-home", kotlinHome)
		if (noCallAssertions) args.add("-Xno-call-assertions")
		if (noParamAssertions) args.add("-Xno-param-assertions")
		if (noOptimize) args.add("-Xno-optimize")
		common.populate(args)
	}

	private fun <T> MutableList<T>.add(first: T, second: T) {
		add(first)
		add(second)
	}
}