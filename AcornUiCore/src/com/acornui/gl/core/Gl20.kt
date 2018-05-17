/*
 * Derived from LibGDX by Nicholas Bilyk
 * https://github.com/libgdx
 * Copyright 2011 See https://github.com/libgdx/libgdx/blob/master/AUTHORS
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

package com.acornui.gl.core

import com.acornui.core.di.DKey
import com.acornui.core.graphics.Texture
import com.acornui.core.io.BufferFactory
import com.acornui.graphics.ColorRo
import com.acornui.io.NativeBuffer
import com.acornui.math.Matrix4Ro
import com.acornui.math.Vector2
import com.acornui.math.Vector3
import kotlin.math.round

interface Gl20 {

	companion object : DKey<Gl20> {

		const val TRUE = 1
		const val FALSE = 0


		/* ClearBufferMask */
		const val DEPTH_BUFFER_BIT: Int = 256
		const val STENCIL_BUFFER_BIT: Int = 1024
		const val COLOR_BUFFER_BIT: Int = 16384

		/* BeginMode */
		const val POINTS: Int = 0
		const val LINES: Int = 1
		const val LINE_LOOP: Int = 2
		const val LINE_STRIP: Int = 3
		const val TRIANGLES: Int = 4
		const val TRIANGLE_STRIP: Int = 5
		const val TRIANGLE_FAN: Int = 6

		/* BlendingFactorDest */
		const val ZERO: Int = 0
		const val ONE: Int = 1
		const val SRC_COLOR: Int = 768
		const val ONE_MINUS_SRC_COLOR: Int = 769
		const val SRC_ALPHA: Int = 770
		const val ONE_MINUS_SRC_ALPHA: Int = 771
		const val DST_ALPHA: Int = 772
		const val ONE_MINUS_DST_ALPHA: Int = 773

		/* BlendingFactorSrc */
		/* ZERO */
		/* ONE */
		const val DST_COLOR: Int = 774
		const val ONE_MINUS_DST_COLOR: Int = 775
		const val SRC_ALPHA_SATURATE: Int = 776

		/* BlendEquationSeparate */
		const val FUNC_ADD: Int = 32774
		const val BLEND_EQUATION: Int = 32777
		const val BLEND_EQUATION_RGB: Int = 32777 /* same as BLEND_EQUATION */
		const val BLEND_EQUATION_ALPHA: Int = 34877

		/* BlendSubtract */
		const val FUNC_SUBTRACT: Int = 32778
		const val FUNC_REVERSE_SUBTRACT: Int = 32779

		/* Separate Blend Functions */
		const val BLEND_DST_RGB: Int = 32968
		const val BLEND_SRC_RGB: Int = 32969
		const val BLEND_DST_ALPHA: Int = 32970
		const val BLEND_SRC_ALPHA: Int = 32971
		const val CONSTANT_COLOR: Int = 32769
		const val ONE_MINUS_CONSTANT_COLOR: Int = 32770
		const val CONSTANT_ALPHA: Int = 32771
		const val ONE_MINUS_CONSTANT_ALPHA: Int = 32772
		const val BLEND_COLOR: Int = 32773

		/* Buffer Objects */
		const val ARRAY_BUFFER: Int = 34962
		const val ELEMENT_ARRAY_BUFFER: Int = 34963
		const val ARRAY_BUFFER_BINDING: Int = 34964
		const val ELEMENT_ARRAY_BUFFER_BINDING: Int = 34965

		/**
		 * The user will be changing the data after every use. Or almost every use.
		 */
		const val STREAM_DRAW: Int = 35040

		/**
		 * The user will set the data once.
		 */
		const val STATIC_DRAW: Int = 35044

		/**
		 * The user will set the data occasionally.
		 */
		const val DYNAMIC_DRAW: Int = 35048

		const val BUFFER_SIZE: Int = 34660
		const val BUFFER_USAGE: Int = 34661

		const val CURRENT_VERTEX_ATTRIB: Int = 34342

		/* CullFaceMode */
		const val FRONT: Int = 1028
		const val BACK: Int = 1029
		const val FRONT_AND_BACK: Int = 1032

		const val TEXTURE_2D: Int = 3553

		/* EnableCap */
		const val CULL_FACE: Int = 2884
		const val BLEND: Int = 3042
		const val DITHER: Int = 3024
		const val STENCIL_TEST: Int = 2960
		const val DEPTH_TEST: Int = 2929
		const val SCISSOR_TEST: Int = 3089
		const val POLYGON_OFFSET_FILL: Int = 32823
		const val SAMPLE_ALPHA_TO_COVERAGE: Int = 32926
		const val SAMPLE_COVERAGE: Int = 32928

		/* ErrorCode */
		const val NO_ERROR: Int = 0
		const val INVALID_ENUM: Int = 1280
		const val INVALID_VALUE: Int = 1281
		const val INVALID_OPERATION: Int = 1282
		const val OUT_OF_MEMORY: Int = 1285

		/* FrontFaceDirection */
		const val CW: Int = 2304
		const val CCW: Int = 2305

		/* GetpName */
		const val LINE_WIDTH: Int = 2849
		const val ALIASED_POINT_SIZE_RANGE: Int = 33901
		const val ALIASED_LINE_WIDTH_RANGE: Int = 33902
		const val CULL_FACE_MODE: Int = 2885
		const val FRONT_FACE: Int = 2886
		const val DEPTH_RANGE: Int = 2928
		const val DEPTH_WRITEMASK: Int = 2930
		const val DEPTH_CLEAR_VALUE: Int = 2931
		const val DEPTH_FUNC: Int = 2932
		const val STENCIL_CLEAR_VALUE: Int = 2961
		const val STENCIL_FUNC: Int = 2962
		const val STENCIL_FAIL: Int = 2964
		const val STENCIL_PASS_DEPTH_FAIL: Int = 2965
		const val STENCIL_PASS_DEPTH_PASS: Int = 2966
		const val STENCIL_REF: Int = 2967
		const val STENCIL_VALUE_MASK: Int = 2963
		const val STENCIL_WRITEMASK: Int = 2968
		const val STENCIL_BACK_FUNC: Int = 34816
		const val STENCIL_BACK_FAIL: Int = 34817
		const val STENCIL_BACK_PASS_DEPTH_FAIL: Int = 34818
		const val STENCIL_BACK_PASS_DEPTH_PASS: Int = 34819
		const val STENCIL_BACK_REF: Int = 36003
		const val STENCIL_BACK_VALUE_MASK: Int = 36004
		const val STENCIL_BACK_WRITEMASK: Int = 36005
		const val VIEWPORT: Int = 2978
		const val SCISSOR_BOX: Int = 3088

		/* SCISSOR_TEST */
		const val COLOR_CLEAR_VALUE: Int = 3106
		const val COLOR_WRITEMASK: Int = 3107
		const val UNPACK_ALIGNMENT: Int = 3317
		const val PACK_ALIGNMENT: Int = 3333
		const val MAX_TEXTURE_SIZE: Int = 3379
		const val MAX_VIEWPORT_DIMS: Int = 3386
		const val SUBPIXEL_BITS: Int = 3408
		const val RED_BITS: Int = 3410
		const val GREEN_BITS: Int = 3411
		const val BLUE_BITS: Int = 3412
		const val ALPHA_BITS: Int = 3413
		const val DEPTH_BITS: Int = 3414
		const val STENCIL_BITS: Int = 3415
		const val POLYGON_OFFSET_UNITS: Int = 10752

		/* POLYGON_OFFSET_FILL */
		const val POLYGON_OFFSET_FACTOR: Int = 32824
		const val TEXTURE_BINDING_2D: Int = 32873
		const val SAMPLE_BUFFERS: Int = 32936
		const val SAMPLES: Int = 32937
		const val SAMPLE_COVERAGE_VALUE: Int = 32938
		const val SAMPLE_COVERAGE_INVERT: Int = 32939

		/* GetTextureParameter */
		/* TEXTURE_MAG_FILTER */
		/* TEXTURE_MIN_FILTER */
		/* TEXTURE_WRAP_S */
		/* TEXTURE_WRAP_T */

		const val NUM_COMPRESSED_TEXTURE_FORMATS: Int = 34466
		const val COMPRESSED_TEXTURE_FORMATS: Int = 34467

		/* HintMode */
		const val DONT_CARE: Int = 4352
		const val FASTEST: Int = 4353
		const val NICEST: Int = 4354

		/* HintTarget */
		const val GENERATE_MIPMAP_HINT: Int = 33170

		/* DataType */
		const val BYTE: Int = 5120
		const val UNSIGNED_BYTE: Int = 5121
		const val SHORT: Int = 5122
		const val UNSIGNED_SHORT: Int = 5123
		const val INT: Int = 5124
		const val UNSIGNED_INT: Int = 5125
		const val FLOAT: Int = 5126

		/* PixelFormat */
		const val DEPTH_COMPONENT: Int = 6402
		const val ALPHA: Int = 6406
		const val RGB: Int = 6407
		const val RGBA: Int = 6408
		const val LUMINANCE: Int = 6409
		const val LUMINANCE_ALPHA: Int = 6410

		/* PixelType */
		/* UNSIGNED_BYTE */
		const val UNSIGNED_SHORT_4_4_4_4: Int = 32819
		const val UNSIGNED_SHORT_5_5_5_1: Int = 32820
		const val UNSIGNED_SHORT_5_6_5: Int = 33635

		/* Shaders */
		const val FRAGMENT_SHADER: Int = 35632
		const val VERTEX_SHADER: Int = 35633
		const val MAX_VERTEX_ATTRIBS: Int = 34921
		const val MAX_VERTEX_UNIFORM_VECTORS: Int = 36347
		const val MAX_VARYING_VECTORS: Int = 36348
		const val MAX_COMBINED_TEXTURE_IMAGE_UNITS: Int = 35661
		const val MAX_VERTEX_TEXTURE_IMAGE_UNITS: Int = 35660
		const val MAX_TEXTURE_IMAGE_UNITS: Int = 34930
		const val MAX_FRAGMENT_UNIFORM_VECTORS: Int = 36349
		const val SHADER_TYPE: Int = 35663
		const val DELETE_STATUS: Int = 35712
		const val LINK_STATUS: Int = 35714
		const val VALIDATE_STATUS: Int = 35715
		const val ATTACHED_SHADERS: Int = 35717
		const val ACTIVE_UNIFORMS: Int = 35718
		const val ACTIVE_UNIFORM_MAX_LENGTH: Int = 35719
		const val ACTIVE_ATTRIBUTES: Int = 35721
		const val ACTIVE_ATTRIBUTE_MAX_LENGTH: Int = 35722
		const val SHADING_LANGUAGE_VERSION: Int = 35724
		const val CURRENT_PROGRAM: Int = 35725

		/* StencilFunction */
		const val NEVER: Int = 512
		const val LESS: Int = 513
		const val EQUAL: Int = 514
		const val LEQUAL: Int = 515
		const val GREATER: Int = 516
		const val NOTEQUAL: Int = 517
		const val GEQUAL: Int = 518
		const val ALWAYS: Int = 519

		/* StencilOp */
		/* ZERO */
		const val KEEP: Int = 7680
		const val REPLACE: Int = 7681
		const val INCR: Int = 7682
		const val DECR: Int = 7683
		const val INVERT: Int = 5386
		const val INCR_WRAP: Int = 34055
		const val DECR_WRAP: Int = 34056

		/* StringName */
		const val VENDOR: Int = 7936
		const val RENDERER: Int = 7937
		const val VERSION: Int = 7938

		/* TextureMagFilter */
		const val NEAREST: Int = 9728
		const val LINEAR: Int = 9729

		/* TextureMinFilter */
		/* NEAREST */
		/* LINEAR */
		const val NEAREST_MIPMAP_NEAREST: Int = 9984
		const val LINEAR_MIPMAP_NEAREST: Int = 9985
		const val NEAREST_MIPMAP_LINEAR: Int = 9986
		const val LINEAR_MIPMAP_LINEAR: Int = 9987

		/* TextureParameterName */
		const val TEXTURE_MAG_FILTER: Int = 10240
		const val TEXTURE_MIN_FILTER: Int = 10241
		const val TEXTURE_WRAP_S: Int = 10242
		const val TEXTURE_WRAP_T: Int = 10243

		/* TextureTarget */
		/* TEXTURE_2D */
		const val TEXTURE: Int = 5890

		const val TEXTURE_CUBE_MAP: Int = 34067
		const val TEXTURE_BINDING_CUBE_MAP: Int = 34068
		const val TEXTURE_CUBE_MAP_POSITIVE_X: Int = 34069
		const val TEXTURE_CUBE_MAP_NEGATIVE_X: Int = 34070
		const val TEXTURE_CUBE_MAP_POSITIVE_Y: Int = 34071
		const val TEXTURE_CUBE_MAP_NEGATIVE_Y: Int = 34072
		const val TEXTURE_CUBE_MAP_POSITIVE_Z: Int = 34073
		const val TEXTURE_CUBE_MAP_NEGATIVE_Z: Int = 34074
		const val MAX_CUBE_MAP_TEXTURE_SIZE: Int = 34076

		/* TextureUnit */
		const val TEXTURE0: Int = 33984
		const val TEXTURE1: Int = 33985
		const val TEXTURE2: Int = 33986
		const val TEXTURE3: Int = 33987
		const val TEXTURE4: Int = 33988
		const val TEXTURE5: Int = 33989
		const val TEXTURE6: Int = 33990
		const val TEXTURE7: Int = 33991
		const val TEXTURE8: Int = 33992
		const val TEXTURE9: Int = 33993
		const val TEXTURE10: Int = 33994
		const val TEXTURE11: Int = 33995
		const val TEXTURE12: Int = 33996
		const val TEXTURE13: Int = 33997
		const val TEXTURE14: Int = 33998
		const val TEXTURE15: Int = 33999
		const val TEXTURE16: Int = 34000
		const val TEXTURE17: Int = 34001
		const val TEXTURE18: Int = 34002
		const val TEXTURE19: Int = 34003
		const val TEXTURE20: Int = 34004
		const val TEXTURE21: Int = 34005
		const val TEXTURE22: Int = 34006
		const val TEXTURE23: Int = 34007
		const val TEXTURE24: Int = 34008
		const val TEXTURE25: Int = 34009
		const val TEXTURE26: Int = 34010
		const val TEXTURE27: Int = 34011
		const val TEXTURE28: Int = 34012
		const val TEXTURE29: Int = 34013
		const val TEXTURE30: Int = 34014
		const val TEXTURE31: Int = 34015
		const val ACTIVE_TEXTURE: Int = 34016

		/* TextureWrapMode */
		const val REPEAT: Int = 10497
		const val CLAMP_TO_EDGE: Int = 33071
		const val MIRRORED_REPEAT: Int = 33648

		/* Uniform Types */
		const val FLOAT_VEC2: Int = 35664
		const val FLOAT_VEC3: Int = 35665
		const val FLOAT_VEC4: Int = 35666
		const val INT_VEC2: Int = 35667
		const val INT_VEC3: Int = 35668
		const val INT_VEC4: Int = 35669
		const val BOOL: Int = 35670
		const val BOOL_VEC2: Int = 35671
		const val BOOL_VEC3: Int = 35672
		const val BOOL_VEC4: Int = 35673
		const val FLOAT_MAT2: Int = 35674
		const val FLOAT_MAT3: Int = 35675
		const val FLOAT_MAT4: Int = 35676
		const val SAMPLER_2D: Int = 35678
		const val SAMPLER_CUBE: Int = 35680

		/* Vertex Arrays */
		const val VERTEX_ATTRIB_ARRAY_ENABLED: Int = 34338
		const val VERTEX_ATTRIB_ARRAY_SIZE: Int = 34339
		const val VERTEX_ATTRIB_ARRAY_STRIDE: Int = 34340
		const val VERTEX_ATTRIB_ARRAY_TYPE: Int = 34341
		const val VERTEX_ATTRIB_ARRAY_NORMALIZED: Int = 34922
		const val VERTEX_ATTRIB_ARRAY_POINTER: Int = 34373
		const val VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: Int = 34975

		/* Read Format */
		const val IMPLEMENTATION_COLOR_READ_TYPE: Int = 35738
		const val IMPLEMENTATION_COLOR_READ_FORMAT: Int = 35739

		/* Shader Source */
		const val COMPILE_STATUS: Int = 35713
		const val INFO_LOG_LENGTH: Int = 35716
		const val SHADER_SOURCE_LENGTH: Int = 35720

		/* Shader Precision-Specified Types */
		const val LOW_FLOAT: Int = 36336
		const val MEDIUM_FLOAT: Int = 36337
		const val HIGH_FLOAT: Int = 36338
		const val LOW_INT: Int = 36339
		const val MEDIUM_INT: Int = 36340
		const val HIGH_INT: Int = 36341

		/* Framebuffer Object. */
		const val FRAMEBUFFER: Int = 36160
		const val RENDERBUFFER: Int = 36161

		const val RGBA4: Int = 32854
		const val RGB5_A1: Int = 32855
		const val RGB565: Int = 36194
		const val DEPTH_COMPONENT16: Int = 33189
		const val STENCIL_INDEX: Int = 6401
		const val STENCIL_INDEX8: Int = 36168
		const val DEPTH_STENCIL: Int = 34041

		const val RENDERBUFFER_WIDTH: Int = 36162
		const val RENDERBUFFER_HEIGHT: Int = 36163
		const val RENDERBUFFER_INTERNAL_FORMAT: Int = 36164
		const val RENDERBUFFER_RED_SIZE: Int = 36176
		const val RENDERBUFFER_GREEN_SIZE: Int = 36177
		const val RENDERBUFFER_BLUE_SIZE: Int = 36178
		const val RENDERBUFFER_ALPHA_SIZE: Int = 36179
		const val RENDERBUFFER_DEPTH_SIZE: Int = 36180
		const val RENDERBUFFER_STENCIL_SIZE: Int = 36181

		const val FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE: Int = 36048
		const val FRAMEBUFFER_ATTACHMENT_OBJECT_NAME: Int = 36049
		const val FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL: Int = 36050
		const val FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE: Int = 36051

		const val COLOR_ATTACHMENT0: Int = 36064
		const val DEPTH_ATTACHMENT: Int = 36096
		const val STENCIL_ATTACHMENT: Int = 36128
		const val DEPTH_STENCIL_ATTACHMENT: Int = 33306

		const val NONE: Int = 0

		const val FRAMEBUFFER_COMPLETE: Int = 36053
		const val FRAMEBUFFER_INCOMPLETE_ATTACHMENT: Int = 36054
		const val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: Int = 36055
		const val FRAMEBUFFER_INCOMPLETE_DIMENSIONS: Int = 36057
		const val FRAMEBUFFER_UNSUPPORTED: Int = 36061

		const val FRAMEBUFFER_BINDING: Int = 36006
		const val RENDERBUFFER_BINDING: Int = 36007
		const val MAX_RENDERBUFFER_SIZE: Int = 34024

		const val INVALID_FRAMEBUFFER_OPERATION: Int = 1286
	}

	/**
	 * Set the texture unit subsequent texture operations apply to.
	 *
	 * must be one of TEXTURE0, TEXTURE1, to getParameter(gl.MAX_COMBINED_TEXTURE_IMAGE_UNITS) - 1.
	 * The default value is TEXTURE0. A texture must be bound to the active texture unit using bindTexture().
	 */
	fun activeTexture(texture: Int)

	/**
	 * Attaches shader to program. A program must have both a VERTEX_SHADER and FRAGMENT_SHADER before it can be used.
	 * shader can be attached before its source has been set. See also detachShader().
	 */
	fun attachShader(program: GlProgramRef, shader: GlShaderRef)

	/**
	 * Associates a number (location) with an attribute (a shader input such as vertex position) in program. Other
	 * Gl functions (such as enableVertexAttribArray() or vertexAttribPointer()) deal with an attribute location
	 * number instead of the name used in the program and bindAttribLocation is used to choose the number used for
	 * that attribute. Locations are automatically assigned if you do not call bindAttribLocation so this method is
	 * only necessary if you wish to assign a specific location for an attribute. Use getAttribLocation() to retrieve
	 * the automatically assigned location. bindAttribLocation() must be called before calling linkProgram(program) and
	 * location must be an integer in the range 0 to getParameter(gl.MAX_VERTEX_ATTRIBS) - 1.
	 */
	fun bindAttribLocation(program: GlProgramRef, index: Int, name: String)

	/**
	 * Sets the current buffer for target to buffer. target must be either ARRAY_BUFFER or ELEMENT_ARRAY_BUFFER.
	 * Use bufferData() to fill the bound buffer with data.
	 */
	fun bindBuffer(target: Int, buffer: GlBufferRef?)

	/**
	 * Sets the current framebuffer to framebuffer. target must be FRAMEBUFFER. See createFramebuffer() for an
	 * example of using bindFramebuffer().
	 */
	fun bindFramebuffer(target: Int, framebuffer: GlFramebufferRef?)

	/**
	 * Sets the current renderbuffer to renderbuffer. target must be RENDERBUFFER.
	 */
	fun bindRenderbuffer(target: Int, renderbuffer: GlRenderbufferRef?)

	/**
	 * Sets the specified target and texture (created with createTexture) for the bound texture in the active texture
	 * unit (set through activeTexture() and bindTexture). target must be one of TEXTURE_2D or TEXTURE_CUBE_MAP
	 */
	fun bindTexture(target: Int, texture: GlTextureRef?)

	/**
	 * Specifies the blend color used with blendFunc(). Each component must be in the range 0.0 to 1.0.
	 */
	fun blendColor(red: Float, green: Float, blue: Float, alpha: Float)

	/**
	 * Same as blendEquationSeparate(mode, mode).
	 */
	fun blendEquation(mode: Int)

	/**
	 * Sets how the newly rendered pixel color and alpha (src) is combined with the existing framebuffer color and
	 * alpha (dst) before storing in the framebuffer.
	 * modeRGB and modeAlpha must be one of FUNC_ADD, FUNC_SUBTRACT, or FUNC_REVERSE_SUBTRACT. If the mode is FUNC_ADD,
	 * the destination color will be src + dst. If the mode is FUNC_SUBTRACT, the destination color will be src - dst.
	 * If the mode is FUNC_REVERSE_SUBTRACT, the destination color will be dst - src. Both modeRGB and modeAlpha
	 * default to FUNC_ADD. Use getParameter(gl.BLEND_EQUATION_RGB) and getParameter(gl.BLEND_EQUATION_ALPHA) to get
	 * the current values. See blendFuncSeparate() for how src and dst are computed. Blending must be enabled with
	 * enable(BLEND).
	 */
	fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int)

	/**
	 * Same as blendFuncSeparate(srcFactor, dstFactor, srcFactor, dstFactor).
	 */
	fun blendFunc(sfactor: Int, dfactor: Int)

	/**
	 * Adjusts the newly rendered pixel color and alpha (src) and existing framebuffer color and alpha in the
	 * framebuffer (dst) before being combined using blendEquationSeparate(). srcRGB, dstRGB, srcAlpha, and dstAlpha
	 * must be one of ZERO, ONE, SRC_COLOR, ONE_MINUS_SRC_COLOR, DST_COLOR, ONE_MINUS_DST_COLOR, SRC_ALPHA,
	 * ONE_MINUS_SRC_ALPHA, DST_ALPHA, ONE_MINUS_DST_ALPHA, CONSTANT_COLOR, ONE_MINUS_CONSTANT_COLOR, CONSTANT_ALPHA,
	 * ONE_MINUS_CONSTANT_ALPHA, or SRC_ALPHA_SATURATE srcRGB and srcAlpha default to ONE. dstRGB and dstAlpha
	 * default to NONE. For traditional alpha blending, use: gl.blendFuncSeparate(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA,
	 * gl.ONE, gl.ZERO). For premultiplied alpha blending, use: gl.blendFuncSeparate(gl.ONE, gl.ONE_MINUS_SRC_ALPHA,
	 * gl.ONE, gl.ZERO). Use getParameter(gl.BLEND_SRC_RGB), getParameter(gl.BLEND_DST_RGB),
	 * getParameter(gl.BLEND_SRC_ALPHA), and getParameter(gl.BLEND_DST_ALPHA) to get the current values.
	 */
	fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int)

	/**
	 * Creates the storage for the currently bound buffer.
	 * @param target must be one of ARRAY_BUFFER or ELEMENT_ARRAY_BUFFER.
	 * @param size the size in bytes of the buffer to allocate.
	 * @param usage must be one of STREAM_DRAW, STATIC_DRAW, or DYNAMIC_DRAW.
	 */
	fun bufferData(target: Int, size: Int, usage: Int)

	/**
	 * Creates and initializes a buffer object's data store
	 */
	fun bufferDatabv(target: Int, data: NativeBuffer<Byte>, usage: Int)

	/**
	 * Creates and initializes a buffer object's data store
	 */
	fun bufferDatafv(target: Int, data: NativeBuffer<Float>, usage: Int)

	/**
	 * Creates and initializes a buffer object's data store
	 */
	fun bufferDatasv(target: Int, data: NativeBuffer<Short>, usage: Int)

	/**
	 * Updates a subset of a buffer object's data store
	 */
	fun bufferSubDatafv(target: Int, offset: Int, data: NativeBuffer<Float>)

	/**
	 * Updates a subset of a buffer object's data store
	 */
	fun bufferSubDatasv(target: Int, offset: Int, data: NativeBuffer<Short>)

	/**
	 * Returns one of the following to indicate the current status of the framebuffer: FRAMEBUFFER_COMPLETE,
	 * FRAMEBUFFER_INCOMPLETE_ATTACHMENT, FRAMEBUFFER_INCOMPLETE_DIMENSIONS, FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT,
	 * or FRAMEBUFFER_UNSUPPORTED.
	 */
	fun checkFramebufferStatus(target: Int): Int

	/**
	 * Clears the buffers specified by mask where mask is the bitwise OR (|) of one or more of the following values:
	 * COLOR_BUFFER_BIT, STENCIL_BUFFER_BIT, and DEPTH_BUFFER_BIT.
	 */
	fun clear(mask: Int)

	/**
	 * Specifies the color to fill the color buffer when reset() is called with the COLOR_BUFFER_BIT.
	 * The parameters are clamped to the range 0 to 1.
	 */
	fun clearColor(red: Float, green: Float, blue: Float, alpha: Float)

	fun clearColor(color: ColorRo) {
		clearColor(color.r, color.g, color.b, color.a)
	}

	/**
	 * Specifies the value to fill the depth buffer when reset() is called with the DEPTH_BUFFER_BIT.
	 * depth is clamped to the range 0 (near) to 1 (far). Defaults to 1 if not specified.
	 */
	fun clearDepth(depth: Float)

	/**
	 * Specifies the value (integer) to fill the depth buffer when reset() is called with the STENCIL_BUFFER_BIT.
	 */
	fun clearStencil(s: Int)

	/**
	 * Turns on or off writing to the specified channels of the frame buffer. Defaults to true for all channels.
	 * Use getParameter(gl.COLOR_WRITEMASK) to get the current value.
	 */
	fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean)

	/**
	 * Compiles the specified shader. Must be called after setting the source with shaderSource(). If the shader had
	 * errors during compilation, gl.getShaderParameter(shader, gl.COMPILE_STATUS) will return false and you can use
	 * getShaderInfoLog() to get details about the error.
	 */
	fun compileShader(shader: GlShaderRef)

	/**
	 * Copies pixels from the framebuffer to the bound texture in the active texture unit (set through activeTexture()
	 * and bindTexture).
	 * @param target must be one of TEXTURE_2D, TEXTURE_CUBE_MAP_POSITIVE_X, TEXTURE_CUBE_MAP_NEGATIVE_X, TEXTURE_CUBE_MAP_POSITIVE_Y, TEXTURE_CUBE_MAP_NEGATIVE_Y, TEXTURE_CUBE_MAP_POSITIVE_Z, or TEXTURE_CUBE_MAP_NEGATIVE_Z.
	 * @param level specifies the mipmap level to copy into.
	 * @param internalFormat ALPHA, LUMINANCE, LUMINANCE_ALPHA, RGB, or RGBA.
	 * @param x
	 * @param y
	 * @param width
	 * @param height the rectangle in framebuffer to copy.
	 * @param border must be 0.
	 */
	fun copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int)

	/**
	 * Copies pixels from the framebuffer to a subregion of the bound texture in the active texture unit (set through activeTexture() and bindTexture).
	 * @param target must be one of TEXTURE_2D, TEXTURE_CUBE_MAP_POSITIVE_X, TEXTURE_CUBE_MAP_NEGATIVE_X, TEXTURE_CUBE_MAP_POSITIVE_Y, TEXTURE_CUBE_MAP_NEGATIVE_Y, TEXTURE_CUBE_MAP_POSITIVE_Z, or TEXTURE_CUBE_MAP_NEGATIVE_Z.
	 * @param level specifies the mipmap level to copy into.
	 * @param xOffset
	 * @param yOffset the position in the texture to store the copied pixels.
	 * @param x
	 * @param y the position in the framebuffer to read the pixels to copy.
	 * @param width
	 * @param height the size of the region to copy.
	 */
	fun copyTexSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, x: Int, y: Int, width: Int, height: Int)

	/**
	 * Creates a buffer. A buffer is memory used to store data passed to the shader program through attributes.
	 * See also bindBuffer(), bufferData(), bufferSubData(), deleteBuffer(), isBuffer(), vertexAttribPointer() and
	 * enableVertexAttribArray().
	 */
	fun createBuffer(): GlBufferRef

	/**
	 * Creates a framebuffer that can be used for offscreen rendering. See also bindFramebuffer(),
	 * checkFramebufferStatus(), deleteFramebuffer(), framebufferRenderbuffer(), framebufferTexture2D(),
	 * getFramebufferAttachmentParameter(), and isFramebuffer().
	 */
	fun createFramebuffer(): GlFramebufferRef

	/**
	 * Creates a shader program. A shader program consists of a vertex shader and fragment shader. Use
	 * attachShader() to associate shaders with the program and linkProgram() to finalize the program. After linking,
	 * use useProgram() to select the program to use.
	 */
	fun createProgram(): GlProgramRef

	/**
	 * Creates a renderbuffer. A renderbuffer is an offscreen section of memory used to store the result of rendering,
	 * such as the color buffer, depth buffer, or stencil buffer. See also framebufferRenderbuffer(),
	 * renderbufferStorage().
	 */
	fun createRenderbuffer(): GlRenderbufferRef

	/**
	 * Creates a vertex or fragment shader. type must be either VERTEX_SHADER or FRAGMENT_SHADER. Shaders must be
	 * compiled using compileShader() and then attached to a GlProgram using attachShader() before they can be used.
	 */
	fun createShader(type: Int): GlShaderRef

	/**
	 * Creates a texture. Use activeTexture() to select a texture unit and then bindTexture() to bind a texture to
	 * that unit. See also copyTexImage2D(), copyTexSubImage2D(), deleteTexture(), framebufferTexture2D(),
	 * getTexParameter(), isTexture(), texImage2D(), texParameterf(), texParameteri(), and texSubImage2D().
	 */
	fun createTexture(): GlTextureRef

	/**
	 * Sets which side of the triangle is culled (not drawn). mode must be one of BACK, FRONT, or FRONT_AND_BACK.
	 * Defaults to BACK. To turn on culling, you must call enable(CULL_FACE). To select which face is the front or back,
	 * use frontFace().
	 */
	fun cullFace(mode: Int)

	/**
	 * Deletes the specified buffer.
	 */
	fun deleteBuffer(buffer: GlBufferRef)

	/**
	 * Deletes the specified framebuffer.
	 */
	fun deleteFramebuffer(framebuffer: GlFramebufferRef)

	/**
	 * Deletes the specified program.
	 */
	fun deleteProgram(program: GlProgramRef)

	/**
	 * Deletes the specified renderbuffer.
	 */
	fun deleteRenderbuffer(renderbuffer: GlRenderbufferRef)

	/**
	 * Deletes the specified shader.
	 */
	fun deleteShader(shader: GlShaderRef)

	/**
	 * Deletes the specified texture.
	 */
	fun deleteTexture(texture: GlTextureRef)

	/**
	 * Specifies what function used to compare the rendered depth with the existing depth in the framebuffer to
	 * determine if the pixel will be written to the framebuffer. func must be one of NEVER, LESS, EQUAL, LEQUAL,
	 * GREATER, NOTEQUAL, GEQUAL, or ALWAYS. Defaults to LESS. Depth test will only be used if enabled with
	 * enable(DEPTH_TEST).
	 */
	fun depthFunc(func: Int)

	/**
	 * Turns on or off writing to the depth buffer. Defaults to true. Use getParameter(gl.DEPTH_WRITEMASK) to get the
	 * current value. Depth test will only be used if enabled with enable(DEPTH_TEST).
	 */
	fun depthMask(flag: Boolean)

	/**
	 * Sets how z values returned from the vertex shader are mapped to values to store in the depth buffer.
	 * This mapping is necessary because the vertex shader output z values will be clipped to the range -1 to 1 but the
	 * depth buffer stores depth values in the range 0 to 1.
	 * @param zNear specifies what the vertex shader's -1 maps to in the depth buffer.
	 * @param zFar specifies what the vertex shader's 1 maps to in the depth buffer. By default, zNear is 0 and zFar is 1.
	 */
	fun depthRange(zNear: Float, zFar: Float)

	/**
	 * Detaches shader from program. shader must have been attached to program using attachShader().
	 */
	fun detachShader(program: GlProgramRef, shader: GlShaderRef)

	/**
	 * Turns off a capability. See enable() for a list of capabilities.
	 */
	fun disable(cap: Int)

	fun disableVertexAttribArray(index: Int)

	/**
	 * Draws primitives using the vertex buffer data (stored in the ARRAY_BUFFER buffer).
	 * @param mode must be one of POINTS, LINE_STRIP, LINE_LOOP, LINES, TRIANGLE_STRIP, TRIANGLE_FAN, or TRIANGLES.
	 * @param first specifies the index of the first vertex to draw.
	 * @param count specifies the number of vertices to draw.
	 * You must call enableVertexAttribArray() for each attribute in the vertex shader that uses the vertex data.
	 */
	fun drawArrays(mode: Int, first: Int, count: Int)

	/**
	 * Draws primitives using the vertex buffer data (stored in the ARRAY_BUFFER buffer) and the index buffer data
	 * (stored in the ELEMENT_ARRAY_BUFFER buffer).
	 * @param mode must be one of POINTS, LINE_STRIP, LINE_LOOP, LINES, TRIANGLE_STRIP, TRIANGLE_FAN, or TRIANGLES.
	 * @param count the number of elements to be rendered.
	 * @param type must be one of UNSIGNED_BYTE or UNSIGNED_SHORT.
	 * @param offset an offset in the element array buffer. Must be a valid multiple of the size of the given type.
	 * You must call enableVertexAttribArray() for each attribute in the vertex shader that uses the vertex data.
	 */
	fun drawElements(mode: Int, count: Int, type: Int, offset: Int)

	/**
	 * Turns on a capability. capability must be one of the following:
	 * BLEND if enabled, will combine the color generated by the fragment shader with the existing color in the framebuffer using the method specified by blendFunc(). Most commonly used to enable alpha blending. Defaults to disabled.
	 * CULL_FACE if enabled, will cull (not draw) triangles based on which face is visible. See cullFace() and frontFace() to configure culling. Defaults to disabled.
	 * DEPTH_TEST if enabled, fragments will only be written to the framebuffer if they pass the depth function (set with gl.depthFunc()). See also depthMask(), and depthRange(). Most commonly used to draw closer objects on top of further away objects. Defaults to disabled.
	 * DITHER if enabled, the colors will be dithered when written to the color buffer. Defaults to enabled.
	 * POLYGON_OFFSET_FILL if enabled, the offset specified by polygonOffset will be added to the depth for the fragment when writing to the depth buffer. Most commonly used to draw decals on top of already drawn surfaces. Defaults to disabled.
	 * SAMPLE_COVERAGE Defaults to disabled.
	 * SAMPLE_ALPHA_TO_COVERAGE Defaults to disabled.
	 * SCISSOR_TEST if enabled, fragments outside the scissor rectangle (set with scissor() will not be drawn. Defaults to disabled.
	 * STENCIL_TEST if enabled, perform a stencil test on each fragment and update the stencil buffer. See also stencilFunc and stencilOp. Defaults to disabled.
	 * Use disable() to turn off the capability.
	 */
	fun enable(cap: Int)

	/**
	 * Turns on passing data to the vertex shader from the vertex buffer for the specified attribute. Use
	 * getAttribLocation() to retrieve the location of an attribute by name.
	 */
	fun enableVertexAttribArray(index: Int)

	fun finish()

	fun flush()

	/**
	 * Specifies the renderbuffer to use as destination of rendering for the current framebuffer (set with the most recent bindFramebuffer() call).
	 * target must be FRAMEBUFFER.
	 * attachment determines what is rendered into renderbuffer. Pass COLOR_ATTACHMENT0 for color data, DEPTH_ATTACHMENT for depth data, or STENCIL_ATTACHMENT for stencil data.
	 * renderbufferTarget must be RENDERBUFFER.
	 * renderbuffer the buffer to store the rendered output.
	 */
	fun framebufferRenderbuffer(target: Int, attachment: Int, renderbufferTarget: Int, renderbuffer: GlRenderbufferRef)

	/**
	 * Specifies the texture to use as destination of rendering for the current framebuffer (set with the most recent bindFramebuffer() call).
	 * target must be FRAMEBUFFER.
	 * attachment determines what is rendered into renderbuffer. Pass COLOR_ATTACHMENT0 for color data, DEPTH_ATTACHMENT for depth data, or STENCIL_ATTACHMENT for stencil data.
	 * target must be one of TEXTURE_2D, TEXTURE_CUBE_MAP_POSITIVE_X, TEXTURE_CUBE_MAP_NEGATIVE_X, TEXTURE_CUBE_MAP_POSITIVE_Y, TEXTURE_CUBE_MAP_NEGATIVE_Y, TEXTURE_CUBE_MAP_POSITIVE_Z, or TEXTURE_CUBE_MAP_NEGATIVE_Z.
	 * texture the texture to store the rendered output.
	 * level must be 0.
	 */
	fun framebufferTexture2D(target: Int, attachment: Int, textureTarget: Int, texture: GlTextureRef, level: Int)

	/**
	 * Determines which side of triangles is the front face. mode must be one of CW or CCW. To turn on culling, you must call enable(CULL_FACE). To select which face is culled, use cullFace().
	 */
	fun frontFace(mode: Int)

	/**
	 * Generate the mipmap for the bound texture in the active texture unit (set through activeTexture() and bindTexture). A mipmap is a set of textures that are 1/2, 1/4, 1/8, etc of the original image. The mipmap allows higher quality rendering when drawing the texture at smaller sizes. target must be one of TEXTURE_2D or TEXTURE_CUBE_MAP. Note, you can only generate mipmaps for textures where the width and height are both powers of 2 (such as 128, 256, 512, etc).
	 */
	fun generateMipmap(target: Int)

	/**
	 * Returns information about an attribute in program. program must be linked before calling getActiveAttrib(). index must be between 0 and gl.getProgramParameter(program, ACTIVE_ATTRIBUTES) - 1.
	 */
	fun getActiveAttrib(program: GlProgramRef, index: Int): GlActiveInfoRef

	/**
	 * Returns information about a uniform in program. program must be linked before calling getActiveUniform(). index must be between 0 and gl.getProgramParameter(program, ACTIVE_UNIFORMS) - 1.
	 */
	fun getActiveUniform(program: GlProgramRef, index: Int): GlActiveInfoRef

	/**
	 * return the handles of the shader objects attached to a program object.
	 */
	fun getAttachedShaders(program: GlProgramRef): Array<GlShaderRef>

	/**
	 * Return the location of an attribute variable.
	 */
	fun getAttribLocation(program: GlProgramRef, name: String): Int

	/**
	 * Returns the first error hit since the last time getError() was called. The returned value will be one of
	 * NO_ERROR, INVALID_ENUM, INVALID_VALUE, INVALID_OPERATION, INVALID_FRAMEBUFFER_OPERATION, or OUT_OF_MEMORY.
	 */
	fun getError(): Int

	fun getProgramInfoLog(program: GlProgramRef): String?

	fun getShaderInfoLog(shader: GlShaderRef): String?

	fun getUniformLocation(program: GlProgramRef, name: String): GlUniformLocationRef?

	fun hint(target: Int, mode: Int)

	fun isBuffer(buffer: GlBufferRef): Boolean

	fun isEnabled(cap: Int): Boolean

	fun isFramebuffer(framebuffer: GlFramebufferRef): Boolean

	fun isProgram(program: GlProgramRef): Boolean

	fun isRenderbuffer(renderbuffer: GlRenderbufferRef): Boolean

	fun isShader(shader: GlShaderRef): Boolean

	fun isTexture(texture: GlTextureRef): Boolean

	fun lineWidth(width: Float)

	/**
	 * linkProgram links the program object specified by program. A shader object of type GL_VERTEX_SHADER
	 * attached to program is used to create an executable that will run on the programmable vertex processor.
	 * A shader object of type GL_FRAGMENT_SHADER attached to program is used to create an executable that will run
	 * on the programmable fragment processor.
	 */
	fun linkProgram(program: GlProgramRef)

	/**
	 * Sets options that affect readPixels, texImage2D, texSubImage2D.
	 *
	 * PACK_ALIGNMENT						1, 2, 4, or 8					4
	 * UNPACK_ALIGNMENT						1, 2, 4, or 8					4
	 * UNPACK_FLIP_Y_WEBGL					true or false					false
	 * UNPACK_PREMULTIPLY_ALPHA_WEBGL		true or false					false
	 * UNPACK_COLORSPACE_CONVERSION_WEBGL	BROWSER_DEFAULT_WEBGL or NONE	BROWSER_DEFAULT_WEBGL
	 */
	fun pixelStorei(pName: Int, param: Int)

	fun polygonOffset(factor: Float, units: Float)

	/**
	 * Reads pixels from the framebuffer and copies them to data.
	 * Note: If the window is obscured and the frame buffer being read is not an off-screen buffer, the data may contain
	 * garbage.
	 */
	fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: NativeBuffer<Byte>)

	fun renderbufferStorage(target: Int, internalFormat: Int, width: Int, height: Int)

	fun sampleCoverage(value: Float, invert: Boolean)

	fun scissor(x: Int, y: Int, width: Int, height: Int)

	fun shaderSource(shader: GlShaderRef, source: String)

	/**
	 * @param ref Specifies the reference value for the stencil test. ref is clamped to the range [0,2n−1], where n is
	 * the number of bitplanes in the stencil buffer. The initial value is 0.
	 */
	fun stencilFunc(func: Int, ref: Int, mask: Int)

	fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int)

	fun stencilMask(mask: Int)

	fun stencilMaskSeparate(face: Int, mask: Int)

	fun stencilOp(fail: Int, zfail: Int, zpass: Int)

	fun stencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int)

	/**
	 * Specifies the size and data of the bound texture in the active texture unit (set through activeTexture() and bindTexture).
	 * @param target must be one of TEXTURE_2D, TEXTURE_CUBE_MAP_POSITIVE_X, TEXTURE_CUBE_MAP_NEGATIVE_X, TEXTURE_CUBE_MAP_POSITIVE_Y, TEXTURE_CUBE_MAP_NEGATIVE_Y, TEXTURE_CUBE_MAP_POSITIVE_Z, or TEXTURE_CUBE_MAP_NEGATIVE_Z.
	 * @param level is the mipmap level (0 is base level).
	 * @param internalFormat must be one of ALPHA, LUMINANCE, LUMINANCE_ALPHA, RGB, or RGBA.
	 * @param width  the size of the texture.
	 * @param height
	 * @param border must be 0.
	 * @param format must match internalFormat.
	 * @param type must be one of UNSIGNED_BYTE, UNSIGNED_SHORT_5_6_5, UNSIGNED_SHORT_4_4_4_4, or UNSIGNED_5_5_5_1;
	 * @param pixels is the data to load into the texture. May be null to allocate the texture without data.
	 * The loaded data is affected by the pixelStorei() options. You can also use copyTexSubImage2D or texSubImage2D to initialize the texture.
	 */
	fun texImage2Db(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: NativeBuffer<Byte>?)

	/**
	 * Specifies the size and data of the bound texture in the active texture unit (set through activeTexture() and bindTexture).
	 * @param target must be one of TEXTURE_2D, TEXTURE_CUBE_MAP_POSITIVE_X, TEXTURE_CUBE_MAP_NEGATIVE_X, TEXTURE_CUBE_MAP_POSITIVE_Y, TEXTURE_CUBE_MAP_NEGATIVE_Y, TEXTURE_CUBE_MAP_POSITIVE_Z, or TEXTURE_CUBE_MAP_NEGATIVE_Z.
	 * @param level is the mipmap level (0 is base level).
	 * @param internalFormat must be one of ALPHA, LUMINANCE, LUMINANCE_ALPHA, RGB, or RGBA.
	 * @param width  the size of the texture.
	 * @param height
	 * @param border must be 0.
	 * @param format must match internalFormat.
	 * @param type must be one of UNSIGNED_BYTE, UNSIGNED_SHORT_5_6_5, UNSIGNED_SHORT_4_4_4_4, or UNSIGNED_5_5_5_1;
	 * @param pixels is the data to load into the texture. May be null to allocate the texture without data.
	 * The loaded data is affected by the pixelStorei() options. You can also use copyTexSubImage2D or texSubImage2D to initialize the texture.
	 */
	fun texImage2Df(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: NativeBuffer<Float>?)

	/**
	 * Specifies the data for the bound texture in the active texture unit (set through activeTexture() and bindTexture).
	 * @param target must be one of TEXTURE_2D, TEXTURE_CUBE_MAP_POSITIVE_X, TEXTURE_CUBE_MAP_NEGATIVE_X, TEXTURE_CUBE_MAP_POSITIVE_Y, TEXTURE_CUBE_MAP_NEGATIVE_Y, TEXTURE_CUBE_MAP_POSITIVE_Z, or TEXTURE_CUBE_MAP_NEGATIVE_Z.
	 * @param level is the mipmap level (0 is base level).
	 * @param internalFormat must be one of ALPHA, LUMINANCE, LUMINANCE_ALPHA, RGB, or RGBA.
	 * @param format must match internalformat.
	 * @param type must be one of UNSIGNED_BYTE, UNSIGNED_SHORT_5_6_5, UNSIGNED_SHORT_4_4_4_4, or UNSIGNED_SHORT_5_5_5_1;
	 * @param texture is the data to load into the texture.
	 * The loaded data is affected by the pixelStorei() options. You can also use copyTexSubImage2D or texSubImage2D to initialize the texture.
	 */
	fun texImage2D(target: Int, level: Int, internalFormat: Int, format: Int, type: Int, texture: Texture)

	fun texParameterf(target: Int, pName: Int, param: Float)


	/**
	 * Sets the value of the specified parameter for the bound texture in the active texture unit
	 * (set through activeTexture() and bindTexture). Use getTexParameter() to get texture parameters.
	 * @param pName parameter				Initial Value				Valid Values
	 *				TEXTURE_MIN_FILTER		NEAREST_MIPMAP_LINEAR 		NEAREST, LINEAR, NEAREST_MIPMAP_NEAREST, LINEAR_MIPMAP_NEAREST, NEAREST_MIPMAP_LINEAR, or LINEAR_MIPMAP_LINEAR
	 *				TEXTURE_MAG_FILTER		LINEAR						NEAREST or LINEAR
	 *				TEXTURE_WRAP_S			REPEAT						CLAMP_TO_EDGE, MIRRORED_REPEAT, or REPEAT
	 *				TEXTURE_WRAP_T			REPEAT						CLAMP_TO_EDGE, MIRRORED_REPEAT, or REPEAT
	 */
	fun texParameteri(target: Int, pName: Int, param: Int)

