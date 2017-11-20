/*
 * Copyright 2014 Nicholas Bilyk
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

package com.acornui.js.gl

import com.acornui.component.*
import com.acornui.component.scroll.ScrollArea
import com.acornui.component.scroll.ScrollRect
import com.acornui.component.text.EditableTextField
import com.acornui.component.text.TextArea
import com.acornui.component.text.TextField
import com.acornui.component.text.TextInput
import com.acornui.core.AppConfig
import com.acornui.core.assets.AssetManager
import com.acornui.core.assets.AssetTypes
import com.acornui.core.di.Owned
import com.acornui.core.di.dKey
import com.acornui.core.focus.FakeFocusMouse
import com.acornui.core.graphics.Window
import com.acornui.gl.component.*
import com.acornui.gl.component.text.GlEditableTextField
import com.acornui.gl.component.text.GlTextArea
import com.acornui.gl.component.text.GlTextField
import com.acornui.gl.component.text.GlTextInput
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.js.JsApplicationBase
import com.acornui.js.html.WebGl
import com.acornui.js.input.JsClickDispatcher
import org.khronos.webgl.WebGLContextAttributes
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.dom.clear

/**
 * @author nbilyk
 */
@Suppress("unused")
open class WebGlApplication(private val rootId: String) : JsApplicationBase() {

	override val isOpenGl = true

	override val canvasTask by BootTask {
		val rootElement = document.getElementById(rootId) ?: throw Exception("Could not find root canvas $rootId")
		val root = rootElement as HTMLElement
		root.style.setProperty("-webkit-tap-highlight-color", "rgba(0,0,0,0)")
		root.clear()
		val canvas = document.createElement("canvas") as HTMLCanvasElement
		canvas.style.setProperty("-webkit-tap-highlight-color", "rgba(0,0,0,0)")
		canvas.style.width = "100%"
		canvas.style.height = "100%"
		root.appendChild(canvas)
		set(CANVAS, canvas)
	}

	protected open val glTask by BootTask {
		val config = get(AppConfig)
		val glConfig = config.gl
		val attributes = WebGLContextAttributes()
		attributes.alpha = glConfig.alpha
		attributes.antialias = glConfig.antialias
		attributes.depth = glConfig.depth
		attributes.stencil = glConfig.stencil
		attributes.premultipliedAlpha = false

		val context = WebGl.getContext(get(CANVAS), attributes) ?: throw Exception("Browser does not support WebGL") // TODO: Make this a better UX
		val gl = WebGl20(context)
		set(Gl20, gl)
	}
	
	override val windowTask by BootTask {
		val config = get(AppConfig)
		set(Window, WebGlWindowImpl(get(CANVAS), config.window, get(Gl20)))
	}

	protected open val glStateTask by BootTask {
		set(GlState, GlState(get(Gl20)))
	}

	protected open val textureLoaderTask by BootTask {
		val gl = get(Gl20)
		val glState = get(GlState)
		get(AssetManager).setLoaderFactory(AssetTypes.TEXTURE, { path, estimatedBytesTotal -> WebGlTextureLoader(path, estimatedBytesTotal, gl, glState) })
	}

	/**
	 * The last chance to set dependencies on the application scope.
	 */
	override val componentsTask  by BootTask {
		set(NativeComponent.FACTORY_KEY, { _ -> NativeComponentDummy })
		set(NativeContainer.FACTORY_KEY, { _ -> NativeContainerDummy })
		set(TextField.FACTORY_KEY, ::GlTextField)
		set(EditableTextField.FACTORY_KEY, ::GlEditableTextField)
		set(TextInput.FACTORY_KEY, ::GlTextInput)
		set(TextArea.FACTORY_KEY, ::GlTextArea)
		set(TextureComponent.FACTORY_KEY, ::GlTextureComponent)
		set(ScrollArea.FACTORY_KEY, ::GlScrollArea)
		set(ScrollRect.FACTORY_KEY, ::GlScrollRect)
		set(Rect.FACTORY_KEY, ::GlRect)
	}

	override suspend fun createStage(owner: Owned): Stage {
		return GlStageImpl(owner)
	}

	override suspend fun initializeSpecialInteractivity(owner: Owned) {
		FakeFocusMouse(owner.injector)
		JsClickDispatcher(get(CANVAS), owner.injector)
	}
	
	companion object {
		val CANVAS = dKey<HTMLCanvasElement, HTMLElement>(JsApplicationBase.CANVAS)
	}

}