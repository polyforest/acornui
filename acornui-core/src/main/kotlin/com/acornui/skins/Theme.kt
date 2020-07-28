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

package com.acornui.skins

import com.acornui.css.css
import com.acornui.css.cssProp
import kotlinx.serialization.Serializable
import org.w3c.dom.HTMLStyleElement

/**
 * The Theme is a set of common styling properties, used by many components.
 */
@Serializable
data class Theme(

	/**
	 * The default background of the stage.
	 */
	val stageBackground: String = css("#444"),

	/**
	 * The default color of text.
	 */
	val textColor: String = css("#ddd"),

	val scrollbarTrackColor: String = css("#111"),
	val scrollbarButtonColor: String = css("#666"),
	val scrollbarCornerColor: String = css("#333"),
	val scrollbarThickness: String = css("8px"),

	/**
	 * The focus highlight color.
	 */
	val focus: String = css("rgba(49, 104, 254, .8)"),

	val focusThickness: String = css("2px"),

	/**
	 * The accent for components when toggled/checked.
	 */
	val toggled: String = css("#275efe"),

	/**
	 * The fill for components when toggled/checked.
	 */
	val toggledInner: String = css("#fff"),

	/**
	 * The fill for disabled components when toggled/checked.
	 */
	val toggledInnerDisabled: String = css("#888"),

	/**
	 * The border for components.
	 */
	val border: String = css("#bbc1e1"),

	/**
	 * The border for components when mouse is over.
	 */
	val borderHover: String = css("#0f3ef8"),

	/**
	 * The border for components when mouse is down.
	 */
	val borderActive: String = css("#445ed9"),

	/**
	 * The border for components when the component is disabled.
	 */
	val borderDisabled: String = css("#bbc1e1"),

	/**
	 * The background for components.
	 */
	val background: String = css("#fff"),

	/**
	 * The background for disabled components.
	 */
	val disabled: String = css("#7a7b82"),

	val disabledOpacity: String = css("0.8"),

	val disabledInner: String = css("#bfbfbf"),

	val buttonBackground: String = css("#f3f4f7"),

	val buttonBackgroundHover: String = css("#fff"),

	val buttonBackgroundActive: String = css("#e3e4e7"),

	val buttonTextColor: String = css("#333"),

	val buttonTextHoverColor: String = css("#444"),

	val buttonTextActiveColor: String = css("#222"),

	val borderThickness: String = css("1px"),

	val componentPadding: String = css("4px"),

	val borderRadius: String = css("8px"),

	/**
	 * The background for input components.
	 */
	val inputBackground: String = css("#fffe"),

	/**
	 * The text color for input components. Default is same as the button text color.
	 */
	val inputTextColor: String = buttonTextColor,

	/**
	 * The border radius for input components.
	 */
	val inputBorderRadius: String = css("2px"),

	/**
	 * The padding for input components.
	 */
	val inputPadding: String = css("2px"),

	/**
	 * The default gap between elements.
	 */
	val gap: String = css("6px"),

	/**
	 * The default padding for containers.
	 */
	val padding: String = css("6px"),

	/**
	 * A shadow for components.
	 */
	val componentShadow: String = css("1px 1px 3px rgba(0, 0, 0, 0.2)"),

	/**
	 * The background of a panel.
	 */
	val panelBackground: String = css("#ddd"),

	val panelTextColor: String = css("#222"),
	val panelScrollbarTrackColor: String = css("transparent"),
	val panelScrollbarButtonColor: String = css("#666"),
	val panelScrollbarCornerColor: String = css("transparent"),

	/**
	 * The box-shadow for panels.
	 */
	val panelShadow: String = css("2px 2px 5px rgba(0, 0, 0, 0.4)"),

	val dataRowEvenBackground: String = css("#eeef"),
	val dataRowOddBackground: String = css("#ddde")

) {

	fun toCss(selector: String = ":root"): String {
		//language=CSS
		return """
			$selector {

			  ${cssProp(::stageBackground)}
			  ${cssProp(::textColor)}
			  ${cssProp(::scrollbarTrackColor)}
			  ${cssProp(::scrollbarButtonColor)}
			  ${cssProp(::scrollbarCornerColor)}
			  ${cssProp(::scrollbarThickness)}
			  
			  ${cssProp(::focus)}
			  ${cssProp(::focusThickness)}
			  ${cssProp(::toggled)}
			  ${cssProp(::toggledInner)}
			  ${cssProp(::toggledInnerDisabled)}
			  
			  ${cssProp(::border)}
			  ${cssProp(::borderHover)}
			  ${cssProp(::borderActive)}
			  ${cssProp(::borderDisabled)}
			  
			  ${cssProp(::background)}
			  ${cssProp(::disabled)}
			  ${cssProp(::disabledOpacity)}
			  ${cssProp(::disabledInner)}

			  ${cssProp(::buttonBackground)}
			  ${cssProp(::buttonBackgroundHover)}
			  ${cssProp(::buttonBackgroundActive)}
			  
			  
			  ${cssProp(::buttonTextColor)}
			  ${cssProp(::buttonTextHoverColor)}
			  ${cssProp(::buttonTextActiveColor)}
			  
			  ${cssProp(::borderThickness)}
			  ${cssProp(::componentPadding)}
			  ${cssProp(::borderRadius)}
			  ${cssProp(::inputBackground)}
			  ${cssProp(::inputTextColor)}
			  ${cssProp(::inputBorderRadius)}
			  ${cssProp(::inputPadding)}
			  
			  ${cssProp(::gap)}
			  ${cssProp(::padding)}
			  
			  ${cssProp(::componentShadow)}
			  
			  ${cssProp(::panelBackground)}
			  ${cssProp(::panelTextColor)}
			  ${cssProp(::panelScrollbarTrackColor)}
			  ${cssProp(::panelScrollbarButtonColor)}
			  ${cssProp(::panelScrollbarCornerColor)}
			  ${cssProp(::panelShadow)}
			  
			  ${cssProp(::dataRowEvenBackground)}
			  ${cssProp(::dataRowOddBackground)}

			  
			}
		""".trimIndent()
	}
}

fun Theme.addCssToHead(selector: String = ":root"): HTMLStyleElement {
	DefaultStyles // Adds the default css to the head.
	return com.acornui.dom.addCssToHead(toCss(selector))
}

val darkTheme = Theme(

	stageBackground = css("#222"),

	textColor = css("#ccc"),

	toggledInner = css("#fff"),

	border = css("#353535"),

	background = css("rgba(33, 33, 33, 0.9)"),

	buttonBackground = css("rgb(43 43 43 / 90%)"),

	buttonBackgroundHover = css("rgb(45 45 45 / 90%)"),

	buttonBackgroundActive = css("rgb(39 39 39 / 90%)"),

	buttonTextColor = css("#ccc"),

	buttonTextHoverColor = css("#ddd"),

	buttonTextActiveColor = css("#bbb"),

	inputBackground = css("rgba(33, 33, 33, 0.9)"),

	componentShadow = css("1px 1px 3px rgba(0, 0, 0, 0.8)"),

	panelBackground = css("rgba(30,51,88,0.39)"),

	panelShadow = css("2px 2px 5px rgba(0, 0, 0, 0.9)"),

	dataRowEvenBackground = css("#242424"),
	dataRowOddBackground = css("#222")
)