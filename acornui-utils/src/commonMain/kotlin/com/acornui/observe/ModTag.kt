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

interface ModTagRo {

	val crc: Long

}

interface ModTag : ModTagRo {

	/**
	 * Marks the modification tag as having been changed.
	 */
	fun increment()

}

/**
 * @author nbilyk
 */
class ModTagImpl : ModTag {

	private var _id: Int = ++counter
	private var _modCount: Int = 0

	override val crc: Long
		get() = _id.toLong() shl 32 or _modCount.toLong()

	override fun increment() {
		_modCount++
	}

	companion object {
		private var counter = 0
	}

}

class ModTagWatch : Clearable {

	private var crc = -1L

	/**
	 * Sets this ModTag to match the source ModTag
	 * Returns true if there was a change, false if modification tag is current.
	 */
	fun set(target: ModTagRo): Boolean {
		if (crc == target.crc) return false
		crc = target.crc
		return true
	}

	/**
	 * Sets this ModTag to have a crc that matches the list of provided mod tags.
	 * Returns true if there was a change, false if modification tag is current.
	 */
	fun set(targets: List<ModTagRo>): Boolean {
		Crc32.CRC.reset()
		for (i in 0..targets.lastIndex) {
			val target = targets[i]
			Crc32.CRC.update(target.crc)
		}
		val newCrc = Crc32.CRC.getValue()
		if (crc == newCrc) return false
		crc = newCrc
		return true
	}

	override fun clear() {
		crc = -1L
	}
}
