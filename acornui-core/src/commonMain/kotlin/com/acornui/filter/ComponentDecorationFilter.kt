package com.acornui.filter

import com.acornui.component.RenderContext
import com.acornui.component.RenderContextRo
import com.acornui.component.UiComponent
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.renderContext
import com.acornui.gl.core.GlState
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.MinMaxRo

class ComponentDecorationFilter(owner: Owned, private val component: UiComponent) : RenderFilterBase(owner) {

	private val _renderContext = RenderContext(inject(RenderContextRo))

	private val glState by GlState

	override fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		val contents = contents ?: return
		component.setSize(contents.width, contents.height)
		component.update() // Not typical, but the component is not a part of any display hierarchy.
		_renderContext.parentContext = renderContext
		_renderContext.colorTintOverride = component.naturalRenderContext.colorTint
		component.renderContextOverride = _renderContext

		val prev = glState.colorTransformation
		glState.colorTransformation = null
		component.render()
		glState.colorTransformation = prev
		contents.render()
	}
}