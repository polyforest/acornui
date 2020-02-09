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

package com.acornui.popup

import com.acornui.component.WindowPanel
import com.acornui.component.layout.algorithm.CanvasLayoutData
import com.acornui.component.scroll.scrollArea
import com.acornui.component.style.StyleTag
import com.acornui.component.text.text
import com.acornui.di.Context

class AlertWindow(owner: Context) : WindowPanel(owner) {

	init {
		styleTags.add(AlertWindow)
	}

	companion object : StyleTag
}

fun Context.alert(title: String, message: String, priority: Float = 1f, width: Float? = 500f, height: Float? = null): PopUpInfo<AlertWindow> = alert(title, message, priority, CanvasLayoutData().apply { this.width = width; this.height = height; center() })
fun Context.alert(title: String, message: String, priority: Float = 1f, layoutData: CanvasLayoutData = CanvasLayoutData().apply { width = 500f; height = null; center() }): PopUpInfo<AlertWindow> {
	val alertWindow = AlertWindow(this)
	alertWindow.label = title
	alertWindow.apply {
		+scrollArea {
			+text { text = message } layout { widthPercent = 1f }
		} layout { fill() }
	}
	val info = PopUpInfo(alertWindow, isModal = true, priority = priority, layoutData = layoutData, dispose = true)
	inject(PopUpManager).addPopUp(info)
	return info
}
