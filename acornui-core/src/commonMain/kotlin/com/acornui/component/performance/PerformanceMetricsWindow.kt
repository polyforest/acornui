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

package com.acornui.component.performance

import com.acornui.component.*
import com.acornui.component.layout.algorithm.*
import com.acornui.component.style.addStyleRule
import com.acornui.component.text.FontSize
import com.acornui.component.text.TextField
import com.acornui.component.text.charStyle
import com.acornui.component.text.text
import com.acornui.di.Owned
import com.acornui.di.own
import com.acornui.time.timer
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.seconds

class PerformanceMetricsWindow(owner: Owned): WindowPanel(owner) {

	private val avgDrawCallsText: TextField
	private val avgFps: TextField

	init {
		addStyleRule(charStyle {
			fontSize = FontSize.SMALL
		})
		+vGroup {
			+text("Update")
			val updateMetricsView = +PerformanceMetricsView(this) layout { widthPercent = 1f }
			+text("Render")
			val renderMetricsView = +PerformanceMetricsView(this) layout { widthPercent = 1f }
			+form {
				+text("AVG DC") {
					tooltip("Average number of draw calls per second.")
				}
				avgDrawCallsText = +text {
					flowStyle.horizontalAlign = FlowHAlign.RIGHT
				} layout { widthPercent = 1f }

				+text("AVG FPS") {
					tooltip("Average number of frames per second.")
				}
				avgFps = +text {
					flowStyle.horizontalAlign = FlowHAlign.RIGHT
				} layout { widthPercent = 1f }
			} layout { widthPercent = 1f }

			own(timer(3.seconds, -1, 0f) {
				updateMetricsView.data = updatePerformance.copy()
				renderMetricsView.data = renderPerformance.copy()
				avgDrawCallsText.text = (totalDrawCalls / totalFrames).toString()
				avgFps.text = (totalFrames / 3).toString()
				updatePerformance.clear()
				renderPerformance.clear()
				totalFrames = 0
				totalDrawCalls = 0
			})
		} layout { widthPercent = 1f }

		measureFramePerformanceEnabled = true
	}

	override fun dispose() {
		super.dispose()
		measureFramePerformanceEnabled = false
	}
}

inline fun Owned.performanceMetricsWindow(init: ComponentInit<PerformanceMetricsWindow> = {}): PerformanceMetricsWindow {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return PerformanceMetricsWindow(this).apply(init)
}


class PerformanceMetricsView(owner: Owned) : FormContainer<UiComponent>(owner), ItemRenderer<PerformanceMetrics> {

	private val frameTimeText: TextField
	private val maxText: TextField

	init {
		+formLabel("AVG:") {
			tooltip("Average Time")
		}
		frameTimeText = +text {
			flowStyle.horizontalAlign = FlowHAlign.RIGHT
		} layout { widthPercent = 1f }

		+formLabel("MAX:") {
			tooltip("Maximum Time")
		}
		maxText = +text {
			flowStyle.horizontalAlign = FlowHAlign.RIGHT
		} layout { widthPercent = 1f }
	}

	override var data: PerformanceMetrics? = null
		set(value) {
			field = value
			frameTimeText.text = value?.average?.toString() ?: ""
			maxText.text = value?.max?.toString() ?: ""
		}
}