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

package com.acornui.js.html

import org.w3c.dom.Element
import org.w3c.dom.events.UIEvent


open external class TouchEvent(typeArg: String) : UIEvent {

	open val ctrlKey: Boolean
	open val shiftKey: Boolean
	open val altKey: Boolean
	open val metaKey: Boolean
	open val changedTouches: Array<Touch>
	open val targetTouches: Array<Touch>
	open val touches: Array<Touch>
}

open external class Touch {

	open val target: Element
	open val clientX: Double
	open val clientY: Double
	open val force: Double
	open val identifier: Int
	open val pageX: Double
	open val pageY: Double
	open val radiusX: Double
	open val radiusY: Double
	open val rotationAngle: Double
	open val screenX: Int
	open val screenY: Int
}
