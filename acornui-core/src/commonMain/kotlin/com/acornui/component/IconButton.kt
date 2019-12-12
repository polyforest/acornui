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

package com.acornui.component

import com.acornui.collection.mapTo
import com.acornui.component.style.StyleTag
import com.acornui.di.Owned
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class IconButton(
		owner: Owned
) : ButtonImpl(owner), SingleElementContainer<UiComponent> {

	init {
		styleTags.add(IconButton)
	}

	private var _iconMap: Map<ButtonState, UiComponent>? = null

	/**
	 * Sets a map of icons to use.
	 * @throws IllegalArgumentException If the map does not contain the key [ButtonState.UP]
	 */
	fun iconMap(map: Map<ButtonState, UiComponent>) {
		require(map.containsKey(ButtonState.UP)) { "iconMap must at least set the icon for the UP state." }
		element = null
		_iconMap = map
		refreshContents()
	}

	override var element: UiComponent? by validationProp(null, ValidationFlags.PROPERTIES)

	override fun updateProperties() {
		super.updateProperties()
		refreshContents()
	}

	private var _contentsContainer: SingleElementContainer<UiComponent>? = null

	private fun getContents(): UiComponent? {
		val iconMap = _iconMap
		if (iconMap != null) {
			return currentState.fallbackWalk {
				iconMap[it]
			}
		}
		return element
	}

	private fun refreshContents() {
		val contents = getContents()
		@Suppress("UNCHECKED_CAST")
		val currentContentsContainer = skin as? SingleElementContainer<UiComponent>
		if (currentContentsContainer != null && currentContentsContainer.element != contents) {
			_contentsContainer?.element = null
			_contentsContainer = currentContentsContainer
			currentContentsContainer.element = contents
		}
	}

	companion object : StyleTag
}

inline fun Owned.iconButton(init: ComponentInit<IconButton> = {}): IconButton  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = IconButton(this)
	b.init()
	return b
}

inline fun Owned.iconButton(imagePath: String, init: ComponentInit<IconButton> = {}): IconButton  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val button = IconButton(this).apply {
		element = iconImage(imagePath)
	}
	button.init()
	return button
}

inline fun Owned.iconButton(atlasPath: String, region: String, init: ComponentInit<IconButton> = {}): IconButton  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val button = IconButton(this).apply {
		element = iconAtlas(atlasPath, region)
	}
	button.init()
	return button
}

inline fun Owned.iconButton(atlasPath: String, regions: Map<ButtonState, String>, init: ComponentInit<IconButton> = {}): IconButton  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = IconButton(this)
	b.iconMap(regions.mapTo { key, value ->
		key to iconAtlas(atlasPath, value)
	})
	b.init()
	return b
}