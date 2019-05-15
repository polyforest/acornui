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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer

/**
 * BasicMessageCollector just outputs errors to System.err and everything else to System.out
 */
class BasicMessageCollector(val verbose: Boolean = true) : MessageCollector {

	private var _hasErrors = false

	override fun clear() {
	}

	override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
		if (severity.isError) {
			_hasErrors = true
			System.err.println(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
		} else {
			if (verbose || !CompilerMessageSeverity.VERBOSE.contains(severity)) {
				System.out.println(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
			}
		}
	}

	override fun hasErrors(): Boolean = _hasErrors
}
