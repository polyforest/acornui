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

import com.acornui.component.ButtonImpl
import com.acornui.component.ComponentInit
import com.acornui.component.button
import com.acornui.component.text.TextInput
import com.acornui.di.Context
import com.acornui.focus.enterTarget
import com.acornui.i18n.string
import com.acornui.input.interaction.click
import com.acornui.observe.bind
import com.acornui.observe.dataBinding
import com.acornui.observe.or
import com.acornui.replaceTokens
import com.acornui.system.userInfo
import kotlinx.coroutines.launch
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ValidationBuilder(context: Context) : Context by context {

	var results: MutableList<ValidationInfo> = ArrayList()

	operator fun ValidationInfo.unaryPlus() = results.add(this)

}

val validEmailRegex = Regex("""[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?""")

suspend fun <T : TextInput> ValidationBuilder.requiredEmail(input: T, name: String): T {
	return emailOrBlank(required(input, name), name)
}

suspend fun <T : TextInput> ValidationBuilder.emailOrBlank(input: T, name: String): T {
	val text = input.text
	+ValidationInfo(
			message = string("validation.email", "ui").replaceTokens(name),
			level = (text.isBlank() || validEmailRegex.matches(text)).toValidationLevel(),
			componentId = input.componentId
	)
	return input
}

suspend fun <T : TextInput> ValidationBuilder.required(input: T, name: String): T {
	+ValidationInfo(
			message = string("validation.required", "ui").replaceTokens(name),
			level = input.text.isNotBlank().toValidationLevel(),
			componentId = input.componentId
	)
	return input
}

private fun Boolean.toValidationLevel(): ValidationLevel = if (this) ValidationLevel.SUCCESS else ValidationLevel.ERROR

/**
 * Creates a button with the following attributes:
 * - Must be inside
 */
fun <T> ValidationContainer<T>.submitButton(
		i18nBundleName: String = "ui",
		i18nBundleKey: String = "form.submit",
		submit: suspend (T) -> Unit,
		init: ComponentInit<ButtonImpl> = {}
): ButtonImpl {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return button {
		bind(userInfo.currentLocale) {
			launch {
				label = string(i18nBundleKey, i18nBundleName, it)
			}
		}
		enterTarget(this)
		val submitting = dataBinding(false)
		bind(isBusy or submitting) {
			disabled = isBusy.value || submitting.value
		}
		click().add {
			launch {
				submitting.value = true
				val data = validateData()
				if (data?.success == true) {
					submit.invoke(data.data)
					submitting.value = false
				}
			}
		}
		init()
	}
}