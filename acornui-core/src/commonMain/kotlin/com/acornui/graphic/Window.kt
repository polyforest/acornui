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

package com.acornui.graphic

import com.acornui.Disposable
import com.acornui.browser.Location
import com.acornui.di.DKey
import com.acornui.di.Scoped
import com.acornui.di.inject
import com.acornui.signal.Cancel
import com.acornui.signal.Signal


/**
 * @author nbilyk
 */
interface Window : Disposable {

	/**
	 * Dispatched when the window has requested to be closed.
	 */
	val closeRequested: Signal<(Cancel) -> Unit>

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
	val isVisible: Boolean

	/**
	 * Dispatched when this window size has changed, in screen coordinates.
	 * (newWidth, newHeight)
	 * This will not be dispatched in response to a [setSize] call.
	 */
	val sizeChanged: Signal<(Float, Float) -> Unit>

	/**
	 * Dispatched when this window scale has changed.
	 * (newWidth, newHeight)
	 * newWidth and newHeight are in points, not pixels.
	 */
	val scaleChanged: Signal<(Float, Float) -> Unit>

	/**
	 * The width of the window, in points.
	 */
	val width: Float

	/**
	 * The height of the window, in points.
	 */
	val height: Float

	val framebufferWidth: Int

	val framebufferHeight: Int

	/**
	 * The monitor content scale x factor.
	 */
	val scaleX: Float

	/**
	 * The monitor content scale y factor.
	 */
	val scaleY: Float

	/**
	 * Sets the size of the content area of this window, in screen coordinates, not pixels.
	 * @param width The width of the new content area.
	 * @param height The width of the new content area.
	 */
	fun setSize(width: Float, height: Float)

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
	 * If [force] is false, then a [closeRequested] signal will be dispatched, giving opportunity to cancel the close.
	 */
	fun requestClose(force: Boolean = false)

	/**
	 * Displays a native alert with a message.
	 */
	fun alert(message: String)

	/**
	 * Dispatched after the [fullScreen] status has changed.
	 */
	val fullScreenChanged: Signal<() -> Unit>

	/**
	 * Returns true if full screen mode is allowed.
	 */
	val fullScreenEnabled: Boolean

	/**
	 * Sets the full screen value.
	 * Note that in a browser, this may only be invoked in response to a user interaction.
	 */
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

/**
 * Requests that the application terminate.
 * @see Window.requestClose
 */
fun Scoped.exit() {
	inject(Window).requestClose()
}