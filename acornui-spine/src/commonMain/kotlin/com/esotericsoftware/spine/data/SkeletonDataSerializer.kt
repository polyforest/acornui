/*
 * Copyright 2016 Nicholas Bilyk
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

package com.esotericsoftware.spine.data

import com.acornui.collection.stringMapOf
import com.acornui.core.graphic.BlendMode
import com.acornui.graphic.Color
import com.acornui.serialization.*
import com.esotericsoftware.spine.data.animation.AnimationData
import com.esotericsoftware.spine.data.animation.FfdtKey
import com.esotericsoftware.spine.data.animation.timeline.*
import com.esotericsoftware.spine.data.attachments.*


object SkeletonDataSerializer : From<SkeletonData> {

	override fun read(reader: Reader): SkeletonData {
		val sk = reader["skeleton"]!!

		val name = sk.string("name")
		val width = sk.float("width")!!
		val height = sk.float("height")!!
		val version = sk.string("version")
		val imagesPath = sk.string("images")
		val bones = reader.array2("bones", BoneDataSerializer) ?: arrayOf()
		val events = reader.map("events", SpineEventDefaultsSerializer) ?: hashMapOf()
		val ikConstraints = reader.array2("ik", IkConstraintDataSerializer) ?: arrayOf()

		val skins = stringMapOf<SkinData>()
		reader["skins"]!!.forEach { skinName, iReader ->
			val attachments = SkinAttachmentSerializer.read(iReader)
			skins[skinName] = SkinData(skinName, attachments)
		}

		val slots = reader.array2("slots", SlotDataSerializer) ?: arrayOf()
		val transformConstraints = reader.array2("transform", TransformConstraintDataSerializer) ?: arrayOf()
		val animations = reader.map("animations", AnimationDataSerializer) ?: hashMapOf()
		val hash = sk.string("hash")

		val d = SkeletonData(
				name = name,
				width = width,
				height = height,
				version = version,
				imagesPath = imagesPath,
				bones = bones,
				events = events,
				ikConstraints = ikConstraints,
				skins = skins,
				slots = slots,
				transformConstraints = transformConstraints,
				animations = animations,
				hash = hash
		)
		return d
	}
}

object BoneDataSerializer : From<BoneData> {
	override fun read(reader: Reader): BoneData {
		return BoneData(
				parentName = reader.string("parent"),
				name = reader.string("name")!!,
				length = reader.float("length") ?: 0f,
				x = reader.float("x") ?: 0f,
				y = reader.float("y") ?: 0f,
				rotation = reader.float("rotation") ?: 0f,
				scaleX = reader.float("scaleX") ?: 1f,
				scaleY = reader.float("scaleY") ?: 1f,
				inheritScale = reader.bool("inheritScale") ?: true,
				inheritRotation = reader.bool("inheritRotation") ?: true,
				color = Color.fromStr(reader.string("color") ?: "#ccccccff")
		)
	}
}

object SpineEventDefaultsSerializer : From<SpineEventDefaults> {
	override fun read(reader: Reader): SpineEventDefaults {
		val int = reader.int("int") ?: 0
		val float = reader.float("float") ?: 0f
		val string = reader.string("string") ?: ""
		return SpineEventDefaults(
				int = int,
				float = float,
				string = string
		)
	}
}

object SkinAttachmentSerializer : From<Map<SkinDataKey, SkinAttachmentData>> {
	override fun read(reader: Reader): Map<SkinDataKey, SkinAttachmentData> {
		val attachments = HashMap<SkinDataKey, SkinAttachmentData>()
		reader.forEach { slotName, iReader ->
			iReader.forEach { attachmentName, jReader ->
				val attachment = SkinAttachmentDataSerializer.read(jReader)
				attachments[SkinDataKey(slotName, attachmentName)] = attachment
			}
		}
		return attachments
	}
}


object IkConstraintDataSerializer : From<IkConstraintData> {
	override fun read(reader: Reader): IkConstraintData {
		val name = reader.string("name")!!
		val targetName = reader.string("target")!!
		val boneNames = reader.stringArray("bones")!!.filterNotNull().toTypedArray()
		val bendDirection = if (reader.bool("bendPositive") != false) 1 else -1
		val mix = reader.float("mix") ?: 1f
		return IkConstraintData(
				name,
				targetName,
				boneNames,
				bendDirection,
				mix
		)
	}
}

object TransformConstraintDataSerializer : From<TransformConstraintData> {
	override fun read(reader: Reader): TransformConstraintData {
		return TransformConstraintData(
				name = reader.string("name")!!,
				boneName = reader.string("bone")!!,
				targetName = reader.string("target")!!,
				translateMix = reader.float("translateMix") ?: 1f,
				x = reader.float("x") ?: 0f,
				y = reader.float("y") ?: 0f
		)
	}
}

object AnimationDataSerializer : From<AnimationData> {
	override fun read(reader: Reader): AnimationData {
		val slotTimelines = stringMapOf<ArrayList<TimelineData>>()
		val slotsReader = reader["slots"]
		slotsReader?.forEach { slotName, iReader ->
			val list = ArrayList<TimelineData>()
			slotTimelines[slotName] = list
			if (iReader.contains("attachment")) {
				list.add(AttachmentTimelineData(
						frames = (iReader.array2("attachment", AttachmentFrameDataSerializer)!!).toList()
				))
			}
			if (iReader.contains("color")) {
				list.add(ColorTimelineData(
						frames = (iReader.array2("color", ColorFrameDataSerializer)!!).toList()
				))
			}
		}

		val boneTimelines = stringMapOf<ArrayList<CurvedTimelineData>>()
		val bonesReader = reader["bones"]
		bonesReader?.forEach { boneName, iReader ->
			val list = ArrayList<CurvedTimelineData>()
			boneTimelines[boneName] = list
			if (iReader.contains("rotate")) {
				list.add(RotateTimelineData(
						frames = (iReader.array2("rotate", RotateFrameDataSerializer)!!).toList()
				))
			}
			if (iReader.contains("scale")) {
				list.add(ScaleTimelineData(
						frames = (iReader.array2("scale", ScaleFrameDataSerializer)!!).toList()
				))
			}
			if (iReader.contains("translate")) {
				list.add(TranslateTimelineData(
						frames = (iReader.array2("translate", TranslateFrameDataSerializer)!!).toList()
				))
			}
		}

		val ikConstraintTimelines = stringMapOf<IkConstraintTimelineData>()
		val ikReader = reader["ik"]
		ikReader?.forEach { ikName, iReader ->
			ikConstraintTimelines[ikName] = IkConstraintTimelineData(
					frames = (iReader.array2(IkConstraintFrameDataSerializer)!!).toList()
			)
		}

		val ffdTimelines = HashMap<FfdtKey, FfdTimelineData>()
		val ffdReader = reader["ffd"]
		ffdReader?.forEach { skinName, iReader ->

			iReader.forEach { slotName, jReader ->

				jReader.forEach { attachmentName, reader ->

					ffdTimelines[FfdtKey(skinName, slotName, attachmentName)] = FfdTimelineData(
							frames = (reader.array2(FfdFrameDataSerializer)!!).toList()
					)
				}
			}
		}

		val drawOrderReader = reader["drawOrder"]
		val drawOrderTimeline: DrawOrderTimelineData?
		drawOrderTimeline = if (drawOrderReader != null) {
			DrawOrderTimelineData(
					frames = (drawOrderReader.array2(DrawOrderFrameDataSerializer)!!).toList()
			)
		} else null

		val eventReader = reader["events"]
		val eventTimeline: EventTimelineData?
		eventTimeline = if (eventReader != null) {
			EventTimelineData(
					frames = (eventReader.array2(EventFrameDataSerializer)!!).toList()
			)
		} else null

		return AnimationData(
				slotTimelines = slotTimelines,
				boneTimelines = boneTimelines,
				ikConstraintTimelines = ikConstraintTimelines,
				ffdTimelines = ffdTimelines,
				drawOrderTimeline = drawOrderTimeline,
				eventTimeline = eventTimeline
		)

	}
}


object SlotDataSerializer : From<SlotData> {
	override fun read(reader: Reader): SlotData {
		return SlotData(
				name = reader.string("name")!!,
				boneName = reader.string("bone")!!,
				color = Color.fromStr(reader.string("color") ?: "ffffffff"),
				attachmentName = reader.string("attachment"),
				blendMode = BlendMode.fromStr((reader.string("blend"))) ?: BlendMode.NORMAL
		)
	}
}

object AttachmentFrameDataSerializer : From<AttachmentFrameData> {
	override fun read(reader: Reader): AttachmentFrameData {
		return AttachmentFrameData(
				time = reader.float("time")!!,
				attachment = reader.string("name")
		)
	}
}

object ColorFrameDataSerializer : From<ColorFrameData> {
	override fun read(reader: Reader): ColorFrameData {
		return ColorFrameData(
				time = reader.float("time")!!,
				color = Color.fromStr(reader.string("color") ?: "FFFFFFFF"),
				curve = CurveDataSerializer.read(reader)
		)
	}
}

/**
 * In Spine, the curve data is either the string "stepped", or a float array of control points for a bezier.
 */
