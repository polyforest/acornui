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

package com.acornui.js.dom

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
import com.acornui.core.focus.FocusManager
import com.acornui.core.focus.fakeFocusMouse
import com.acornui.core.graphics.Window
import com.acornui.core.input.InteractivityManager
import com.acornui.core.selection.SelectionManager
import com.acornui.js.JsApplicationBase
import com.acornui.js.dom.component.*
import com.acornui.js.dom.focus.DomFocusManager
import com.acornui.js.selection.DomSelectionManager
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLStyleElement
import kotlin.browser.document
import kotlin.dom.clear

/**
 * @author nbilyk
 */
open class DomApplication(
		rootId: String,
		config: AppConfig = AppConfig(),
		onReady: Owned.(stage: Stage) -> Unit) : JsApplicationBase(rootId, config, onReady) {

	override lateinit var canvas: HTMLElement

	init {
		UserInfo.isOpenGl = false
	}

	override fun initializeCanvas() {
		val rootElement = document.getElementById(rootId) ?: throw Exception("Could not find root canvas $rootId")
		canvas = rootElement as HTMLElement
		canvas.clear()
	}

	override fun initializeCss() {
		val e = document.createElement("style") as HTMLStyleElement
		e.type = "text/css"
		// language=CSS
		e.innerHTML = """
			.acornComponent {
				position: absolute;
				margin: 0;
				padding: 0;
				transform-origin: 0 0;

				overflow-x: hidden;
				overflow-y: hidden;

				pointer-events: auto;
				user-select: none;
				-webkit-user-select: none;
				-moz-user-select: none;
				-ms-user-select: none;
			}
		"""
		canvas.appendChild(e)
	}

	override fun initializeWindow() {
		bootstrap[Window] = DomWindowImpl(canvas, config.window)
	}

	override fun initializeTextures() {
		bootstrap.on(AssetManager) {
			get(AssetManager).setLoaderFactory(AssetTypes.TEXTURE, { path, estimatedBytesTotal ->  DomTextureLoader(path, estimatedBytesTotal) })
		}
	}

	override fun initializeInteractivity() {
		bootstrap[InteractivityManager] = DomInteractivityManager()
	}

	override fun initializeFocusManager() {
		bootstrap[FocusManager] = DomFocusManager()
	}

	override fun initializeSelectionManager() {
		bootstrap[SelectionManager] = DomSelectionManager(canvas)
	}


	/**
	 * The last chance to set dependencies on the application scope.
	 */
	override fun initializeComponents() {
		bootstrap[NativeComponent.FACTORY_KEY] = { DomComponent() }
		bootstrap[NativeContainer.FACTORY_KEY] = { DomContainer() }
		bootstrap[TextField.FACTORY_KEY] = { DomTextField(it) }
		bootstrap[EditableTextField.FACTORY_KEY] = { DomEditableTextField(it) }
		bootstrap[TextInput.FACTORY_KEY] = { DomTextInput(it) }
		bootstrap[TextArea.FACTORY_KEY] = { DomTextArea(it) }
		bootstrap[TextureComponent.FACTORY_KEY] = { DomTextureComponent(it) }
		bootstrap[ScrollArea.FACTORY_KEY] = { DomScrollArea(it) }
		bootstrap[ScrollRect.FACTORY_KEY] = { DomScrollRect(it) }
		bootstrap[Rect.FACTORY_KEY] = { DomRect(it) }
	}

	override fun initializeStage() {
		bootstrap[Stage] = DomStageImpl(OwnedImpl(null, bootstrap.injector), canvas)
	}

	override fun initializeSpecialInteractivity() {
		bootstrap.fakeFocusMouse()
	}
}