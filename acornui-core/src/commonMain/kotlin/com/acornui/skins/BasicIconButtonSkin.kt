package com.acornui.skins

import com.acornui.component.*
import com.acornui.component.layout.*
import com.acornui.component.layout.algorithm.HorizontalLayout
import com.acornui.component.layout.algorithm.HorizontalLayoutData
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.text.TextField
import com.acornui.component.text.text
import com.acornui.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.reflect.observableAndCall
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface IconButtonSkin : ButtonSkin, SingleElementContainer<UiComponent>

class BasicIconButtonSkin(
		owner: Owned,
		private val texture: ButtonSkin
) : SingleElementContainerImpl<UiComponent>(owner), IconButtonSkin, LayoutDataProvider<HorizontalLayoutData> {

	val layoutStyle = bind(IconButtonLayoutStyle())
	private val layoutAlgorithm = HorizontalLayout()
	override fun createLayoutData() = layoutAlgorithm.createLayoutData()

	private val textField: TextField

	init {
		styleTags.add(BasicIconButtonSkin)
		addChild(texture)
		textField = addChild(text {
			interactivityMode = InteractivityMode.NONE
			visible = false
		})
	}

	override var label: String = ""
		set(value) {
			field = value
			textField.label = value
			texture.label = value
			textField.visible = value.isNotEmpty()
		}

	override var buttonState: ButtonState by observableAndCall(ButtonState.UP) { value ->
		texture.buttonState = value
	}

	override fun updateStyles() {
		super.updateStyles()
		layoutAlgorithm.style.apply {
			gap = layoutStyle.gap
			padding = layoutStyle.padding
			verticalAlign = layoutStyle.verticalAlign
			horizontalAlign = layoutStyle.horizontalAlign
		}
	}

	private val _elementsToLayout = ArrayList<LayoutElement>()
	private val elementsToLayout: List<LayoutElement>
		get() {
			val icon = element
			_elementsToLayout.clear()
			if (icon != null && icon.shouldLayout)
				_elementsToLayout.add(icon)
			if (textField.shouldLayout)
				_elementsToLayout.add(if (layoutStyle.iconOnLeft) _elementsToLayout.size else 0, textField)
			return _elementsToLayout
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		layoutAlgorithm.layout(explicitWidth, explicitHeight, elementsToLayout, out)
		if (explicitWidth != null && explicitWidth > out.width) out.width = explicitWidth
		if (explicitHeight != null && explicitHeight > out.height) out.height = explicitHeight
		texture.setSize(out)
	}

	companion object : StyleTag
}

class IconButtonLayoutStyle : StyleBase() {

	override val type: StyleType<IconButtonLayoutStyle> = Companion

	/**
	 * The horizontal gap between elements.
	 */
	var gap by prop(4f)

	/**
	 * The Padding object with left, bottom, top, and right padding.
	 */
	var padding: PadRo by prop(Pad(4f))

	/**
	 * The horizontal alignment of the entire row within the explicit width.
	 * If the explicit width is null, this will have no effect.
	 */
	var horizontalAlign by prop(HAlign.CENTER)

	/**
	 * The vertical alignment of each element within the measured height.
	 * This can be overridden on the individual element with [HorizontalLayoutData.verticalAlign]
	 */
	var verticalAlign by prop(VAlign.MIDDLE)

	/**
	 * If false, the icon will be on the right instead of left.
	 */
	var iconOnLeft by prop(true)

	companion object : StyleType<IconButtonLayoutStyle>

}

inline fun Owned.basicIconButtonSkin(texture: ButtonSkin, init: ComponentInit<BasicIconButtonSkin> = {}): BasicIconButtonSkin {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return BasicIconButtonSkin(this, texture).apply(init)
}

@Deprecated("DSL changed")
fun Owned.basicIconButtonSkin(texture: ButtonSkin,

							  /**
							   * The padding around the text and icon.
							   */
							  padding: PadRo = Pad(4f),

							  /**
							   * The horizontal gap between the icon and the textfield.
							   */
							  hGap: Float = 4f,

							  /**
							   * The vertical alignment between the icon and the label.
							   */
							  vAlign: VAlign = VAlign.MIDDLE,

							  /**
							   * If false, the icon will be on the right instead of left.
							   */
							  iconOnLeft: Boolean = true

): IconButtonSkin = BasicIconButtonSkin(this, texture).apply {
	layoutStyle.padding = padding
	layoutStyle.gap = hGap
	layoutStyle.verticalAlign = vAlign
	layoutStyle.iconOnLeft = iconOnLeft
}

inline fun Owned.basicIconButtonSkin(theme: Theme, init: ComponentInit<BasicIconButtonSkin> = {}): IconButtonSkin {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return BasicIconButtonSkin(this, basicButtonSkin(theme)).apply {
		layoutStyle.apply {
			padding = theme.buttonPad
			gap = theme.iconButtonGap
			verticalAlign = VAlign.MIDDLE
		}
		init()
	}
}