/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.collection

import com.acornui.core.Disposable

@Suppress("UNUSED_PARAMETER")
/**
 * Binds to a target observable list, keeping a parallel list of values created from a factory.
 */
class ObservableListMapping<E, V>(
		private val target: ObservableList<E>,
		private val factory: (E) -> V,
		private val disposer: (V) -> Unit
) : ListBase<V>(), List<V>, Disposable {

	private val _list = ArrayList<V>()

	init {
		target.added.add(this::addedHandler)
		target.removed.add(this::removedHandler)
		target.changed.add(this::changedHandler)
		target.reset.add(this::resetHandler)
		resetHandler()
	}

	//---------------------------------------------
	// Target list handlers
	//---------------------------------------------

	private fun addedHandler(index: Int, value: E) {
		_list.add(index, factory(value))
	}

	private fun removedHandler(index: Int, value: E) {
		disposer(_list.removeAt(index))
	}

	private fun changedHandler(index: Int, oldValue: E, newValue: E) {
		disposer(_list[index])
		_list[index] = factory(newValue)
	}

	private fun resetHandler() {
		clear()
		for (i in 0..target.lastIndex) {
			_list.add(factory(target[i]))
		}
	}

	private fun clear() {
		for (i in 0.._list.lastIndex) {
			disposer(_list[i])
		}
		_list.clear()
	}

	//---------------------------------------------

	override val size: Int
		get() = _list.size

	override fun get(index: Int): V {
		return _list[index]
	}

	override fun dispose() {
		clear()
		target.added.remove(this::addedHandler)
		target.removed.remove(this::removedHandler)
		target.changed.remove(this::changedHandler)
		target.reset.remove(this::resetHandler)
	}
}