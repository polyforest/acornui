/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.component.layout

import com.acornui.component.*
import com.acornui.core.di.Owned

/**
 * A component with no rendering, just used to take up whitespace.
 */
open class Spacer(
		owner: Owned,
		initialSpacerWidth: Float = 0f,
		initialSpacerHeight: Float = 0f
) : UiComponentImpl(owner) {

	init {
		defaultWidth = initialSpacerWidth
		defaultHeight = initialSpacerHeight
		interactivityMode = InteractivityMode.NONE
	}

}

fun Owned.spacer(width: Float = 0f, height: Float = 0f, init: ComponentInit<Spacer> = {}): Spacer {
	val s = Spacer(this, width, height)
	s.init()
	return s
}