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

package com.acornui.component

import com.acornui.observe.Observable
import com.acornui.signal.Signal

/**
 * A UiComponent that can be observed for changes.
 */
interface InputComponentRo<out T> : Observable, UiComponentRo {

	/**
	 * Dispatched on value commit.
	 * This is only dispatched on a user interaction.
	 */
	override val changed: Signal<(InputComponentRo<T>) -> Unit>

	/**
	 * This component's input value.
	 */
	val inputValue: T
}

interface InputComponent<out T> : UiComponent, InputComponentRo<T>