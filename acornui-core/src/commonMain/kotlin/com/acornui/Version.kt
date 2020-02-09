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
import com.acornui.di.DKey
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

/**
 * A major.minor.patch.build representation
 *
 * MAJOR version increments when there is a major release.
 * MINOR version increments when there are incompatible api changes.
 * PATCH version when there are no incompatible api changes.
 * BUILD version automatically incremented on a build.
 */
@Serializable(with = VersionSerializer::class)
data class Version(
		val major: Int,
		val minor: Int,
		val patch: Int,
		val build: Int = 0
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
		return if (build == 0) "$major.$minor.$patch"
		else "$major.$minor.$patch.$build"
	}

	override fun toString(): String = toVersionString()

	companion object : DKey<Version> {
		fun fromStr(value: String): Version {
			val split = value.split(".")
			if (split.size != 4 && split.size != 3) throw IllegalArgumentException("Version '$value' is not in the format major.minor.patch.[build]")
			return Version(major = split[0].toInt(), minor = split[1].toInt(), patch = split[2].toInt(), build = split.getOrNull(3)?.toInt() ?: 0)
		}
	}
}

val Context.version: Version
	get() = inject(Version)

@Serializer(forClass = Version::class)
object VersionSerializer : KSerializer<Version> {

	override val descriptor: SerialDescriptor =
			StringDescriptor.withName("VersionSerializer")

	override fun serialize(encoder: Encoder, obj: Version) {
		encoder.encodeString(obj.toVersionString())
	}

	override fun deserialize(decoder: Decoder): Version {
		return Version.fromStr(decoder.decodeString())
	}
}