/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.component

import com.acornui.Disposable
import com.acornui.component.style.cssClass
import com.acornui.di.Context
import com.acornui.di.dependencyFactory
import com.acornui.dom.addStyleToHead
import com.acornui.input.*
import com.acornui.signal.EventOptions
import com.acornui.signal.once
import com.acornui.skins.CssProps
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.events.FocusEvent
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class Modal(owner: Context) : Div(owner) {

	private val delegate = div()

	init {
		style.display = "none" // The actual display elements will be delegated to ModalContainer
		val modalContainer = inject(ModalContainer)

		isConnectedChanged.listen {
			if (it) {
				modalContainer.addElement(delegate)
			} else {
				modalContainer.removeElement(delegate)
			}
		}
	}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: WithNode) {
		delegate.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: WithNode) {
		delegate.removeElement(element)
	}
}

class ModalContainer(owner: Context) : Div(owner) {

	private var focusWatch: Disposable? = null

	init {
		addClass(ModalStyle.modal)
		stage.addElement(this)
		tabIndex = 0
	}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: WithNode) {
		super.onElementAdded(oldIndex, newIndex, element)
		if (elements.size == 1) {
			style.display = "flex"
			stage.addClass(ModalStyle.stageWithModal)
			focusWatch = stage.focusedIn.listen(EventOptions(isPassive = false), ::preventFocusHandler)
		}
	}

	override fun onElementRemoved(index: Int, element: WithNode) {
		super.onElementRemoved(index, element)
		if (elements.isEmpty()) {
			focusWatch?.dispose()
			focusWatch = null
			style.display = "none"
			stage.removeClass(ModalStyle.stageWithModal)
		}
	}

	private fun preventFocusHandler(e: FocusEvent) {
		if (!dom.contains(document.activeElement)) {
			e.preventDefault()
			dom.focus()
		}
	}

	companion object : Context.Key<ModalContainer> {
		override val factory = dependencyFactory { ModalContainer(it) }
	}
}

object ModalStyle {

	val modal by cssClass()
	val stageWithModal by cssClass()

	init {
		addStyleToHead(
			"""
$modal {
	display: none;
	position: absolute;
	top: 0;
	left: 0;
	z-index: 10;
	background: #2228;
	justify-content: center;
	align-items: center;
	width: 100vw;
	height: 100vh;
	padding: ${CssProps.padding.v};
}		
		
$stageWithModal > div:not($modal) {
	filter: blur(3px);
}
		"""
		)
	}
}

inline fun Context.modal(init: ComponentInit<Modal> = {}): Modal {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Modal(this).apply(init)
}

inline fun Context.modalWindow(init: ComponentInit<WindowPanel> = {}): Modal {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return modal {
		+windowPanel {
			init()
			tabIndex = 0
			isConnectedChanged.once {
				focus()
			}
			closeButton.clicked.listen {
				if (!it.defaultPrevented)
					this@modal.dispose()
			}
			keyPressed.listen {
				if (!it.defaultPrevented && it.keyCode == Ascii.ESCAPE)
					this@modal.dispose()
			}
		}
	}
}