object CurveDataSerializer : From<CurveData> {
	override fun read(reader: Reader): CurveData {
		val curveType: CurveType
		var bezier: BezierCurveData? = null
		val curveData = reader["curve"]
		if (curveData != null) {
			if (curveData.string() == "stepped") {
				curveType = CurveType.STEPPED
			} else if (curveData.string() == "linear") {
				curveType = CurveType.LINEAR
			} else {
				curveType = CurveType.BEZIER
				bezier = BezierCurveDataSerializer.read(curveData)
			}
		} else {
			curveType = CurveType.LINEAR
		}
		return CurveData(curveType, bezier)
	}
}

object DrawOrderFrameDataSerializer : From<DrawOrderFrameData> {
	override fun read(reader: Reader): DrawOrderFrameData {
		return DrawOrderFrameData(
				time = reader.float("time")!!,
				offsets = reader.array2(DrawOrderOffsetDataSerializer)
		)
	}
}

object DrawOrderOffsetDataSerializer : From<DrawOrderOffsetData> {
	override fun read(reader: Reader): DrawOrderOffsetData {
		val slotName = reader.string("slot")!!
		val offset = reader.int("offset")!!
		return DrawOrderOffsetData(slotName, offset)
	}
}

object EventFrameDataSerializer : From<EventFrameData> {
	override fun read(reader: Reader): EventFrameData {
		val time = reader.float("time")!!
		val name = reader.string("name")!!
		val int = reader.int("int")
		val float = reader.float("float")
		val string = reader.string("string")
		return EventFrameData(
				time = time,
				name = name,
				int = int,
				float = float,
				string = string
		)
	}
}