//	public fun texSubImage2D(target: Int, level: Int, offsetX: Int, yOffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: BufferView)

	fun texSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, format: Int, type: Int, texture: Texture)

	fun uniform1f(location: GlUniformLocationRef, x: Float)

	fun uniform1fv(location: GlUniformLocationRef, v: NativeBuffer<Float>)

	fun uniform1i(location: GlUniformLocationRef, x: Int)

	fun uniform1iv(location: GlUniformLocationRef, v: NativeBuffer<Int>)

	fun uniform2f(location: GlUniformLocationRef, x: Float, y: Float)
	fun uniform2f(location: GlUniformLocationRef, v: Vector2) = uniform2f(location, v.x, v.y)

	fun uniform2fv(location: GlUniformLocationRef, v: NativeBuffer<Float>)

	fun uniform2i(location: GlUniformLocationRef, x: Int, y: Int)

	fun uniform2iv(location: GlUniformLocationRef, v: NativeBuffer<Int>)

	fun uniform3f(location: GlUniformLocationRef, x: Float, y: Float, z: Float)
	fun uniform3f(location: GlUniformLocationRef, v: Vector3) = uniform3f(location, v.x, v.y, v.z)
	fun uniform3f(location: GlUniformLocationRef, c: ColorRo) = uniform3f(location, c.r, c.g, c.b)

	fun uniform3fv(location: GlUniformLocationRef, v: NativeBuffer<Float>)

	fun uniform3i(location: GlUniformLocationRef, x: Int, y: Int, z: Int)

	fun uniform3iv(location: GlUniformLocationRef, v: NativeBuffer<Int>)

	fun uniform4f(location: GlUniformLocationRef, x: Float, y: Float, z: Float, w: Float)
	fun uniform4f(location: GlUniformLocationRef, color: ColorRo) {
		uniform4f(location, color.r, color.g, color.b, color.a)
	}

	fun uniform4fv(location: GlUniformLocationRef, v: NativeBuffer<Float>)

	fun uniform4i(location: GlUniformLocationRef, x: Int, y: Int, z: Int, w: Int)

	fun uniform4iv(location: GlUniformLocationRef, v: NativeBuffer<Int>)

	fun uniformMatrix2fv(location: GlUniformLocationRef, transpose: Boolean, value: NativeBuffer<Float>)

	fun uniformMatrix3fv(location: GlUniformLocationRef, transpose: Boolean, value: NativeBuffer<Float>)

	fun uniformMatrix4fv(location: GlUniformLocationRef, transpose: Boolean, value: NativeBuffer<Float>)

	fun useProgram(program: GlProgramRef?)

	fun validateProgram(program: GlProgramRef)

	fun vertexAttrib1f(index: Int, x: Float)

	fun vertexAttrib1fv(index: Int, values: NativeBuffer<Float>)

	fun vertexAttrib2f(index: Int, x: Float, y: Float)

	fun vertexAttrib2fv(index: Int, values: NativeBuffer<Float>)

	fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float)

	fun vertexAttrib3fv(index: Int, values: NativeBuffer<Float>)

	fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float)

	fun vertexAttrib4fv(index: Int, values: NativeBuffer<Float>)

	/**
	 * Defines the data for the specified shader attribute.
	 * @param index is the location of the shader attribute. Use getAttribLocation() to get the location if you did not specify it explicitly with bindAttribLocation().
	 * @param size is the number of components for each attribute and must be 1, 2, 3, or 4.
	 * @param type must be one of BYTE, UNSIGNED_BYTE, SHORT, UNSIGNED_SHORT, or FLOAT.
	 * @param normalized if true and dataType is not FLOAT, the data will be mapped to the range -1 to 1 for signed types and the range 0 to 1 for unsigned types.
	 * @param stride indicates the number of bytes between the consecutive attributes. If stride is 0, size * sizeof(dataType) will be used.
	 * @param offset is the number of bytes before the first attribute.
	 */
	fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int)

	/**
	 * Specifies the affine transformation of x and y from normalized device coordinates to window coordinates.
	 *
	 * Note - if the current frame buffer is the window, the width and height should be multiplied by
	 * [com.acornui.core.graphics.Window.scaleX] and [com.acornui.core.graphics.Window.scaleY]
	 *
	 * @param x Specify the lower left corner of the viewport rectangle, in pixels. The initial value is (0,0).
	 * @param y
	 * @param width Specify the width and height of the viewport. When a GL context is first attached to a window,
	 * width and height are set to the dimensions of that window.
	 * @param height
	 */
	fun viewport(x: Int, y: Int, width: Int, height: Int)

	/**
	 * Return the uniform value at the passed location in the passed program.
	 */
	fun getUniformb(program: GlProgramRef, location: GlUniformLocationRef): Boolean

	/**
	 * Return the uniform value at the passed location in the passed program.
	 */
	fun getUniformi(program: GlProgramRef, location: GlUniformLocationRef): Int

	/**
	 * Return the uniform value at the passed location in the passed program.
	 */
	fun getUniformf(program: GlProgramRef, location: GlUniformLocationRef): Float

	/**
	 * Return the information requested in pName about the vertex attribute at the passed index.
	 *
	 * @param pName one of VERTEX_ATTRIB_ARRAY_SIZE, VERTEX_ATTRIB_ARRAY_STRIDE, VERTEX_ATTRIB_ARRAY_TYPE
	 */
	fun getVertexAttribi(index: Int, pName: Int): Int

	/**
	 * Return the information requested in pName about the vertex attribute at the passed index.
	 *
	 * @param pName one of VERTEX_ATTRIB_ARRAY_ENABLED, VERTEX_ATTRIB_ARRAY_NORMALIZED
	 */
	fun getVertexAttribb(index: Int, pName: Int): Boolean

	/**
	 * Return the value for the passed pName given the passed target. The type returned is the natural type for the requested
	 * pName, as given in the following table: If an attempt is made to call this function with no GlTexture bound (see above),
	 * an INVALID_OPERATION error is generated.
	 *
	 * @param pName one of TEXTURE_MAG_FILTER, TEXTURE_MIN_FILTER, TEXTURE_WRAP_S, TEXTURE_WRAP_T
	 */
	fun getTexParameter(target: Int, pName: Int): Int

	/**
	 * Return the value for the passed pName given the passed shader.
	 *
	 * @param pName one of DELETE_STATUS, COMPILE_STATUS
	 */
	fun getShaderParameterb(shader: GlShaderRef, pName: Int): Boolean

	/**
	 * Return the value for the passed pName given the passed shader.
	 *
	 * @param pName one of SHADER_TYPE, INFO_LOG_LENGTH, SHADER_SOURCE_LENGTH
	 */
	fun getShaderParameteri(shader: GlShaderRef, pName: Int): Int

	/**
	 * Return the value for the passed pName given the passed target.
	 *
	 * @param pName one of RENDERBUFFER_WIDTH, RENDERBUFFER_HEIGHT, RENDERBUFFER_INTERNAL_FORMAT, RENDERBUFFER_RED_SIZE,
	 *           RENDERBUFFER_GREEN_SIZE, RENDERBUFFER_BLUE_SIZE, RENDERBUFFER_ALPHA_SIZE, RENDERBUFFER_DEPTH_SIZE,
	 *           RENDERBUFFER_STENCIL_SIZE
	 */
	fun getRenderbufferParameter(target: Int, pName: Int): Int

	/**
	 * Return the value for the passed pname.
	 *
	 * @param pname one of BLEND, CULL_FACE, DEPTH_TEST, DEPTH_WRITEMASK, DITHER, POLYGON_OFFSET_FILL, SAMPLE_COVERAGE_INVERT,
	 *           SCISSOR_TEST, STENCIL_TEST, UNPACK_FLIP_Y_WEBGL, UNPACK_PREMULTIPLY_ALPHA_WEBGL
	 */
	fun getParameterb(pName: Int): Boolean

	/**
	 * Return the value for the passed pname.
	 *
	 * @param pname one of ACTIVE_TEXTURE, ALPHA_BITS, BLEND_DST_ALPHA, BLEND_DST_RGB, BLEND_EQUATION_ALPHA, BLEND_EQUATION_RGB,
	 *           BLEND_SRC_ALPHA, BLEND_SRC_RGB, BLUE_BITS, CULL_FACE_MODE, DEPTH_BITS, DEPTH_FUNC, FRONT_FACE,
	 *           GENERATE_MIPMAP_HINT, GREEN_BITS, IMPLEMENTATION_COLOR_READ_FORMAT, IMPLEMENTATION_COLOR_READ_TYPE,
	 *           MAX_COMBINED_TEXTURE_IMAGE_UNITS, MAX_CUBE_MAP_TEXTURE_SIZE, MAX_FRAGMENT_UNIFORM_VECTORS, MAX_RENDERBUFFER_SIZE,
	 *           MAX_TEXTURE_IMAGE_UNITS, MAX_TEXTURE_SIZE, MAX_VARYING_VECTORS, MAX_VERTEX_ATTRIBS,
	 *           MAX_VERTEX_TEXTURE_IMAGE_UNITS, MAX_VERTEX_UNIFORM_VECTORS, NUM_COMPRESSED_TEXTURE_FORMATS, PACK_ALIGNMENT,
	 *           RED_BITS, SAMPLE_BUFFERS, SAMPLES, STENCIL_BACK_FAIL, STENCIL_BACK_FUNC, STENCIL_BACK_PASS_DEPTH_FAIL,
	 *           STENCIL_BACK_PASS_DEPTH_PASS, STENCIL_BACK_REF, STENCIL_BACK_VALUE_MASK, STENCIL_BACK_WRITEMASK, STENCIL_BITS,
	 *           STENCIL_CLEAR_VALUE, STENCIL_FAIL, STENCIL_FUNC, STENCIL_PASS_DEPTH_FAIL, STENCIL_PASS_DEPTH_PASS, STENCIL_REF,
	 *           STENCIL_VALUE_MASK, STENCIL_WRITEMASK, SUBPIXEL_BITS, UNPACK_ALIGNMENT
	 */
	fun getParameteri(pName: Int): Int

	/**
	 * Return the value for the passed pName given the passed program.
	 *
	 * @param pName one of DELETE_STATUS, LINK_STATUS, VALIDATE_STATUS
	 */
	fun getProgramParameterb(program: GlProgramRef, pName: Int): Boolean

	/**
	 * Return the value for the passed pName given the passed program.
	 *
	 * @param pName one of INFO_LOG_LENGTH, ATTACHED_SHADERS, ACTIVE_ATTRIBUTES, ACTIVE_ATTRIBUTE_MAX_LENGTH, ACTIVE_UNIFORMS,
	 *           ACTIVE_UNIFORM_MAX_LENGTH
	 */
	fun getProgramParameteri(program: GlProgramRef, pName: Int): Int

	/**
	 * target must be one of ARRAY_BUFFER or ELEMENT_ARRAY_BUFFER. parameter
	 * must be one of BUFFER_SIZE or BUFFER_USAGE.
	 */
	fun getBufferParameter(target: Int, pName: Int): Int

	/**
	 * Return the value for the passed pName given the passed target and attachment.
	 *
	 * @param pName one of FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL, FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE
	 */
	fun getFramebufferAttachmentParameteri(target: Int, attachment: Int, pName: Int): Int

	fun getSupportedExtensions(): List<String>

}

interface GlProgramRef
interface GlShaderRef
interface GlBufferRef
interface GlFramebufferRef
interface GlRenderbufferRef
interface GlTextureRef
interface GlActiveInfoRef {
	var name: String
	var size: Int
	var type: Int
}

interface GlUniformLocationRef

fun Gl20.scissor(x: Float, y: Float, width: Float, height: Float) {
	scissor(round(x).toInt(), round(y).toInt(), round(width).toInt(), round(height).toInt())
}

private val matrixValuesBuffer: NativeBuffer<Float> by lazy { BufferFactory.instance.floatBuffer(16) }
fun Gl20.uniformMatrix4fv(location: GlUniformLocationRef, transpose: Boolean, value: Matrix4Ro) = uniformMatrix4fv(location, transpose, value.values)
fun Gl20.uniformMatrix4fv(location: GlUniformLocationRef, transpose: Boolean, value: List<Float>) {
	val buffer = matrixValuesBuffer
	buffer.clear()
	for (i in 0..value.lastIndex) {
		buffer.put(value[i])
	}
	buffer.flip()
	uniformMatrix4fv(location, transpose, buffer)
}