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

package com.acornui.component

import com.acornui.asset.loadText
import com.acornui.component.style.cssClass
import com.acornui.component.style.cssProp
import com.acornui.css.percent
import com.acornui.di.Context
import com.acornui.dom.add
import com.acornui.dom.addStyleToHead
import com.acornui.dom.computedStyleChanged
import com.acornui.dom.createElement
import com.acornui.function.as1
import com.acornui.graphic.Color
import com.acornui.input.*
import com.acornui.own
import com.acornui.skins.CssProps
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import org.w3c.dom.svg.SVGElement
import org.w3c.dom.svg.SVGStopElement
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 *
 */
class TintableSvg(owner: Context) : Div(owner) {

	init {
		addClass(TintableSvgStyle.tintableSvg)
		computedStyleChanged(TintableSvgStyle.tint.toString()).listen {
			refreshTint()
		}
		// Mutation observers can't handle pseudo selectors.
		touchStarted.listen(::refreshTint.as1)
		touchEnded.listen(::refreshTint.as1)
		mousePressed.listen(::refreshTint.as1)
		mouseReleased.listen(::refreshTint.as1)
		mouseEntered.listen(::refreshTint.as1)
		mouseExited.listen(::refreshTint.as1)
	}

	private val computed = window.getComputedStyle(dom)

	fun refreshTint() {
		tint = Color.fromStr(computed.getPropertyValue(TintableSvgStyle.tint.toString()))
	}

	var src: String = ""
		set(value) {
			own(launch {
				val text = loadText(value)
				val index = text.indexOf("<svg")
				dom.clear()
				val svg = createElement<SVGElement>("svg")
				dom.add(svg)
				svg.outerHTML = text.substring(index, text.length)

				val stops = dom.getElementsByTagNameNS("http://www.w3.org/2000/svg", "stop")
				for (i in 0 until stops.length) {
					val stop = stops.item(i).unsafeCast<SVGStopElement>()
					val stopColor = stop.style.getPropertyValue("stop-color")
					stop.style.removeProperty("stop-color")
					val color = Color.fromStr(stopColor)
					val hsl = color.toHsl()
					stop.style.setProperty(TintableSvgStyle.luminanceSelf.toString(), (hsl.l * 100.0).percent.toString())
					stop.style.setProperty(TintableSvgStyle.alphaSelf.toString(), hsl.a.toString())
				}
			})
		}

	private var tint: Color = Color.WHITE
		set(value) {
			if (field == value) return
			field = value
			val hsl = value.toHsl()
			style.setProperty(TintableSvgStyle.hue.toString(), hsl.h.toString())
			style.setProperty(TintableSvgStyle.saturation.toString(), hsl.s.toString())
			style.setProperty(TintableSvgStyle.luminance.toString(), hsl.l.toString())
			style.setProperty(TintableSvgStyle.alpha.toString(), hsl.a.toString())
		}
}

object TintableSvgStyle {

	val tintableSvg by cssClass()
	val icon by cssClass()

	val tint by cssProp()

	val luminanceSelf by cssProp()
	val alphaSelf by cssProp()

	val hue by cssProp()
	val saturation by cssProp()
	val luminance by cssProp()
	val alpha by cssProp()

	init {
		addStyleToHead(
			"""
$tintableSvg {
	$tint: white;
	display: inline-block;
}

$tintableSvg svg {
	width: inherit;
	height: inherit;
}

$tintableSvg stop {
	stop-color: hsla(var($hue, 0), calc(var($saturation, 0.0) * 100%), calc(var($luminance, 1.0) * var($luminanceSelf, 100%)), calc(var($alpha, 1.0) * var($alphaSelf, 1.0)));
}

$icon {
	$tint: ${CssProps.accentFill.v};
}

$icon:hover {
	$tint: ${CssProps.accentHover.v};
}

$icon:active {
	$tint: ${CssProps.accentActive.v};
}

		"""
		)
	}
}

inline fun Context.tintableSvg(init: ComponentInit<TintableSvg> = {}): TintableSvg {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return TintableSvg(this).apply(init)
}