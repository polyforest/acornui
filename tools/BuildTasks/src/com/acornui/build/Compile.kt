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

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler


object Compile {
	fun compileKotlinJs(compilerArgs: KotlinJsArguments): ExitCode {
		val args = ArrayList<String>()
		compilerArgs.populate(args)
		return K2JSCompiler().execFullPathsInMessages(System.out, args.toTypedArray())
	}

	fun compileKotlinJvm(compilerArgs: KotlinJvmArguments): ExitCode {
		val args = ArrayList<String>()
		compilerArgs.populate(args)
		return K2JVMCompiler().execFullPathsInMessages(System.out, args.toTypedArray())
	}
}
