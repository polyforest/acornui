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

import com.acornui.AppConfig
import com.acornui.asset.Loaders
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.DependencyMap
import com.acornui.di.dependencyMapOf
import com.acornui.focus.FocusManager
import com.acornui.gl.core.CachedGl20
import com.acornui.graphic.RgbData
import com.acornui.graphic.Window
import com.acornui.input.InteractivityManager
import com.acornui.input.KeyInput
import com.acornui.input.MouseInput
import com.acornui.io.ProgressReporterImpl
import com.acornui.io.byteBuffer
import com.acornui.io.progressReporterKey

object HeadlessDependencies {

	val owner: Context by lazy { ContextImpl(create(AppConfig())) }

	fun create(config: AppConfig): DependencyMap {
		return dependencyMapOf(
				Window to HeadlessWindow(config.window),
				MouseInput to MockMouseInput,
				KeyInput to MockKeyInput,
				progressReporterKey to ProgressReporterImpl(),
				Loaders.binaryLoader to MockLoader(byteBuffer(1)),
				Loaders.musicLoader to MockLoader(MockMusic),
				Loaders.rgbDataLoader to MockLoader(RgbData(1, 1, false)),
				Loaders.soundLoader to MockLoader(MockSoundFactory),
				Loaders.textLoader to MockLoader(""),
				Loaders.textureLoader to MockLoader { MockTexture() },
				InteractivityManager to MockInteractivityManager,
				FocusManager to MockFocusManager,
				CachedGl20 to MockCachedGl20
		)
	}
}