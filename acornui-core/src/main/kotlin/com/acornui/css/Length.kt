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

package com.acornui.css

/**
 * centimeters
 * Absolute String units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Double.cm: String
	get() = "${this}cm"

/**
 * millimeters
 * Absolute String units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Double.mm: String
	get() = "${this}mm"

/**
 * inches
 * Absolute String units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Double.inches: String
	get() = "${this}in"

/**
 * pixels
 * Pixels (px) are relative to the viewing device. For low-dpi devices, 1px is one device pixel (dot) of the display.
 * For printers and high resolution screens 1px implies multiple device pixels.
 */
val Double.px: String
	get() = "${this}px"

/**
 * points (1pt = 1/72 of 1in)
 *
 * Absolute String units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Double.pt: String
	get() = "${this}pt"

/**
 * picas (1pc = 12 pt)
 *
 * Absolute String units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Double.pc: String
	get() = "${this}pc"

/**
 * Relative to the font-size of the element (2em means 2 times the size of the current font)
 */
val Double.em: String
	get() = "${this}em"

/**
 * Relative to the x-height of the current font (rarely used)
 */
val Double.ex: String
	get() = "${this}ex"

/**
 * Relative to the width of the "0" (zero)
 */
val Double.ch: String
	get() = "${this}ch"

/**
 * Relative to font-size of the root element
 */
val Double.rem: String
	get() = "${this}rem"

/**
 * Relative to 1% of the width of the viewport
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Double.vw: String
	get() = "${this}vw"

/**
 * Relative to 1% of the height of the viewport
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Double.vh: String
	get() = "${this}vh"

/**
 * Relative to 1% of viewport's smaller dimension
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Double.vmin: String
	get() = "${this}vmin"

/**
 * Relative to 1% of viewport's larger dimension
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Double.vmax: String
	get() = "${this}vmax"

/**
 * Relative to the parent element
 */
val Double.percent: String
	get() = "${this}%"


// Int

/**
 * centimeters
 * Absolute String units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Int.cm: String
	get() = "${this}cm"

/**
 * millimeters
 * Absolute String units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Int.mm: String
	get() = "${this}mm"

/**
 * inches
 * Absolute String units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Int.inches: String
	get() = "${this}in"

/**
 * pixels
 * Pixels (px) are relative to the viewing device. For low-dpi devices, 1px is one device pixel (dot) of the display.
 * For printers and high resolution screens 1px implies multiple device pixels.
 */
val Int.px: String
	get() = "${this}px"

/**
 * points (1pt = 1/72 of 1in)
 *
 * Absolute String units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Int.pt: String
	get() = "${this}pt"

/**
 * picas (1pc = 12 pt)
 *
 * Absolute String units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Int.pc: String
	get() = "${this}pc"

/**
 * Relative to the font-size of the element (2em means 2 times the size of the current font)
 */
val Int.em: String
	get() = "${this}em"

/**
 * Relative to the x-height of the current font (rarely used)
 */
val Int.ex: String
	get() = "${this}ex"

/**
 * Relative to the width of the "0" (zero)
 */
val Int.ch: String
	get() = "${this}ch"

/**
 * Relative to font-size of the root element
 */
val Int.rem: String
	get() = "${this}rem"

/**
 * Relative to 1% of the width of the viewport
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Int.vw: String
	get() = "${this}vw"

/**
 * Relative to 1% of the height of the viewport
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Int.vh: String
	get() = "${this}vh"

/**
 * Relative to 1% of viewport's smaller dimension
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Int.vmin: String
	get() = "${this}vmin"

/**
 * Relative to 1% of viewport's larger dimension
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Int.vmax: String
	get() = "${this}vmax"

/**
 * Relative to the parent element
 */
val Int.percent: String
	get() = "${this}%"