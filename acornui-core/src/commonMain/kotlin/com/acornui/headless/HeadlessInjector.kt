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
import com.acornui.di.*
import com.acornui.focus.FocusManager
import com.acornui.gl.core.CachedGl20
import com.acornui.graphic.Window
import com.acornui.input.*
import com.acornui.io.byteBuffer

object HeadlessInjector {

	val owner: Owned by lazy { OwnedImpl(injector = create(AppConfig())) }

	fun create(config: AppConfig): Injector {
		return InjectorImpl(null, listOf<DependencyPair<*>>(
				Window to HeadlessWindow(config.window),
				MouseInput to MockMouseInput,
				KeyInput to MockKeyInput,
				Loaders.binaryLoader to MockLoader(byteBuffer(1)),
				Loaders.musicLoader to MockLoader(MockMusic),
				Loaders.rgbDataLoader to MockLoader(MockTexture.rgbData),
				Loaders.soundLoader to MockLoader(MockSoundFactory),
				Loaders.textLoader to MockLoader(""),
				Loaders.textureLoader to MockLoader(MockTexture),
				InteractivityManager to MockInteractivityManager,
				FocusManager to MockFocusManager,
				CachedGl20 to MockGl20
		))
	}
}