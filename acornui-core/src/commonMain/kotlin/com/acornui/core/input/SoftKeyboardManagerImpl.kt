package com.acornui.core.input

import com.acornui.component.StackLayout
import com.acornui.component.StackLayoutData
import com.acornui.component.StackLayoutStyle
import com.acornui.component.UiComponent
import com.acornui.component.layout.ElementLayoutContainerImpl
import com.acornui.core.di.Injector
import com.acornui.core.di.OwnedImpl

class SoftKeyboardManagerImpl(injector: Injector) : ElementLayoutContainerImpl<StackLayoutStyle, StackLayoutData>(OwnedImpl(injector), StackLayout()), SoftKeyboardManager {

	override val view: UiComponent = this



	override fun open(type: String, priority: Float): SoftKeyboardRef {
		return object : SoftKeyboardRef {
			override fun dispose() {
			}
		}
	}
}