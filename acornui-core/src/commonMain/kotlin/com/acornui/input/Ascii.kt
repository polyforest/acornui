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

package com.acornui.input

import com.acornui.collection.stringMapOf

/**
 * Ascii Key codes.
 */
object Ascii {

	const val NUM_0: Int = 48
	const val NUM_1: Int = 49
	const val NUM_2: Int = 50
	const val NUM_3: Int = 51
	const val NUM_4: Int = 52
	const val NUM_5: Int = 53
	const val NUM_6: Int = 54
	const val NUM_7: Int = 55
	const val NUM_8: Int = 56
	const val NUM_9: Int = 57

	const val A: Int = 65
	const val B: Int = 66
	const val C: Int = 67
	const val D: Int = 68
	const val E: Int = 69
	const val F: Int = 70
	const val G: Int = 71
	const val H: Int = 72
	const val I: Int = 73
	const val J: Int = 74
	const val K: Int = 75
	const val L: Int = 76
	const val M: Int = 77
	const val N: Int = 78
	const val O: Int = 79
	const val P: Int = 80
	const val Q: Int = 81
	const val R: Int = 82
	const val S: Int = 83
	const val T: Int = 84
	const val U: Int = 85
	const val V: Int = 86
	const val W: Int = 87
	const val X: Int = 88
	const val Y: Int = 89
	const val Z: Int = 90

	const val NUMPAD_0: Int = 96
	const val NUMPAD_1: Int = 97
	const val NUMPAD_2: Int = 98
	const val NUMPAD_3: Int = 99
	const val NUMPAD_4: Int = 100
	const val NUMPAD_5: Int = 101
	const val NUMPAD_6: Int = 102
	const val NUMPAD_7: Int = 103
	const val NUMPAD_8: Int = 104
	const val NUMPAD_9: Int = 105

	const val F1: Int = 112
	const val F2: Int = 113
	const val F3: Int = 114
	const val F4: Int = 115
	const val F5: Int = 116
	const val F6: Int = 117
	const val F7: Int = 118
	const val F8: Int = 119
	const val F9: Int = 120
	const val F10: Int = 121
	const val F11: Int = 122
	const val F12: Int = 123
	const val F13: Int = 124
	const val F14: Int = 125
	const val F15: Int = 126
	const val F16: Int = 127
	const val F17: Int = 128
	const val F18: Int = 129
	const val F19: Int = 130
	const val F20: Int = 131
	const val F21: Int = 132
	const val F22: Int = 133
	const val F23: Int = 134
	const val F24: Int = 135

	const val LTR: Int = 8206
	const val RTL: Int = 8207

	// Modifiers
	const val ALT: Int = 18
	const val CONTROL: Int = 17
	const val SHIFT: Int = 16
	const val META: Int = 91

	const val ADD: Int = 107
	const val BACK_QUOTE: Int = 192
	const val BACK_SLASH: Int = 220
	const val BACKSPACE: Int = 8
	const val CANCEL: Int = 3
	const val CAPS_LOCK: Int = 20
	const val CLEAR: Int = 12
	const val CLOSE_BRACKET: Int = 221
	const val COMMA: Int = 188
	const val CONTEXT_MENU: Int = 93
	const val DASH: Int = 189
	const val DECIMAL: Int = 110
	const val DELETE: Int = 46
	const val DIVIDE: Int = 111
	const val DOWN: Int = 40
	const val END: Int = 35
	const val ENTER: Int = 14
	const val EQUALS: Int = 187
	const val ESCAPE: Int = 27
	const val HELP: Int = 6
	const val HOME: Int = 36
	const val INSERT: Int = 45
	const val LEFT: Int = 37
	const val MULTIPLY: Int = 106
	const val NUM_LOCK: Int = 144
	const val OPEN_BRACKET: Int = 219
	const val PAGE_DOWN: Int = 34
	const val PAGE_UP: Int = 33
	const val PAUSE: Int = 19
	const val PERIOD: Int = 190
	const val PRINT_SCREEN: Int = 44
	const val QUOTE: Int = 222
	const val RETURN: Int = 13
	const val RIGHT: Int = 39
	const val SCROLL_LOCK: Int = 145
	const val SEMICOLON: Int = 59
	const val SEPARATOR: Int = 108
	const val SLASH: Int = 191
	const val SPACE: Int = 32
	const val SUBTRACT: Int = 109
	const val TAB: Int = 9
	const val UP: Int = 38

