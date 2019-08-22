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

package com.acornui.mock

import com.acornui.input.KeyInput
import com.acornui.input.interaction.CharInteractionRo
import com.acornui.input.interaction.KeyInteractionRo
import com.acornui.input.interaction.KeyLocation
import com.acornui.signal.Signal
import com.acornui.signal.emptySignal

object MockKeyInput : KeyInput {
	override fun keyIsDown(keyCode: Int, location: KeyLocation): Boolean {
		return false
	}

	override val keyDown: Signal<(KeyInteractionRo) -> Unit> = emptySignal()
	override val keyUp: Signal<(KeyInteractionRo) -> Unit> = emptySignal()
	override val char: Signal<(CharInteractionRo) -> Unit> = emptySignal()

	override fun dispose() {
	}
}