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
import com.acornui.core.EqualityCheck

/**
 * Binds to a target observable list, keeping a parallel list of values created from a factory.
 */
@Suppress("UNUSED_PARAMETER")
class ObservableListMapping<E, V>(
		target: ObservableList<E>,
		private val factory: (E) -> V,
		private val disposer: (V) -> Unit,
		equality: EqualityCheck<E> = { o1, o2 -> o1 == o2 }
) : ListBase<V>(), Disposable {

	private val binding = ObservableListBinding(target, configure = this::addedHandler, unconfigure = this::removedHandler, equality = equality)

	private val list = ArrayList<V>()

	//---------------------------------------------
	// Target list handlers
	//---------------------------------------------

	private fun addedHandler(index: Int, value: E) {
		list.add(index, factory(value))
	}

	private fun removedHandler(index: Int, value: E) {
		disposer(list.removeAt(index))
	}

	override val size: Int
		get() = list.size

	override fun get(index: Int): V = list[index]

	override fun dispose() {
		binding.dispose()
		list.clear()
	}
}