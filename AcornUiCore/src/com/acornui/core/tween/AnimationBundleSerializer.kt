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

package com.acornui.core.tween

import com.acornui.core.toUnderscoreCase
import com.acornui.serialization.*

object AnimationBundleSerializer : From<AnimationBundle> {

	override fun read(reader: Reader): AnimationBundle {
		return AnimationBundle(
				library = 0,
				easings = reader.map("easings", AnimationEasingSerializer)!!,
				animations = reader.arrayList("animations", AnimationSerializer)!!)
	}
}

object AnimationEasingSerializer : From<AnimationEasing> {

	override fun read(reader: Reader): AnimationEasing {
		return AnimationEasing(curve = reader.floatArray()!!.toList())
	}
}

object AnimationSerializer : From<Animation> {
	override fun read(reader: Reader): Animation {
		return Animation(
				name = reader.string("name") ?: "",
				timeline = reader.obj("timeline", TimelineSerializer)!!
		)
	}
}

object TimelineSerializer : From<Timeline> {

	override fun read(reader: Reader): Timeline {
		return Timeline(
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