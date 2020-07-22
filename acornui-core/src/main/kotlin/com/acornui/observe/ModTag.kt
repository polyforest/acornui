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

package com.acornui.observe

import com.acornui.recycle.Clearable

interface Mutable {

	/**
	 * A value that changes every time this object changes.
	 * No two objects within this application can will have the same check value.
	 */
	val checkValue: Long

	/**
	 * Updates [checkValue] to indicate that this object has changed.
	 */
	fun updateCheckValue()
}

/**
 * A modification tag allows for quick checking if an object has changed.
 * This is usually paired with [Watch].
 */
class ModTag : Mutable {

	private val id: Long = (++counter).toLong()
	private var modCount = 0

	/**
	 * The first 32 bits represents a unique ID. The last 32 bits represents a modification count.
	 */
	override val checkValue: Long
		get() = id shl 32 or modCount.toLong()

	/**
	 * Marks the modification tag as having been changed.
	 */
	override fun updateCheckValue() {
		modCount++
	}

	companion object {
		private var counter = 0
	}

}

fun modTag(): ModTag = ModTag()

/**
 * A watch checks if a list of objects has changed between calls.
 * Watch supports two types of objects. Objects where the hash code is unique to the data (such as data objects), and
 * [Mutable] objects where the [Mutable.checkValue] is unique to the object and its data.
 */
class Watch : Clearable {

	private var crc = -1L

	/**
	 * Invokes the given callback if any of the targets have changed since the last call to [check].
	 * The values checked will be either [Mutable.checkValue] if the object implements [Mutable], or
	 * [hashCode] otherwise.
	 *
	 * @return Returns true if there was a change, false if modification tag is current.
	 */
	fun check(vararg targets: Any, callback: () -> Unit = {}): Boolean {
		Crc32.CRC.reset()
		for (i in 0..targets.lastIndex) {
			val target = targets[i]
			if (target is Mutable)
				Crc32.CRC.update(target.checkValue)
			else
				Crc32.CRC.update(target.hashCode())

		}
		val newCrc = Crc32.CRC.getValue()
		if (crc == newCrc) return false
		crc = newCrc
		callback()
		return true
	}

	/**
	 * Resets the watch so that the next set will always return true.
	 */
	override fun clear() {
		crc = -1L
	}
}
