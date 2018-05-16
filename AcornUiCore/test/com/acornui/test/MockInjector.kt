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

import com.acornui.component.NativeComponent
import com.acornui.component.NativeComponentDummy
import com.acornui.component.NativeContainer
import com.acornui.component.NativeContainerDummy
import com.acornui.core.assets.AssetManager
import com.acornui.core.di.*
import com.acornui.core.graphics.Camera
import com.acornui.core.graphics.Window
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.KeyState
import com.acornui.core.input.MouseState
import com.acornui.core.io.JSON_KEY
import com.acornui.core.io.file.Files
import com.acornui.core.time.Date
import com.acornui.core.time.TimeDriver
import com.acornui.core.time.TimeProvider
import com.acornui.core.time.time
import com.acornui.serialization.Serializer
import org.mockito.Mockito

object MockInjector {

	val owner: Owned by lazy { OwnedImpl(create()) }

	@Deprecated("", ReplaceWith("owner"))
	fun createOwner(): Owned {
		return OwnedImpl(create())
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
				Camera to  Mockito.mock(Camera::class.java),
				JSON_KEY to json,
				NativeComponent.FACTORY_KEY to  { NativeComponentDummy },
				NativeContainer.FACTORY_KEY to  { NativeContainerDummy }
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