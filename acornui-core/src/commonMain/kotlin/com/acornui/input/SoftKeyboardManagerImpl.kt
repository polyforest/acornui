package com.acornui.input

import com.acornui.component.*
import com.acornui.component.layout.algorithm.flow
import com.acornui.di.*
import com.acornui.input.interaction.*
import com.acornui.observe.dataBinding
import com.acornui.tween.TweenRegistry
import com.acornui.tween.driveTween
import com.acornui.tween.tween
import com.acornui.math.Bounds
import com.acornui.math.Easing
import com.acornui.component.stage as stageDep

class SoftKeyboardManagerImpl(injector: Injector) : ContainerImpl(OwnedImpl(injector)), SoftKeyboardManager {

	override val view: UiComponent = this

	private val softKeyboard = addChild(SoftKeyboardView(this))
	private var hasKeyboard = false

	private var showPercent by validationProp(0f, ValidationFlags.LAYOUT)

	init {
		visible = false
		inject(KeyInput).keyDown.add {
			// If there is a real keyboard, ditch the soft keyboard.
//			hasKeyboard = true
//			show = false
		}

		// Prevent focus to the stage when the soft keyboard is used.
		focusEnabled = false
		focusEnabledChildren = false
		mouseDown(true).add {
			it.preventDefault()
		}
		touchStart(true).add {
			it.preventDefault()
		}
		focusManager.focusedChanged.add(::focusChangedHandler)
	}

	private var closeOnNextClick: Boolean = false
		set(value) {
			if (field != value) {
				field = value
				if (value) {
					stage.click().add(::stageClickHandler)
				} else {
					stage.click().remove(::stageClickHandler)
				}
			}
		}

	private fun focusChangedHandler(old: UiComponentRo?, new: UiComponentRo?) {
		// If the focus has changed, close the keyboard on the next click.
		closeOnNextClick = true
	}

	private fun stageClickHandler(event: ClickInteractionRo) {
		if (owns(event.target)) return
		if (closeOnNextClick) show = false
	}

	private var show = false
		set(value) {
			closeOnNextClick = false
			if (field != value) {
				field = value
				TweenRegistry.kill(this, "showPercent", false)
				val fromP = showPercent
				if (value) {
					visible = true
					val delta = (1f - fromP)
					val t = tween(0.3f * delta, Easing.pow2Out) { previousAlpha, currentAlpha ->
						showPercent = currentAlpha * delta + fromP
					}
					driveTween(t)
					TweenRegistry.register(this, "showPercent", t)
				} else {
					val t = tween(0.3f * fromP, Easing.pow2Out, delay = 0.1f) { previousAlpha, currentAlpha ->
						showPercent = fromP - currentAlpha * fromP
					}
					t.completed.add {
						visible = false
					}
					driveTween(t)
					TweenRegistry.register(this, "showPercent", t)
				}
			}
		}

	override fun show(type: String) {
		show = !hasKeyboard
		softKeyboard.data.value = type
	}

	override fun hide() {
		show = false
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		softKeyboard.setSize(explicitWidth, explicitHeight)
		//softKeyboard.setPosition(0f, softKeyboard.height * (1f - showPercent))
		out.set(softKeyboard.width, softKeyboard.height * showPercent)
	}

	override fun dispose() {
		super.dispose()
		closeOnNextClick = false
		focusManager.focusedChanged.remove(::focusChangedHandler)
	}
}

class SoftKeyboardView(owner: Owned) : LayoutContainer<StackLayoutStyle, StackLayoutData>(owner, StackLayout()) {

	val data = dataBinding(SoftKeyboardType.DEFAULT)
	val capsLock = dataBinding(false)

	private val keyEvent = KeyInteraction()

	init {
		+panel {

			+flow {
				for (i in 'A'.toByte() .. 'Z'.toByte()) {
					+charButton(listOf(i.toChar()))
				}
			} layout { widthPercent = 1f }

		} layout { widthPercent = 1f }
	}

	private fun charButton(chars: List<Char>, init: ComponentInit<CharButton> = {}): CharButton {
		val c = CharButton(this)
		c.data = chars
		c.init()
		return c

	}
}

private class CharButton(owner: Owned) : ButtonImpl(owner) {

	private val charEvent = CharInteraction().apply {
		type = CharInteractionRo.CHAR
		isFabricated = true
	}

	var data: List<Char> = emptyList()
		set(value) {
			field = value
			label = data.firstOrNull()?.toString() ?: " "
		}

	init {
		longPress().add {
			println("Long press.")
		}
		click().add {
			println("Click.")
			data.firstOrNull()?.let {
				charEvent.char = it
				interactivity.dispatch(focusManager.focused ?: stage, charEvent)
			}
		}
	}

}