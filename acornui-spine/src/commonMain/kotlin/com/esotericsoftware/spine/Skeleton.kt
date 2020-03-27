/*
 * Spine Runtimes Software License
 * Version 2.3
 *
 * Copyright (c) 2013-2015, Esoteric Software
 * All rights reserved.
 *
 * You are granted a perpetual, non-exclusive, non-sublicensable and
 * non-transferable license to use, install, execute and perform the Spine
 * Runtimes Software (the "Software") and derivative works solely for personal
 * or internal use. Without the written permission of Esoteric Software (see
 * Section 2 of the Spine Software License Agreement), you may not (a) modify,
 * translate, adapt or otherwise create derivative works, improvements of the
 * Software or develop new applications using the Software or (b) remove,
 * delete, alter or obscure any trademarks or any copyright, trademark, patent
 * or other intellectual property or proprietary rights notices on or in the
 * Software, including any copy thereof. Redistributions in binary or source
 * form must include this license and terms.
 *
 * THIS SOFTWARE IS PROVIDED BY ESOTERIC SOFTWARE "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL ESOTERIC SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.esotericsoftware.spine

import com.acornui.collection.arrayCopy
import com.acornui.collection.firstOrNull2
import com.acornui.collection.indexOfFirst2
import com.acornui.collection.peek
import com.acornui.graphic.Color
import com.acornui.graphic.TextureAtlasData
import com.acornui.math.Bounds
import com.acornui.math.Vector2
import com.acornui.recycle.Clearable
import com.esotericsoftware.spine.animation.Animation
import com.esotericsoftware.spine.attachments.MeshAttachment
import com.esotericsoftware.spine.attachments.RegionAttachment
import com.esotericsoftware.spine.attachments.SkinAttachment
import com.esotericsoftware.spine.attachments.WeightedMeshAttachment
import com.esotericsoftware.spine.component.SpineVertexUtils.positionOffset
import com.esotericsoftware.spine.component.SpineVertexUtils.vertexSize
import com.esotericsoftware.spine.data.SkeletonData
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.firstOrNull
import kotlin.collections.indexOfFirst
import kotlin.collections.iterator
import kotlin.collections.lastIndex
import kotlin.collections.set


class Skeleton(val data: SkeletonData, val atlas: TextureAtlasData) : Clearable {

	val bones: MutableList<Bone>
	val skins: MutableMap<String, Skin>
	val slots: MutableList<Slot>
	val animations: MutableMap<String, Animation>

	/**
	 * The slots and the order they will be drawn.
	 */
	var drawOrder: MutableList<Slot>
	val ikConstraints: MutableList<IkConstraint>
	val transformConstraints: MutableList<TransformConstraint>

	private var _currentSkin: Skin? = null

	val color: Color = Color(1f, 1f, 1f, 1f)
	var time: Float = 0f
	var flipX: Boolean = false
	var flipY: Boolean = false
	var x: Float = 0f
	var y: Float = 0f

	private val updateCache = ArrayList<Updatable>()

	val defaultSkin: Skin?
		get() = skins["default"]

	/**
	 * Caches information about bones and constraints. Must be called if bones or constraints are added or removed.
	 */
	fun updateCache() {
		val bones = this.bones
		val updateCache = this.updateCache
		val ikConstraints = this.ikConstraints
		val transformConstraints = this.transformConstraints
		val ikConstraintsCount = ikConstraints.size
		val transformConstraintsCount = transformConstraints.size
		updateCache.clear()

		var i = 0
		val n = bones.size
		while (i < n) {
			val bone = bones[i]
			updateCache.add(bone)
			for (ii in 0..ikConstraintsCount - 1) {
				val ikConstraint = ikConstraints[ii]
				if (bone === ikConstraint.bones.peek()) {
					updateCache.add(ikConstraint)
					break
				}
			}
			i++
		}

		for (j in 0..transformConstraintsCount - 1) {
			val transformConstraint = transformConstraints[j]
			for (ii in updateCache.size - 1 downTo 0) {
				val obj = updateCache[ii]
				if (obj === transformConstraint.bone || obj === transformConstraint.target) {
					updateCache.add(ii + 1, transformConstraint)
					break
				}
			}
		}
	}

	/**
	 * Updates the world transform for each bone and applies constraints.
	 */
	fun updateWorldTransform() {
		val updateCache = this.updateCache
		for (i in 0..updateCache.lastIndex) {
			updateCache[i].update()
		}
	}

	/**
	 * Sets the bones, constraints, and slots to their setup pose values.
	 */
	fun setToSetupPose() {
		setBonesToSetupPose()
		setSlotsToSetupPose()
	}

	/**
	 * Sets the bones and constraints to their setup pose values.
	 */
	fun setBonesToSetupPose() {
		val bones = this.bones
		var i = 0
		val n = bones.size
		while (i < n) {
			bones[i].setToSetupPose()
			i++
		}

		val ikConstraints = this.ikConstraints
		var i2 = 0
		val n2 = ikConstraints.size
		while (i2 < n2) {
			val constraint = ikConstraints[i2]
			constraint.bendDirection = constraint.data.bendDirection
			constraint.mix = constraint.data.mix
			i2++
		}

		val transformConstraints = this.transformConstraints
		var i3 = 0
		val n3 = transformConstraints.size
		while (i3 < n3) {
			val constraint = transformConstraints[i3]
			constraint.translateMix = constraint.data.translateMix
			constraint.x = constraint.data.x
			constraint.y = constraint.data.y
			i3++
		}
	}

	fun setSlotsToSetupPose() {
		val slots = this.slots
		arrayCopy(slots, 0, drawOrder, 0, slots.size)
		var i = 0
		val n = slots.size
		while (i < n) {
			slots[i].setToSetupPose(i)
			i++
		}
	}

	val rootBone: Bone?
		get() {
			return bones.firstOrNull()
		}

	fun findAnimation(animationName: String): Animation? {
		return animations[animationName]
	}

	fun findBone(boneName: String): Bone? {
		return bones.firstOrNull2 { it: Bone -> it.data.name == boneName }
	}

	/**
	 * @return -1 if the bone was not found.
	 */
	fun findBoneIndex(boneName: String): Int {
		return bones.indexOfFirst2 { it.data.name == boneName }
	}

	fun findSkin(skinName: String): Skin? {
		return skins[skinName]
	}

	fun findSlot(slotName: String): Slot? {
		return slots.firstOrNull2 { it: Slot -> it.data.name == slotName }
	}

	/**
	 * @return -1 if the slot was not found.
	 */
	fun findSlotIndex(slotName: String): Int {
		return data.slots.indexOfFirst { it.name == slotName }
	}

	val currentSkin: Skin?
		get() {
			return _currentSkin
		}

	/**
	 * Sets a skin by name.
	 */
	fun setSkin(skinName: String) {
		val skin = skins[skinName] ?: throw IllegalArgumentException("Skin not found: $skinName")
		setSkin(skin)
	}

	/**
	 * Sets the skin used to look up attachments before looking in the [default skin][SkeletonData.defaultSkin].
	 * Attachments from the new skin are attached if the corresponding attachment from the old skin was attached. If there was no
	 * old skin, each slot's setup mode attachment is attached from the new skin.
	 * @param newSkin May be null.
	 */
	fun setSkin(newSkin: Skin?) {
		if (newSkin != null) {
			if (_currentSkin != null)
				newSkin.attachAll(this, _currentSkin!!)
			else {
				val slots = this.slots
				var i = 0
				val n = slots.size
				while (i < n) {
					val slot = slots[i]
					val name = slot.data.attachmentName
					if (name != null) {
						val attachment = newSkin.getAttachment(i, name)
						if (attachment != null) slot.attachment = attachment
					}
					i++
				}
			}
		}
		_currentSkin = newSkin
	}

	fun getAttachment(slotName: String, attachmentName: String): SkinAttachment? {
		val slotIndex = findSlotIndex(slotName)
		if (slotIndex == -1) return null
		return getAttachment(slotIndex, attachmentName)
	}

	fun getAttachment(slotIndex: Int, attachmentName: String): SkinAttachment? {
		if (_currentSkin != null) {
			val attachment = _currentSkin!!.getAttachment(slotIndex, attachmentName)
			if (attachment != null) return attachment
		}
		return defaultSkin?.getAttachment(slotIndex, attachmentName)
	}

	fun setAttachment(slotName: String, attachmentName: String?) {
		val slot = findSlot(slotName) ?: throw IllegalArgumentException("Slot not found: $slotName")
		slot.attachment = if (attachmentName == null) null else getAttachment(slotName, attachmentName) ?: throw IllegalArgumentException("Attachment not found: $attachmentName, for slot: $slotName")
	}

	fun findIkConstraint(constraintName: String): IkConstraint? {
		return ikConstraints.firstOrNull2 { it: IkConstraint -> it.data.name == constraintName }
	}

	/**
	 * Returns -1 if not found.
	 */
	fun findIkConstraintIndex(constraintName: String): Int {
		return ikConstraints.indexOfFirst2 { it.data.name == constraintName }
	}

	fun findTransformConstraint(constraintName: String): TransformConstraint? {
		return transformConstraints.firstOrNull2 { it: TransformConstraint -> it.data.name == constraintName }
	}

	/**
	 * Returns the axis aligned bounding box (AABB) of the region, mesh, and skinned mesh attachments for the current pose.
	 * @param offset The distance from the skeleton origin to the bottom left corner of the AABB.
	 * *
	 * @param size The width and height of the AABB.
	 */
	fun getBounds(offset: Vector2, size: Bounds) {
		val drawOrder = this.drawOrder
		var minX = Int.MAX_VALUE.toFloat()
		var minY = Int.MAX_VALUE.toFloat()
		var maxX = Int.MIN_VALUE.toFloat()
		var maxY = Int.MIN_VALUE.toFloat()
		var i = 0
		val n = drawOrder.size
		while (i < n) {
			val slot = drawOrder[i]
			val attachment = slot.attachment
			val vertices: List<Float>? = if (attachment is RegionAttachment) {
				attachment.updateGlobalVertices(slot)
			} else if (attachment is MeshAttachment) {
				attachment.updateWorldVertices(slot)
			} else if (attachment is WeightedMeshAttachment) {
				attachment.updateWorldVertices(slot)
			} else null
			if (vertices != null) {
				var j = positionOffset
				val n = vertices.lastIndex
				while (j < n) {
					val x = vertices[j]
					val y = vertices[j + 1]
					minX = minOf(minX, x)
					minY = minOf(minY, y)
					maxX = maxOf(maxX, x)
					maxY = maxOf(maxY, y)
					j += vertexSize
				}
			}
			i++
		}
		offset.set(minX, minY)
		size.set(maxX - minX, maxY - minY)
	}

	fun setFlip(flipX: Boolean, flipY: Boolean) {
		this.flipX = flipX
		this.flipY = flipY
	}

	fun setPosition(x: Float, y: Float) {
		this.x = x
		this.y = y
	}

	fun update(delta: Float) {
		time += delta
	}

	override fun clear() {
		color.set(Color.WHITE)
		setFlip(false, false)
		_currentSkin = null
		setToSetupPose()
	}

	override fun toString(): String {
		return "Skeleton(name='${data.name}')"
	}

	init {
		bones = ArrayList(data.bones.size)
		for (boneData in data.bones) {
			val parent = if (boneData.parentName == null) null else findBone(boneData.parentName)
			bones.add(Bone(boneData, this, parent))
		}
		ikConstraints = ArrayList(data.ikConstraints.size)
		for (ikConstraint in data.ikConstraints)
			ikConstraints.add(IkConstraint(ikConstraint, this))
		transformConstraints = ArrayList(data.transformConstraints.size)
		for (transformConstraint in data.transformConstraints)
			transformConstraints.add(TransformConstraint(transformConstraint, this))
		skins = HashMap()
		for (skinEntry in data.skins.entries) {
			skins[skinEntry.key] = Skin(this, skinEntry.value)
		}
		slots = ArrayList(data.slots.size)
		drawOrder = ArrayList(data.slots.size)
		for (slotData in data.slots) {
			val bone = findBone(slotData.boneName) ?: throw Exception("Could not find bone with name ${slotData.boneName}")
			val slot = Slot(slotData, bone)
			slots.add(slot)
			drawOrder.add(slot)
		}
		animations = HashMap()
		for (animationEntry in data.animations) {
			animations[animationEntry.key] = Animation(this, animationEntry.value)
		}
		updateCache()
	}
}