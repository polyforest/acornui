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

package com.acornui.jvm.input

import com.acornui.collection.MutableMultiMap2
import com.acornui.collection.get
import com.acornui.collection.multiMap2
import com.acornui.collection.remove
import com.acornui.input.Ascii
import com.acornui.input.KeyInput
import com.acornui.input.interaction.*
import com.acornui.signal.Signal1
import com.acornui.time.nowMs
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWCharCallback
import org.lwjgl.glfw.GLFWKeyCallback


/**
 * @author nbilyk
 */
class LwjglKeyInput(private val window: Long) : KeyInput {

	private val _keyDown = Signal1<KeyInteractionRo>()
	override val keyDown = _keyDown.asRo()
	private val _keyUp = Signal1<KeyInteractionRo>()
	override val keyUp = _keyUp.asRo()
	private val _char = Signal1<CharInteractionRo>()
	override val char = _char.asRo()

	private val keyEvent = KeyInteraction()
	private val charEvent = CharInteraction()

	private val keyCodeMap: HashMap<Int, Pair<Int, KeyLocation>>
	private val downMap: MutableMultiMap2<Int, KeyLocation, Boolean> = multiMap2()

	init {
		GLFW.glfwSetInputMode(window, GLFW.GLFW_LOCK_KEY_MODS, 1)
		keyCodeMap = hashMapOf(
				GLFW.GLFW_KEY_LEFT_SHIFT to (Ascii.SHIFT to KeyLocation.LEFT),
				GLFW.GLFW_KEY_LEFT_CONTROL to (Ascii.CONTROL to KeyLocation.LEFT),
				GLFW.GLFW_KEY_LEFT_ALT to (Ascii.ALT to KeyLocation.LEFT),
				GLFW.GLFW_KEY_LEFT_SUPER to (Ascii.META to KeyLocation.LEFT),
				GLFW.GLFW_KEY_RIGHT_SHIFT to (Ascii.SHIFT to KeyLocation.RIGHT),
				GLFW.GLFW_KEY_RIGHT_CONTROL to (Ascii.CONTROL to KeyLocation.RIGHT),
				GLFW.GLFW_KEY_RIGHT_ALT to (Ascii.ALT to KeyLocation.RIGHT),
				GLFW.GLFW_KEY_RIGHT_SUPER to (Ascii.META to KeyLocation.RIGHT),

				GLFW.GLFW_KEY_BACKSLASH to (Ascii.BACK_SLASH to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_CAPS_LOCK to (Ascii.CAPS_LOCK to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_EQUAL to  (Ascii.EQUALS to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_GRAVE_ACCENT to  (Ascii.BACK_QUOTE to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_LEFT_BRACKET to (Ascii.OPEN_BRACKET to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_MINUS to  (Ascii.DASH to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_RIGHT_BRACKET to (Ascii.CLOSE_BRACKET to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_TAB to (Ascii.TAB to KeyLocation.STANDARD),

				GLFW.GLFW_KEY_PAUSE to (Ascii.PAUSE to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_PRINT_SCREEN to (Ascii.PRINT_SCREEN to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_SCROLL_LOCK to (Ascii.SCROLL_LOCK to KeyLocation.STANDARD),

				GLFW.GLFW_KEY_DELETE to (Ascii.DELETE to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_END to (Ascii.END to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_HOME to (Ascii.HOME to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_INSERT to (Ascii.INSERT to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_PAGE_DOWN to (Ascii.PAGE_DOWN to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_PAGE_UP to (Ascii.PAGE_UP to KeyLocation.STANDARD),

				GLFW.GLFW_KEY_KP_ADD to (Ascii.ADD to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_DECIMAL to (Ascii.DECIMAL to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_DIVIDE to (Ascii.DIVIDE to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_ENTER to (Ascii.ENTER to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_MULTIPLY to (Ascii.MULTIPLY to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_SUBTRACT to (Ascii.SUBTRACT to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_NUM_LOCK to (Ascii.NUM_LOCK to KeyLocation.NUMBER_PAD),

				GLFW.GLFW_KEY_KP_0 to (Ascii.INSERT to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_1 to (Ascii.END to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_2 to (Ascii.NUMPAD_2 to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_3 to (Ascii.PAGE_DOWN to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_4 to (Ascii.NUMPAD_4 to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_5 to (Ascii.NUMPAD_5 to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_6 to (Ascii.NUMPAD_6 to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_7 to (Ascii.HOME to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_8 to (Ascii.NUMPAD_8 to KeyLocation.NUMBER_PAD),
				GLFW.GLFW_KEY_KP_9 to (Ascii.PAGE_UP to KeyLocation.NUMBER_PAD),

				GLFW.GLFW_KEY_BACKSPACE to (Ascii.BACKSPACE to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_COMMA to (Ascii.COMMA to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_ENTER to (Ascii.ENTER to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_ESCAPE to (Ascii.ESCAPE to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_PERIOD to (Ascii.PERIOD to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_SLASH to (Ascii.SLASH to KeyLocation.STANDARD),

				GLFW.GLFW_KEY_UP to (Ascii.UP to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_RIGHT to (Ascii.RIGHT to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_DOWN to (Ascii.DOWN to KeyLocation.STANDARD),
				GLFW.GLFW_KEY_LEFT to (Ascii.LEFT to KeyLocation.STANDARD)
		)

		val fOffset = GLFW.GLFW_KEY_F1 - Ascii.F1
		for (i in Ascii.F1 .. Ascii.F24) {
			keyCodeMap[i + fOffset] = i to KeyLocation.STANDARD
		}
	}

	private val keyCallback: GLFWKeyCallback = object : GLFWKeyCallback() {

		override fun invoke(window: Long, key: Int, scanCode: Int, action: Int, mods: Int) {
			keyEvent.clear()
			if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9 && (mods and GLFW.GLFW_MOD_NUM_LOCK > 0)) {
				keyEvent.keyCode = Ascii.NUMPAD_0 + key - GLFW.GLFW_KEY_KP_0
				keyEvent.location = KeyLocation.NUMBER_PAD
			} else if (keyCodeMap.containsKey(key)) {
				val (keyCode, location) = keyCodeMap[key]!!
				keyEvent.keyCode = keyCode
				keyEvent.location = location
			} else {
				keyEvent.keyCode = key
				keyEvent.location = KeyLocation.STANDARD
			}

			keyEvent.altKey = mods and GLFW.GLFW_MOD_ALT > 0
			keyEvent.ctrlKey = mods and GLFW.GLFW_MOD_CONTROL > 0
			keyEvent.metaKey = mods and GLFW.GLFW_MOD_SUPER > 0
			keyEvent.shiftKey = mods and GLFW.GLFW_MOD_SHIFT > 0

			keyEvent.timestamp = nowMs()
			when (action) {
				GLFW.GLFW_PRESS -> {
					downMap[keyEvent.keyCode][keyEvent.location] = true
					keyEvent.type = KeyInteractionRo.KEY_DOWN
					_keyDown.dispatch(keyEvent)
				}
				GLFW.GLFW_RELEASE -> {
					downMap.remove(keyEvent.keyCode, keyEvent.location, removeEmptyMaps = false)
					keyEvent.type = KeyInteractionRo.KEY_UP
					_keyUp.dispatch(keyEvent)
				}
				GLFW.GLFW_REPEAT -> {
					keyEvent.type = KeyInteractionRo.KEY_DOWN
					keyEvent.isRepeat = true
					_keyDown.dispatch(keyEvent)
				}
			}
		}
	}

	private val charCallback: GLFWCharCallback = object : GLFWCharCallback() {
		override fun invoke(window: Long, codepoint: Int) {
			charEvent.char = codepoint.toChar()
			_char.dispatch(charEvent)
		}
	}

	init {
		GLFW.glfwSetKeyCallback(window, keyCallback)
		GLFW.glfwSetCharCallback(window, charCallback)
	}

	override fun keyIsDown(keyCode: Int, location: KeyLocation): Boolean {
		return if (location == KeyLocation.UNKNOWN) {
			return if (!downMap.containsKey(keyCode)) false
			else downMap[keyCode].isNotEmpty()
		} else {
			downMap[keyCode, location] ?: false
		}
	}

	override fun dispose() {
		GLFW.glfwSetKeyCallback(window, null)
		GLFW.glfwSetCharCallback(window, null)
		_keyDown.dispose()
		_keyUp.dispose()
		_char.dispose()
	}
}
