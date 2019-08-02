/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.test

import com.acornui.asset.AssetManager
import com.acornui.component.RenderContextRo
import com.acornui.di.Injector
import com.acornui.di.InjectorImpl
import com.acornui.di.Owned
import com.acornui.di.OwnedImpl
import com.acornui.focus.FocusManager
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.graphic.Window
import com.acornui.input.InteractivityManager
import com.acornui.input.KeyState
import com.acornui.input.MouseState
import com.acornui.io.file.Files
import com.acornui.time.TimeDriver

object MockInjector {

	val owner: Owned by lazy { OwnedImpl(injector = create()) }

	@Deprecated("", ReplaceWith("owner"))
	fun createOwner(): Owned {
		return OwnedImpl(injector = create())
	}

	fun create(): Injector {
		return InjectorImpl(null, listOf(
				TimeDriver to MockTimeDriver,
				Window to MockWindow,
				MouseState to MockMouseState,
				KeyState to MockKeyState,
				Files to MockFiles,
				AssetManager to MockAssetManager,
				InteractivityManager to MockInteractivityManager,
				RenderContextRo to MockRenderContext,
				FocusManager to MockFocusManager,
				Gl20 to MockGl20,
				GlState to MockGlState
		))
	}
}