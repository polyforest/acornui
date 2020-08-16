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

import com.acornui.component.UiComponent
import com.acornui.component.UiComponentImpl
import com.acornui.own
import kotlinx.coroutines.launch

//---------------------------------------------------------------------------
// Utilities for common i18n bindings.
//---------------------------------------------------------------------------

/**
 * Sets the [UiComponent.label] on this component to the i18n value.
 *
 * @param key The resource key to query on the default i18n bundle.
 * @param bundleName The name of the resource bundle (Default [i18nBundleName])
 * @return Returns the coroutine Job. This will be cancelled automatically if the host component is disposed.
 */
fun UiComponentImpl<*>.labelI18n(key: String, bundleName: String = i18nBundleName) = own(launch {
	label = string(key, bundleName)
})

/**
 * Sets the [UiComponent.title] on this component to the i18n value.
 *
 * @param key The resource key to query on the default i18n bundle.
 * @param bundleName The name of the resource bundle (Default [i18nBundleName])
 * @return Returns the coroutine Job. This will be cancelled automatically if the host component is disposed.
 */
fun UiComponent.titleI18n(key: String, bundleName: String = i18nBundleName) = own(launch {
	title = string(key, bundleName)
})