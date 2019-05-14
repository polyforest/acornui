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

import com.acornui.core.EqualityCheck

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
 * @param data The updated list of data items.
 * @param existingElements The stale list of item renderers. This will be modified to reflect the new item renderers.
 * @param factory Used to create new item renderers as needed. [configure] will be called after factory to configure
 * the new element.
 * @param configure Used to configure the element.
 * @param disposer Used to dispose the element.
 * @param equality If set, uses custom equality rules. This guides how to know whether an item can be recycled or not.
 */
fun <E, T : ItemRendererRo<E>> recycle(
		data: List<E>?,
		existingElements: MutableList<T>,
		factory: (item: E, index: Int) -> T,
		configure: (element: T, item: E, index: Int) -> Unit,
		disposer: (element: T) -> Unit,
		equality: EqualityCheck<E?> = { a, b -> a == b }
) = com.acornui.recycle.recycle(data, existingElements, factory, configure, disposer, { it.data }, equality)
