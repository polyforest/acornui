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

package com.acornui.validation

import com.acornui.i18n.string
import com.acornui.replaceTokens

/**
 * RFC2822 email validation regex.
 */
val validEmailRegex = Regex("""[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?""")

fun FormInput<String>.requiredEmail() {
	required()
	emailOrBlank()
}

fun FormInput<String>.emailOrBlank() {
	addValidator {
		string("validation.email", "ui").replaceTokens(name, it) to
				(it.isBlank() || validEmailRegex.matches(it)).toValidationLevel()
	}
}

fun FormInput<String>.required() {
	addValidator {
		string("validation.required", "ui").replaceTokens(name) to it.isNotBlank().toValidationLevel()
	}
}

private fun Boolean.toValidationLevel(): ValidationLevel = if (this) ValidationLevel.SUCCESS else ValidationLevel.ERROR
