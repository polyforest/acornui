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
import com.acornui.component.style.cssProp

/**
 * Common CssProps many components use.
 */
object CssProps {

	val scrollbarTrackColor by cssProp()
	val scrollbarButtonColor by cssProp()
	val scrollbarBorderRadius by cssProp()
	val scrollbarCornerColor by cssProp()
	val scrollbarThickness by cssProp()

	/**
	 * The focus highlight color.
	 */
	val focus by cssProp()

	val focusThickness by cssProp()

	/**
	 * The accent for components when toggled/checked.
	 */
	val toggled by cssProp()

	/**
	 * The fill for components when toggled/checked.
	 */
	val toggledInner by cssProp()

	/**
	 * The fill for disabled components when toggled/checked.
	 */
	val toggledInnerDisabled by cssProp()

	/**
	 * The border for components.
	 */
	val borderColor by cssProp()

	/**
	 * The border for components when mouse is over.
	 */
	val borderHover by cssProp()

	/**
	 * The border for components when mouse is down.
	 */
	val borderActive by cssProp()

	/**
	 * The border for components when the component is disabled.
	 */
	val borderDisabled by cssProp()

	/**
	 * The background for components.
	 */
	val componentBackground by cssProp()

	/**
	 * The background for disabled components.
	 */
	val disabled by cssProp()

	val disabledOpacity by cssProp()

	val disabledInner by cssProp()

	/**
	 * The background of buttons and button-like components.
	 */
	val buttonBackground by cssProp()

	/**
	 * The :hover background of buttons and button-like components.
	 */
	val buttonBackgroundHover by cssProp()

	/**
	 * The [com.acornui.component.style.CommonStyleTags.active] background of buttons and button-like components.
	 */
	val buttonBackgroundActive by cssProp()

	val buttonTextColor by cssProp()

	val buttonTextHoverColor by cssProp()

	val buttonTextActiveColor by cssProp()

	val borderThickness by cssProp()

	val componentPadding by cssProp()

	val borderRadius by cssProp()

	/**
	 * The background for input components.
	 */
	val inputBackground by cssProp()

	/**
	 * The text color for input components. Default is same as the button text color.
	 */
	val inputTextColor by cssProp()

	/**
	 * The border radius for input components.
	 */
	val inputBorderRadius by cssProp()

	/**
	 * The padding for input components.
	 */
	val inputPadding by cssProp()

	/**
	 * The default gap between elements.
	 */
	val gap by cssProp()

	/**
	 * The default padding for containers.
	 */
	val padding by cssProp()

	/**
	 * A shadow for components.
	 */
	val componentShadow by cssProp()

	val strongWeight by cssProp()

	val dataRowEvenBackground by cssProp()
	val dataRowOddBackground by cssProp()

	val loadingSpinnerColor by cssProp()
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
