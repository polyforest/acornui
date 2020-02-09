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

package com.acornui.ecs

import com.acornui.Disposable

interface Component : Disposable {

	val type: ComponentType<*>

	var parentEntity: Entity?

	fun assertValid(): Boolean

	/**
	 * Returns the sibling component of the given type.
	 */
	fun <T : Component> getSibling(type: ComponentType<T>): T? {
		return parentEntity?.getComponent(type)
	}

	fun remove() {
		parentEntity?.removeComponent(this)
	}

	override fun dispose() {
		remove()
	}
}

/**
 * A Component is an abstract piece of data representing information about an Entity.

 * @author nbilyk
 */
abstract class ComponentBase : Component {

	/**
	 * Returns the entity to which the component is attached.
	 */
	override var parentEntity: Entity? = null

	protected open val requiredSiblings: Array<ComponentType<*>> = emptyArray()

	override fun assertValid(): Boolean {
		for (i in 0..requiredSiblings.lastIndex) {
			val requiredSibling = requiredSiblings[i]
			check(getSibling(requiredSibling) != null) { "$type is missing sibling: $requiredSibling" }
		}
		return true
	}
}

interface ComponentType<T : Component> {
}

class UnknownComponent(val originalType: String) : ComponentBase() {

	override val type: ComponentType<*> = UnknownComponent

	companion object : ComponentType<UnknownComponent>
}
