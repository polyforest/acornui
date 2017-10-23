package com.acornui.core


/**
 * A major.minor.patch.build representation
 *
 * MAJOR version when you make incompatible API changes,
 * MINOR version when you add functionality in a backwards-compatible manner, and
 * PATCH version when you make backwards-compatible bug fixes.
 * BUILD version automatically incremented on a build.
 */
data class Version(
		val major: Int,
		val minor: Int,
		val patch: Int,
		val build: Int
) : Comparable<Version> {

	override fun compareTo(other: Version): Int {
		val c1 = major.compareTo(other.major)
		if (c1 != 0) return c1
		val c2 = minor.compareTo(other.minor)
		if (c2 != 0) return c2
		val c3 = patch.compareTo(other.patch)
		if (c3 != 0) return c3
		val c4 = build.compareTo(other.build)
		if (c4 != 0) return c4
		return 0
	}

	fun isApiCompatible(other: Version): Boolean {
		return major == other.major && minor == other.minor
	}

	fun toVersionString(): String {
		return "$major.$minor.$patch.$build"
	}

	override fun toString(): String = toVersionString()

	companion object {
		fun fromStr(value: String): Version {
			val split = value.split(".")
			if (split.size != 4) throw IllegalArgumentException("Version '$value' is not in the format major.minor.patch.build")
			val v = Version(major = split[0].toInt(), minor = split[1].toInt(), patch = split[2].toInt(), build = split[3].toInt())
			return v
		}
	}
}