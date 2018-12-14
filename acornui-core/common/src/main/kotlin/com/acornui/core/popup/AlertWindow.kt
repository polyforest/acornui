/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.core.popup

import com.acornui.component.WindowPanel
import com.acornui.component.style.StyleTag
import com.acornui.component.text.text
import com.acornui.core.di.Owned
import com.acornui.core.di.inject

class AlertWindow(owner: Owned) : WindowPanel(owner) {

	init {
		styleTags.add(AlertWindow)
	}

	companion object : StyleTag
}

fun Owned.alert(title: String, message: String, priority: Float = 1f): PopUpInfo<AlertWindow> {
	val alertWindow = AlertWindow(this)
	alertWindow.label = title
	alertWindow.apply {
		+text(message)
	}
	val info = PopUpInfo(alertWindow, isModal = true, priority = priority, dispose = true)
	inject(PopUpManager).addPopUp(info)
	return info
}