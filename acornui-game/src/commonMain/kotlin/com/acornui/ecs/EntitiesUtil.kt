package com.acornui.ecs

import com.acornui.core.di.Scoped
import com.acornui.core.mvc.Command
import com.acornui.core.mvc.CommandType
import com.acornui.core.mvc.Commander
import com.acornui.core.mvc.invokeCommand

class AddEntity(val entity: Entity) : Command {

	override val type = Companion

	companion object : CommandType<AddEntity>
}

class RemoveEntity(val entity: Entity) : Command {

	override val type = Companion

	companion object : CommandType<RemoveEntity>
}

inline fun watchEntities(entities: List<Entity>, cmd: Commander, crossinline initEntity: (e: Entity) -> Unit, crossinline disposeEntity: (Entity) -> Unit) {
	for (i in 0..entities.lastIndex) {
		val e = entities[i]
		initEntity(e)
	}
	cmd.onCommandInvoked(AddEntity) {
		initEntity(it.entity)
	}
	cmd.onCommandInvoked(RemoveEntity) {
		disposeEntity(it.entity)
	}
}

fun Scoped.addEntity(e: Entity) {
	invokeCommand(AddEntity(e))
}

fun Scoped.addEntity(init: Entity.() -> Unit) {
	val e = entity()
	e.init()
	invokeCommand(AddEntity(e))
}

fun Scoped.removeEntity(e: Entity) {
	invokeCommand(RemoveEntity(e))
}

fun entitiesList(entities: List<Entity>, cmd: Commander, filtered: MutableList<Entity>, componentType: ComponentType<*>) {
	watchEntities(entities, cmd, {
		if (it.containsComponent(componentType)) {
			filtered.add(it)
		}
	}, {
		if (it.containsComponent(componentType)) {
			filtered.remove(it)
		}
	})
}

fun <T : Component> componentList(entities: List<Entity>, cmd: Commander, filtered: MutableList<T>, componentType: ComponentType<T>) {
	watchEntities(entities, cmd, {
		if (it.containsComponent(componentType)) {
			filtered.add(it.getComponent(componentType))
		}
	}, {
		if (it.containsComponent(componentType)) {
			filtered.remove(it.getComponent(componentType))
		}
	})
}