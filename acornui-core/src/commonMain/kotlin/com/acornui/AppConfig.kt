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

package com.acornui

import com.acornui.di.Context
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import kotlinx.serialization.Serializable

/**
 * Application configuration common across back-end types.
 * @author nbilyk
 */
@Serializable
data class AppConfig(

		/**
		 * All relative files will be prepended with this string.
		 */
		val rootPath: String = "",

		/**
		 * The target number of frames per second.
		 * NB: This has no effect for browser; browser uses the window's animation frame.
		 */
		val frameRate: Int = 60,

		/**
		 * The properties for the Window.
		 */
		val window: WindowConfig = WindowConfig(),

		/**
		 * The Config for OpenGL properties. Only used in GL applications.
		 */
		val gl: GlConfig = GlConfig(),

		/**
		 * The config for Input properties.
		 */
		val input: InputConfig = InputConfig()
) {

	/**
	 * The number of seconds between each frame.
	 * (The inverse of [frameRate])
	 */
	val frameTime: Float
		get() = 1f / frameRate.toFloat()


	companion object : Context.Key<AppConfig>

}

@Serializable
data class WindowConfig(

		val title: String = "",

		/**
		 * The initial width of the window (For JS backends, set the width style on the root div instead).
		 */
		val initialWidth: Float = 800f,

		/**
		 * The initial height of the window (For JS backends, set the width style on the root div instead).
		 */
		val initialHeight: Float = 600f,

		/**
		 * The initial background color.
		 */
		val backgroundColor: ColorRo = Color(0xf1f2f3ff)


) {
	override fun toString(): String {
		return "WindowConfig(title='$title', initialWidth=$initialWidth, initialHeight=$initialHeight, backgroundColor=#${backgroundColor.toRgbaString()})"
	}
}

@Serializable
data class InputConfig(

		/**
		 * By default, keyboard input for the JS backend will be processed from the window. This is so that
		 * keyboard input can be processed immediately without first focusing the canvas.
		 * However, if there are other elements on the page that need to receive keyboard input, this can cause a
		 * problem.
		 * Set to false in order to only accept keyboard input from the canvas.
		 */
		val jsCaptureAllKeyboardInput: Boolean = true
)

@Serializable
data class GlConfig(

		/**
		 * Post-scene 4x4 MSAA.
		 */
		val antialias: Boolean = false,

		/**
		 * Use a depth buffer.
		 */
		val depth: Boolean = false,

		/**
		 * Applies to webgl only, if true, the canvas will be transparent.
		 * The stage background color should have transparency if this is true.
		 */
		val alpha: Boolean = false,

		/**
		 * Use a stencil buffer.
		 */
		val stencil: Boolean = true,

		/**
		 * Enable vertical sync.
		 */
		val vSync: Boolean = true
)

/**
 * A convenient way to get the scoped AppConfig.
 */
val Context.config: AppConfig
	get() = inject(AppConfig)

/**
 * The number of seconds between each time driver tick.
 */
val Context.tickTime: Float
	get() = config.frameTime
