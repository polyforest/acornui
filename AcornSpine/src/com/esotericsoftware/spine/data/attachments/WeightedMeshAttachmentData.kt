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

package com.esotericsoftware.spine.data.attachments

import com.acornui.graphic.Color
import com.esotericsoftware.spine.data.SkinAttachmentType

@Suppress("ArrayInDataClass") // Lazy
data class WeightedMeshAttachmentData(

		/**
		 * For each vertex, the number of bones affecting the vertex followed by that many bone indices. Ie: count, boneIndex, ...
		 */
		val bones: IntArray,

		/**
		 * For each bone affecting the vertex, the vertex position in the bone's coordinate system and the weight for the bone's
		 * influence. Ie: x, y, weight, ...
		 */
		val weights: FloatArray,

		/**
		 * For each vertex, a texture coordinate pair. Ie: u, v, ...
		 */
		val regionUVs: FloatArray,

		/**
		 * Vertex number triplets which describe the mesh's triangulation.
		 */
		val triangles: ShortArray,

		val color: Color,

		// Nonessential.
		val hullLength: Int = 0,
		val edges: ShortArray? = null,
		val width: Float = 0f,
		val height: Float = 0f
) : SkinAttachmentData {

	override val type = SkinAttachmentType.WEIGHTEDMESH
}