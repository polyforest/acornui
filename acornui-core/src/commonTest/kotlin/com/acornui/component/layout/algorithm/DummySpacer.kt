package com.acornui.component.layout.algorithm

import com.acornui.component.layout.Spacer
import com.acornui.headless.HeadlessInjector

class DummySpacer(private val name: String,
				  initialSpacerWidth: Float = 0f,
				  initialSpacerHeight: Float = 0f
) : Spacer(HeadlessInjector.owner, initialSpacerWidth, initialSpacerHeight) {

	override fun toString(): String {
		return name
	}
}