package com.acornui.core.focus

import com.acornui.component.ContainerImpl
import com.acornui.component.InteractivityMode
import com.acornui.component.alpha
import com.acornui.core.di.Owned
import com.acornui.component.atlas
import com.acornui.core.tween.driveTween
import com.acornui.core.tween.tweenAlpha
import com.acornui.graphic.ColorRo
import com.acornui.math.Bounds
import com.acornui.math.Easing
import com.acornui.math.Matrix4Ro
import com.acornui.math.MinMaxRo

open class SimpleHighlight(
		owner: Owned,
		atlasPath: String,
		regionName: String,
		private val animate: Boolean = true
) : ContainerImpl(owner) {

	private val highlight = addChild(atlas(atlasPath, regionName))

	init {
		interactivityMode = InteractivityMode.NONE
		includeInLayout = false
	}

	override fun onActivated() {
		super.onActivated()
		if (animate) {
			highlight.alpha = 0.3f
			driveTween(highlight.tweenAlpha(0.2f, Easing.pow2Out, 1f))
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val w = explicitWidth ?: 0f
		val h = explicitHeight ?: 0f
		val splits = highlight.region?.splits
		if (splits != null) {
			// left, top, right, bottom
			// If the highlight is a nine patch, offset the highlight by the padding. This allows for the ability to
			// curve around the highlighted target without cutting into it.
			highlight.setSize(w + splits[0] + splits[2], h + splits[1] + splits[3])
			highlight.moveTo(-splits[0], -splits[1])
		} else {
			highlight.setSize(w, h)
			highlight.moveTo(0f, 0f)
		}
	}
}