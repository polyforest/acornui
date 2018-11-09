package com.acornui.component

interface ItemRendererRo<out E> {

	/**
	 * The data this item renderer represents.
	 */
	val data: E?
}

interface ItemRenderer<E> : ItemRendererRo<E> {

	/**
	 * The data this item renderer represents.
	 */
	override var data: E?
}