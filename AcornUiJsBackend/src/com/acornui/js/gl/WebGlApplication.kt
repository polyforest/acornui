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
import com.acornui.core.UserInfo
import com.acornui.core.assets.AssetManager
import com.acornui.core.assets.AssetTypes
import com.acornui.core.di.Owned
import com.acornui.core.di.OwnedImpl
import com.acornui.core.focus.fakeFocusMouse
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
open class WebGlApplication(
		rootId: String,
		config: AppConfig = AppConfig(),
		onReady: Owned.(stage: Stage) -> Unit) : JsApplicationBase(rootId, config, onReady) {

	override lateinit var canvas: HTMLCanvasElement

	init {
		UserInfo.isOpenGl = true
	}

	override fun initializeBootstrap() {
		println("webgl init bootstrap")
		super.initializeBootstrap()
		initializeGl()
		initializeGlState()
	}

	override fun initializeCanvas() {
		val rootElement = document.getElementById(rootId) ?: throw Exception("Could not find root canvas $rootId")
		val root = rootElement as HTMLElement
		root.clear()
		val canvas = document.createElement("canvas") as HTMLCanvasElement
		canvas.style.width = "100%"
		canvas.style.height = "100%"
		root.appendChild(canvas)
		this.canvas = canvas
	}

	protected open fun initializeGl() {
		val glConfig = config.gl
		val attributes = WebGLContextAttributes()
		attributes.alpha = glConfig.alpha
		attributes.antialias = glConfig.antialias
		attributes.depth = glConfig.depth
		attributes.stencil = glConfig.stencil
		attributes.premultipliedAlpha = false

		val context = WebGl.getContext(canvas, attributes) ?: throw Exception("Browser does not support WebGL") // TODO: Make this a better UX
		val gl = WebGl20(context)
		bootstrap[Gl20] = gl
	}

	override fun initializeWindow() {
		bootstrap.on(Gl20) {
			this[Window] = WebGlWindowImpl(canvas, config.window, get(Gl20))
		}
	}

	protected open fun initializeGlState() {
		bootstrap.on(Gl20) {
			this[GlState] = GlState(get(Gl20))
		}
	}

	override fun initializeTextures() {
		bootstrap.on(AssetManager, Gl20, GlState) {
			get(AssetManager).setLoaderFactory(AssetTypes.TEXTURE, { path, estimatedBytesTotal ->  WebGlTextureLoader(path, estimatedBytesTotal, get(Gl20), get(GlState)) })
		}
	}

	/**
	 * The last chance to set dependencies on the application scope.
	 */
	override fun initializeComponents() {
		bootstrap[NativeComponent.FACTORY_KEY] = { owner -> NativeComponentDummy }
		bootstrap[NativeContainer.FACTORY_KEY] = { owner -> NativeContainerDummy }
		bootstrap[TextField.FACTORY_KEY] = ::GlTextField
		bootstrap[EditableTextField.FACTORY_KEY] = ::GlEditableTextField
		bootstrap[TextInput.FACTORY_KEY] = ::GlTextInput
		bootstrap[TextArea.FACTORY_KEY] = ::GlTextArea
		bootstrap[TextureComponent.FACTORY_KEY] = ::GlTextureComponent
		bootstrap[ScrollArea.FACTORY_KEY] = ::GlScrollArea
		bootstrap[ScrollRect.FACTORY_KEY] = ::GlScrollRect
		bootstrap[Rect.FACTORY_KEY] = ::GlRect
	}

	override fun initializeStage() {
		bootstrap[Stage] = GlStageImpl(OwnedImpl(null, bootstrap.injector))
	}

	override fun initializeSpecialInteractivity() {
		bootstrap.fakeFocusMouse()
		val clickDispatcher = JsClickDispatcher(bootstrap.injector, canvas)
		clickDispatcher.init()
	}

}