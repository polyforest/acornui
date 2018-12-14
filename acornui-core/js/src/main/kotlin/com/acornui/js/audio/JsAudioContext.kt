package com.acornui.js.audio

object JsAudioContext {
	val instance: AudioContext by lazy {
		js("var JsAudioContext = window.AudioContext || window.webkitAudioContext; new JsAudioContext();")
	}
}