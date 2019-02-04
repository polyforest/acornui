package com.acornui.gl.core

enum class BufferUsage(val value: Int) {

	/**
	 * The user will be changing the data after every use. Or almost every use.
	 */
	STREAM(Gl20.STREAM_DRAW),

	/**
	 * The user will set the data once.
	 */
	STATIC(Gl20.STATIC_DRAW),

	/**
	 * The user will set the data occasionally.
	 */
	DYNAMIC(Gl20.DYNAMIC_DRAW)

}