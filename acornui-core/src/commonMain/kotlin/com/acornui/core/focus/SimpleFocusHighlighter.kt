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

package com.acornui.core.focus

import com.acornui.component.ContainerImpl
import com.acornui.component.InteractivityMode
import com.acornui.component.atlas
import com.acornui.core.di.Owned
import com.acornui.math.Bounds

open class SimpleHighlight(
		owner: Owned,
		atlasPath: String,
		regionName: String
) : ContainerImpl(owner) {

	private val highlight = addChild(atlas(atlasPath, regionName))

	init {
		interactivityMode = InteractivityMode.NONE
		includeInLayout = false
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
