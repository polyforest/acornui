package com.acornui.core.nav

import com.acornui.browser.decodeUriComponent2
import com.acornui.browser.encodeUriComponent2
import com.acornui.collection.Clearable
import com.acornui.collection.copy
import com.acornui.component.Button
import com.acornui.component.ElementContainer
import com.acornui.component.UiComponent
import com.acornui.component.showAssetLoadingBar
import com.acornui.core.ChildRo
import com.acornui.core.Disposable
import com.acornui.core.Lifecycle
import com.acornui.core.LifecycleRo
import com.acornui.core.di.*
import com.acornui.core.focus.focusFirst
import com.acornui.core.input.interaction.click
import com.acornui.factory.LazyInstance
import com.acornui.factory.disposeInstance
import com.acornui.signal.Signal
import com.acornui.signal.Signal1


interface NavigationManager : Clearable, Disposable {

	val changed: Signal<(NavEvent) -> Unit>

	/**
	 * Returns a clone of the current path.
	 */
	fun path(): List<NavNode>

	/**
	 * Sets the current path.
	 * A [changed] signal will be invoked if the path has changed.
	 */
	fun path(path: List<NavNode>)

	/**
	 * Sets the path as an absolute url string.
	 * This should be in the form of
	 * baz/foo?param1=a&param2=b/bar?param1=a&param2=b
	 */
	fun path(value: String) {
		val split = value.split("/")
		val nodes = List(split.size, {
			NavNode.fromStr(split[it])
		})
		path(nodes)
	}

	override fun clear() {
		path(listOf())
	}

	fun push(node: NavNode) {
		path(path() + node)
	}

	fun push(nodes: Array<NavNode>) {
		path(path() + nodes)
	}

	fun pop() {
		val p = path()
		if (p.isNotEmpty()) {
			path(p.subList(0, p.lastIndex))
		}
	}

	fun pathToString(): String {
		return pathToString(path())
	}

	companion object : DKey<NavigationManager> {

		override fun factory(injector: Injector) = NavigationManagerImpl()

		fun pathToString(path: List<NavNode>): String {
			return "" + path.joinToString("/")
		}
	}
}

interface NavEvent {

	val oldPath: List<NavNode>

	val newPath: List<NavNode>

	val oldPathStr: String
		get() = NavigationManager.pathToString(oldPath)

	val newPathStr: String
		get() = NavigationManager.pathToString(newPath)
}

fun Scoped.navigate(absolutePath: String) {
	inject(NavigationManager).path(absolutePath)
}

data class NavNode(
		val name: String,
		val params: Map<String, String> = HashMap()
) {

	override fun toString(): String {
		if (params.isEmpty()) return encodeUriComponent2(name)
		var str = "${encodeUriComponent2(name)}?"
		var isFirst = true
		val orderedParams = params.entries.sortedBy { it.key }
		for (entry in orderedParams) {
			if (!isFirst) str += "&"
			else isFirst = false
			str += encodeUriComponent2(entry.key) + "=" + encodeUriComponent2(entry.value)
		}
		return str
	}

	companion object {
		fun fromStr(str: String): NavNode {
			val split = str.split("?")
			val params = HashMap<String, String>()
			val name = decodeUriComponent2(split[0])
			if (split.size > 1) {
				val paramsSplit = split[1].split("&")
				for (i in 0..paramsSplit.lastIndex) {
					val keyValue = paramsSplit[i].split("=")
					val key = decodeUriComponent2(keyValue[0])
					val value = decodeUriComponent2(keyValue[1])
					params[key] = value
				}
			}
			return NavNode(name, params)
		}
	}
}

class NavigationManagerImpl : NavigationManager {

	override val changed = Signal1<NavEvent>()

	private var lastPath: List<NavNode> = listOf()
	private var currentPath: List<NavNode> = listOf()
	private val event = NavEventImpl()

	private var isDispatching: Boolean = false

	override fun path(): List<NavNode> = currentPath.copy()

	override fun path(path: List<NavNode>) {
		if (lastPath == path) return
		currentPath = path
		if (isDispatching)
			return
		isDispatching = true
		event.oldPath.clear()
		event.oldPath.addAll(lastPath)
		event.newPath.clear()
		event.newPath.addAll(path)
		changed.dispatch(event)
		lastPath = path
		isDispatching = false
		path(currentPath)
	}

	override fun dispose() {
		changed.dispose()
	}
}

class NavEventImpl : NavEvent {
	override val oldPath = ArrayList<NavNode>()
	override val newPath = ArrayList<NavNode>()
}

interface NavBindable : ChildRo, Scoped

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
/**
 * A NavBinding watches the [NavigationManager] and invokes signals when the path section at the host's depth changes.
 */
