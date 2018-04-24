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

package com.acornui.core.input.interaction

import com.acornui.component.Stage
import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuse
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.focus.FocusManager
import com.acornui.core.input.*
import com.acornui.signal.StoppableSignal

interface UndoInteractionRo : InteractionEventRo {

	companion object {
		val UNDO = InteractionType<UndoInteractionRo>("undo")
		val REDO = InteractionType<UndoInteractionRo>("redo")
	}
}

class UndoInteraction : UndoInteractionRo, InteractionEventBase()

fun UiComponentRo.undo(isCapture: Boolean = false): StoppableSignal<UndoInteractionRo> {
	return createOrReuse(UndoInteractionRo.UNDO, isCapture)
}

fun UiComponentRo.redo(isCapture: Boolean = false): StoppableSignal<UndoInteractionRo> {
	return createOrReuse(UndoInteractionRo.REDO, isCapture)
}

class UndoDispatcher(override val injector: Injector) : Scoped {

	private val key = inject(KeyInput)
	private val interactivity = inject(InteractivityManager)
	private val focus = inject(FocusManager)
	private val stage = inject(Stage)

	private val event = UndoInteraction()

	init {
		//val history = stateCommandHistory()

		// UNDO / REDO
		// TODO: Mac
		key.keyDown.add { e ->
			if (!e.handled) {
				if (e.ctrlKey && (e.keyCode == Ascii.Y || (e.shiftKey && e.keyCode == Ascii.Z))) {
					e.handled = true
					event.clear()
					event.type = UndoInteractionRo.REDO
					interactivity.dispatch(focus.focused() ?: stage, event)
				} else if (e.ctrlKey && e.keyCode == Ascii.Z) {
					e.handled = true
					event.clear()
					event.type = UndoInteractionRo.UNDO
					interactivity.dispatch(focus.focused() ?: stage, event)
				}
			}
		}
	}

}