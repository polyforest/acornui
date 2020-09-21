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

@file:Suppress("CssOverwrittenProperties")

package com.acornui.skins

import com.acornui.component.Stage
import com.acornui.component.style.CssClass
import com.acornui.component.style.cssVar

/**
 * Common CssProps many components use.
 */
object CssProps {

	val scrollbarTrackColor by cssVar()
	val scrollbarButtonColor by cssVar()
	val scrollbarBorderRadius by cssVar()
	val scrollbarCornerColor by cssVar()
	val scrollbarThickness by cssVar()

	/**
	 * The focus highlight color.
	 */
	val focus by cssVar()

	val focusThickness by cssVar()

	/**
	 * The accent for components when toggled/checked.
	 */
	val accentFill by cssVar()

	/**
	 * The border for components when mouse is over.
	 */
	val accentHover by cssVar()

	/**
	 * The border for components when mouse is down.
	 */
	val accentActive by cssVar()

	/**
	 * The fill for components when toggled/checked.
	 */
	val toggledInner by cssVar()

	/**
	 * The fill for disabled components when toggled/checked.
	 */
	val toggledInnerDisabled by cssVar()

	/**
	 * The border for components.
	 */
	val borderColor by cssVar()

	/**
	 * The border for components when the component is disabled.
	 */
	val borderDisabled by cssVar()

	/**
	 * The background for components.
	 */
	val componentBackground by cssVar()

	/**
	 * The background for disabled components.
	 */
	val disabled by cssVar()

	val disabledOpacity by cssVar()

	val disabledInner by cssVar()

	/**
	 * The background of buttons and button-like components.
	 */
	val buttonBackground by cssVar()

	/**
	 * The :hover background of buttons and button-like components.
	 */
	val buttonBackgroundHover by cssVar()

	/**
	 * The [com.acornui.component.style.CommonStyleTags.active] background of buttons and button-like components.
	 */
	val buttonBackgroundActive by cssVar()

	val buttonTextColor by cssVar()

	val buttonTextHoverColor by cssVar()

	val buttonTextActiveColor by cssVar()

	val borderThickness by cssVar()

	val componentPadding by cssVar()

	val borderRadius by cssVar()

	/**
	 * The background for input components.
	 */
	val inputBackground by cssVar()

	/**
	 * The text color for input components. Default is same as the button text color.
	 */
	val inputTextColor by cssVar()

	/**
	 * The border radius for input components.
	 */
	val inputBorderRadius by cssVar()

	/**
	 * The padding for input components.
	 */
	val inputPadding by cssVar()

	/**
	 * The default gap between elements.
	 */
	val gap by cssVar()

	/**
	 * The default padding for containers.
	 */
	val padding by cssVar()

	/**
	 * A shadow for components.
	 */
	val componentShadow by cssVar()

	val strongWeight by cssVar()

	val dataRowEvenBackground by cssVar()
	val dataRowOddBackground by cssVar()

	val loadingSpinnerColor by cssVar()
}

private object ThemeClass

var Stage.theme: CssClass?
	get() = getAttachment(ThemeClass)
	set(value) {
		val oldTheme = theme
		if (oldTheme != null)
			removeClass(oldTheme)
		setAttachment(ThemeClass, value)
		if (value != null)
			addClass(value)
	}
