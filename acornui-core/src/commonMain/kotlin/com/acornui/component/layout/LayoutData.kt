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

package com.acornui.component.layout

import com.acornui.signal.Signal
import com.acornui.signal.emptySignal

/**
 * A class representing extra layout data, specific to use with LayoutAlgorithm objects.
 */
interface LayoutData {

	/**
	 * Dispatched when a property in this layout data has changed.
	 */
	val changed: Signal<() -> Unit>
}

object NoopLayoutData : LayoutData {
	override val changed: Signal<() -> Unit> = emptySignal()
}