class NavBinding(
		val host: NavBindable,
		val defaultPath: String) : Disposable {

	/**
	 * Dispatched when the navigation at the current level has changed.
	 */
	val changed = Signal1<NavBindingEvent>()

	private val event = NavBindingEvent()

	private var depth = -1

	private var path: String? = null
	private var params: Map<String, String> = emptyMap()

	private val activatedHandler = {
		c: LifecycleRo ->
		refreshDepth()
		navManager.changed.add(navChangedHandler)
		onNavChanged()
	}

	private val deactivatedHandler = {
		c: LifecycleRo ->
		depth = -1
		navManager.changed.remove(navChangedHandler)
	}

	private val disposedHandler = {
		c: LifecycleRo ->
		dispose()
	}

	private val navChangedHandler = {
		e: NavEvent ->
		onNavChanged()
	}

	private val navManager: NavigationManager = host.inject(NavigationManager)

	init {
		if (host is LifecycleRo) {
			host.activated.add(activatedHandler)
			host.deactivated.add(deactivatedHandler)
			host.disposed.add(disposedHandler)
			if (host.isActive) {
				refreshDepth()
				onNavChanged()
			}
		} else {
			navManager.changed.add(navChangedHandler)
			refreshDepth()
			onNavChanged()
		}
	}

	private fun refreshDepth() {
		depth = 0
		var p = host.parent
		while (p != null) {
			if (p is NavBindable) {
				depth++
			}
			p = p.parent
		}
	}

	private fun onNavChanged() {
		val fullPath = navManager.path()
		val oldPath = path
		val oldParams = params
		if (depth < fullPath.size) {
			this.path = fullPath[depth].name
		} else {
			this.path = null
		}
		if (path.isNullOrEmpty())
			path = defaultPath

		if (depth > 0 && depth - 1 < fullPath.size) {
			this.params = fullPath[depth - 1].params
		} else {
			this.params = emptyMap()
		}
		if (oldPath != path || oldParams != params) {
			event.oldParams = oldParams
			event.oldPath = oldPath
			event.newPath = path
			event.newParams = params
			changed.dispatch(event)
		}
	}

	fun navigate(path: String = defaultPath, params: Map<String, String> = hashMapOf()) = navigate(NavNode(path, params))

	fun navigate(node: NavNode) {
		if (depth >= 0) {
			val newPath = expandedPath
			newPath[depth] = node
			navManager.path(newPath)
		}
	}

	/**
	 * Sets the path as a url string.
	 * This should be in the form of
	 *
	 * Absolute:
	 * /foo?param1=val1&param2=val2/bar?param1=val1
	 * Relative:
	 * ./foo?param1=val1&param2=val2/bar?param1=val1
	 * foo?param1=val1&param2=val2/bar?param1=val1
	 * ../../foo?param1=val1&param2=val2/bar?param1=val1
	 */
	fun navigate(value: String) {
		val split = value.split("/")
		if (split.isEmpty()) {
			navManager.path(emptyList())
		} else {
			val relativeTo: List<NavNode>
			var up = 0
			if (split.size >= 2 && split[0] == "") {
				// Absolute
				relativeTo = emptyList()
				up = 1
			} else {
				while (up < split.size && split[up] == "..") {
					up++
				}
				val oldPath = expandedPath
				relativeTo = oldPath.subList(0, oldPath.size - up)
			}
			val nodes = Array(split.size - up, {
				NavNode.fromStr(split[it + up])
			})
			navManager.path(relativeTo + nodes)
		}
	}

	/**
	 * Returns the navigation manager's path, filled with empty nodes until it is as large as this binding's depth.
	 */
	private val expandedPath: MutableList<NavNode>
		get() {
			if (depth < 0) throw Exception("This binding is not currently active.")
			val fullPath = navManager.path()
			return (fullPath + List(maxOf(0, depth - fullPath.lastIndex), { NavNode("") })).toMutableList()
		}

	/**
	 * Invokes a callback when the path at this component's depth has changed to the given value
	 */
	fun bindPathEnter(path: String?, callback: () -> Unit) {
		changed.add({
			event ->
			if (event.newPath == path)
				callback()
		})
		if (this.path == path)
			callback()
	}

	/**
	 * Invokes a callback when the path at this component's depth has changed from the given value
	 */
	fun bindPathExit(path: String?, callback: () -> Unit) {
		changed.add({
			event: NavBindingEvent ->
			if (event.oldPath == path) callback()
		})
	}

	/**
	 * Invokes a callback when the specified parameter at this component's depth has changed.
	 */
	fun bindParam(key: String, callback: (oldValue: String?, newValue: String?) -> Unit) {
		changed.add({
			event: NavBindingEvent ->
			val oldValue = event.oldParams[key]
			val newValue = event.newParams[key]
			if (oldValue != newValue)
				callback(oldValue, newValue)
		})
	}

	override fun dispose() {
		navManager.changed.remove(navChangedHandler)
		if (host is Lifecycle) {
			host.activated.remove(activatedHandler)
			host.deactivated.remove(deactivatedHandler)
			host.disposed.remove(disposedHandler)
		}
		changed.dispose()
	}
}

class NavBindingEvent {
	var oldPath: String? = null
	var newPath: String? = null

	var oldParams: Map<String, String> = emptyMap()
	var newParams: Map<String, String> = emptyMap()
}

fun NavBindable.navBinding(defaultPath: String): NavBinding {
	return NavBinding(this, defaultPath)
}

fun ElementContainer<UiComponent>.navAddElement(nav: NavBinding, path: String?, component: UiComponent) {
	nav.bindPathEnter(path) { addElement(component) }
	nav.bindPathExit(path) { removeElement(component) }
}

fun ElementContainer<UiComponent>.navAddElement(nav: NavBinding, path: String?, showPreloader: Boolean = true, disposeOnRemove: Boolean = false, factory: Owned.() -> UiComponent) {
	val lazy = LazyInstance(this, factory)
	val c = this
	nav.bindPathEnter(path) {
		val child = lazy.instance

		if (showPreloader) {
			showAssetLoadingBar {
				if (!child.isDisposed) {
					c.addElement(child)
					child.focusFirst()
				}
			}
		} else {
			if (!child.isDisposed) {
				c.addElement(child)
				child.focusFirst()
			}
		}
	}
	nav.bindPathExit(path) {
		if (lazy.created) {
			c.removeElement(lazy.instance)
			if (disposeOnRemove) {
				lazy.disposeInstance()
			}
		}
	}
}

fun Button.bindToPath(nav: NavBinding, path: String) {
	click().add {
		nav.navigate("../$path")
	}
	nav.bindPathEnter(path) { toggled = (true) }
	nav.bindPathExit(path) { toggled = (false) }
}