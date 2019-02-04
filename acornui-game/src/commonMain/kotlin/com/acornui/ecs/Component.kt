package com.acornui.ecs

import com.acornui._assert
import com.acornui.core.Disposable
import com.acornui.serialization.*

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

	protected open val requiredSiblings: Array<SerializableComponentType<*>> = emptyArray()

	override fun assertValid(): Boolean {
		for (i in 0..requiredSiblings.lastIndex) {
			val requiredSibling = requiredSiblings[i]
			_assert(getSibling(requiredSibling) != null) { "$type is missing sibling: $requiredSibling" }
		}
		return true
	}


}

interface ComponentType<T : Component> {
}

interface SerializableComponentType<T : Component> : ComponentType<T>, From<T>, To<T> {

	/**
	 * The ID of this component type, used for serialization.
	 */
	val name: String

}

class UnknownComponent(val originalType: String) : ComponentBase() {

	override val type: SerializableComponentType<*> = UnknownComponent

	companion object : SerializableComponentType<UnknownComponent> {
		override val name: String = "_Unknown_"

		override fun UnknownComponent.write(writer: Writer) {
			writer.string("originalType", originalType)
		}

		override fun read(reader: Reader): UnknownComponent {
			val originalType = reader.string("originalType") ?: reader.string("__type") ?: "UnknownType"
			return UnknownComponent(originalType)
		}
	}
}

class ComponentSerializer(
		val componentTypes: Array<SerializableComponentType<*>>
) : To<Component>, From<Component> {

	override fun Component.write(writer: Writer) {
		@Suppress("UNCHECKED_CAST")
		val type = type as SerializableComponentType<Component>
		writer.string("__type", type.name)
		type.write2(this, writer)
	}

	override fun read(reader: Reader): Component {
		val name = reader.string("__type")
		val componentType = componentTypes.find { it.name == name } ?: UnknownComponent
		return componentType.read(reader)
	}
}

