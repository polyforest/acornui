package com.acornui.core.input.interaction

import com.acornui.core.input.InteractionEventBase
import com.acornui.core.input.InteractionEventRo
import com.acornui.core.input.InteractionType

interface CharInteractionRo : InteractionEventRo {
	val char: Char

	companion object {
		val CHAR = InteractionType<CharInteractionRo>("char")
	}
}

/**
 * An event representing a character input.
 */
open class CharInteraction : InteractionEventBase(), CharInteractionRo {

	override var char: Char = 0.toChar()

	fun set(other: CharInteractionRo) {
		char = other.char
	}

	override fun clear() {
		super.clear()
		char = 0.toChar()
	}
}
