package com.acornui.component

import com.acornui.component.layout.SizeConstraints
import com.acornui.component.style.OptionalSkinPart
import com.acornui.component.style.SkinPart
import com.acornui.component.style.noSkin
import com.acornui.component.style.noSkinOptional
import com.acornui.di.Owned
import com.acornui.factory.LazyInstance
import com.acornui.math.Bounds

class StatesButtonSkin(owner: Owned,
					   upState: SkinPart = noSkin,
					   overState: OptionalSkinPart = noSkinOptional,
					   downState: OptionalSkinPart = noSkinOptional,
					   toggledUpState: OptionalSkinPart = noSkinOptional,
					   toggledOverState: OptionalSkinPart = noSkinOptional,
					   toggledDownState: OptionalSkinPart = noSkinOptional,
					   indeterminateUpState: OptionalSkinPart = noSkinOptional,
					   indeterminateOverState: OptionalSkinPart = noSkinOptional,
					   indeterminateDownState: OptionalSkinPart = noSkinOptional,
					   disabledState: OptionalSkinPart = noSkinOptional

) : ContainerImpl(owner), ButtonSkin {

	private val _stateSkinMap = HashMap<ButtonState, LazyInstance<Owned, UiComponent?>>()

	private var currentSkinPart: UiComponent? = null

	init {
		_stateSkinMap[ButtonState.UP] = LazyInstance(this, upState)
		_stateSkinMap[ButtonState.OVER] = LazyInstance(this, overState)
		_stateSkinMap[ButtonState.DOWN] = LazyInstance(this, downState)
		_stateSkinMap[ButtonState.TOGGLED_UP] = LazyInstance(this, toggledUpState)
		_stateSkinMap[ButtonState.TOGGLED_OVER] = LazyInstance(this, toggledOverState)
		_stateSkinMap[ButtonState.TOGGLED_DOWN] = LazyInstance(this, toggledDownState)
		_stateSkinMap[ButtonState.INDETERMINATE_UP] = LazyInstance(this, indeterminateUpState)
		_stateSkinMap[ButtonState.INDETERMINATE_OVER] = LazyInstance(this, indeterminateOverState)
		_stateSkinMap[ButtonState.INDETERMINATE_DOWN] = LazyInstance(this, indeterminateDownState)
		_stateSkinMap[ButtonState.DISABLED] = LazyInstance(this, disabledState)
	}

	override var label: String = ""
		set(value) {
			if (field != value) {
				field = value
				(currentSkinPart as? Labelable)?.label = (value)
				invalidateSize()
			}
		}
	override var buttonState: ButtonState = ButtonState.UP
		set(value) {
			if (isDisposed) return
			val previousState = field
			field = value
			val newSkinPart = value.fallbackWalk { state ->
				_stateSkinMap[state]?.instance
			}
			val previousSkinPart = currentSkinPart
			if (previousSkinPart == newSkinPart) return
			currentSkinPart = newSkinPart
			if (newSkinPart is Labelable) {
				newSkinPart.label = label
			}
			if (newSkinPart != null) addChild(newSkinPart)
			removeChild(previousSkinPart)
		}
	override fun updateSizeConstraints(out: SizeConstraints) {
		if (currentSkinPart == null) return
		out.set(currentSkinPart!!.sizeConstraints)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		if (currentSkinPart != null) {
			currentSkinPart!!.setSize(explicitWidth, explicitHeight)
			out.set(currentSkinPart!!.bounds)
		}
	}
}


fun ButtonStyle.set(skinPartFactory: (ButtonState) -> OptionalSkinPart): ButtonStyle {
	@Suppress("UNCHECKED_CAST")
	val upState = skinPartFactory(ButtonState.UP) as SkinPart
	skin = {
		StatesButtonSkin(this,
				upState = upState,
				overState = skinPartFactory(ButtonState.OVER),
				downState = skinPartFactory(ButtonState.DOWN),
				toggledUpState = skinPartFactory(ButtonState.TOGGLED_UP),
				toggledOverState = skinPartFactory(ButtonState.TOGGLED_OVER),
				toggledDownState = skinPartFactory(ButtonState.TOGGLED_DOWN),
				indeterminateUpState = skinPartFactory(ButtonState.INDETERMINATE_UP),
				indeterminateOverState = skinPartFactory(ButtonState.INDETERMINATE_OVER),
				indeterminateDownState = skinPartFactory(ButtonState.INDETERMINATE_DOWN),
				disabledState = skinPartFactory(ButtonState.DISABLED)
		)
	}
	return this
}
