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

package com.acornui.component.style

import com.acornui.component.UiComponent

object CommonStyleTags {

	/**
	 * Some components may be disabled, when they are, they are expected to add this tag.
	 */
	val disabled by cssClass()

	val toggled by cssClass()

	val active by cssClass()

	val hidden by cssClass()

	val popup by cssClass()
	val controlBar by cssClass()
}

var UiComponent.disabledTag: Boolean by CssClassToggle(CommonStyleTags.disabled)
