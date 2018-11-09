/*
 * Derived from com.google.gwt.webgl.client.WebGLRenderingContext by Nicholas Bilyk 2015
 * Copyright 2010 Google Inc.
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
 *
 * Documentation derived from http://www.javascripture.com/WebGLRenderingContext
 *     http://creativecommons.org/licenses/by-sa/2.5/
 */

package com.acornui.js.html

import org.khronos.webgl.WebGLContextAttributes
import org.khronos.webgl.WebGLRenderingContext
import org.w3c.dom.HTMLCanvasElement

object WebGl {

	/**
	 * Returns a WebGLRenderingContext for the given canvas element. Returns null if no 3d context is available.
	 */
	fun getContext(canvas: HTMLCanvasElement, attributes: WebGLContextAttributes = WebGLContextAttributes()): WebGLRenderingContext? {
		// TODO: debug context?
		val names = arrayOf("webgl", "experimental-webgl", "moz-webgl", "webkit-webgl", "webkit-3d")
		for (i in 0..names.lastIndex) {
			val context = canvas.getContext(names[i], attributes)
			if (context != null) {
				return context as WebGLRenderingContext
			}
		}
		return null
	}

} // end WebGL object

/**
 * The WebGLRenderingContext is an object that is used to issue WebGL rendering commands to a canvas. The
 * WebGLRenderingContext is obtained by passing 'webgl' (many browsers use 'experimental-webgl' currently) to the
 * HTMLCanvasElement.getContext() method. See WebGLContextAttributes for configuration options you can specify when
 * calling getContext().
 * For detailed information on the shader language used by WebGL, see the GLSL Specification.
 * http://www.khronos.org/registry/gles/specs/2.0/GLSL_ES_Specification_1.0.17.pdf
 *
 */
