/*
 * Copyright 2019 Nicholas Bilyk
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

package com.acornui.component

import com.acornui.core.di.Owned


interface SingleElementContainer<T : UiComponent> : ContainerRo, Container {

	/**
	 * Sets the single element on this container. If there was previously an element set, it will be removed but
	 * not disposed.
	 */
	var element : T?

	/**
	 * Syntax sugar for addElement.
	 */
	operator fun <P : T> P.unaryPlus(): P {
		element = this
		return this
	}

	operator fun <P : T> P.unaryMinus(): P {
		element = null
		return this
	}
}

open class SingleElementContainerImpl<T : UiComponent>(owner: Owned) : ContainerImpl(owner), SingleElementContainer<T> {

	private var _element: T? = null
	override var element: T?
		get() = _element
		set(value) {
			if (value === _element) return
			val oldElement = _element
			_element = value
			onElementChanged(oldElement, value)
		}

	protected open fun onElementChanged(oldElement: T?, newElement: T?) {
		removeChild(oldElement)
		addOptionalChild(newElement)
	}
}