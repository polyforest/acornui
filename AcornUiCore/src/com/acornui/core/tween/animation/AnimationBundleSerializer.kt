/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.core.tween.animation

import com.acornui.core.toUnderscoreCase
import com.acornui.math.Vector2
import com.acornui.math.Vector2Ro
import com.acornui.serialization.*

object AnimationBundleSerializer : From<AnimationBundle> {

	override fun read(reader: Reader): AnimationBundle {
		val library = HashMap<String, LibraryItem>()
		reader["library"]!!.forEach {
			name, reader ->
			val type = reader.string("type")!!
			library[name] = when (type) {
				"image" -> reader.obj(ImageLibraryItemSerializer)!!
				"atlas" -> reader.obj(AtlasLibraryItemSerializer)!!
				"animation" -> reader.obj(AnimationLibraryItemSerializer)!!
				else -> throw Exception("Unknown library item type $type")
			}
		}
		return AnimationBundle(
				library = library,
				easings = reader.map("easings", AnimationEasingSerializer)!!)
	}
}

object ImageLibraryItemSerializer : From<ImageLibraryItem> {
	override fun read(reader: Reader): ImageLibraryItem {
		return ImageLibraryItem(path = reader.string("path")!!)
	}
}

object AtlasLibraryItemSerializer : From<AtlasLibraryItem> {
	override fun read(reader: Reader): AtlasLibraryItem {
		return AtlasLibraryItem(
				atlasPath = reader.string("atlasPath")!!,
				regionName = reader.string("regionName")!!
		)
	}
}

object AnimationLibraryItemSerializer : From<AnimationLibraryItem> {
	override fun read(reader: Reader): AnimationLibraryItem {
		return AnimationLibraryItem(
				timeline = reader.obj("timeline", TimelineSerializer)!!
		)
	}
}

object TimelineSerializer : From<Timeline> {

	override fun read(reader: Reader): Timeline {
		return Timeline(
				duration = reader.float("duration")!!,
				layers = reader.arrayList("layers", LayerSerializer)!!
		)
	}
}

object LayerSerializer : From<Layer> {

	override fun read(reader: Reader): Layer {
		return Layer(
				name = reader.string("name") ?: "",
				symbolName = reader.string("symbolName")!!,
				visible = reader.bool("visible") ?: true,
				keyFrames = reader.arrayList("keyFrames", KeyFrameSerializer)!!
		)
	}
}

object KeyFrameSerializer : From<KeyFrame> {
	override fun read(reader: Reader): KeyFrame {
		val props = HashMap<PropType, Prop>()
		reader["props"]!!.forEach {
			propName, reader ->
			val prop = if (propName != propName.toUpperCase()) {
				PropType.valueOf(propName.toUnderscoreCase().toUpperCase())
			} else {
				PropType.valueOf(propName)
			}
			props[prop] = PropSerializer.read(reader)
		}

		return KeyFrame(
				time = reader.float("time")!!,
				easings = reader.map("easings", AnimationEasingSerializer)!!,
				props = props
		)
	}
}

object PropSerializer : From<Prop> {

	override fun read(reader: Reader): Prop {
		return Prop(
				value = reader.float("value")!!,
				easing = reader.string("easing")
		)
	}
}

object AnimationEasingSerializer : From<AnimationEasing> {

	override fun read(reader: Reader): AnimationEasing {
		val arr = reader.floatArray()!!
		val points = ArrayList<Vector2Ro>()
		for (i in 0..arr.lastIndex step 2) {
			points.add(Vector2(arr[i], arr[i + 1]))
		}
		return AnimationEasing(points = points)
	}
}