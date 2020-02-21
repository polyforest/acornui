package com.acornui.input

import com.acornui.component.*
import com.acornui.component.layout.algorithm.flow
import com.acornui.component.style.StyleTag
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.own
import com.acornui.di.owns
import com.acornui.function.as2
import com.acornui.input.interaction.*
import com.acornui.logging.Log
import com.acornui.math.Bounds
import com.acornui.math.Easing
import com.acornui.observe.Observable
import com.acornui.observe.dataBinding
import com.acornui.signal.Signal1
import com.acornui.signal.bind
import com.acornui.start
import com.acornui.stop
import com.acornui.tween.Tween
import com.acornui.tween.tween

class SoftKeyboardManagerImpl(owner: Context) : ContextImpl(owner), SoftKeyboardManager, Observable {

	override fun createView(owner: Context): UiComponent = SoftKeyboardContainer(owner, this)

	private val _changed = Signal1<SoftKeyboardManager>()
	override val changed = _changed.asRo()

	override var keyboardType: String? = null
		private set

	override val isShowing: Boolean
		get() = keyboardType != null

	override fun show(type: String) {
		if (keyboardType == type) return
		keyboardType = type
		_changed.dispatch(this)
	}

	override fun hide() {
		if (keyboardType == null) return
		keyboardType = null
		_changed.dispatch(this)
	}

	override fun dispose() {
		super.dispose()
		hide()
		_changed.dispose()
	}
}

private class SoftKeyboardContainer(owner: Context, private val manager: SoftKeyboardManager) : ContainerImpl(owner) {

	private val softKeyboard = addChild(SoftKeyboardView(this))
	private var showPercent by validationProp(0f, ValidationFlags.LAYOUT)

	init {
		Log.info("Soft keyboard created")
		visible = false
		inject(KeyInput).keyDown.add {
			// On typing, hide the soft keyboard
			show = false
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
		focusManager.focusedChanged.add(::focusChangedHandler.as2)

		own(manager.bind {
			val type = manager.keyboardType
			if (type != null) {
				softKeyboard.data.value = type
				show = true
			} else {
				show = false
			}
		})
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

	private fun focusChangedHandler() {
		// If the focus has changed, close the keyboard on the next click.
		closeOnNextClick = true
	}

	private fun stageClickHandler(event: ClickInteractionRo) {
		if (owns(event.target)) return
		if (closeOnNextClick) show = false
	}

	private var showPercentTween: Tween? = null

	private var show = false
		set(value) {
			closeOnNextClick = false
			if (field != value) {
				field = value
				showPercentTween?.stop()
				val fromP = showPercent
				if (value) {
					visible = true
					val delta = (1f - fromP)
					showPercentTween = tween(0.3f * delta, Easing.pow2Out) { _, currentAlpha ->
						showPercent = currentAlpha * delta + fromP
					}.start()
				} else {
					showPercentTween = tween(0.3f * fromP, Easing.pow2Out, delay = 0.1f) { _, currentAlpha ->
						showPercent = fromP - currentAlpha * fromP
					}.apply {
						completed.add {
							visible = false
						}
					}.start()
				}
			}
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		softKeyboard.setSize(explicitWidth, explicitHeight)
		out.set(softKeyboard.width, softKeyboard.height * showPercent)
	}

	override fun dispose() {
		super.dispose()
		closeOnNextClick = false
		focusManager.focusedChanged.remove(::focusChangedHandler.as2)
	}
}

class SoftKeyboardView(owner: Context) : Panel(owner) {

	val data = dataBinding(SoftKeyboardType.DEFAULT)
	val capsLock = dataBinding(false)

	private val keyEvent = KeyInteraction()

	init {
		styleTags.add(Companion)
			
		+flow {
			for (i in 'A'.toByte()..'Z'.toByte()) {
				+charButton(listOf(i.toChar()))
			}
		} layout { widthPercent = 1f }

	}

	private fun charButton(chars: List<Char>, init: ComponentInit<CharButton> = {}): CharButton {
		val c = CharButton(this)
		c.data = chars
		c.init()
		return c

	}
	
	companion object : StyleTag
}

private class CharButton(owner: Context) : ButtonImpl(owner) {

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
				val target = focusManager.focused
				if (target != null)
					interactivity.dispatch(target, charEvent)
			}
		}
	}

}