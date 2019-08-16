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

package com.acornui.time

import com.acornui.*
import com.acornui.collection.ActiveList
import com.acornui.collection.iterate
import com.acornui.di.Scoped
import com.acornui.recycle.Clearable

/**
 * @author nbilyk
 */
object FrameDriver : Parent<UpdatableChild>, Updatable, Clearable {

	override val parent: Parent<out ChildRo>? = null

	private val _children = ActiveList<UpdatableChild>()
	private val childIterator = _children.concurrentIterator()

	override fun update(dT: Float) {
		childIterator.iterate {
			it.update(dT)
			true
		}
	}

	//-----------------------------------------------
	// Parent
	//-----------------------------------------------

	override val children: List<UpdatableChild>
		get() = _children

	/**
	 * Adds the specified child to this container.
	 * @param index The index of where to insert the child. By default this is the end of the list.
	 */
	override fun <S : UpdatableChild> addChild(index: Int, child: S): S {
		val n = _children.size
		_assert(index <= n, "index is out of bounds.")
		_assert(child.parent == null, "Remove the child before adding it again.")
		_children.add(index, child)
		child.parent = this
		return child
	}

	/**
	 * Removes a child at the given index from this container.
	 * @return Returns true if a child was removed, or false if the index was out of range.
	 */
	override fun removeChild(index: Int): UpdatableChild {
		val c = _children
		val child = c.removeAt(index)
		child.parent = null
		return child
	}

	override fun clear() {
		_children.clear()
	}
}

/**
 * Invokes the callback on every frame. This is similar to [onTick] except the receiver isn't watched for activation
 * or disposal.
 */
fun Scoped.drive(update: (dT: Float) -> Unit): UpdatableChild {
	val child = object : UpdatableChildBase() {
		override fun update(dT: Float) {
			update(dT)
		}
	}
	FrameDriver.addChild(child)
	return child
}