object FfdFrameDataSerializer : From<FfdFrameData> {
	override fun read(reader: Reader): FfdFrameData {
		return FfdFrameData(
				time = reader.float("time")!!,
				offset = reader.int("offset") ?: 0,
				vertices = reader.floatArray("vertices"),
				curve = CurveDataSerializer.read(reader)
		)
	}
}

object IkConstraintFrameDataSerializer : From<IkConstraintFrameData> {
	override fun read(reader: Reader): IkConstraintFrameData {
		return IkConstraintFrameData(
				time = reader.float("time")!!,
				mix = reader.float("mix") ?: 1f,
				bendPositive = reader.bool("bendPositive") ?: true,
				curve = CurveDataSerializer.read(reader)
		)
	}
}

object RotateFrameDataSerializer : From<RotateFrameData> {
	override fun read(reader: Reader): RotateFrameData {
		return RotateFrameData(
				time = reader.float("time")!!,
				angle = reader.float("angle")!!,
				curve = CurveDataSerializer.read(reader)
		)
	}
}

object ScaleFrameDataSerializer : From<ScaleFrameData> {
	override fun read(reader: Reader): ScaleFrameData {
		return ScaleFrameData(
				time = reader.float("time")!!,
				scaleX = reader.float("x") ?: 1f,
				scaleY = reader.float("y") ?: 1f,
				curve = CurveDataSerializer.read(reader)
		)
	}
}

object BezierCurveDataSerializer : From<BezierCurveData> {
	override fun read(reader: Reader): BezierCurveData {
		val points = reader.floatArray()!!
		return BezierCurveData(points[0], points[1], points[2], points[3])
	}
}

