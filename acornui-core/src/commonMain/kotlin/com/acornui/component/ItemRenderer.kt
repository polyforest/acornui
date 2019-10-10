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

import com.acornui.EqualityCheck
import com.acornui.component.layout.ListItemRenderer
import com.acornui.recycle.ObjectPool
import com.acornui.recycle.Pool
import com.acornui.recycle.recycle

interface ItemRendererRo<out E> : UiComponentRo {

	/**
	 * The data this item renderer represents.
	 */
	val data: E?
}

interface ItemRenderer<E> : ItemRendererRo<E>, UiComponent {

	/**
	 * The data this item renderer represents.
	 */
	override var data: E?
}

/**
 * Recycles a list of item renderers, creating or disposing renderers only as needed.
 * This method will automatically add and remove the renderers from the receiver element container.
 *
 * @receiver The element container on which to add item renderers.
 * @param data The updated set of data items.
 * @param existingElements The stale list of item renderers. This will be modified to reflect the new item renderers.
 * @param configure If set, when the element is recycled, configure will be called after [ListItemRenderer.data] has
 * been set.
 * @param unconfigure If set, when the element is returned to a managed object pool, unconfigure will be called.
 * ([ListItemRenderer.data] will automatically be set to null)
 * @param equality If set, uses custom equality rules. This guides how to know whether an item can be recycled or not.
 * @param factory Used to create new item renderers as needed.
 */
fun <E, T : ItemRenderer<E>> ElementContainer<T>.recycleItemRenderers(
		data: Iterable<E>?,
		existingElements: MutableList<T> = elements,
		configure: (element: T, item: E, index: Int) -> Unit = { _, _, _ -> },
		unconfigure: (element: T) -> Unit = {},
		equality: EqualityCheck<E?> = { a, b -> a == b },
		factory: ElementContainer<T>.() -> T
) {
	@Suppress("UNCHECKED_CAST")
	val pool = createOrReuseAttachment(RendererPoolKey(factory)) {
		ObjectPool { factory() }
	} as Pool<T>
	recycle(
			data,
			existingElements,
			factory = { _, _ -> pool.obtain() },
			configure = {
				element, item, index ->
				if (element.data != item)
					element.data = item
				addElement(index, element)
				configure(element, item, index)
			},
			disposer = {
				unconfigure(it)
				it.data = null
				pool.free(it)
			},
			retriever = { it.data },
			equality = equality
	)
}

internal data class RendererPoolKey(val factory: Any)