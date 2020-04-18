/*
 * Copyright 2019 Poly Forest, LLC
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

package com.acornui.headless

import com.acornui.audio.Music
import com.acornui.audio.MusicReadyState
import com.acornui.signal.Signal
import com.acornui.signal.emptySignal
import kotlin.time.Duration

object MockMusic : Music {
	override var onCompleted: (() -> Unit)? = null
	override val duration: Duration = Duration.ZERO
	override val readyStateChanged: Signal<() -> Unit> = emptySignal()
	override val readyState: MusicReadyState = MusicReadyState.NOTHING
	override val isPlaying: Boolean = false
	override var loop: Boolean = false
	override var volume: Float = 0f

	override fun play() {
	}

	override fun pause() {
	}

	override fun stop() {
	}

	override var currentTime: Duration = Duration.ZERO

	override fun update() {
	}

	override fun dispose() {
	}
}