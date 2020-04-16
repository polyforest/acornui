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

package com.acornui.i18n

import com.acornui.Disposable
import com.acornui.component.Labelable
import com.acornui.component.UiComponentRo
import com.acornui.component.tooltip

//---------------------------------------------------------------------------
// Utilities for common i18n bindings.
//---------------------------------------------------------------------------

/**
 * Binds a text key for the default i18n bundle to this object's label.
 *
 * @param key The resource key to query on the default i18n bundle.
 * @param default If the key cannot be found, this value will be set.
 * @return Returns a [Disposable] handle to dispose of the binding. This will be disposed automatically when this
 * component is disposed.
 */
fun <T : Labelable> T.labelI18n(key: String = "", default: String = ""): Disposable = i18n {
	label = string(key, default)
}

/**
 * Binds a text key for the default i18n bundle to this object's tool tip.
 * @param key The resource key to query on the default i18n bundle.
 * @param default If the key cannot be found, this value will be set.
 * @return Returns a [Disposable] handle to dispose of the binding. This will be disposed automatically when this
 * component is disposed.
 * @see UiComponentRo.tooltip
 */
fun UiComponentRo.tooltipI18n(key: String, default: String = ""): Disposable = i18n {
	tooltip(string(key, default))
}