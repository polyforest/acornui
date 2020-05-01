package com.acornui.input.interaction

import org.w3c.dom.events.KeyboardEvent


/**
 * Sets the values of this key interaction to match that of the javascript keyboard event.
 * @return Returns the receiver for chaining.
 */
fun KeyInteraction.set(jsEvent: KeyboardEvent): KeyInteraction {
	clear()
	timestamp = jsEvent.timeStamp.toLong()
	location = keyLocationFromInt(jsEvent.location)
	keyCode = jsEvent.keyCode
	altKey = jsEvent.altKey
	ctrlKey = jsEvent.ctrlKey
	metaKey = jsEvent.metaKey
	shiftKey = jsEvent.shiftKey
	isRepeat = jsEvent.repeat
	return this
}

/**
 * Sets the values of this char interaction to match that of the javascript keyboard event.
 * @return Returns the receiver for chaining.
 */
fun CharInteraction.set(jsEvent: KeyboardEvent): CharInteraction {
	clear()
	char = jsEvent.charCode.toChar()
	return this
}

fun keyLocationFromInt(location: Int): KeyLocation {
	return when (location) {
		0 -> KeyLocation.STANDARD
		1 -> KeyLocation.LEFT
		2 -> KeyLocation.RIGHT
		3 -> KeyLocation.NUMBER_PAD
		else -> KeyLocation.UNKNOWN
	}
}