object TranslateFrameDataSerializer : From<TranslateFrameData> {
	override fun read(reader: Reader): TranslateFrameData {
		return TranslateFrameData(
				time = reader.float("time")!!,
				translateX = reader.float("x") ?: 0f,
				translateY = reader.float("y") ?: 0f,
				curve = CurveDataSerializer.read(reader)
		)
	}
}

object BoundingBoxAttachmentDataSerializer : From<BoundingBoxAttachmentData> {
	override fun read(reader: Reader): BoundingBoxAttachmentData {
		return BoundingBoxAttachmentData(reader.floatArray("vertices")!!)
	}
}

object MeshAttachmentDataSerializer : From<MeshAttachmentData> {

	override fun read(reader: Reader): MeshAttachmentData {
		return MeshAttachmentData(
				vertices = reader.floatArray("vertices")!!,
				regionUVs = reader.floatArray("uvs")!!,
				triangles = reader.shortArray("triangles")!!,
				color = Color.fromStr(reader.string("color") ?: "FFFFFFFF"),
				hullLength = (reader.int("hull") ?: 0) * 2,
				edges = reader.shortArray("edges"),
				width = reader.float("width") ?: 0f,
				height = reader.float("height") ?: 0f
		)
	}
}

object RegionAttachmentDataSerializer : From<RegionAttachmentData> {

	override fun read(reader: Reader): RegionAttachmentData {
		return RegionAttachmentData(
				x = reader.float("x") ?: 0f,
				y = reader.float("y") ?: 0f,
				scaleX = reader.float("scaleX") ?: 1f,
				scaleY = reader.float("scaleY") ?: 1f,
				rotation = reader.float("rotation") ?: 0f,
				width = reader.float("width") ?: 0f,
				height = reader.float("height") ?: 0f,
				color = Color.fromStr(reader.string("color") ?: "FFFFFFFF")
		)
	}
}

object SkinAttachmentDataSerializer : From<SkinAttachmentData> {
	override fun read(reader: Reader): SkinAttachmentData {
		// TODO: Linked meshes.
		var type = (reader.string("type") ?: SkinAttachmentType.REGION.name).toUpperCase()
		if (type == "SKINNEDMESH") type = SkinAttachmentType.WEIGHTEDMESH.name
		else if (type == "LINKEDMESH") type = SkinAttachmentType.MESH.name
		else if (type == "WEIGHTEDLINKEDMESH") type = SkinAttachmentType.WEIGHTEDMESH.name

		val attachmentType = SkinAttachmentType.valueOf(type)
		return when (attachmentType) {
			SkinAttachmentType.BOUNDINGBOX -> BoundingBoxAttachmentDataSerializer.read(reader)
			SkinAttachmentType.REGION -> RegionAttachmentDataSerializer.read(reader)
			SkinAttachmentType.MESH -> MeshAttachmentDataSerializer.read(reader)
			SkinAttachmentType.WEIGHTEDMESH -> WeightedMeshAttachmentDataSerializer.read(reader)
		}
	}
}

object WeightedMeshAttachmentDataSerializer : From<WeightedMeshAttachmentData> {
	override fun read(reader: Reader): WeightedMeshAttachmentData {
		val uvs = reader.floatArray("uvs")!!
		val vertices = reader.floatArray("vertices")!!
		val weights = ArrayList<Float>()
		val bones = ArrayList<Int>()
		var i = 0
		val n = vertices.size
		while (i < n) {
			val boneCount = vertices[i++].toInt()
			bones.add(boneCount) // bone count
			val nn = i + boneCount * 4
			while (i < nn) {
				bones.add(vertices[i + 0].toInt()) // bone index
				weights.add(vertices[i + 1]) // x
				weights.add(vertices[i + 2]) // y
				weights.add(vertices[i + 3]) // weight
				i += 4
			}
		}

		return WeightedMeshAttachmentData(
				bones = IntArray(bones.size) { bones[it] },
				weights = FloatArray(weights.size) { weights[it] },
				regionUVs = uvs,
				triangles = reader.shortArray("triangles")!!,
				color = Color.fromStr(reader.string("color") ?: "FFFFFFFF"),
				hullLength = (reader.int("hull") ?: 0) * 2,
				edges = reader.shortArray("edges"),
				width = reader.float("width") ?: 0f,
				height = reader.float("height") ?: 0f

		)
	}
}