//external interface WebGLRenderingContext {
//
//	companion object {
//
//		// These constants are populated with values, but they're just for reference, native traits will not produce code.
//
//		/* ClearBufferMask */
//		val DEPTH_BUFFER_BIT: Int = 256
//		val STENCIL_BUFFER_BIT: Int = 1024
//		val COLOR_BUFFER_BIT: Int = 16384
//
//		/* BeginMode */
//		val POINTS: Int = 0
//		val LINES: Int = 1
//		val LINE_LOOP: Int = 2
//		val LINE_STRIP: Int = 3
//		val TRIANGLES: Int = 4
//		val TRIANGLE_STRIP: Int = 5
//		val TRIANGLE_FAN: Int = 6
//
//		/* BlendingFactorDest */
//		val ZERO: Int = 0
//		val ONE: Int = 1
//		val SRC_COLOR: Int = 768
//		val ONE_MINUS_SRC_COLOR: Int = 769
//		val SRC_ALPHA: Int = 770
//		val ONE_MINUS_SRC_ALPHA: Int = 771
//		val DST_ALPHA: Int = 772
//		val ONE_MINUS_DST_ALPHA: Int = 773
//
//		/* BlendingFactorSrc */
//		/* ZERO */
//		/* ONE */
//		val DST_COLOR: Int = 774
//		val ONE_MINUS_DST_COLOR: Int = 775
//		val SRC_ALPHA_SATURATE: Int = 776
//
//		/* BlendEquationSeparate */
//		val FUNC_ADD: Int = 32774
//		val BLEND_EQUATION: Int = 32777
//		val BLEND_EQUATION_RGB: Int = 32777 /* same as BLEND_EQUATION */
//		val BLEND_EQUATION_ALPHA: Int = 34877
//
//		/* BlendSubtract */
//		val FUNC_SUBTRACT: Int = 32778
//		val FUNC_REVERSE_SUBTRACT: Int = 32779
//
//		/* Separate Blend Functions */
//		val BLEND_DST_RGB: Int = 32968
//		val BLEND_SRC_RGB: Int = 32969
//		val BLEND_DST_ALPHA: Int = 32970
//		val BLEND_SRC_ALPHA: Int = 32971
//		val CONSTANT_COLOR: Int = 32769
//		val ONE_MINUS_CONSTANT_COLOR: Int = 32770
//		val CONSTANT_ALPHA: Int = 32771
//		val ONE_MINUS_CONSTANT_ALPHA: Int = 32772
//		val BLEND_COLOR: Int = 32773
//
//		/* Buffer Objects */
//		val ARRAY_BUFFER: Int = 34962
//		val ELEMENT_ARRAY_BUFFER: Int = 34963
//		val ARRAY_BUFFER_BINDING: Int = 34964
//		val ELEMENT_ARRAY_BUFFER_BINDING: Int = 34965
//
//		val STREAM_DRAW: Int = 35040
//		val STATIC_DRAW: Int = 35044
//		val DYNAMIC_DRAW: Int = 35048
//
//		val BUFFER_SIZE: Int = 34660
//		val BUFFER_USAGE: Int = 34661
//
//		val CURRENT_VERTEX_ATTRIB: Int = 34342
//
//		/* CullFaceMode */
//		val FRONT: Int = 1028
//		val BACK: Int = 1029
//		val FRONT_AND_BACK: Int = 1032
//
//		val TEXTURE_2D: Int = 3553
//
//		/* EnableCap */
//		val CULL_FACE: Int = 2884
//		val BLEND: Int = 3042
//		val DITHER: Int = 3024
//		val STENCIL_TEST: Int = 2960
//		val DEPTH_TEST: Int = 2929
//		val SCISSOR_TEST: Int = 3089
//		val POLYGON_OFFSET_FILL: Int = 32823
//		val SAMPLE_ALPHA_TO_COVERAGE: Int = 32926
//		val SAMPLE_COVERAGE: Int = 32928
//
//		/* ErrorCode */
//		val NO_ERROR: Int = 0
//		val INVALID_ENUM: Int = 1280
//		val INVALID_VALUE: Int = 1281
//		val INVALID_OPERATION: Int = 1282
//		val OUT_OF_MEMORY: Int = 1285
//
//		/* FrontFaceDirection */
//		val CW: Int = 2304
//		val CCW: Int = 2305
//
//		/* GetPName */
//		val LINE_WIDTH: Int = 2849
//		val ALIASED_POINT_SIZE_RANGE: Int = 33901
//		val ALIASED_LINE_WIDTH_RANGE: Int = 33902
//		val CULL_FACE_MODE: Int = 2885
//		val FRONT_FACE: Int = 2886
//		val DEPTH_RANGE: Int = 2928
//		val DEPTH_WRITEMASK: Int = 2930
//		val DEPTH_CLEAR_VALUE: Int = 2931
//		val DEPTH_FUNC: Int = 2932
//		val STENCIL_CLEAR_VALUE: Int = 2961
//		val STENCIL_FUNC: Int = 2962
//		val STENCIL_FAIL: Int = 2964
//		val STENCIL_PASS_DEPTH_FAIL: Int = 2965
//		val STENCIL_PASS_DEPTH_PASS: Int = 2966
//		val STENCIL_REF: Int = 2967
//		val STENCIL_VALUE_MASK: Int = 2963
//		val STENCIL_WRITEMASK: Int = 2968
//		val STENCIL_BACK_FUNC: Int = 34816
//		val STENCIL_BACK_FAIL: Int = 34817
//		val STENCIL_BACK_PASS_DEPTH_FAIL: Int = 34818
//		val STENCIL_BACK_PASS_DEPTH_PASS: Int = 34819
//		val STENCIL_BACK_REF: Int = 36003
//		val STENCIL_BACK_VALUE_MASK: Int = 36004
//		val STENCIL_BACK_WRITEMASK: Int = 36005
//		val VIEWPORT: Int = 2978
//		val SCISSOR_BOX: Int = 3088
//
//		/* SCISSOR_TEST */
//		val COLOR_CLEAR_VALUE: Int = 3106
//		val COLOR_WRITEMASK: Int = 3107
//		val UNPACK_ALIGNMENT: Int = 3317
//		val PACK_ALIGNMENT: Int = 3333
//		val MAX_TEXTURE_SIZE: Int = 3379
//		val MAX_VIEWPORT_DIMS: Int = 3386
//		val SUBPIXEL_BITS: Int = 3408
//		val RED_BITS: Int = 3410
//		val GREEN_BITS: Int = 3411
//		val BLUE_BITS: Int = 3412
//		val ALPHA_BITS: Int = 3413
//		val DEPTH_BITS: Int = 3414
//		val STENCIL_BITS: Int = 3415
//		val POLYGON_OFFSET_UNITS: Int = 10752
//
//		/* POLYGON_OFFSET_FILL */
//		val POLYGON_OFFSET_FACTOR: Int = 32824
//		val TEXTURE_BINDING_2D: Int = 32873
//		val SAMPLE_BUFFERS: Int = 32936
//		val SAMPLES: Int = 32937
//		val SAMPLE_COVERAGE_VALUE: Int = 32938
//		val SAMPLE_COVERAGE_INVERT: Int = 32939
//
//		/* GetTextureParameter */
//		/* TEXTURE_MAG_FILTER */
//		/* TEXTURE_MIN_FILTER */
//		/* TEXTURE_WRAP_S */
//		/* TEXTURE_WRAP_T */
//
//		val NUM_COMPRESSED_TEXTURE_FORMATS: Int = 34466
//		val COMPRESSED_TEXTURE_FORMATS: Int = 34467
//
//		/* HintMode */
//		val DONT_CARE: Int = 4352
//		val FASTEST: Int = 4353
//		val NICEST: Int = 4354
//
//		/* HintTarget */
//		val GENERATE_MIPMAP_HINT: Int = 33170
//
//		/* DataType */
//		val BYTE: Int = 5120
//		val UNSIGNED_BYTE: Int = 5121
//		val SHORT: Int = 5122
//		val UNSIGNED_SHORT: Int = 5123
//		val INT: Int = 5124
//		val UNSIGNED_INT: Int = 5125
//		val FLOAT: Int = 5126
//
//		/* PixelFormat */
//		val DEPTH_COMPONENT: Int = 6402
//		val ALPHA: Int = 6406
//		val RGB: Int = 6407
//		val RGBA: Int = 6408
//		val LUMINANCE: Int = 6409
//		val LUMINANCE_ALPHA: Int = 6410
//
//		/* PixelType */
//		/* UNSIGNED_BYTE */
//		val UNSIGNED_SHORT_4_4_4_4: Int = 32819
//		val UNSIGNED_SHORT_5_5_5_1: Int = 32820
//		val UNSIGNED_SHORT_5_6_5: Int = 33635
//
//		/* Shaders */
//		val FRAGMENT_SHADER: Int = 35632
//		val VERTEX_SHADER: Int = 35633
//		val MAX_VERTEX_ATTRIBS: Int = 34921
//		val MAX_VERTEX_UNIFORM_VECTORS: Int = 36347
//		val MAX_VARYING_VECTORS: Int = 36348
//		val MAX_COMBINED_TEXTURE_IMAGE_UNITS: Int = 35661
//		val MAX_VERTEX_TEXTURE_IMAGE_UNITS: Int = 35660
//		val MAX_TEXTURE_IMAGE_UNITS: Int = 34930
//		val MAX_FRAGMENT_UNIFORM_VECTORS: Int = 36349
//		val SHADER_TYPE: Int = 35663
//		val DELETE_STATUS: Int = 35712
//		val LINK_STATUS: Int = 35714
//		val VALIDATE_STATUS: Int = 35715
//		val ATTACHED_SHADERS: Int = 35717
//		val ACTIVE_UNIFORMS: Int = 35718
//		val ACTIVE_UNIFORM_MAX_LENGTH: Int = 35719
//		val ACTIVE_ATTRIBUTES: Int = 35721
//		val ACTIVE_ATTRIBUTE_MAX_LENGTH: Int = 35722
//		val SHADING_LANGUAGE_VERSION: Int = 35724
//		val CURRENT_PROGRAM: Int = 35725
//
//		/* StencilFunction */
//		val NEVER: Int = 512
//		val LESS: Int = 513
//		val EQUAL: Int = 514
//		val LEQUAL: Int = 515
//		val GREATER: Int = 516
//		val NOTEQUAL: Int = 517
//		val GEQUAL: Int = 518
//		val ALWAYS: Int = 519
//
//		/* StencilOp */
//		/* ZERO */
//		val KEEP: Int = 7680
//		val REPLACE: Int = 7681
//		val INCR: Int = 7682
//		val DECR: Int = 7683
//		val INVERT: Int = 5386
//		val INCR_WRAP: Int = 34055
//		val DECR_WRAP: Int = 34056
//
//		/* StringName */
//		val VENDOR: Int = 7936
//		val RENDERER: Int = 7937
//		val VERSION: Int = 7938
//
//		/* TextureMagFilter */
//		val NEAREST: Int = 9728
//		val LINEAR: Int = 9729
//
//		/* TextureMinFilter */
//		/* NEAREST */
//		/* LINEAR */
//		val NEAREST_MIPMAP_NEAREST: Int = 9984
//		val LINEAR_MIPMAP_NEAREST: Int = 9985
//		val NEAREST_MIPMAP_LINEAR: Int = 9986
//		val LINEAR_MIPMAP_LINEAR: Int = 9987
//
//		/* TextureParameterName */
//		val TEXTURE_MAG_FILTER: Int = 10240
//		val TEXTURE_MIN_FILTER: Int = 10241
//		val TEXTURE_WRAP_S: Int = 10242
//		val TEXTURE_WRAP_T: Int = 10243
//
//		/* TextureTarget */
//		/* TEXTURE_2D */
//		val TEXTURE: Int = 5890
//
//		val TEXTURE_CUBE_MAP: Int = 34067
//		val TEXTURE_BINDING_CUBE_MAP: Int = 34068
//		val TEXTURE_CUBE_MAP_POSITIVE_X: Int = 34069
//		val TEXTURE_CUBE_MAP_NEGATIVE_X: Int = 34070
//		val TEXTURE_CUBE_MAP_POSITIVE_Y: Int = 34071
//		val TEXTURE_CUBE_MAP_NEGATIVE_Y: Int = 34072
//		val TEXTURE_CUBE_MAP_POSITIVE_Z: Int = 34073
//		val TEXTURE_CUBE_MAP_NEGATIVE_Z: Int = 34074
//		val MAX_CUBE_MAP_TEXTURE_SIZE: Int = 34076
//
//		/* TextureUnit */
//		val TEXTURE0: Int = 33984
//		val TEXTURE1: Int = 33985
//		val TEXTURE2: Int = 33986
//		val TEXTURE3: Int = 33987
//		val TEXTURE4: Int = 33988
//		val TEXTURE5: Int = 33989
//		val TEXTURE6: Int = 33990
//		val TEXTURE7: Int = 33991
//		val TEXTURE8: Int = 33992
//		val TEXTURE9: Int = 33993
//		val TEXTURE10: Int = 33994
//		val TEXTURE11: Int = 33995
//		val TEXTURE12: Int = 33996
//		val TEXTURE13: Int = 33997
//		val TEXTURE14: Int = 33998
//		val TEXTURE15: Int = 33999
//		val TEXTURE16: Int = 34000
//		val TEXTURE17: Int = 34001
//		val TEXTURE18: Int = 34002
//		val TEXTURE19: Int = 34003
//		val TEXTURE20: Int = 34004
//		val TEXTURE21: Int = 34005
//		val TEXTURE22: Int = 34006
//		val TEXTURE23: Int = 34007
//		val TEXTURE24: Int = 34008
//		val TEXTURE25: Int = 34009
//		val TEXTURE26: Int = 34010
//		val TEXTURE27: Int = 34011
//		val TEXTURE28: Int = 34012
//		val TEXTURE29: Int = 34013
//		val TEXTURE30: Int = 34014
//		val TEXTURE31: Int = 34015
//		val ACTIVE_TEXTURE: Int = 34016
//
//		/* TextureWrapMode */
//		val REPEAT: Int = 10497
//		val CLAMP_TO_EDGE: Int = 33071
//		val MIRRORED_REPEAT: Int = 33648
//
//		/* Uniform Types */
//		val FLOAT_VEC2: Int = 35664
//		val FLOAT_VEC3: Int = 35665
//		val FLOAT_VEC4: Int = 35666
//		val INT_VEC2: Int = 35667
//		val INT_VEC3: Int = 35668
//		val INT_VEC4: Int = 35669
//		val BOOL: Int = 35670
//		val BOOL_VEC2: Int = 35671
//		val BOOL_VEC3: Int = 35672
//		val BOOL_VEC4: Int = 35673
//		val FLOAT_MAT2: Int = 35674
//		val FLOAT_MAT3: Int = 35675
//		val FLOAT_MAT4: Int = 35676
//		val SAMPLER_2D: Int = 35678
//		val SAMPLER_CUBE: Int = 35680
//
//		/* Vertex Arrays */
//		val VERTEX_ATTRIB_ARRAY_ENABLED: Int = 34338
//		val VERTEX_ATTRIB_ARRAY_SIZE: Int = 34339
//		val VERTEX_ATTRIB_ARRAY_STRIDE: Int = 34340
//		val VERTEX_ATTRIB_ARRAY_TYPE: Int = 34341
//		val VERTEX_ATTRIB_ARRAY_NORMALIZED: Int = 34922
//		val VERTEX_ATTRIB_ARRAY_POINTER: Int = 34373
//		val VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: Int = 34975
//
//		/* Read Format */
//		val IMPLEMENTATION_COLOR_READ_TYPE: Int = 35738
//		val IMPLEMENTATION_COLOR_READ_FORMAT: Int = 35739
//
//		/* Shader Source */
//		val COMPILE_STATUS: Int = 35713
//		val INFO_LOG_LENGTH: Int = 35716
//		val SHADER_SOURCE_LENGTH: Int = 35720
//
//		/* Shader Precision-Specified Types */
//		val LOW_FLOAT: Int = 36336
//		val MEDIUM_FLOAT: Int = 36337
//		val HIGH_FLOAT: Int = 36338
//		val LOW_INT: Int = 36339
//		val MEDIUM_INT: Int = 36340
//		val HIGH_INT: Int = 36341
//
//		/* Framebuffer Object. */
//		val FRAMEBUFFER: Int = 36160
//		val RENDERBUFFER: Int = 36161
//
//		val RGBA4: Int = 32854
//		val RGB5_A1: Int = 32855
//		val RGB565: Int = 36194
//		val DEPTH_COMPONENT16: Int = 33189
//		val STENCIL_INDEX: Int = 6401
//		val STENCIL_INDEX8: Int = 36168
//		val DEPTH_STENCIL: Int = 34041
//
//		val RENDERBUFFER_WIDTH: Int = 36162
//		val RENDERBUFFER_HEIGHT: Int = 36163
//		val RENDERBUFFER_INTERNAL_FORMAT: Int = 36164
//		val RENDERBUFFER_RED_SIZE: Int = 36176
//		val RENDERBUFFER_GREEN_SIZE: Int = 36177
//		val RENDERBUFFER_BLUE_SIZE: Int = 36178
//		val RENDERBUFFER_ALPHA_SIZE: Int = 36179
//		val RENDERBUFFER_DEPTH_SIZE: Int = 36180
//		val RENDERBUFFER_STENCIL_SIZE: Int = 36181
//
//		val FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE: Int = 36048
//		val FRAMEBUFFER_ATTACHMENT_OBJECT_NAME: Int = 36049
//		val FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL: Int = 36050
//		val FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE: Int = 36051
//
//		val COLOR_ATTACHMENT0: Int = 36064
//		val DEPTH_ATTACHMENT: Int = 36096
//		val STENCIL_ATTACHMENT: Int = 36128
//		val DEPTH_STENCIL_ATTACHMENT: Int = 33306
//
//		val NONE: Int = 0
//
//		val FRAMEBUFFER_COMPLETE: Int = 36053
//		val FRAMEBUFFER_INCOMPLETE_ATTACHMENT: Int = 36054
//		val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: Int = 36055
//		val FRAMEBUFFER_INCOMPLETE_DIMENSIONS: Int = 36057
//		val FRAMEBUFFER_UNSUPPORTED: Int = 36061
//
//		val FRAMEBUFFER_BINDING: Int = 36006
//		val RENDERBUFFER_BINDING: Int = 36007
//		val MAX_RENDERBUFFER_SIZE: Int = 34024
//
//		val INVALID_FRAMEBUFFER_OPERATION: Int = 1286
//
//		/* WebGL-specific enums */
//		val UNPACK_FLIP_Y_WEBGL: Int = 37440
//		val UNPACK_PREMULTIPLY_ALPHA_WEBGL: Int = 37441
//		val CONTEXT_LOST_WEBGL: Int = 37442
//	}
//
//	var canvas: HTMLCanvasElement
//
//	/**
//	 * Returns the height of the owner canvas in pixels.
//	 */
//	var drawingBufferHeight: Float
//
//	/**
//	 * Returns the width of the owner canvas in pixels.
//	 */
//	var drawingBufferWidth: Float
//
//	/**
//	 * Enables the specified extension and returns an object that contains any constants or functions provided by the extension. Call getSupportedExtensions() to get an array of valid extension names. If name is not in the Array returned by getSupportedExtensions(), getExtension() will return null.
//	 */
//	fun getExtension(name: String): dynamic
//
//
//	//-----------------------------------------------------------
//	// GL2.0 methods
//	//-----------------------------------------------------------
//
//	/**
//	 * Set the texture unit subsequent texture operations apply to.
//	 *
//	 * must be one of TEXTURE0, TEXTURE1, to getParameter(gl.MAX_COMBINED_TEXTURE_IMAGE_UNITS) - 1.
//	 * The default value is TEXTURE0. A texture must be bound to the active texture unit using bindTexture().
//	 */
//	fun activeTexture(texture: Int)
//
//	/**
//	 * Attaches shader to program. A program must have both a VERTEX_SHADER and FRAGMENT_SHADER before it can be used.
//	 * shader can be attached before its source has been set. See also detachShader().
//	 */
//	fun attachShader(program: WebGLProgram, shader: WebGLShader)
//
//	/**
//	 * Associates a number (location) with an attribute (a shader input such as vertex position) in program. Other
//	 * webgl functions (such as enableVertexAttribArray() or vertexAttribPointer()) deal with an attribute location
//	 * number instead of the name used in the program and bindAttribLocation is used to choose the number used for
//	 * that attribute. Locations are automatically assigned if you do not call bindAttribLocation so this method is
//	 * only necessary if you wish to assign a specific location for an attribute. Use getAttribLocation() to retrieve
//	 * the automatically assigned location. bindAttribLocation() must be called before calling linkProgram(program) and
//	 * location must be an integer in the range 0 to getParameter(gl.MAX_VERTEX_ATTRIBS) - 1.
//	 */
//	fun bindAttribLocation(program: WebGLProgram, index: Int, name: String)
//
//	/**
//	 * Sets the current buffer for target to buffer. target must be either ARRAY_BUFFER or ELEMENT_ARRAY_BUFFER.
//	 * Use bufferData() to fill the bound buffer with data.
//	 */
//	fun bindBuffer(target: Int, buffer: WebGLBuffer?)
//
//	/**
//	 * Sets the current framebuffer to framebuffer. target must be FRAMEBUFFER. See createFramebuffer() for an
//	 * example of using bindFramebuffer().
//	 */
//	fun bindFramebuffer(target: Int, framebuffer: WebGLFramebuffer?)
//
//	/**
//	 * Sets the current renderbuffer to renderbuffer. target must be RENDERBUFFER.
//	 */
//	fun bindRenderbuffer(target: Int, renderbuffer: WebGLRenderbuffer?)
//
//	/**
//	 * Sets the specified target and texture (created with createTexture) for the bound texture in the active texture
//	 * unit (set through activeTexture() and bindTexture). target must be one of TEXTURE_2D or TEXTURE_CUBE_MAP
//	 */
//	fun bindTexture(target: Int, texture: WebGLTexture?)
//
//	/**
//	 * Specifies the blend color used with blendFunc(). Each component must be in the range 0.0 to 1.0.
//	 */
//	fun blendColor(red: Float, green: Float, blue: Float, alpha: Float)
//
//	/**
//	 * Same as blendEquationSeparate(mode, mode).
//	 */
//	fun blendEquation(mode: Int)
//
//	/**
//	 * Sets how the newly rendered pixel color and alpha (src) is combined with the existing framebuffer color and
//	 * alpha (dst) before storing in the framebuffer.
//	 * modeRGB and modeAlpha must be one of FUNC_ADD, FUNC_SUBTRACT, or FUNC_REVERSE_SUBTRACT. If the mode is FUNC_ADD,
//	 * the destination color will be src + dst. If the mode is FUNC_SUBTRACT, the destination color will be src - dst.
//	 * If the mode is FUNC_REVERSE_SUBTRACT, the destination color will be dst - src. Both modeRGB and modeAlpha
//	 * default to FUNC_ADD. Use getParameter(gl.BLEND_EQUATION_RGB) and getParameter(gl.BLEND_EQUATION_ALPHA) to get
//	 * the current values. See blendFuncSeparate() for how src and dst are computed. Blending must be enabled with
//	 * enable(BLEND).
//	 */
//	fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int)
//
//	/**
//	 * Same as blendFuncSeparate(srcFactor, dstFactor, srcFactor, dstFactor).
//	 */
//	fun blendFunc(sfactor: Int, dfactor: Int)
//
//	/**
//	 * Adjusts the newly rendered pixel color and alpha (src) and existing framebuffer color and alpha in the
//	 * framebuffer (dst) before being combined using blendEquationSeparate(). srcRGB, dstRGB, srcAlpha, and dstAlpha
//	 * must be one of ZERO, ONE, SRC_COLOR, ONE_MINUS_SRC_COLOR, DST_COLOR, ONE_MINUS_DST_COLOR, SRC_ALPHA,
//	 * ONE_MINUS_SRC_ALPHA, DST_ALPHA, ONE_MINUS_DST_ALPHA, CONSTANT_COLOR, ONE_MINUS_CONSTANT_COLOR, CONSTANT_ALPHA,
//	 * ONE_MINUS_CONSTANT_ALPHA, or SRC_ALPHA_SATURATE srcRGB and srcAlpha default to ONE. dstRGB and dstAlpha
//	 * default to NONE. For traditional alpha blending, use: gl.blendFuncSeparate(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA,
//	 * gl.ONE, gl.ZERO). For premultiplied alpha blending, use: gl.blendFuncSeparate(gl.ONE, gl.ONE_MINUS_SRC_ALPHA,
//	 * gl.ONE, gl.ZERO). Use getParameter(gl.BLEND_SRC_RGB), getParameter(gl.BLEND_DST_RGB),
//	 * getParameter(gl.BLEND_SRC_ALPHA), and getParameter(gl.BLEND_DST_ALPHA) to get the current values.
//	 */
//	fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int)
//
//	/**
//	 * Creates the storage for the currently bound buffer.
//	 * @param target must be one of ARRAY_BUFFER or ELEMENT_ARRAY_BUFFER.
//	 * @param size the size in bytes of the buffer to allocate.
//	 * @param usage must be one of STREAM_DRAW, STATIC_DRAW, or DYNAMIC_DRAW.
//	 */
//	fun bufferData(target: Int, size: Int, usage: Int)
//
//	fun bufferData(target: Int, data: ArrayBufferView, usage: Int)
//
//	fun bufferData(target: Int, data: ArrayBuffer, usage: Int)
//
//	fun bufferSubData(target: Int, offset: Int, data: ArrayBufferView)
//
//	fun bufferSubData(target: Int, offset: Int, data: ArrayBuffer)
//
//	/**
//	 * Returns one of the following to indicate the current status of the framebuffer: FRAMEBUFFER_COMPLETE,
//	 * FRAMEBUFFER_INCOMPLETE_ATTACHMENT, FRAMEBUFFER_INCOMPLETE_DIMENSIONS, FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT,
//	 * or FRAMEBUFFER_UNSUPPORTED.
//	 */
//	fun checkFramebufferStatus(target: Int): Int
//
//	/**
//	 * Clears the buffers specified by mask where mask is the bitwise OR (|) of one or more of the following values:
//	 * COLOR_BUFFER_BIT, STENCIL_BUFFER_BIT, and DEPTH_BUFFER_BIT.
//	 */
//	fun clear(mask: Int)
//
//	/**
//	 * Specifies the color to fill the color buffer when clear() is called with the COLOR_BUFFER_BIT.
//	 * The parameters are clamped to the range 0 to 1.
//	 */
//	fun clearColor(red: Float, green: Float, blue: Float, alpha: Float)
//
//	/**
//	 * Specifies the value to fill the depth buffer when clear() is called with the DEPTH_BUFFER_BIT.
//	 * depth is clamped to the range 0 (near) to 1 (far). Defaults to 1 if not specified.
//	 */
//	fun clearDepth(depth: Float)
//
//	/**
//	 * Specifies the value (integer) to fill the depth buffer when clear() is called with the STENCIL_BUFFER_BIT.
//	 */
//	fun clearStencil(s: Int)
//
//	/**
//	 * Turns on or off writing to the specified channels of the frame buffer. Defaults to true for all channels.
//	 * Use getParameter(gl.COLOR_WRITEMASK) to get the current value.
//	 */
//	fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean)
//
//	/**
//	 * Compiles the specified shader. Must be called after setting the source with shaderSource(). If the shader had
//	 * errors during compilation, gl.getShaderParameter(shader, gl.COMPILE_STATUS) will return false and you can use
//	 * getShaderInfoLog() to get details about the error.
//	 */
//	fun compileShader(shader: WebGLShader)
//
//	/**
//	 * Copies pixels from the framebuffer to the bound texture in the active texture unit (set through activeTexture()
//	 * and bindTexture).
//	 * @param target must be one of TEXTURE_2D, TEXTURE_CUBE_MAP_POSITIVE_X, TEXTURE_CUBE_MAP_NEGATIVE_X, TEXTURE_CUBE_MAP_POSITIVE_Y, TEXTURE_CUBE_MAP_NEGATIVE_Y, TEXTURE_CUBE_MAP_POSITIVE_Z, or TEXTURE_CUBE_MAP_NEGATIVE_Z.
//	 * @param level specifies the mipmap level to copy into.
//	 * @param internalformat ALPHA, LUMINANCE, LUMINANCE_ALPHA, RGB, or RGBA.
//	 * @param x
//	 * @param y
//	 * @param width
//	 * @param height the rectangle in framebuffer to copy.
//	 * @param border must be 0.
//	 */
//	fun copyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int)
//
//	/**
//	 * Copies pixels from the framebuffer to a subregion of the bound texture in the active texture unit (set through activeTexture() and bindTexture).
//	 * @param target must be one of TEXTURE_2D, TEXTURE_CUBE_MAP_POSITIVE_X, TEXTURE_CUBE_MAP_NEGATIVE_X, TEXTURE_CUBE_MAP_POSITIVE_Y, TEXTURE_CUBE_MAP_NEGATIVE_Y, TEXTURE_CUBE_MAP_POSITIVE_Z, or TEXTURE_CUBE_MAP_NEGATIVE_Z.
//	 * @param level specifies the mipmap level to copy into.
//	 * @param xoffset
//	 * @param yoffset the position in the texture to store the copied pixels.
//	 * @param x
//	 * @param y the position in the framebuffer to read the pixels to copy.
//	 * @param width
//	 * @param height the size of the region to copy.
//	 */
//	fun copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int)
//
//	/**
//	 * Creates a buffer. A buffer is memory used to store data passed to the shader program through attributes.
//	 * See also bindBuffer(), bufferData(), bufferSubData(), deleteBuffer(), isBuffer(), vertexAttribPointer() and
//	 * enableVertexAttribArray().
//	 */
//	fun createBuffer(): WebGLBuffer
//
//	/**
//	 * Creates a framebuffer that can be used for offscreen rendering. See also bindFramebuffer(),
//	 * checkFramebufferStatus(), deleteFramebuffer(), framebufferRenderbuffer(), framebufferTexture2D(),
//	 * getFramebufferAttachmentParameter(), and isFramebuffer().
//	 */
//	fun createFramebuffer(): WebGLFramebuffer
//
//	/**
//	 * Creates a shader program. A shader program consists of a vertex shader and fragment shader. Use
//	 * attachShader() to associate shaders with the program and linkProgram() to finalize the program. After linking,
//	 * use useProgram() to select the program to use.
//	 */
//	fun createProgram(): WebGLProgram
//
//	/**
//	 * Creates a renderbuffer. A renderbuffer is an offscreen section of memory used to store the result of rendering,
//	 * such as the color buffer, depth buffer, or stencil buffer. See also framebufferRenderbuffer(),
//	 * renderbufferStorage().
//	 */
//	fun createRenderbuffer(): WebGLRenderbuffer
//
//	/**
//	 * Creates a vertex or fragment shader. type must be either VERTEX_SHADER or FRAGMENT_SHADER. Shaders must be
//	 * compiled using compileShader() and then attached to a WebGLProgram using attachShader() before they can be used.
//	 */
//	fun createShader(type: Int): WebGLShader
//
//	/**
//	 * Creates a texture. Use activeTexture() to select a texture unit and then bindTexture() to bind a texture to
//	 * that unit. See also copyTexImage2D(), copyTexSubImage2D(), deleteTexture(), framebufferTexture2D(),
//	 * getTexParameter(), isTexture(), texImage2D(), texParameterf(), texParameteri(), and texSubImage2D().
//	 */
//	fun createTexture(): WebGLTexture
//
//	/**
//	 * Sets which side of the triangle is culled (not drawn). mode must be one of BACK, FRONT, or FRONT_AND_BACK.
//	 * Defaults to BACK. To turn on culling, you must call enable(CULL_FACE). To select which face is the front or back,
//	 * use frontFace().
//	 */
//	fun cullFace(mode: Int)
//
//	/**
//	 * Deletes the specified buffer.
//	 */
//	fun deleteBuffer(buffer: WebGLBuffer)
//
//	/**
//	 * Deletes the specified framebuffer.
//	 */
//	fun deleteFramebuffer(framebuffer: WebGLFramebuffer)
//
//	/**
//	 * Deletes the specified program.
//	 */
//	fun deleteProgram(program: WebGLProgram)
//
//	/**
//	 * Deletes the specified renderbuffer.
//	 */
//	fun deleteRenderbuffer(renderbuffer: WebGLRenderbuffer)
//
//	/**
//	 * Deletes the specified shader.
//	 */
//	fun deleteShader(shader: WebGLShader)
//
//	/**
//	 * Deletes the specified texture.
//	 */
//	fun deleteTexture(texture: WebGLTexture)
//
//	/**
//	 * Specifies what function used to compare the rendered depth with the existing depth in the framebuffer to
//	 * determine if the pixel will be written to the framebuffer. func must be one of NEVER, LESS, EQUAL, LEQUAL,
//	 * GREATER, NOTEQUAL, GEQUAL, or ALWAYS. Defaults to LESS. Depth test will only be used if enabled with
//	 * enable(DEPTH_TEST).
//	 */
//	fun depthFunc(func: Int)
//
//	/**
//	 * Turns on or off writing to the depth buffer. Defaults to true. Use getParameter(gl.DEPTH_WRITEMASK) to get the
//	 * current value. Depth test will only be used if enabled with enable(DEPTH_TEST).
//	 */
//	fun depthMask(flag: Boolean)
//
//	/**
//	 * Sets how z values returned from the vertex shader are mapped to values to store in the depth buffer.
//	 * This mapping is necessary because the vertex shader output z values will be clipped to the range -1 to 1 but the
//	 * depth buffer stores depth values in the range 0 to 1.
//	 * @param zNear specifies what the vertex shader's -1 maps to in the depth buffer.
//	 * @param zFar specifies what the vertex shader's 1 maps to in the depth buffer. By default, zNear is 0 and zFar is 1.
//	 */
//	fun depthRange(zNear: Float, zFar: Float)
//
//	/**
//	 * Detaches shader from program. shader must have been attached to program using attachShader().
//	 */
//	fun detachShader(program: WebGLProgram, shader: WebGLShader)
//
//	/**
//	 * Turns off a capability. See enable() for a list of capabilities.
//	 */
//	fun disable(cap: Int)
//
//	fun disableVertexAttribArray(index: Int)
//
//	/**
//	 * Draws primitives using the vertex buffer data (stored in the ARRAY_BUFFER buffer).
//	 * mode must be one of POINTS, LINE_STRIP, LINE_LOOP, LINES, TRIANGLE_STRIP, TRIANGLE_FAN, or TRIANGLES.
//	 * vertexOffset specifies the index of the first vertex to draw.
//	 * vertexCount specifies the number of vertices to draw.
//	 * You must call enableVertexAttribArray() for each attribute in the vertex shader that uses the vertex data.
//	 * @see drawElements()
//	 */
//	fun drawArrays(mode: Int, first: Int, count: Int)
//
//	/**
//	 * Draws primitives using the vertex buffer data (stored in the ARRAY_BUFFER buffer) and the index buffer data
//	 * (stored in the ELEMENT_ARRAY_BUFFER buffer).
//	 * mode must be one of POINTS, LINE_STRIP, LINE_LOOP, LINES, TRIANGLE_STRIP, TRIANGLE_FAN, or TRIANGLES.
//	 * indicesCount specifies the number of number of indices to use.
//	 * indicesNumberType must be one of UNSIGNED_BYTE or UNSIGNED_SHORT.
//	 * indicesOffset specifies the index into the indices array of the first element to draw.
//	 * You must call enableVertexAttribArray() for each attribute in the vertex shader that uses the vertex data.
//	 * @see drawArrays()
//	 */
//	fun drawElements(mode: Int, count: Int, type: Int, offset: Int)
//
//	/**
//	 * Turns on a capability. capability must be one of the following:
//	 * BLEND if enabled, will combine the color generated by the fragment shader with the existing color in the framebuffer using the method specified by blendFunc(). Most commonly used to enable alpha blending. Defaults to disabled.
//	 * CULL_FACE if enabled, will cull (not draw) triangles based on which face is visible. See cullFace() and frontFace() to configure culling. Defaults to disabled.
//	 * DEPTH_TEST if enabled, fragments will only be written to the framebuffer if they pass the depth function (set with gl.depthFunc()). See also depthMask(), and depthRange(). Most commonly used to draw closer objects on top of further away objects. Defaults to disabled.
//	 * DITHER if enabled, the colors will be dithered when written to the color buffer. Defaults to enabled.
//	 * POLYGON_OFFSET_FILL if enabled, the offset specified by polygonOffset will be added to the depth for the fragment when writing to the depth buffer. Most commonly used to draw decals on top of already drawn surfaces. Defaults to disabled.
//	 * SAMPLE_COVERAGE Defaults to disabled.
//	 * SAMPLE_ALPHA_TO_COVERAGE Defaults to disabled.
//	 * SCISSOR_TEST if enabled, fragments outside the scissor rectangle (set with scissor() will not be drawn. Defaults to disabled.
//	 * STENCIL_TEST if enabled, perform a stencil test on each fragment and update the stencil buffer. See also stencilFunc and stencilOp. Defaults to disabled.
//	 * Use disable() to turn off the capability.
//	 * @see disable()
//	 */
//	fun enable(cap: Int)
//
//	/**
//	 * Turns on passing data to the vertex shader from the vertex buffer for the specified attribute. Use
//	 * getAttribLocation() to retrieve the location of an attribute by name.
//	 */
//	fun enableVertexAttribArray(index: Int)
//
//	fun finish()
//
//	fun flush()
//
//	/**
//	 * Specifies the renderbuffer to use as destination of rendering for the current framebuffer (set with the most recent bindFramebuffer() call).
//	 * target must be FRAMEBUFFER.
//	 * attachment determines what is rendered into renderbuffer. Pass COLOR_ATTACHMENT0 for color data, DEPTH_ATTACHMENT for depth data, or STENCIL_ATTACHMENT for stencil data.
//	 * renderbuffertarget must be RENDERBUFFER.
//	 * renderbuffer the buffer to store the rendered output.
//	 */
//	fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: WebGLRenderbuffer)
//
//	/**
//	 * Specifies the texture to use as destination of rendering for the current framebuffer (set with the most recent bindFramebuffer() call).
//	 * target must be FRAMEBUFFER.
//	 * attachment determines what is rendered into renderbuffer. Pass COLOR_ATTACHMENT0 for color data, DEPTH_ATTACHMENT for depth data, or STENCIL_ATTACHMENT for stencil data.
//	 * target must be one of TEXTURE_2D, TEXTURE_CUBE_MAP_POSITIVE_X, TEXTURE_CUBE_MAP_NEGATIVE_X, TEXTURE_CUBE_MAP_POSITIVE_Y, TEXTURE_CUBE_MAP_NEGATIVE_Y, TEXTURE_CUBE_MAP_POSITIVE_Z, or TEXTURE_CUBE_MAP_NEGATIVE_Z.
//	 * texture the texture to store the rendered output.
//	 * level must be 0.
//	 */
//	fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: WebGLTexture, level: Int)
//
//	/**
//	 * Determines which side of triangles is the front face. mode must be one of CW or CCW. To turn on culling, you must call enable(CULL_FACE). To select which face is culled, use cullFace().
//	 */
//	fun frontFace(mode: Int)
//
//	/**
//	 * Generate the mipmap for the bound texture in the active texture unit (set through activeTexture() and bindTexture). A mipmap is a set of textures that are 1/2, 1/4, 1/8, etc of the original image. The mipmap allows higher quality rendering when drawing the texture at smaller sizes. target must be one of TEXTURE_2D or TEXTURE_CUBE_MAP. Note, you can only generate mipmaps for textures where the width and height are both powers of 2 (such as 128, 256, 512, etc).
//	 */
//	fun generateMipmap(target: Int)
//
//	/**
//	 * Returns information about an attribute in program. program must be linked before calling getActiveAttrib(). index must be between 0 and gl.getProgramParameter(program, ACTIVE_ATTRIBUTES) - 1.
//	 */
//	fun getActiveAttrib(program: WebGLProgram, index: Int): WebGLActiveInfo
//
//	/**
//	 * Returns information about a uniform in program. program must be linked before calling getActiveUniform(). index must be between 0 and gl.getProgramParameter(program, ACTIVE_UNIFORMS) - 1.
//	 */
//	fun getActiveUniform(program: WebGLProgram, index: Int): WebGLActiveInfo
//
//	/**
//	 * return the handles of the shader objects attached to a program object.
//	 */
//	fun getAttachedShaders(program: WebGLProgram): Array<WebGLShader>
//
//	/**
//	 * Return the location of an attribute variable.
//	 */
//	fun getAttribLocation(program: WebGLProgram, name: String): Int
//
//	/**
//	 * Returns the first error hit since the last time getError() was called. The returned value will be one of
//	 * NO_ERROR, INVALID_ENUM, INVALID_VALUE, INVALID_OPERATION, INVALID_FRAMEBUFFER_OPERATION, or OUT_OF_MEMORY.
//	 */
//	fun getError(): Int
//
//	/**
//	 * Return the information log for a program object
//	 */
//	fun getProgramInfoLog(program: WebGLProgram): String
//
//	/**
//	 * Return the information log for a shader object
//	 */
//	fun getShaderInfoLog(shader: WebGLShader): String
//
//	fun getShaderSource(shader: WebGLShader): String
//
//	/**
//	 * Return the location of a uniform variable
//	 */
//	fun getUniformLocation(program: WebGLProgram, name: String): WebGLUniformLocation?
//
//	fun getVertexAttribOffset(index: Int, pname: Int): Int
//
//	fun hint(target: Int, mode: Int)
//
//	fun isBuffer(buffer: WebGLBuffer): Boolean
//
//	fun isEnabled(cap: Int): Boolean
//
//	fun isFramebuffer(framebuffer: WebGLFramebuffer): Boolean
//
//	fun isProgram(program: WebGLProgram): Boolean
//
//	fun isRenderbuffer(renderbuffer: WebGLRenderbuffer): Boolean
//
//	fun isShader(shader: WebGLShader): Boolean
//
//	fun isTexture(texture: WebGLTexture): Boolean
//
//	fun lineWidth(width: Float)
//
//	fun linkProgram(program: WebGLProgram)
//
//	fun pixelStorei(pname: Int, param: Int)
//
//	fun polygonOffset(factor: Float, units: Float)
//
//	fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: ArrayBufferView)
//
//	fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int)
//
//	fun sampleCoverage(value: Float, invert: Boolean)
//
//	fun scissor(x: Int, y: Int, width: Int, height: Int)
//
//	fun shaderSource(shader: WebGLShader, source: String)
//
//	fun stencilFunc(func: Int, ref: Int, mask: Int)
//
//	fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int)
//
//	fun stencilMask(mask: Int)
//
//	fun stencilMaskSeparate(face: Int, mask: Int)
//
//	fun stencilOp(fail: Int, zfail: Int, zpass: Int)
//
//	fun stencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int)
//
//	fun texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ArrayBufferView?)
//
//	fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, pixels: ImageData)
//
//	fun texImage2D(target: Int, level: Int, internalformat: Number, format: Number, type: Number, image: HTMLImageElement)
//
//	fun texImage2D(target: Int, level: Int, internalformat: Number, format: Number, type: Number, canvas: HTMLCanvasElement)
//
//	fun texParameterf(target: Int, pname: Int, param: Float)
//
//	fun texParameteri(target: Int, pname: Int, param: Int)
//
//	fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: ArrayBufferView?)
//
//	fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, format: Int, type: Int, image: HTMLImageElement)
//
//	fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, format: Int, type: Int, canvas: HTMLCanvasElement)
//
//	fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, format: Int, type: Int, video: HTMLVideoElement)
//
//	fun uniform1f(location: WebGLUniformLocation, x: Float)
//
//	fun uniform1fv(location: WebGLUniformLocation, v: Float32Array)
//
//	fun uniform1fv(location: WebGLUniformLocation, v: FloatArray)
//
//	fun uniform1i(location: WebGLUniformLocation, x: Int)
//
//	fun uniform1iv(location: WebGLUniformLocation, v: Int32Array)
//
//	fun uniform1iv(location: WebGLUniformLocation, v: IntArray)
//
//	fun uniform2f(location: WebGLUniformLocation, x: Float, y: Float)
//
//	fun uniform2fv(location: WebGLUniformLocation, v: Float32Array)
//
//	fun uniform2fv(location: WebGLUniformLocation, v: FloatArray)
//
//	fun uniform2i(location: WebGLUniformLocation, x: Int, y: Int)
//
//	fun uniform2iv(location: WebGLUniformLocation, v: Int32Array)
//
//	fun uniform2iv(location: WebGLUniformLocation, v: IntArray)
//
//	fun uniform3f(location: WebGLUniformLocation, x: Float, y: Float, z: Float)
//
//	fun uniform3fv(location: WebGLUniformLocation, v: Float32Array)
//
//	fun uniform3fv(location: WebGLUniformLocation, v: FloatArray)
//
//	fun uniform3i(location: WebGLUniformLocation, x: Int, y: Int, z: Int)
//
//	fun uniform3iv(location: WebGLUniformLocation, v: Int32Array)
//
//	fun uniform3iv(location: WebGLUniformLocation, v: IntArray)
//
////	private fun uniform3iv(location: WebGLUniformLocation, v: Any)
//
//	fun uniform4f(location: WebGLUniformLocation, x: Float, y: Float, z: Float, w: Float)
//
//	fun uniform4fv(location: WebGLUniformLocation, v: Float32Array)
//
//	fun uniform4fv(location: WebGLUniformLocation, v: FloatArray)
//
//	fun uniform4i(location: WebGLUniformLocation, x: Int, y: Int, z: Int, w: Int)
//
//	fun uniform4iv(location: WebGLUniformLocation, v: Int32Array)
//
//	fun uniform4iv(location: WebGLUniformLocation, v: IntArray)
//
//	fun uniformMatrix2fv(location: WebGLUniformLocation, transpose: Boolean, value: Float32Array)
//
//	fun uniformMatrix2fv(location: WebGLUniformLocation, transpose: Boolean, value: FloatArray)
//
//	fun uniformMatrix3fv(location: WebGLUniformLocation, transpose: Boolean, value: Float32Array)
//
//	fun uniformMatrix3fv(location: WebGLUniformLocation, transpose: Boolean, value: FloatArray)
//
//	fun uniformMatrix4fv(location: WebGLUniformLocation, transpose: Boolean, value: Float32Array)
//
//	fun uniformMatrix4fv(location: WebGLUniformLocation, transpose: Boolean, value: FloatArray)
//
//	fun useProgram(program: WebGLProgram?)
//
//	fun validateProgram(program: WebGLProgram)
//
//	fun vertexAttrib1f(indx: Int, x: Float)
//
//	fun vertexAttrib1fv(indx: Int, values: Float32Array)
//
//	fun vertexAttrib1fv(indx: Int, values: FloatArray)
//
//	fun vertexAttrib2f(indx: Int, x: Float, y: Float)
//
//	fun vertexAttrib2fv(indx: Int, values: Float32Array)
//
//	fun vertexAttrib2fv(indx: Int, values: FloatArray)
//
//	fun vertexAttrib3f(indx: Int, x: Float, y: Float, z: Float)
//
//	fun vertexAttrib3fv(indx: Int, values: Float32Array)
//
//	fun vertexAttrib3fv(indx: Int, values: FloatArray)
//
////	private fun vertexAttrib3fv(indx: Int, values: Any)
//
//	fun vertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float)
//
//	fun vertexAttrib4fv(indx: Int, values: Float32Array)
//
//	fun vertexAttrib4fv(indx: Int, values: FloatArray)
//
////	private fun vertexAttrib4fv(indx: Int, values: Any)
//
//	fun vertexAttribPointer(indx: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int)
//
//	fun viewport(x: Int, y: Int, width: Int, height: Int)
//
//	/**
//	 * Return the value for the passed pname.
//	 *
//	 * @param pname one of RENDERER, SHADING_LANGUAGE_VERSION, VENDOR, VERSION
//	 */
//	fun getParameterString(pname: Int): String
//
//	/**
//	 * Return the value for the passed pname.
//	 *
//	 * @param pname one of ACTIVE_TEXTURE, ALPHA_BITS, BLEND_DST_ALPHA, BLEND_DST_RGB, BLEND_EQUATION_ALPHA, BLEND_EQUATION_RGB,
//	*           BLEND_SRC_ALPHA, BLEND_SRC_RGB, BLUE_BITS, CULL_FACE_MODE, DEPTH_BITS, DEPTH_FUNC, FRONT_FACE,
//	*           GENERATE_MIPMAP_HINT, GREEN_BITS, IMPLEMENTATION_COLOR_READ_FORMAT, IMPLEMENTATION_COLOR_READ_TYPE,
//	*           MAX_COMBINED_TEXTURE_IMAGE_UNITS, MAX_CUBE_MAP_TEXTURE_SIZE, MAX_FRAGMENT_UNIFORM_VECTORS, MAX_RENDERBUFFER_SIZE,
//	*           MAX_TEXTURE_IMAGE_UNITS, MAX_TEXTURE_SIZE, MAX_VARYING_VECTORS, MAX_VERTEX_ATTRIBS,
//	*           MAX_VERTEX_TEXTURE_IMAGE_UNITS, MAX_VERTEX_UNIFORM_VECTORS, NUM_COMPRESSED_TEXTURE_FORMATS, PACK_ALIGNMENT,
//	*           RED_BITS, SAMPLE_BUFFERS, SAMPLES, STENCIL_BACK_FAIL, STENCIL_BACK_FUNC, STENCIL_BACK_PASS_DEPTH_FAIL,
//	*           STENCIL_BACK_PASS_DEPTH_PASS, STENCIL_BACK_REF, STENCIL_BACK_VALUE_MASK, STENCIL_BACK_WRITEMASK, STENCIL_BITS,
//	*           STENCIL_CLEAR_VALUE, STENCIL_FAIL, STENCIL_FUNC, STENCIL_PASS_DEPTH_FAIL, STENCIL_PASS_DEPTH_PASS, STENCIL_REF,
//	*           STENCIL_VALUE_MASK, STENCIL_WRITEMASK, SUBPIXEL_BITS, UNPACK_ALIGNMENT
//	 */
//	external("getParameter") fun getParameteri(pname: Int): Int
//
//	/**
//	 * Return the value for the passed pname.
//	 *
//	 * @param pname one of BLEND, CULL_FACE, DEPTH_TEST, DEPTH_WRITEMASK, DITHER, POLYGON_OFFSET_FILL, SAMPLE_COVERAGE_INVERT,
//	 *           SCISSOR_TEST, STENCIL_TEST, UNPACK_FLIP_Y_WEBGL, UNPACK_PREMULTIPLY_ALPHA_WEBGL
//	 */
//	external("getParameter") fun getParameterb(pname: Int): Boolean
//
//	/**
//	 * Return the value for the passed pname.
//	 *
//	 * @param pname one of DEPTH_CLEAR_VALUE, LINE_WIDTH, POLYGON_OFFSET_FACTOR, POLYGON_OFFSET_UNITS, SAMPLE_COVERAGE_VALUE
//	 */
//	external("getParameter") fun getParameterf(pname: Int): Float
//
//	/**
//	 * Return the value for the passed pname.
//	 *
//	 * @param pname one of ARRAY_BUFFER_BINDING, COMPRESSED_TEXTURE_FORMATS, CURRENT_PROGRAM, ELEMENT_ARRAY_BUFFER_BINDING,
//	*           FRAMEBUFFER_BINDING, RENDERBUFFER_BINDING, TEXTURE_BINDING_2D, TEXTURE_BINDING_CUBE_MAP
//	 */
//	external("getParameter") fun getParametero(pname: Int): WebGLObject
//
//	/**
//	 * Return the value for the passed pname.
//	 *
//	 * @param pname one of ALIASED_LINE_WIDTH_RANGE, ALIASED_POINT_SIZE_RANGE, BLEND_COLOR, COLOR_CLEAR_VALUE, COLOR_WRITEMASK,
//	*           DEPTH_RANGE, MAX_VIEWPORT_DIMS, SCISSOR_BOX, VIEWPORT
//	 */
//	external("getParameter") fun getParameterv(pname: Int): ArrayBufferView
//
//	/**
//	 * Return the uniform value at the passed location in the passed program.
//	 */
//	external("getUniform") fun getUniformb(program: WebGLProgram, location: WebGLUniformLocation): Boolean
//
//	/**
//	 * Return the uniform value at the passed location in the passed program.
//	 */
//	external("getUniform") fun getUniformi(program: WebGLProgram, location: WebGLUniformLocation): Int
//
//	/** Return the uniform value at the passed location in the passed program. */
//	external("getUniform") fun getUniformf(program: WebGLProgram, location: WebGLUniformLocation): Float
//
//	/** Return the uniform value at the passed location in the passed program. */
//	external("getUniform") fun getUniformv(program: WebGLProgram, location: WebGLUniformLocation): ArrayBufferView
//
//	/** Return the information requested in pname about the vertex attribute at the passed index.
//	 *
//	 * @param pname one of VERTEX_ATTRIB_ARRAY_SIZE, VERTEX_ATTRIB_ARRAY_STRIDE, VERTEX_ATTRIB_ARRAY_TYPE */
//	external("getVertexAttrib") fun getVertexAttribi(index: Int, pname: Int): Int
//
//	/** Return the information requested in pname about the vertex attribute at the passed index.
//	 *
//	 * @param pname one of VERTEX_ATTRIB_ARRAY_ENABLED, VERTEX_ATTRIB_ARRAY_NORMALIZED */
//	external("getVertexAttrib") fun getVertexAttribb(index: Int, pname: Int): Boolean
//
//	/** Return the information requested in pname about the vertex attribute at the passed index.
//	 *
//	 * @param pname VERTEX_ATTRIB_ARRAY_BUFFER_BINDING
//	 * @return {@link WebGLBuffer} */
//	external("getVertexAttrib") fun getVertexAttribo(index: Int, pname: Int): WebGLObject
//
//	/** Return the information requested in pname about the vertex attribute at the passed index.
//	 *
//	 * @param pname CURRENT_VERTEX_ATTRIB
//	 * @return a {@link TypedArray<Float>} with 4 elements */
//	external("getVertexAttrib") fun getVertexAttribv(index: Int, pname: Int): TypedArray<Float>
//
//	/** Return the value for the passed pname given the passed target. The type returned is the natural type for the requested
//	 * pname, as given in the following table: If an attempt is made to call this function with no WebGLTexture bound (see above),
//	 * an INVALID_OPERATION error is generated.
//	 *
//	 * @param pname one of TEXTURE_MAG_FILTER, TEXTURE_MIN_FILTER, TEXTURE_WRAP_S, TEXTURE_WRAP_T */
//	fun getTexParameter(target: Int, pname: Int): Int
//
//	/** Return the value for the passed pname given the passed shader.
//	 *
//	 * @param pname one of DELETE_STATUS, COMPILE_STATUS */
//	external("getShaderParameter") fun getShaderParameterb(shader: WebGLShader, pname: Int): Boolean
//
//	/** Return the value for the passed pname given the passed shader.
//	 *
//	 * @param pname one of SHADER_TYPE, INFO_LOG_LENGTH, SHADER_SOURCE_LENGTH */
//	external("getShaderParameter")fun getShaderParameteri(shader: WebGLShader, pname: Int): Int
//
//	/** Return the value for the passed pname given the passed target.
//	 *
//	 * @param pname one of RENDERBUFFER_WIDTH, RENDERBUFFER_HEIGHT, RENDERBUFFER_INTERNAL_FORMAT, RENDERBUFFER_RED_SIZE,
//	*           RENDERBUFFER_GREEN_SIZE, RENDERBUFFER_BLUE_SIZE, RENDERBUFFER_ALPHA_SIZE, RENDERBUFFER_DEPTH_SIZE,
//	*           RENDERBUFFER_STENCIL_SIZE */
//	fun getRenderbufferParameter(target: Int, pname: Int): Int
//
//	/** Return the value for the passed pname given the passed program.
//	 *
//	 * @param pname one of DELETE_STATUS, LINK_STATUS, VALIDATE_STATUS */
//	external("getProgramParameter") fun getProgramParameterb(program: WebGLProgram, pname: Int): Boolean
//
//	/** Return the value for the passed pname given the passed program.
//	 *
//	 * @param pname one of INFO_LOG_LENGTH, ATTACHED_SHADERS, ACTIVE_ATTRIBUTES, ACTIVE_ATTRIBUTE_MAX_LENGTH, ACTIVE_UNIFORMS,
//	*           ACTIVE_UNIFORM_MAX_LENGTH */
//	external("getProgramParameter") fun getProgramParameteri(program: WebGLProgram, pname: Int): Int
//
//	/**
//	 * target must be one of ARRAY_BUFFER or ELEMENT_ARRAY_BUFFER. parameter
//	 * must be one of BUFFER_SIZE or BUFFER_USAGE.
//	 */
//	fun getBufferParameter(target: Int, pname: Int): Int
//
//	/**
//	 * Return the value for the passed pname given the passed target and attachment.
//	 *
//	 * @param pname one of FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL, FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE
//	 */
//	external("getFramebufferAttachmentParameter") fun getFramebufferAttachmentParameteri(target: Int, attachment: Int, pname: Int): Int
//
//	/**
//	 * Return the value for the passed pname given the passed target and attachment.
//	 *
//	 * @param pname FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
//	 * @return {@link WebGLRenderbuffer} or {@link WebGLTexture}
//	 */
//	external("getFramebufferAttachmentParameter") fun getFramebufferAttachmentParametero(target: Int, attachment: Int, pname: Int): WebGLObject
//
//	fun getSupportedExtensions(): Array<String>
//
//}
//
///**
// * The WebGLContextAttributes interface contains drawing surface attributes and is passed as the second parameter to getContext. A
// * native object may be supplied as this parameter; the specified attributes will be queried from this object.
// */
//class WebGLContextAttributes() {
//
//	/**
//	 * Default: true. If the value is true, the drawing buffer has an alpha channel for the purposes of performing OpenGL
//	 * destination alpha operations and compositing with the page. If the value is false, no alpha buffer is available.
//	 */
//	var alpha: Boolean = true
//
//	/**
//	 * Default: false. If the value is true and the implementation supports antialiasing the drawing buffer will perform
//	 * antialiasing using its choice of technique (multisample/supersample) and quality. If the value is false or the
//	 * implementation does not support antialiasing, no antialiasing is performed.
//	 */
//	var antialias: Boolean = false
//
//	/**
//	 * Default: true. If the value is true, the drawing buffer has a depth buffer of at least 16 bits. If the value is
//	 * false, no depth buffer is available.
//	 */
//	var depth: Boolean = true
//
//	/**
//	 * Default: true. If the value is true the page compositor will assume the drawing buffer contains colors with premultiplied
//	 * alpha. If the value is false the page compositor will assume that colors in the drawing buffer are not premultiplied. This
//	 * flag is ignored if the alpha flag is false. See Premultiplied Alpha for more information on the effects of the
//	 * premultipliedAlpha flag.
//	 */
//	var premultipliedAlpha: Boolean = false
//
//	/**
//	 * If set to false, the buffer will be cleared after rendering. If you wish to use canvas.toDataURL(),
//	 * you will either need to draw to the canvas immediately before calling toDataURL(), or set preserveDrawingBuffer
//	 * to true to keep the buffer available after the browser has displayed the buffer (at the cost of increased
//	 * memory use). Defaults to false.
//	 */
//	var preserveDrawingBuffer: Boolean = false
//
//	/**
//	 * Default: false. If the value is true, the drawing buffer has a stencil buffer of at least 8 bits. If the value
//	 * is false, no stencil buffer is available.
//	 */
//	var stencil: Boolean = false
//
//}
//
//interface WebGLProgram {}
//interface WebGLShader {}
//interface WebGLBuffer {}
//interface WebGLFramebuffer {}
//interface WebGLRenderbuffer {}
//interface WebGLObject {}
//interface WebGLTexture {}
//
//interface WebGLActiveInfo {
//
//	/**
//	 * The name of the attribute or uniform.
//	 */
//	var name:String;
//
//	/**
//	 * If the attribute or uniform is an array, this will be the number of elements in the array. Otherwise, this will be 1.
//	 */
//	var size:Int;
//
//	/**
//	 * The data type of the attribute or uniform. Will be one of: FLOAT, FLOAT_MAT2, FLOAT_MAT3, FLOAT_MAT4,
//	 * FLOAT_VEC2, FLOAT_VEC3, FLOAT_VEC4, INT, INT_VEC2, INT_VEC3, INT_VEC4, or UNSIGNED_INT.
//	 */
//	var type:Int
//}
//
//interface WebGLUniformLocation {}