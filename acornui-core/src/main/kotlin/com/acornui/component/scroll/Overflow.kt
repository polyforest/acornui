/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.component.scroll

object Overflow {

	/**
	 * The overflow is not clipped. The content renders outside the element's box.
	 */
	const val VISIBLE = "visible"

	/**
	 * The overflow is clipped, and the rest of the content will be invisible.
	 */
	const val HIDDEN = "hidden"

	/**
	 * The overflow is clipped, and a scrollbar is added to see the rest of the content.
	 */
	const val SCROLL = "scroll"

	/**
	 * Similar to scroll, but it adds scrollbars only when necessary.
	 */
	const val AUTO = "auto"

}