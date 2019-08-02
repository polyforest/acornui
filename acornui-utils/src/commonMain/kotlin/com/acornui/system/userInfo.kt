@file:JvmName("UserInfoUtils")

package com.acornui.system

import com.acornui.i18n.Locale
import com.acornui.observe.DataBinding
import com.acornui.observe.DataBindingImpl
import kotlin.jvm.JvmName

/**
 * A singleton reference to the user info. This does not need to be scoped; there can only be one machine.
 */
expect val userInfo: UserInfo

/**
 * Details about the user.
 */
data class UserInfo(

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
	val currentLocale: DataBinding<List<Locale>> = DataBindingImpl(systemLocale)

	init {
		currentLocale.bind {
			if (it.isEmpty()) throw Exception("currentLocale chain may not be empty.")
		}
	}

	val platform: Platform by lazy {
		val p = platformStr.toLowerCase()
		when {
			p.contains("android") -> Platform.ANDROID
			p.containsAny("os/2", "pocket pc", "windows", "win16", "win32", "wince") -> Platform.MICROSOFT
			p.containsAny("iphone", "ipad", "ipod", "mac", "pike") -> Platform.APPLE
			p.containsAny("linux", "unix", "mpe/ix", "freebsd", "openbsd", "irix") -> Platform.UNIX
			p.containsAny("sunos", "sun os", "solaris", "hp-ux", "aix") -> Platform.POSIX_UNIX
			p.contains("nintendo") -> Platform.NINTENDO
			p.containsAny("palmos", "webos") -> Platform.PALM
			p.containsAny("playstation", "psp") -> Platform.SONY
			p.contains("blackberry") -> Platform.BLACKBERRY
			p.containsAny("nokia", "s60", "symbian") -> Platform.SYMBIAN
			else -> Platform.OTHER
		}
	}

	override fun toString(): String {
		return "UserInfo(isBrowser=$isBrowser isMobile=$isMobile languages=${systemLocale.joinToString(",")})"
	}

	companion object {

		/**
		 * The string [platformStr] is set to when the platform could not be determined.
		 */
		const val UNKNOWN_PLATFORM = "unknown"
	}
}

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


// TODO: isMac, isWindows, isLinux

private fun String.containsAny(vararg string: String): Boolean {
	for (s in string) {
		if (this.contains(s)) return true
	}
	return false
}