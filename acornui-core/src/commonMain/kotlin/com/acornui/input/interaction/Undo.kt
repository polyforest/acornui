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

package com.acornui.input.interaction

import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuse
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.input.*
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

class UndoDispatcher(owner: Context) : ContextImpl(owner) {

	private val key by KeyInput
	private val interactivity by InteractivityManager

	private val event = UndoInteraction()

	private val keyDownHandler =  { e: KeyInteractionRo ->
		if (!e.handled) {
			if (e.commandPlat && (e.keyCode == Ascii.Y || (e.shiftKey && e.keyCode == Ascii.Z))) {
				e.handled = true
				event.clear()
				event.type = UndoInteractionRo.REDO
				interactivity.dispatch(event)
			} else if (e.commandPlat && e.keyCode == Ascii.Z) {
				e.handled = true
				event.clear()
				event.type = UndoInteractionRo.UNDO
				interactivity.dispatch(event)
			}
		}
	}

	init {
		key.keyDown.add(keyDownHandler)
	}

	override fun dispose() {
		super.dispose()
		key.keyDown.remove(keyDownHandler)
	}
}
