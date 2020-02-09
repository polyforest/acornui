package com.acornui.component.layout.algorithm

import com.acornui.component.layout.Spacer
import com.acornui.headless.HeadlessDependencies

class DummySpacer(private val name: String,
				  initialSpacerWidth: Float = 0f,
				  initialSpacerHeight: Float = 0f
) : Spacer(HeadlessDependencies.owner, initialSpacerWidth, initialSpacerHeight) {

	override fun toString(): String {
		return name
	}
}