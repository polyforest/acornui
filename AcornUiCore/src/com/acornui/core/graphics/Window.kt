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

package com.acornui.core.graphics

import com.acornui.core.Disposable
import com.acornui.core.browser.Location
import com.acornui.core.di.DKey
import com.acornui.graphics.ColorRo
import com.acornui.signal.Signal


/**
 * @author nbilyk
 */
interface Window : Disposable {

	/**
	 * Dispatched when the [isActive] value has changed.
	 */
	val isActiveChanged: Signal<(Boolean) -> Unit>

	/**
	 * True if this window has focus.
	 */
	val isActive: Boolean

	/**
	 * Dispatched when the [isVisible] value has changed.
	 */
	val isVisibleChanged: Signal<(Boolean) -> Unit>

	/**
	 * True if this window is currently visible.
	 */
	fun isVisible(): Boolean

	/**
	 * Dispatched when this window size has changed.
	 * (newWidth, newHeight, isUserInteraction)
	 * The isUserInteraction parameter will be true if this window was resized by the user dragging the resize handles.
	 */
	val sizeChanged: Signal<(Float, Float, Boolean) -> Unit>

	val width: Float

	val height: Float

	val scaleChanged: Signal<(Float, Float) -> Unit>

	val scaleX: Float

	val scaleY: Float

	/**
	 * Sets the size of this window.
	 * Note, cameras will only automatically center if [Camera.autoCenter] is true.
	 * @see Camera.centerCamera
	 */
	fun setSize(width: Float, height: Float, isUserInteraction: Boolean)

	/**
	 * Sets the window's opaque background color.
	 */
	var clearColor: ColorRo

	/**
	 * If true, every frame will invoke a render. If false, only frames where [requestRender] has been called
	 * will trigger a render.
	 */
	var continuousRendering: Boolean

	/**
	 * True if a render and update has been requested, or if [continuousRendering] is true.
	 * @param clearRenderRequest If true, clears the flag set via [requestRender].
	 */
	fun shouldRender(clearRenderRequest: Boolean): Boolean

	/**
	 * Requests that a render and update should happen on the next frame.
	 */
	fun requestRender()

	/**
	 * Prepares this window for the next rendering frame. Clears the graphics canvas.
	 */
	fun renderBegin()

	/**
	 * Signifies the rendering completion of a frame. On desktop this will swap buffers.
	 */
	fun renderEnd()

	/**
	 * Returns true if this window has been asked to close.
	 */
	fun isCloseRequested(): Boolean

	/**
	 * Requests that this window close.
	 */
	fun requestClose()

	var fullScreen: Boolean

	val location: Location

	companion object : DKey<Window>

}


data class PopUpSpecs(

		/**
		 * The height of the window. Min. value is 100
		 */
		val height: Int? = null,

		/**
		 * The left position of the window. Negative values not allowed
		 */
		val left: Int? = null,

		val menuBar: Boolean? = null,

		/**
		 * Whether or not to add a status bar
		 */
		val status: Boolean? = null,

		/**
		 * Whether or not to display the title bar. Ignored unless the calling application is an HTML Application or a trusted dialog box
		 */
		val titlebar: Boolean? = null,


		/**
		 * The top position of the window. Negative values not allowed
		 */
		val top: Int? = null,

		/**
		 * The width of the window. Min. value is 100
		 */
		val width: Int? = null) {


	fun toSpecsString(): String {
		val strs = ArrayList<String>()
		if (height != null) strs.add("height=$height")
		if (left != null) strs.add("left=$left")
		if (menuBar != null) strs.add("menuBar=$menuBar")
		if (status != null) strs.add("status=$status")
		if (titlebar != null) strs.add("titlebar=$titlebar")
		if (top != null) strs.add("top=$top")
		if (width != null) strs.add("width=$width")
		return strs.joinToString(",")
	}
}