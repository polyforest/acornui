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

package com.acornui.core.input.interaction

import com.acornui.component.UiComponent
import com.acornui.core.input.Ascii
import com.acornui.core.input.keyDown

data class Hotkey(

		/**
		 * The ascii keyCode.
		 * @see Ascii
		 */
		val keyCode: Int,

		/**
		 * The location of the key.
		 * If there is only one location for the related key, it will have a location of STANDARD.
		 */
		val location: KeyLocation = KeyLocation.UNKNOWN,

		/**
		 * True if the Alt key was active when the KeyboardEvent was generated.
		 */
		val altKey: Boolean = false,

		/**
		 * True if the Control key was active when the KeyboardEvent was generated.
		 */
		val ctrlKey: Boolean = false,

		/**
		 * True if the Meta key, (or Command key on OS X) was active when the KeyboardEvent was generated.
		 * Also called the "super" key.
		 */
		val metaKey: Boolean = false,

		/**
		 * True if the Shift key was active when the KeyboardEvent was generated.
		 */
		val shiftKey: Boolean = false

) {
	val label: String by lazy {
		val labelList = ArrayList<String>().apply {
			if (metaKey) add("⌘")
			if (ctrlKey) add("Ctrl")
			if (altKey) add("Alt")
			if (shiftKey) add("Shift")
			val symbol = Ascii.toString(keyCode)
			if (symbol != null) add(symbol)
		}
		labelList.joinToString("+")
	}

	fun matches(interaction: KeyInteractionRo): Boolean {
		return interaction.keyCode == keyCode &&
				(location == KeyLocation.UNKNOWN || interaction.location == location) &&
				interaction.altKey == altKey &&
				interaction.ctrlKey == ctrlKey &&
				interaction.metaKey == metaKey &&
				interaction.shiftKey == shiftKey
	}

}

fun Hotkey.bind(component: UiComponent, callback: (KeyInteractionRo) -> Unit) {
	component.keyDown().add {
		if (matches(it)) {
			callback(it)
		}
	}
}