	/**
	 * @return a human readable representation of the keycode. The returned value can be used in
	 * [Ascii.valueOf]
	 */
	fun toString(keyCode: Int): String? {
		if (keyCode < 0 || keyCode > 512) return null
		when (keyCode) {
			NUM_0 -> return "0"
			NUM_1 -> return "1"
			NUM_2 -> return "2"
			NUM_3 -> return "3"
			NUM_4 -> return "4"
			NUM_5 -> return "5"
			NUM_6 -> return "6"
			NUM_7 -> return "7"
			NUM_8 -> return "8"
			NUM_9 -> return "9"

			A -> return "A"
			B -> return "B"
			C -> return "C"
			D -> return "D"
			E -> return "E"
			F -> return "F"
			G -> return "G"
			H -> return "H"
			I -> return "I"
			J -> return "J"
			K -> return "K"
			L -> return "L"
			M -> return "M"
			N -> return "N"
			O -> return "O"
			P -> return "P"
			Q -> return "Q"
			R -> return "R"
			S -> return "S"
			T -> return "T"
			U -> return "U"
			V -> return "V"
			W -> return "W"
			X -> return "X"
			Y -> return "Y"
			Z -> return "Z"

			NUMPAD_0 -> return "NUMPAD 0"
			NUMPAD_1 -> return "NUMPAD 1"
			NUMPAD_2 -> return "NUMPAD 2"
			NUMPAD_3 -> return "NUMPAD 3"
			NUMPAD_4 -> return "NUMPAD 4"
			NUMPAD_5 -> return "NUMPAD 5"
			NUMPAD_6 -> return "NUMPAD 6"
			NUMPAD_7 -> return "NUMPAD 7"
			NUMPAD_8 -> return "NUMPAD 8"
			NUMPAD_9 -> return "NUMPAD 9"

			F1 -> return "F1"
			F2 -> return "F2"
			F3 -> return "F3"
			F4 -> return "F4"
			F5 -> return "F5"
			F6 -> return "F6"
			F7 -> return "F7"
			F8 -> return "F8"
			F9 -> return "F9"
			F10 -> return "F10"
			F11 -> return "F11"
			F12 -> return "F12"
			F13 -> return "F13"
			F14 -> return "F14"
			F15 -> return "F15"
			F16 -> return "F16"
			F17 -> return "F17"
			F18 -> return "F18"
			F19 -> return "F19"
			F20 -> return "F20"
			F21 -> return "F21"
			F22 -> return "F22"
			F23 -> return "F23"
			F24 -> return "F24"

			ALT -> return "ALT"
			CONTROL -> return "CONTROL"
			SHIFT -> return "SHIFT"
			META -> return "META"

			ADD -> return "ADD"
			BACK_QUOTE -> return "BACK_QUOTE"
			BACK_SLASH -> return "BACK_SLASH"
			BACKSPACE -> return "BACKSPACE"
			CANCEL -> return "CANCEL"
			CAPS_LOCK -> return "CAPS_LOCK"
			CLEAR -> return "CLEAR"
			CLOSE_BRACKET -> return "CLOSE_BRACKET"
			COMMA -> return "COMMA"
			CONTEXT_MENU -> return "CONTEXT_MENU"
			DASH -> return "DASH"
			DECIMAL -> return "DECIMAL"
			DELETE -> return "DELETE"
			DIVIDE -> return "DIVIDE"
			DOWN -> return "DOWN"
			END -> return "END"
			ENTER -> return "ENTER"
			EQUALS -> return "EQUALS"
			ESCAPE -> return "ESCAPE"
			HELP -> return "HELP"
			HOME -> return "HOME"
			INSERT -> return "INSERT"
			LEFT -> return "LEFT"
			MULTIPLY -> return "MULTIPLY"
			NUM_LOCK -> return "NUM_LOCK"
			OPEN_BRACKET -> return "OPEN_BRACKET"
			PAGE_DOWN -> return "PAGE_DOWN"
			PAGE_UP -> return "PAGE_UP"
			PAUSE -> return "PAUSE"
			PERIOD -> return "PERIOD"
			PRINT_SCREEN -> return "PRINT_SCREEN"
			QUOTE -> return "QUOTE"
			RETURN -> return "RETURN"
			RIGHT -> return "RIGHT"
			SCROLL_LOCK -> return "SCROLL_LOCK"
			SEMICOLON -> return "SEMICOLON"
			SEPARATOR -> return "SEPARATOR"
			SLASH -> return "SLASH"
			SPACE -> return "SPACE"
			SUBTRACT -> return "SUBTRACT"
			TAB -> return "TAB"
			UP -> return "UP"

			else -> // key name not found
				return null
		}
	}

	private val keyNames: MutableMap<String, Int> by lazy(LazyThreadSafetyMode.NONE) {
		val keyNames = stringMapOf<Int>()
		for (i in 0..512) {
			val name = toString(i)
			if (name != null) keyNames[name] = i
		}
		keyNames
	}

	/**
	 * @param keyName the key name returned by the [Ascii.toString] method
	 * @return the int keycode
	 */
	fun valueOf(keyName: String): Int {
		return keyNames[keyName.toUpperCase()] ?: -1
	}
}
