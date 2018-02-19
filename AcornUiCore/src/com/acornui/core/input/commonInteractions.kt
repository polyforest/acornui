/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.core.input

import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuse
import com.acornui.core.input.interaction.*
import com.acornui.signal.StoppableSignal

/**
 * Dispatched when the mouse or touch is pressed down on this element.
 */
fun UiComponentRo.mouseDown(isCapture: Boolean = false): StoppableSignal<MouseInteractionRo> {
	return createOrReuse(MouseInteractionRo.MOUSE_DOWN, isCapture)
}

/**
 * Dispatched when the mouse or touch is released from this element.
 */
fun UiComponentRo.mouseUp(isCapture: Boolean = false): StoppableSignal<MouseInteractionRo> {
	return createOrReuse(MouseInteractionRo.MOUSE_UP, isCapture)
}

/**
 * Dispatched when the mouse or touch has moved within the bounds of this element.
 */
fun UiComponentRo.mouseMove(isCapture: Boolean = false): StoppableSignal<MouseInteractionRo> {
	return createOrReuse(MouseInteractionRo.MOUSE_MOVE, isCapture)
}

/**
 * Dispatched when a key has been pressed while this interactive element has focus.
 */
fun UiComponentRo.keyDown(isCapture: Boolean = false): StoppableSignal<KeyInteractionRo> {
	return createOrReuse(KeyInteractionRo.KEY_DOWN, isCapture)
}

/**
 * Dispatched when a key has been released while this interactive element has focus.
 */
fun UiComponentRo.keyUp(isCapture: Boolean = false): StoppableSignal<KeyInteractionRo> {
	return createOrReuse(KeyInteractionRo.KEY_UP, isCapture)
}

/**
 * Dispatched when a character has been inputted while this interactive element has focus.
 */
fun UiComponentRo.char(isCapture: Boolean = false): StoppableSignal<CharInteractionRo> {
	return createOrReuse(CharInteractionRo.CHAR, isCapture)
}

/**
 * Dispatched when the mouse is moved over this target. This will not trigger for touch surface interaction.
 */
fun UiComponentRo.mouseOver(isCapture: Boolean = false): StoppableSignal<MouseInteractionRo> {
	return createOrReuse(MouseInteractionRo.MOUSE_OVER, isCapture)
}

/**
 * Dispatched when the mouse has moved off of this target. This will not trigger for touch surface interaction.
 */
fun UiComponentRo.mouseOut(isCapture: Boolean = false): StoppableSignal<MouseInteractionRo> {
	return createOrReuse(MouseInteractionRo.MOUSE_OUT, isCapture)
}

/**
 * Dispatched when the mouse wheel has been scrolled.
 */
fun UiComponentRo.wheel(isCapture: Boolean = false): StoppableSignal<WheelInteractionRo> {
	return createOrReuse(WheelInteractionRo.MOUSE_WHEEL, isCapture)
}

fun UiComponentRo.touchStart(isCapture: Boolean = false): StoppableSignal<TouchInteractionRo> {
	return createOrReuse(TouchInteractionRo.TOUCH_START, isCapture)
}

fun UiComponentRo.touchMove(isCapture: Boolean = false): StoppableSignal<TouchInteractionRo> {
	return createOrReuse(TouchInteractionRo.TOUCH_MOVE, isCapture)
}

fun UiComponentRo.touchEnd(isCapture: Boolean = false): StoppableSignal<TouchInteractionRo> {
	return createOrReuse(TouchInteractionRo.TOUCH_END, isCapture)
}

fun UiComponentRo.touchCancel(isCapture: Boolean = false): StoppableSignal<TouchInteractionRo> {
	return createOrReuse(TouchInteractionRo.TOUCH_CANCEL, isCapture)
}

fun UiComponentRo.clipboardCopy(isCapture: Boolean = false): StoppableSignal<ClipboardInteractionRo> {
	return createOrReuse(ClipboardInteractionRo.COPY, isCapture)
}

fun UiComponentRo.clipboardCut(isCapture: Boolean = false): StoppableSignal<ClipboardInteractionRo> {
	return createOrReuse(ClipboardInteractionRo.CUT, isCapture)
}

fun UiComponentRo.clipboardPaste(isCapture: Boolean = false): StoppableSignal<ClipboardInteractionRo> {
	return createOrReuse(ClipboardInteractionRo.PASTE, isCapture)
}