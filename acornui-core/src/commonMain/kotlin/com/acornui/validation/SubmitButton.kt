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
import com.acornui.component.style.styleTag
import com.acornui.di.Context
import com.acornui.focus.enterTarget
import com.acornui.i18n.string
import com.acornui.input.interaction.click
import com.acornui.observe.bind
import com.acornui.observe.dataBinding
import com.acornui.system.userInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * A style tag for submit buttons.
 */
val submitButtonStyleTag = styleTag()

/**
 * Creates a button with the following attributes:
 * - The label is set to the given i18n string.
 * - When ENTER is pressed within the form, this button will be virtually clicked.
 * - When this button is clicked, the form is validated and if there are are no validation errors, submitted.
 * - Must be inside a validation container.
 *
 * @param i18nBundleName The i18n bundle for localized strings.
 * @param i18nBundleKey The string key within the i18n bundle.
 * @param submit The action to invoke when the form is valid and this button has been clicked.
 * @param init An initialization block for further button configuration.
 */
fun <T> ValidationForm<T, *, *>.submitButton(
		i18nBundleName: String = "ui",
		i18nBundleKey: String = "form.submit",
		submit: suspend (T) -> Unit,
		init: ComponentInit<ButtonImpl> = {}
): ButtonImpl {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return button {
		styleTags.add(submitButtonStyleTag)
		bind(userInfo.currentLocale) {
			launch {
				label = string(i18nBundleKey, i18nBundleName, it)
			}
		}
		enterTarget(this)
		click().add {
			launch {
				disabled = true
				try {
					val data = validateForm()
					if (data.success)
						submit.invoke(data.data)
				} catch (e: CancellationException) {}
				disabled = false
			}
		}
		init()
	}
}