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
import com.acornui.core.assets.AssetManager
import com.acornui.core.assets.AssetType
import com.acornui.core.assets.LoaderFactory
import com.acornui.core.di.Owned
import com.acornui.core.focus.FakeFocusMouse
import com.acornui.core.focus.FocusManager
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
		private val rootId: String
) : JsApplicationBase() {

	override val isOpenGl = false

	override val canvasTask by BootTask {
		val rootElement = document.getElementById(rootId) ?: throw Exception("Could not find root canvas $rootId")
		val canvas = rootElement as HTMLElement
		canvas.clear()
		set(CANVAS, canvas)
	}

	open protected val cssTask by BootTask {
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
		get(CANVAS).appendChild(e)
	}

	override val windowTask by BootTask {
		set(Window, DomWindowImpl(get(CANVAS), get(AppConfig).window))
	}

	override val componentsTask by BootTask {
		set(NativeComponent.FACTORY_KEY, { DomComponent() })
		set(NativeContainer.FACTORY_KEY, { DomContainer() })
		set(TextField.FACTORY_KEY, { DomTextField(it) })
		set(EditableTextField.FACTORY_KEY, { DomEditableTextField(it) })
		set(TextInput.FACTORY_KEY, { DomTextInput(it) })
		set(TextArea.FACTORY_KEY, { DomTextArea(it) })
		set(TextureComponent.FACTORY_KEY, { DomTextureComponent(it) })
		set(ScrollArea.FACTORY_KEY, { DomScrollArea(it) })
		set(ScrollRect.FACTORY_KEY, { DomScrollRect(it) })
		set(Rect.FACTORY_KEY, { DomRect(it) })
	}


	override fun addAssetLoaders(loaders: HashMap<AssetType<*>, LoaderFactory<*>>) {
		super.addAssetLoaders(loaders)
		loaders[AssetType.TEXTURE] = { path, estimatedBytesTotal -> DomTextureLoader(path, estimatedBytesTotal) }
	}

	override val interactivityTask by BootTask {
		set(InteractivityManager, DomInteractivityManager())
	}

	override val focusManagerTask by BootTask {
		set(FocusManager, DomFocusManager())
	}

	override val selectionManagerTask by BootTask {
		set(SelectionManager, DomSelectionManager(get(CANVAS)))
	}

	override suspend fun createStage(owner: Owned): Stage {
		return DomStageImpl(owner, get(CANVAS))
	}

	override suspend fun initializeSpecialInteractivity(owner: Owned) {
		super.initializeSpecialInteractivity(owner)
		FakeFocusMouse(owner.injector)
	}
}