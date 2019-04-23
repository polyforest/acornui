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

import com.acornui.component.RenderContextRo
import com.acornui.core.asset.AssetManager
import com.acornui.core.di.Injector
import com.acornui.core.di.InjectorImpl
import com.acornui.core.di.Owned
import com.acornui.core.di.OwnedImpl
import com.acornui.core.focus.FocusManager
import com.acornui.core.graphic.Camera
import com.acornui.core.graphic.Window
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.KeyState
import com.acornui.core.input.MouseState
import com.acornui.core.io.JSON_KEY
import com.acornui.core.io.file.Files
import com.acornui.core.time.Date
import com.acornui.core.time.TimeDriver
import com.acornui.core.time.TimeProvider
import com.acornui.core.time.time
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.serialization.Serializer
import org.mockito.Mockito

object MockInjector {

	val owner: Owned by lazy { OwnedImpl(injector = create()) }

	@Deprecated("", ReplaceWith("owner"))
	fun createOwner(): Owned {
		return OwnedImpl(injector = create())
	}

	fun create(): Injector {

		@Suppress("UNCHECKED_CAST")
		val json = Mockito.mock(Serializer::class.java) as Serializer<String>

		val injector = InjectorImpl(null, listOf(
				TimeDriver to  Mockito.mock(TimeDriver::class.java),
				Window to  Mockito.mock(Window::class.java),
				MouseState to  Mockito.mock(MouseState::class.java),
				KeyState to  Mockito.mock(KeyState::class.java),
				Files to  Mockito.mock(Files::class.java),
				AssetManager to  Mockito.mock(AssetManager::class.java),
				InteractivityManager to  Mockito.mock(InteractivityManager::class.java),
				RenderContextRo to  Mockito.mock(RenderContextRo::class.java),
				FocusManager to  Mockito.mock(FocusManager::class.java),
				Gl20 to  Mockito.mock(Gl20::class.java),
				GlState to  Mockito.mock(GlState::class.java),
				JSON_KEY to json
		))
		time = MockTimeProvider()

		return injector

	}
}

class MockTimeProvider : TimeProvider {
	override fun now(): Date {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun nowMs(): Long = 0

	override fun nanoElapsed(): Long = 0
}