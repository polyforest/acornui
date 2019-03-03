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

package com.acornui.core

import com.acornui.core.di.DKey
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.i18n.Locale
import com.acornui.core.observe.DataBindingImpl
import com.acornui.core.time.TimeDriver
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import kotlin.properties.Delegates

/**
 * Application configuration common across back-end types.
 * @author nbilyk
 */
data class AppConfig(

		/**
		 * The application's version. The version's build number will be set to the number within the file:
		 * `config.rootPath + "assets/build.txt"`
		 */
		val version: Version = Version(0, 1, 0, 0),

		/**
		 * All relative files will be prepended with this string.
		 */
		val rootPath: String = "",

		/**
		 * A flag for enabling various debugging features like debug logging.
		 * Don't set this to true directly, it will be automatically set to true if:
		 * On the JS backend debug=true exists as a querystring parameter.
		 * On the JVM backend -Ddebug=true exists as a vm parameter.
		 */
		val debug: Boolean = false,

		/**
		 * A flag for enabling debugging co-routines. This is useful if a co-routine is hanging and you wish
		 * to track down where it was invoked.
		 * Don't set this to true directly, it will be automatically set to true if:
		 * On the JS backend debugCoroutines=true exists as a querystring parameter.
		 * On the JVM backend -DdebugCoroutines=true exists as a vm parameter.
		 */
		val debugCoroutines: Boolean = false,

		/**
		 * The target number of frames per second.
		 */
		val frameRate: Int = 50,

		/**
		 * The configuration for the [com.acornui.core.time.TimeDriver] dependency.
		 */
		val timeDriverConfig: TimeDriverConfig = TimeDriverConfig(
				tickTime = 1f / frameRate.toFloat(),
				maxTicksPerUpdate = 30
		),

		/**
		 * The location of the files.json file created by the AcornUI assets task.
		 */
		val assetsManifestPath: String = "assets/files.json",

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

	companion object : DKey<AppConfig>
}

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
		val backgroundColor: ColorRo = Color(0xF1F2F3FF)

)

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

data class GlConfig(

		/**
		 * Post-scene 4x4 MSAA. May make text look blurry on certain systems.
		 */
		val antialias: Boolean = true,

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
 * A singleton reference to the user info. This does not need to be scoped; there can only be one machine.
 */
var userInfo: UserInfo by Delegates.notNull()

/**
 * Details about the user.
 */
data class UserInfo(

		val isTouchDevice: Boolean = false,

		val isBrowser: Boolean = false,
		val isDesktop: Boolean = false,

		val isMobile: Boolean = false,

		val userAgent: String,

		/**
		 * The operating system.
		 */
		val platformStr: String,

		/**
		 * The locale chain supplied by the operating system.
		 * @see currentLocale
		 */
		val systemLocale: List<Locale> = listOf()
) {

	/**
	 * The current Locale chain of the user. This may be set.
	 */
	val currentLocale = DataBindingImpl(systemLocale)

	init {
		currentLocale.bind {
			if (it.isEmpty()) throw Exception("currentLocale chain may not be empty.")
		}
	}

	override fun toString(): String {
		return "UserInfo(isTouchDevice=$isTouchDevice isBrowser=$isBrowser isMobile=$isMobile languages=${systemLocale.joinToString(",")})"
	}

	companion object : DKey<UserInfo> {

		/**
		 * The string [platformStr] is set to when the platform could not be determined.
		 */
		const val UNKNOWN_PLATFORM = "unknown"
	}
}

data class TimeDriverConfig(

		/**
		 * The number of seconds between each [com.acornui.core.time.TimeDriver] tick.
		 * The time driver is updated every frame by the application, and the default time driver implementation
		 * supports having a separated time tick from the framerate. This means that subscribers can rely on a fixed
		 * update interval, which is important for things such as physics-based animations.
		 *
		 * The default matches the framerate, but games may for example wish to divide this by 5 in order to have 5
		 * ticks per frame.
		 */
		val tickTime: Float,

		/**
		 * The maximum number of ticks per time driver update.
		 */
		val maxTicksPerUpdate: Int
)

/**
 * A way to get at the user's average internet bandwidth.
 */
object Bandwidth {
	// TODO: Calculate bandwidth

	/**
	 * Download speed, bytes per second.
	 */
	var downBps: Float = 196608f

	val downBpsInv: Float
		get() = 1f / downBps

	/**
	 * Upload speed, bytes per second.
	 */
	var upBps: Float = 196608f

}

// TODO: isMac, isWindows, isLinux

enum class Platform {
	ANDROID,
	APPLE,
	BLACKBERRY,
	PALM,
	MICROSOFT,
	UNIX,
	POSIX_UNIX,
	NINTENDO,
	SONY,
	SYMBIAN,
	OTHER
}

val platform: Platform by lazy {
	val p = userInfo.platformStr.toLowerCase()
	if (p.contains("android")) {
		Platform.ANDROID
	} else if (p.containsAny("os/2", "pocket pc", "windows", "win16", "win32", "wince")) {
		Platform.MICROSOFT
	} else if (p.containsAny("iphone", "ipad", "ipod", "mac", "pike")) {
		Platform.APPLE
	} else if (p.containsAny("linux", "unix", "mpe/ix", "freebsd", "openbsd", "irix")) {
		Platform.UNIX
	} else if (p.containsAny("sunos", "sun os", "solaris", "hp-ux", "aix")) {
		Platform.POSIX_UNIX
	} else if (p.contains("nintendo")) {
		Platform.NINTENDO
	} else if (p.containsAny("palmos", "webos")) {
		Platform.PALM
	} else if (p.containsAny("playstation", "psp")) {
		Platform.SONY
	} else if (p.contains("blackberry")) {
		Platform.BLACKBERRY
	} else if (p.containsAny("nokia", "s60", "symbian")) {
		Platform.SYMBIAN
	} else {
		Platform.OTHER
	}
}

private fun String.containsAny(vararg string: String): Boolean {
	for (s in string) {
		if (this.contains(s)) return true
	}
	return false
}

/**
 * A convenient way to get the scoped AppConfig.
 */
val Scoped.config: AppConfig
	get() = inject(AppConfig)

/**
 * The number of seconds between each time driver tick.
 */
val Scoped.tickTime: Float
	get() = inject(TimeDriver).config.tickTime

//enum class Browser {
//	NONE,
//	IE,
//	FX,
//	CH,
//	SF
//}