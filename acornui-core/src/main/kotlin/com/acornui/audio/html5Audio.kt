/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.audio

import com.acornui.system.userInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLMediaElement
import org.w3c.dom.events.EventTarget

val audioContextSupported: Boolean by lazy {
    userInfo.isBrowser && js("""var JsAudioContext = window.AudioContext || window.webkitAudioContext; JsAudioContext != null""") as Boolean
}

external class AudioContext : EventTarget {

    val currentTime: Double

    val destination: AudioDestinationNode

    val listener: AudioListener

    val sampleRate: Double

    /**
     * suspended, running, closed
     */
    val state: String

    fun close()

    fun createBuffer(numOfChannels: Int, length: Int, sampleRate: Int): AudioBuffer
    fun createBufferSource(): AudioBufferSourceNode
    fun createMediaElementSource(myMediaElement: HTMLMediaElement): MediaElementAudioSourceNode

    fun decodeAudioData(audioData: ArrayBuffer, callback: (decodedData: ArrayBuffer) -> Unit)

    fun createPanner(): PannerNode
    fun createGain(): GainNode
}

fun AudioContext.decodeAudioData(audioData: ArrayBuffer): Deferred<ArrayBuffer> {
    val c = CompletableDeferred<ArrayBuffer>()
    // The Audio Context handles creating source buffers from raw binary
    decodeAudioData(audioData) {
        c.complete(it)
    }
    return c
}

external class AudioDestinationNode : AudioNode {

    var maxChannelCount: Int

}

external class MediaElementAudioSourceNode : AudioNode

external class AudioListener : AudioNode {
    fun setOrientation(x: Double, y: Double, z: Double, xUp: Double, yUp: Double, zUp: Double)
}

external class AudioBuffer

external class AudioBufferSourceNode : AudioNode {

    var buffer: ArrayBuffer


    var loop: Boolean
    var loopStart: Double
    var loopEnd: Double

    fun start()
    fun start(start: Double)
    fun start(start: Double, offset: Double)
    fun start(start: Double, offset: Double, duration: Double)

    fun stop(delay: Double)
}

abstract external class AudioNode : EventTarget {
    val context: AudioContext
    val numberOfInputs: Int
    val numberOfOutputs: Int
    var channelCount: Int
    fun connect(other: AudioNode)
    fun connect(other: AudioParam)

    fun disconnect()
}

external interface AudioParam {

    /**
     * Gets or sets the current value of this AudioParam. Initially, the value is set to AudioParam.defaultValue.
     * Part of the Web Audio API.
     *
     * Note: Though value can be set, any modifications happening while there are automation events scheduled—that is,
     * events scheduled using the methods of the AudioParam—are ignored, without raising any exception.
     */
    var value: Double

    val defaultValue: Double

    /**
     * @param value A floating point number representing the value the AudioParam will change to at the given time.
     * @param startTime A double representing the time (in seconds) after the AudioContext was first created that the
     * change in value will happen. A TypeError is thrown if this value is negative.
     */
    fun setValueAtTime(value: Double, startTime: Double)
    fun linearRampToValueAtTime(value: Double, endTime: Double)
    fun exponentialRampToValueAtTime(value: Double, endTime: Double)

    //....
}

external class GainNode : AudioNode {
    val gain: AudioParam
}

external class PannerNode : AudioNode {
    var panningModel: String

    var distanceModel: String

    var refDistance: Double
    var maxDistance: Double
    var rolloffFactor: Double
    var coneInnerAngle: Double
    var coneOuterAngle: Double
    var coneOuterGain: Double

    fun setPosition(x: Double, y: Double, z: Double)
    fun setOrientation(x: Double, y: Double, z: Double)
}

enum class PanningModel(val value: String) {
    EQUAL_POWER("equalpower"),

    // Sounds crackly to me when changing the position...
    HRTF("HRTF")
}
