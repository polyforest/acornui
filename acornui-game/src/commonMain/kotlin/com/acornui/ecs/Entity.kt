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

import com.acornui._assert
import com.acornui.assertionsEnabled
import com.acornui.UidUtil
import com.acornui.logging.Log
import com.acornui.serialization.*


/**
 * An entity is a bucket of components.
 * @author nbilyk
 */
class Entity(
		val id: String = UidUtil.createUid()
) {

	private val _components = HashMap<ComponentType<*>, Component>(16)

	val components: Map<ComponentType<*>, Component>
		get() = _components

	fun assertValid(): Boolean {
		if (!assertionsEnabled) return true
		_assert(components.isNotEmpty()) { "Entity is empty." }
		for (entry in components) {
			entry.value.assertValid()
		}
		return true
	}

	fun addComponents(vararg components: Component): Entity {
		for (i in 0..components.lastIndex) {
			addComponent(components[i])
		}
		return this
	}

	fun addComponents(components: List<Component>): Entity {
		for (i in 0..components.lastIndex) {
			addComponent(components[i])
		}
		return this
	}

	fun addComponent(component: Component): Entity {
		_assert(component.parentEntity == null, "Component must be removed first.")
		component.parentEntity = this
		_components[component.type] = component
		return this
	}

	fun removeComponent(component: Component) {
		if (component.parentEntity == this) {
			component.parentEntity = null
			_components.remove(component.type)
		}
	}

	/**
	 * Returns a component for the given subType.
	 * @param type The subType of component to retrieve.
	 *
	 * @return Returns the component for the given subType, or null if no such component has been added.
	 */
	fun <T : Component> getComponent(type: ComponentType<T>): T {
		return getOptionalComponent(type) ?: throw Exception("Does not contain component of type $type")
	}

	fun <T : Component> getOptionalComponent(type: ComponentType<T>): T? {
		@Suppress("UNCHECKED_CAST")
		return _components[type] as T?
	}

	/**
	 * @param component The component to search for.
	 *
	 * @return Returns true if the Entity containsKey the given component.
	 */
	fun containsComponent(component: Component): Boolean {
		return component.parentEntity === this
	}

	/**
	 * @param type The component class to search for.
	 * @return Returns true if the Entity containsKey a component for the class.
	 */
	fun containsComponent(type: ComponentType<*>): Boolean {
		return _components.containsKey(type)
	}

	operator fun <T : Component> T.unaryPlus(): T {
		addComponent(this)
		return this
	}

	operator fun List<Component>.unaryPlus() {
		addComponents(this)
	}

	operator fun Component.unaryMinus() {
		removeComponent(this)
	}

	/**
	 * Removes and disposes the entity.
	 * This also disposes all components on the entity.
	 */
	fun dispose() {
		for (entry in components) {
			entry.value.parentEntity = null
			entry.value.dispose()
		}
		_components.clear()
	}

	companion object {

		/**
		 * Creates an entity with a list of components.
		 */
		fun fromComponents(vararg components: Component): Entity {
			val entity = Entity()
			entity.addComponents(*components)
			return entity
		}
	}

}


class EntitySerializer(componentTypes: Array<SerializableComponentType<*>>) : To<Entity>, From<Entity> {

	private val componentSerializer = ComponentSerializer(componentTypes)

	override fun Entity.write(writer: Writer) {
		writer.string("id", id)
		val w = writer.property("components")
		w.array(true) {
			// Only serialize serializable components.
			for (v in components.values) {
				if (v.type is SerializableComponentType) {
					it.element().obj(true) {
						componentSerializer.write2(v, it)
					}
				}
			}
		}
	}

	override fun read(reader: Reader): Entity {
		val e = Entity(reader.string("id")!!)
		val c = reader.array2("components", componentSerializer)
		if (c != null) {
			e.addComponents(*c)
			for (entry in e.components) {
				if (entry.value.type == UnknownComponent) {
					// Attempt to recover from unknown components.
					Log.warn("Unknown or old component type: ${(entry.value as UnknownComponent).originalType}")
				}
			}
		}
		return e
	}
}

fun entity(init: Entity.() -> Unit = {}): Entity {
	val e = Entity()
	e.init()
	return e
}
