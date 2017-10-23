package com.acornui.component

import com.acornui.core.di.Owned
import com.acornui.core.di.dKey

interface Rect : UiComponent {

	val style: BoxStyle

	companion object {
		val FACTORY_KEY = dKey<(owner: Owned) -> Rect>()
	}
}

fun Owned.rect(init: ComponentInit<Rect> = {}): Rect {
	val r = injector.inject(Rect.FACTORY_KEY)(this)
	r.init()
	return r
}