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

package com.acornui.skins

import com.acornui.async.launch
import com.acornui.component.*
import com.acornui.component.datagrid.DataGrid
import com.acornui.component.datagrid.DataGridGroupHeader
import com.acornui.component.datagrid.DataGridGroupHeaderStyle
import com.acornui.component.datagrid.DataGridStyle
import com.acornui.component.layout.*
import com.acornui.component.layout.algorithm.*
import com.acornui.component.layout.algorithm.virtual.VirtualHorizontalLayoutStyle
import com.acornui.component.layout.algorithm.virtual.VirtualVerticalLayoutStyle
import com.acornui.component.scroll.*
import com.acornui.component.style.*
import com.acornui.component.text.*
import com.acornui.core.AppConfig
import com.acornui.core.asset.cachedGroup
import com.acornui.core.di.*
import com.acornui.core.focus.FocusManager
import com.acornui.core.focus.SimpleHighlight
import com.acornui.core.input.interaction.ContextMenuStyle
import com.acornui.core.input.interaction.ContextMenuView
import com.acornui.core.input.interaction.enableDownRepeat
import com.acornui.core.popup.PopUpManager
import com.acornui.core.userInfo
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.color
import com.acornui.math.*
import com.acornui.serialization.*

open class BasicUiSkin(
		val target: UiComponent
) : Scoped {

	final override val injector = target.injector

	protected val theme = inject(Theme)

	open fun apply() {
		target.styleRules.clear()
		theme.apply {
			bgColor = inject(AppConfig).window.backgroundColor
			evenRowBgColor = bgColor + Color(0x03030300)
			oddRowBgColor = bgColor - Color(0x03030300)
		}
		initTheme()

		target.populateButtonStyle(Button) { labelButtonSkin(theme, it) }
		target.populateButtonStyle(Checkbox) { checkboxSkin(theme, it) }
		target.populateButtonStyle(CollapseButton) { collapseButtonSkin(theme, it) }
		target.populateButtonStyle(RadioButton) { radioButtonSkin(theme, it) }
		target.populateButtonStyle(Checkbox.NO_LABEL) { checkboxNoLabelSkin(theme, it) }
		target.populateButtonStyle(IconButton) { iconButtonSkin(theme, it) }

		popUpStyle()
		focusStyle()
		textStyle()
		panelStyle()
		windowPanelStyle()
		headingGroupStyle()
		themeRectStyle()
		tabNavigatorStyle()
		dividerStyle()
		numericStepperStyle()
		scrollAreaStyle()
		scrollBarStyle()
		progressBarStyle()
		sliderStyle()
		colorPickerStyle()
		dataScrollerStyle()
		optionListStyle()
		dataGridStyle()
		rowsStyle()
		formStyle()
		treeStyle()
		contextMenuStyle()
		calendarStyle()
		target.invalidateStyles()
	}

	open fun initTheme() {
	}

	protected open fun popUpStyle() {
		val modalStyle = BoxStyle()
		modalStyle.backgroundColor = Color(0f, 0f, 0f, 0.7f)
		target.addStyleRule(modalStyle, PopUpManager.MODAL_STYLE)
	}

	protected open fun focusStyle() {
		val focusManager = inject(FocusManager)
		val focusHighlight = SimpleHighlight(target, theme.atlasPath, "FocusRect")
		focusHighlight.colorTint = theme.strokeToggled
		focusManager.setHighlightIndicator(focusHighlight)
	}

	protected open fun textStyle() {
		target.addStyleRule(charStyle { colorTint = theme.textColor })
		target.addStyleRule(charStyle { colorTint = theme.headingColor }, withAnyAncestor(
				TextStyleTags.h1,
				TextStyleTags.h2,
				TextStyleTags.h3,
				TextStyleTags.h4
		))
		target.addStyleRule(charStyle { colorTint = theme.formLabelColor }, withAncestor(formLabelStyle))

		target.addStyleRule(charStyle { selectable = true }, withAncestor(TextInput) or withAncestor(TextArea))
		loadBitmapFonts()

		val textInputBoxStyle = BoxStyle()
		textInputBoxStyle.apply {
			backgroundColor = theme.inputFill
			borderColors = BorderColors(theme.stroke)
			borderThicknesses = Pad(theme.strokeThickness)
			padding = Pad(theme.strokeThickness + 2f)
		}
		target.addStyleRule(textInputBoxStyle, TextInput)
		target.addStyleRule(textInputBoxStyle, TextArea)

		val textInputFlowStyle = TextFlowStyle()
		textInputFlowStyle.multiline = false
		target.addStyleRule(textInputFlowStyle, withAncestor(TextInput))

		val textAreaStyle = TextFlowStyle()
		textAreaStyle.multiline = true
		target.addStyleRule(textAreaStyle, withAncestor(TextArea))

		val errorMessageStyle = CharStyle()
		errorMessageStyle.colorTint = theme.errorColor
		target.addStyleRule(errorMessageStyle, withAncestor(TextStyleTags.error))

		val warningMessageStyle = CharStyle()
		warningMessageStyle.colorTint = theme.warningColor
		target.addStyleRule(warningMessageStyle, withAncestor(TextStyleTags.warning))

		val infoMessageStyle = CharStyle()
		infoMessageStyle.colorTint = theme.infoColor
		target.addStyleRule(infoMessageStyle, withAncestor(TextStyleTags.info))
	}

	protected open fun loadBitmapFonts() {
		val group = cachedGroup()
		launch {
			loadFontFromAtlas("assets/uiskin/verdana_14.fnt", theme.atlasPath, group)
			loadFontFromAtlas("assets/uiskin/verdana_14_bold.fnt", theme.atlasPath, group)
			loadFontFromAtlas("assets/uiskin/verdana_14_italic.fnt", theme.atlasPath, group)
			loadFontFromAtlas("assets/uiskin/verdana_14_bold_italic.fnt", theme.atlasPath, group)

			target.addStyleRule(charStyle { fontKey = "assets/uiskin/verdana_14.fnt" })
			target.addStyleRule(charStyle { fontKey = "assets/uiskin/verdana_14_bold.fnt" }, withAnyAncestor(
					TextStyleTags.h1,
					TextStyleTags.h2,
					TextStyleTags.h3,
					TextStyleTags.h4,
					formLabelStyle
			))

			target.addStyleRule(charStyle { fontKey = "assets/uiskin/verdana_bold_14.fnt" }, withAncestor(TextStyleTags.strong))
			target.addStyleRule(charStyle { fontKey = "assets/uiskin/verdana_italic_14.fnt" }, withAncestor(TextStyleTags.emphasis))
			target.addStyleRule(charStyle { fontKey = "assets/uiskin/verdana_bold_italic_14.fnt" }, withAncestor(TextStyleTags.strong) and withAncestor(TextStyleTags.emphasis))
		}
	}

	protected open fun panelStyle() {
		val panelStyle = PanelStyle()
		panelStyle.background = {
			stack {
				+atlas(theme.atlasPath, "CurvedFill") {
					colorTint = theme.panelBgColor
				} layout { fill() }
				+atlas(theme.atlasPath, "CurvedStroke") {
					colorTint = theme.stroke
				} layout { fill() }
			}
		}
		target.addStyleRule(panelStyle, Panel)
	}

	protected open fun windowPanelStyle() {
		val windowPanelStyle = WindowPanelStyle().apply {
			background = {
				rect {
					style.apply {
						backgroundColor = theme.panelBgColor
						val borderRadius = Corners(theme.borderRadius)
						borderRadius.topLeft.clear()
						borderRadius.topRight.clear()
						this.borderRadii = borderRadius
						val borderThickness = Pad(theme.strokeThickness)
						borderThickness.top = 0f
						this.borderThicknesses = borderThickness
						borderColors = BorderColors(theme.stroke)
					}
				}
			}
			titleBarBackground = {
				rect {
					style.apply {
						backgroundColor = theme.controlBarBgColor
						val borderRadius = Corners(theme.borderRadius)
						borderRadius.bottomLeft.clear()
						borderRadius.bottomRight.clear()
						this.borderRadii = borderRadius
						borderThicknesses = Pad(theme.strokeThickness)
						borderColors = BorderColors(theme.stroke)
					}
				}
			}
			closeButton = {
				button {
					label = "x"
				}
			}
		}
		target.addStyleRule(windowPanelStyle)
	}

	protected open fun headingGroupStyle() {
		val headingGroupStyle = HeadingGroupStyle()
		headingGroupStyle.background = {
			rect {
				style.backgroundColor = theme.panelBgColor
				style.borderThicknesses = Pad(theme.strokeThickness)
				style.borderColors = BorderColors(theme.stroke)
				style.borderRadii = Corners(theme.borderRadius)
			}
		}
		headingGroupStyle.headingPadding.bottom = 0f

		headingGroupStyle.heading = {
			text {
				styleTags.add(TextStyleTags.h1)
			}
		}

		target.addStyleRule(headingGroupStyle, HeadingGroup)
	}

	protected open fun themeRectStyle() {
		val themeRect = BoxStyle()
		themeRect.backgroundColor = theme.fill
		themeRect.borderColors = BorderColors(theme.stroke)
		themeRect.borderThicknesses = Pad(theme.strokeThickness)
		target.addStyleRule(themeRect, CommonStyleTags.themeRect)
	}

	protected open fun tabNavigatorStyle() {
		val tabNavStyle = TabNavigatorStyle()
		tabNavStyle.background = { rect { styleTags.add(CommonStyleTags.themeRect) } }
		target.addStyleRule(tabNavStyle, TabNavigator)

		target.populateButtonStyle(TabNavigator.DEFAULT_TAB_STYLE) { tabButtonSkin(theme, it) }
		target.populateButtonStyle(TabNavigator.DEFAULT_TAB_STYLE_FIRST) { tabButtonSkin(theme, it) }
		target.populateButtonStyle(TabNavigator.DEFAULT_TAB_STYLE_LAST) { tabButtonSkin(theme, it) }
	}

	protected open fun dividerStyle() {
		val hDividerStyle = DividerStyle()
		hDividerStyle.handle = { atlas(theme.atlasPath, "HDividerHandle") }
		hDividerStyle.divideBar = { atlas(theme.atlasPath, "HDividerBar") }
		target.addStyleRule(hDividerStyle, HDivider)

		val vDividerStyle = DividerStyle()
		vDividerStyle.handle = { atlas(theme.atlasPath, "VDividerHandle") }
		vDividerStyle.divideBar = { atlas(theme.atlasPath, "VDividerBar") }
		target.addStyleRule(vDividerStyle, VDivider)

		val ruleStyle = RuleStyle()
		ruleStyle.thickness = 2f
		ruleStyle.borderColors = BorderColors(Color(1f, 1f, 1f, 0.7f))
		ruleStyle.backgroundColor = theme.stroke
		target.addStyleRule(ruleStyle)

		val vRuleStyle = RuleStyle()
		vRuleStyle.borderThicknesses = Pad().set(right = 1f)
		target.addStyleRule(vRuleStyle, Rule.VERTICAL_STYLE)

		val hRuleStyle = RuleStyle()
		hRuleStyle.borderThicknesses = Pad().set(bottom = 1f)
		target.addStyleRule(hRuleStyle, Rule.HORIZONTAL_STYLE)
	}

	protected open fun numericStepperStyle() {
		val stepperPad = Pad(left = 4f, right = 4f, top = 4f, bottom = 4f)
		target.populateButtonStyle(NumericStepper.STEP_UP_STYLE) { iconButtonSkin(it, "UpArrowStepper", padding = stepperPad) }
		target.populateButtonStyle(NumericStepper.STEP_DOWN_STYLE) { iconButtonSkin(it, "DownArrowStepper", padding = stepperPad) }
	}

	protected open fun scrollAreaStyle() {
		// Scroll area (used in GL versions)
		val scrollAreaStyle = ScrollAreaStyle()
		scrollAreaStyle.corner = {
			rect {
				style.backgroundColor = theme.strokeDisabled
			}
		}
		target.addStyleRule(scrollAreaStyle, ScrollArea)
	}

	protected open fun scrollBarStyle() {
		// Note that this does not style native scroll bars.
		val size = if (userInfo.isTouchDevice) 16f else 10f

		val thumb: Owned.() -> UiComponent = {
			button {
				focusEnabled = false
				populateButtonStyle(style) {
					{
						rect {
							style.backgroundColor = Color(0f, 0f, 0f, 0.6f)
							minWidth(size)
							minHeight(size)
						}
					}
				}
			}
		}

		val track: Owned.() -> UiComponent = {
			rect {
				style.backgroundColor = Color(1f, 1f, 1f, 0.4f)
				enableDownRepeat()
			}
		}

		val vScrollBarStyle = ScrollBarStyle()
		vScrollBarStyle.decrementButton = { spacer(size, 0f) }
		vScrollBarStyle.incrementButton = { spacer(size, 0f) }
		vScrollBarStyle.thumb = thumb
		vScrollBarStyle.track = track
		vScrollBarStyle.inactiveAlpha = 0.2f
		target.addStyleRule(vScrollBarStyle, VScrollBar)

		val hScrollBarStyle = ScrollBarStyle()
		hScrollBarStyle.decrementButton = { spacer(0f, size) }
		hScrollBarStyle.incrementButton = { spacer(0f, size) }
		hScrollBarStyle.thumb = thumb
		hScrollBarStyle.track = track
		hScrollBarStyle.inactiveAlpha = 0.2f
		target.addStyleRule(hScrollBarStyle, HScrollBar)
	}

	private fun progressBarStyle() {
		val s = ProgressBarRectStyle()
		s.borderColors = BorderColors(theme.stroke)
		s.borderRadii = Corners(0f)
		s.borderThicknesses = Pad(theme.strokeThickness)
		s.fillColor = theme.fill
		s.bgColor = Color(0f, 0f, 0f, 0.2f)
		target.addStyleRule(s, ProgressBarRect)
	}

	protected open fun sliderStyle() {
		// Scroll bars (usually only used in GL versions)

		val vSliderStyle = ScrollBarStyle()
		vSliderStyle.defaultSize = 200f
		vSliderStyle.inactiveAlpha = 1f
		vSliderStyle.decrementButton = { spacer() }
		vSliderStyle.incrementButton = { spacer() }
		vSliderStyle.thumb = {
			atlas(theme.atlasPath, "SliderArrowRightLarge") {
				layoutData = basicLayoutData {}
			}
		}
		vSliderStyle.track = {
			rect {
				style.apply {
					backgroundColor = theme.fillShine
					borderThicknesses = Pad(top = 0f, right = 0f, bottom = 0f, left = 4f)
					borderColors = BorderColors(Color(0f, 0f, 0f, 0.4f))
				}
				enableDownRepeat()
				layoutData = basicLayoutData {
					width = 13f
					heightPercent = 1f
				}
			}
		}
		vSliderStyle.pageMode = false
		target.addStyleRule(vSliderStyle, VSlider)

		val hSliderStyle = ScrollBarStyle()
		hSliderStyle.defaultSize = 200f
		hSliderStyle.inactiveAlpha = 1f
		hSliderStyle.decrementButton = { spacer() }
		hSliderStyle.incrementButton = { spacer() }
		hSliderStyle.thumb = {
			atlas(theme.atlasPath, "SliderArrowDownLarge") {
				layoutData = basicLayoutData {}
			}
		}
		hSliderStyle.track = {
			rect {
				style.apply {
					backgroundColor = theme.fillShine
					borderThicknesses = Pad(top = 0f, right = 0f, bottom = 4f, left = 0f)
					borderColors = BorderColors(Color(0f, 0f, 0f, 0.4f))
				}
				enableDownRepeat()
				layoutData = basicLayoutData {
					height = 13f
					widthPercent = 1f
				}
			}
		}
		hSliderStyle.pageMode = false
		target.addStyleRule(hSliderStyle, HSlider)
	}

	protected open fun colorPickerStyle() {
		val colorPaletteStyle = ColorPaletteStyle()
		colorPaletteStyle.apply {
			background = {
				rect {
					styleTags.add(CommonStyleTags.themeRect)
					style.borderRadii = Corners(theme.borderRadius)
				}
			}
			hueSaturationIndicator = {
				atlas(theme.atlasPath, "Picker")
			}
			valueIndicator = {
				atlas(theme.atlasPath, "SliderArrowRight")
			}
		}
		target.addStyleRule(colorPaletteStyle, ColorPalette)

		val colorPickerStyle = ColorPickerStyle()
		colorPickerStyle.apply {
			background = {
				button { focusEnabled = false }
			}
		}
		target.addStyleRule(colorPickerStyle, ColorPicker)

		val colorSwatchStyle = BoxStyle()
		colorSwatchStyle.borderRadii = Corners(theme.borderRadius)
		target.addStyleRule(colorSwatchStyle, ColorPicker.COLOR_SWATCH_STYLE)
	}

	protected open fun dataScrollerStyle() {
		val dataScrollerStyle = DataScrollerStyle()
		dataScrollerStyle.padding = Pad(theme.strokeThickness)
		dataScrollerStyle.background = {
			rect {
				style.apply {
					backgroundColor = theme.panelBgColor
					borderThicknesses = Pad(theme.strokeThickness)
					borderColors = BorderColors(theme.stroke)
				}
			}
		}
		target.addStyleRule(dataScrollerStyle, DataScroller)

		val verticalLayoutStyle = VirtualVerticalLayoutStyle()
		verticalLayoutStyle.padding = Pad(top = 0f, right = 5f, bottom = 0f, left = 5f)
		target.addStyleRule(verticalLayoutStyle, withParent(DataScroller))

		val horizontalLayoutStyle = VirtualHorizontalLayoutStyle()
		horizontalLayoutStyle.padding = Pad(top = 5f, right = 0f, bottom = 5f, left = 0f)
		target.addStyleRule(horizontalLayoutStyle, withParent(DataScroller))
	}

	protected open fun optionListStyle() {
		val optionListStyle = OptionListStyle().apply {
			downArrow = {
				atlas(theme.atlasPath, "OptionListArrow")
			}
			padding = Pad(theme.strokeThickness, theme.strokeThickness + 2f, theme.strokeThickness, theme.strokeThickness)
			background = {
				rect {
					style.apply {
						backgroundColor = theme.inputFill
						borderThicknesses = Pad(theme.strokeThickness)
						borderRadii = Corners(0f)
						borderColors = BorderColors(theme.stroke)
					}
				}
			}
		}
		target.addStyleRule(optionListStyle, OptionList)

		val pad = Pad(top = 0f, right = theme.strokeThickness, bottom = theme.strokeThickness, left = theme.strokeThickness)
		val dataScrollerStyle = DataScrollerStyle().apply {
			padding = pad
			background = {
				rect {
					style.apply {
						backgroundColor = theme.panelBgColor
						borderThicknesses = pad
						borderRadii = Corners(0f, 0f, theme.borderRadius, theme.borderRadius)
						borderColors = BorderColors(theme.stroke)
					}
				}
			}
		}
		target.styleRules.add(StyleRule(dataScrollerStyle, withAncestor(OptionList)))

		val scrollRectStyle = ScrollRectStyle().apply {
			borderRadii = Corners(0f, 0f, 0f, theme.borderRadius - theme.strokeThickness)
		}
		target.styleRules.add(StyleRule(scrollRectStyle, withAncestor(OptionList)))

		val textInputBoxStyle = BoxStyle()
		textInputBoxStyle.apply {
			backgroundColor = Color.CLEAR
			borderThicknesses = Pad(0f)
		}
		target.addStyleRule(textInputBoxStyle, withAncestor(OptionList) and TextInput)
	}

	protected open fun dataGridStyle() {
		val dataGridStyle = DataGridStyle()
		dataGridStyle.background = {
			rect {
				style.apply {
					backgroundColor = theme.fill
					borderThicknesses = Pad(theme.strokeThickness)
					borderColors = BorderColors(theme.stroke)
					borderRadii = Corners(theme.borderRadius)
				}
			}
		}
		dataGridStyle.resizeHandleWidth = if (userInfo.isTouchDevice) 16f else 8f
		dataGridStyle.sortDownArrow = { atlas(theme.atlasPath, "DownArrow") }
		dataGridStyle.sortUpArrow = { atlas(theme.atlasPath, "UpArrow") }
		dataGridStyle.borderRadius = Corners(theme.borderRadius)
		dataGridStyle.borderThickness = Pad(theme.strokeThickness)

		val headerCellBackground = styleTag()
		target.populateButtonStyle(headerCellBackground) { buttonState ->
			{ buttonTexture(buttonState, Corners(0f), Pad(0f)) }
		}
		dataGridStyle.headerCellBackground = { button { styleTags.add(headerCellBackground) } }

		target.addStyleRule(dataGridStyle, DataGrid)

		val bodyCharStyle = CharStyle()
		bodyCharStyle.selectable = false
		target.addStyleRule(bodyCharStyle, withAncestor(TextField) andThen withAncestor(DataGrid.BODY_CELL))

		val headerFlowStyle = FlowLayoutStyle()
		headerFlowStyle.horizontalAlign = FlowHAlign.CENTER
		headerFlowStyle.multiline = false
		target.addStyleRule(headerFlowStyle, withAncestor(TextField) andThen withAncestor(DataGrid.HEADER_CELL))

		val groupHeaderCharStyle = CharStyle()
		groupHeaderCharStyle.selectable = false
		target.addStyleRule(groupHeaderCharStyle, withAncestor(TextField) andThen (withAncestor(DataGridGroupHeader) or withAncestor(DataGrid.HEADER_CELL)))

		val dataGridGroupHeaderStyle = DataGridGroupHeaderStyle()
		dataGridGroupHeaderStyle.collapseButton = { collapseButton { toggleOnClick = false } }
		dataGridGroupHeaderStyle.background = {
			rect {
				style.backgroundColor = theme.controlBarBgColor
				style.borderThicknesses = Pad(0f, 0f, 1f, 0f)
				style.borderColors = BorderColors(theme.stroke)
			}
		}
		target.addStyleRule(dataGridGroupHeaderStyle, DataGridGroupHeader)

		val dataGridGroupHeaderLayoutStyle = HorizontalLayoutStyle()
		dataGridGroupHeaderLayoutStyle.padding = Pad(6f)
		dataGridGroupHeaderLayoutStyle.gap = 2f
		dataGridGroupHeaderLayoutStyle.verticalAlign = VAlign.MIDDLE
		target.addStyleRule(dataGridGroupHeaderStyle, DataGridGroupHeader)

		val columnMoveIndicatorStyle = BoxStyle()
		columnMoveIndicatorStyle.backgroundColor = Color(0.5f, 0.5f, 0.5f, 0.5f)
		target.addStyleRule(columnMoveIndicatorStyle, DataGrid.COLUMN_MOVE_INDICATOR)

		val columnInsertionIndicatorStyle = RuleStyle()
		columnInsertionIndicatorStyle.thickness = 4f
		columnInsertionIndicatorStyle.backgroundColor = Color.DARK_GRAY
		target.addStyleRule(columnInsertionIndicatorStyle, DataGrid.COLUMN_INSERTION_INDICATOR)
	}

	protected open fun rowsStyle() {
		val rowBackgroundsStyle = RowBackgroundStyle()
		rowBackgroundsStyle.evenColor = theme.evenRowBgColor
		rowBackgroundsStyle.oddColor = theme.oddRowBgColor
		rowBackgroundsStyle.highlightedEvenColor = theme.highlightedEvenRowBgColor
		rowBackgroundsStyle.highlightedOddColor = theme.highlightedOddRowBgColor
		rowBackgroundsStyle.toggledEvenColor = theme.toggledEvenRowBgColor
		rowBackgroundsStyle.toggledOddColor = theme.toggledOddRowBgColor
		target.addStyleRule(rowBackgroundsStyle, RowBackground)
	}

	protected open fun formStyle() {
		val formStyle = GridLayoutStyle()
		formStyle.verticalAlign = VAlign.TOP
		target.addStyleRule(formStyle, FormContainer)
	}

	protected open fun treeStyle() {
		val itemRendererStyle = DefaultTreeItemRendererStyle()
		itemRendererStyle.openedFolderIcon = {
			atlas(theme.atlasPath, "folder-horizontal-open.png")
		}
		itemRendererStyle.closedFolderIcon = {
			atlas(theme.atlasPath, "folder-horizontal.png")
		}
		itemRendererStyle.leafIcon = {
			atlas(theme.atlasPath, "document.png")
		}
		target.addStyleRule(itemRendererStyle, DefaultTreeItemRenderer)
		val horizontalLayoutStyle = HorizontalLayoutStyle()
		horizontalLayoutStyle.verticalAlign = VAlign.MIDDLE
		target.addStyleRule(horizontalLayoutStyle, withParent(DefaultTreeItemRenderer))

		val charStyle = CharStyle()
		charStyle.selectable = false
		target.addStyleRule(charStyle, withParent(DefaultTreeItemRenderer))
	}

	protected open fun contextMenuStyle() {
		val contextMenuStyle = ContextMenuStyle()
		contextMenuStyle.rightArrow = {
			atlas(theme.atlasPath, "RightArrow")
		}
		target.addStyleRule(contextMenuStyle, ContextMenuView)
	}

	protected open fun calendarStyle() {
		val datePickerStyle = DatePickerStyle().apply {
			downArrow = {
				atlas(theme.atlasPath, "calendar")
			}
			padding = Pad(theme.strokeThickness, theme.strokeThickness + 2f, theme.strokeThickness, theme.strokeThickness)
			background = {
				rect {
					style.apply {
						backgroundColor = theme.inputFill
						borderThicknesses = Pad(theme.strokeThickness)
						borderRadii = Corners(0f)
						borderColors = BorderColors(theme.stroke)
					}
				}
			}
		}

		target.addStyleRule(datePickerStyle, DatePicker)

		val textInputBoxStyle = BoxStyle()
		textInputBoxStyle.apply {
			backgroundColor = Color.CLEAR
			borderThicknesses = Pad(0f)
		}
		target.addStyleRule(textInputBoxStyle, withAncestor(DatePicker) and TextInput)
	}

}

fun UiComponent.populateButtonStyle(tag: StyleTag, skinPartFactory: (ButtonState) -> Owned.() -> UiComponent) {
	val buttonStyle = ButtonStyle()
	populateButtonStyle(buttonStyle, skinPartFactory)
	addStyleRule(buttonStyle, tag)
}

fun populateButtonStyle(buttonStyle: ButtonStyle, skinPartFactory: (ButtonState) -> (Owned.() -> UiComponent)): ButtonStyle {
	buttonStyle.upState = skinPartFactory(ButtonState.UP)
	buttonStyle.overState = skinPartFactory(ButtonState.OVER)
	buttonStyle.downState = skinPartFactory(ButtonState.DOWN)
	buttonStyle.toggledUpState = skinPartFactory(ButtonState.TOGGLED_UP)
	buttonStyle.toggledOverState = skinPartFactory(ButtonState.TOGGLED_OVER)
	buttonStyle.toggledDownState = skinPartFactory(ButtonState.TOGGLED_DOWN)
	buttonStyle.disabledState = skinPartFactory(ButtonState.DISABLED)
	return buttonStyle
}

fun iconButtonSkin(buttonState: ButtonState, icon: String, padding: PadRo = Pad(5f), hGap: Float = 4f): Owned.() -> UiComponent = {
	val texture = buttonTexture(buttonState)
	val skinPart = IconButtonSkinPart(this, texture, padding, hGap)
	val theme = inject(Theme)
	skinPart.contentsAtlas(theme.atlasPath, icon)
	skinPart
}

fun labelButtonSkin(theme: Theme, buttonState: ButtonState): Owned.() -> UiComponent = {
	val texture = buttonTexture(buttonState)
	LabelButtonSkinPart(this, texture, theme.buttonPad)
}

fun tabButtonSkin(theme: Theme, buttonState: ButtonState): Owned.() -> UiComponent = {
	val texture = buttonTexture(buttonState, Corners(topLeft = theme.borderRadius, topRight = theme.borderRadius, bottomLeft = 0f, bottomRight = 0f), Pad(theme.strokeThickness), isTab = true)
	IconButtonSkinPart(this, texture, theme.buttonPad, theme.iconButtonGap)
}

/**
 * A convenience function to create a button skin part.
 */
fun iconButtonSkin(theme: Theme, buttonState: ButtonState): Owned.() -> UiComponent = {
	val texture = buttonTexture(buttonState)
	IconButtonSkinPart(this, texture, theme.buttonPad, theme.iconButtonGap)
}

fun checkboxNoLabelSkin(theme: Theme, buttonState: ButtonState): Owned.() -> CheckboxSkinPart = {
	val s = checkboxSkin(theme, buttonState)()
	val lD = s.createLayoutData()
	lD.widthPercent = 1f
	lD.heightPercent = 1f
	s.box.layoutData = lD
	s
}

/**
 * A checkbox skin part.
 */
fun checkboxSkin(theme: Theme, buttonState: ButtonState): Owned.() -> CheckboxSkinPart = {
	val box = buttonTexture(buttonState, borderRadius = Corners(), borderThickness = Pad(theme.strokeThickness))
	if (buttonState.toggled) {
		val checkMark = scaleBox {
			+atlas(theme.atlasPath, "CheckMark") layout {
				horizontalAlign = HAlign.CENTER
				verticalAlign = VAlign.MIDDLE
			}
			layoutData = box.createLayoutData().apply {
				widthPercent = 1f
				heightPercent = 1f
			}
		}
		box.addElement(checkMark)
	}
	CheckboxSkinPart(
			this,
			box
	).apply {
		box layout {
			width = 18f
			height = 18f
		}
	}
}

/**
 * A checkbox skin part.
 */
fun collapseButtonSkin(theme: Theme, buttonState: ButtonState): Owned.() -> CheckboxSkinPart = {
	val box = atlas(theme.atlasPath, if (buttonState.toggled) "CollapseSelected" else "CollapseUnselected")
	CheckboxSkinPart(
			this,
			box
	)
}

/**
 * A convenience function to create a radio button skin part.
 */
fun radioButtonSkin(theme: Theme, buttonState: ButtonState): Owned.() -> CheckboxSkinPart = {
	val radio = buttonTexture(buttonState, borderRadius = Corners(1000f), borderThickness = Pad(theme.strokeThickness))
	if (buttonState.toggled) {
		val filledCircle = rect {
			style.margin = Pad(4f)
			style.borderRadii = Corners(1000f)
			style.backgroundColor = Color.DARK_GRAY.copy()
			layoutData = radio.createLayoutData().apply {
				fill()
			}
		}
		radio.addElement(filledCircle)
	}

	CheckboxSkinPart(
			this,
			radio
	).apply {
		radio layout {
			width = 18f
			height = 18f
		}
	}
}

fun Owned.buttonTexture(buttonState: ButtonState) = stack {
	val theme = inject(Theme)
	val fillRegion = when (buttonState) {
		ButtonState.TOGGLED_UP, ButtonState.UP -> "Button_up"
		ButtonState.TOGGLED_OVER, ButtonState.OVER -> "Button_over"
		ButtonState.TOGGLED_DOWN, ButtonState.DOWN -> "Button_down"
		ButtonState.DISABLED -> "Button_disabled"
	}
	+atlas(theme.atlasPath, fillRegion) {
		colorTint = when (buttonState) {
			ButtonState.DISABLED -> theme.fillDisabled
			ButtonState.UP, ButtonState.TOGGLED_UP -> theme.fill
			ButtonState.OVER, ButtonState.TOGGLED_OVER -> theme.fillHighlight
			ButtonState.DOWN, ButtonState.TOGGLED_DOWN -> theme.fill
		}

	} layout { fill() }
	+atlas(theme.atlasPath, "CurvedStroke") {
		colorTint = if (buttonState == ButtonState.DISABLED) theme.strokeDisabled else if (buttonState.toggled) theme.strokeToggled else theme.stroke
	} layout { fill() }
}

fun Owned.buttonTexture(buttonState: ButtonState, borderRadius: CornersRo, borderThickness: PadRo, isTab: Boolean = false): CanvasLayoutContainer = canvas {
	val theme = inject(Theme)
	+rect {
		style.apply {
			backgroundColor = getButtonFillColor(buttonState)
			borderColors = BorderColors(getButtonStrokeColor(buttonState))
			val bT = borderThickness.copy()
			if (isTab && buttonState.toggled) {
				bT.bottom = 0f
			}
			this.borderThicknesses = bT
			this.borderRadii = borderRadius
		}
	} layout { widthPercent = 1f; heightPercent = 1f }
	when (buttonState) {
		ButtonState.UP,
		ButtonState.OVER,
		ButtonState.TOGGLED_UP,
		ButtonState.TOGGLED_OVER -> {
			+rect {
				style.apply {
					margin = Pad(top = borderThickness.top, right = borderThickness.right, bottom = 0f, left = borderThickness.left)
					backgroundColor = theme.fillShine
					this.borderRadii = Corners(
							topLeft = Vector2(borderRadius.topLeft.x - borderThickness.left, borderRadius.topLeft.y - borderThickness.top),
							topRight = Vector2(borderRadius.topRight.x - borderThickness.right, borderRadius.topRight.y - borderThickness.top),
							bottomLeft = Vector2(), bottomRight = Vector2()
					)
				}
			} layout {
				widthPercent = 1f
				heightPercent = 0.5f
			}
		}
		ButtonState.DISABLED -> {
		}
		else -> {
			+rect {
				style.apply {
					margin = Pad(top = 0f, right = borderThickness.right, bottom = borderThickness.bottom, left = borderThickness.left)
					backgroundColor = theme.fillShine
					this.borderRadii = Corners(
							topLeft = Vector2(), topRight = Vector2(),
							bottomLeft = Vector2(borderRadius.bottomLeft.x - borderThickness.left, borderRadius.bottomLeft.y - borderThickness.bottom),
							bottomRight = Vector2(borderRadius.bottomRight.x - borderThickness.right, borderRadius.bottomRight.y - borderThickness.bottom)
					)
				}
			} layout {
				widthPercent = 1f
				verticalCenter = 0f
				bottom = 0f
			}
		}
	}
}


/**
 * A typical implementation of a skin part for a labelable button state.
 */
open class CheckboxSkinPart(
		owner: Owned,
		val box: UiComponent
) : HorizontalLayoutContainer(owner), Labelable {

	val textField: TextField

	init {
		style.verticalAlign = VAlign.MIDDLE
		+box
		textField = +text("") {
			selectable = false
			includeInLayout = false
		} layout {
			widthPercent = 1f
		}
	}

	override var label: String
		get() = textField.label
		set(value) {
			textField.includeInLayout = value.isNotEmpty()
			textField.text = value
		}
}

/**
 * A typical implementation of a skin part for a labelable button state.
 */
open class LabelButtonSkinPart(
		owner: Owned,
		val texture: UiComponent,
		val padding: PadRo
) : ElementContainerImpl<UiComponent>(owner), Labelable {

	val textField: TextField = text()

	init {
		+texture
		+textField

		textField.selectable = false
		textField.flowStyle.horizontalAlign = FlowHAlign.CENTER
	}

	override var label: String
		get() = textField.label
		set(value) {
			textField.label = value
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val textWidth = padding.reduceWidth(explicitWidth)
		textField.setSize(textWidth, null)
		val w = explicitWidth ?: maxOf(minWidth ?: 0f, padding.expandWidth2(textField.width))
		var h = maxOf(minHeight ?: 0f, padding.expandHeight2(textField.height))
		if (explicitHeight != null && explicitHeight > h) h = explicitHeight
		texture.setSize(w, h)
		textField.moveTo((padding.reduceWidth2(w) - textField.width) * 0.5f + padding.left, (padding.reduceHeight2(h) - textField.height) * 0.5f + padding.top)
		out.set(texture.bounds)
	}
}

fun Owned.getButtonFillColor(buttonState: ButtonState): ColorRo {
	val theme = inject(Theme)
	return when (buttonState) {
		ButtonState.UP,
		ButtonState.DOWN,
		ButtonState.TOGGLED_UP,
		ButtonState.TOGGLED_DOWN -> theme.fill

		ButtonState.OVER,
		ButtonState.TOGGLED_OVER -> theme.fillHighlight

		ButtonState.DISABLED -> theme.fillDisabled
	}
}

fun Owned.getButtonStrokeColor(buttonState: ButtonState): ColorRo {
	val theme = inject(Theme)
	return when (buttonState) {

		ButtonState.UP,
		ButtonState.DOWN -> theme.stroke

		ButtonState.OVER -> theme.strokeHighlight

		ButtonState.TOGGLED_UP,
		ButtonState.TOGGLED_DOWN -> theme.strokeToggled

		ButtonState.TOGGLED_OVER -> theme.strokeToggledHighlight

		ButtonState.DISABLED -> theme.strokeDisabled
	}
}

/**
 * The Theme is a set of common styling properties, used to build a skin.
 */
class Theme {

	/**
	 * This will be set to AppConfig.window.backgroundColor
	 */
	var bgColor: ColorRo = Color(0xF1F2F3FF)
	var panelBgColor: ColorRo = Color(0xE7EDF1FF)

	private val brighten: ColorRo = Color(0x15151500)

	var fill: ColorRo = Color(0xF3F9FAFF)
	var fillHighlight: ColorRo = fill + brighten
	var fillDisabled: ColorRo = Color(0xCCCCCCFF)
	var fillShine: ColorRo = Color(1f, 1f, 1f, 0.9f)
	var inputFill: ColorRo = Color(0.97f, 0.97f, 0.97f, 1f)

	var stroke: ColorRo = Color(0x888888FF)
	var strokeThickness = 1f
	var strokeHighlight: ColorRo = stroke + brighten
	var strokeDisabled: ColorRo = Color(0x999999FF)

	var strokeToggled: ColorRo = Color(0x0235ACFF)
	var strokeToggledHighlight: ColorRo = strokeToggled + brighten

	var borderRadius = 8f

	var textColor: ColorRo = Color(0x333333FF)
	var headingColor: ColorRo = Color(0x333333FF)
	var formLabelColor: ColorRo = Color(0x555555FF)

	var errorColor: ColorRo = Color(0xcc3333FF)
	var warningColor: ColorRo = Color(0xff9933FF)
	var infoColor: ColorRo = Color(0x339933FF)

	var controlBarBgColor: ColorRo = Color(0xDAE5F0FF)

	var evenRowBgColor: ColorRo = bgColor + Color(0x03030300)
	var oddRowBgColor: ColorRo = bgColor - Color(0x03030300)

	var highlightedEvenRowBgColor: ColorRo = Color(0xFEFFD2FF)
	var highlightedOddRowBgColor: ColorRo = Color(0xFEFFD2FF)

	var toggledEvenRowBgColor: ColorRo = Color(0xFCFD7CFF)
	var toggledOddRowBgColor: ColorRo = Color(0xFCFD7CFF)

	var buttonPad: PadRo = Pad(4f)
	var iconButtonGap = 2f

	var atlasPath = "assets/uiskin/uiskin.json"

	fun set(other: Theme) {
		bgColor = other.bgColor
		panelBgColor = other.panelBgColor

		fill = other.fill
		fillHighlight = other.fillHighlight
		fillDisabled = other.fillDisabled
		fillShine = other.fillShine
		inputFill = other.inputFill

		stroke = other.stroke
		strokeThickness = other.strokeThickness
		strokeHighlight = other.strokeHighlight
		strokeDisabled = other.strokeDisabled

		strokeToggled = other.strokeToggled
		strokeToggledHighlight = other.strokeToggledHighlight

		borderRadius = other.borderRadius

		textColor = other.textColor
		headingColor = other.headingColor
		formLabelColor = other.formLabelColor

		errorColor = other.errorColor
		warningColor = other.warningColor
		infoColor = other.infoColor

		controlBarBgColor = other.controlBarBgColor

		evenRowBgColor = other.evenRowBgColor
		oddRowBgColor = other.oddRowBgColor

		highlightedEvenRowBgColor = other.highlightedEvenRowBgColor
		highlightedOddRowBgColor = other.highlightedOddRowBgColor

		toggledEvenRowBgColor = other.toggledEvenRowBgColor
		toggledOddRowBgColor = other.toggledOddRowBgColor

		buttonPad = other.buttonPad
		iconButtonGap = other.iconButtonGap

		atlasPath = other.atlasPath
	}

	companion object : DKey<Theme> {
		override fun factory(injector: Injector) = Theme()
	}
}

object ThemeSerializer : To<Theme>, From<Theme> {

	override fun read(reader: Reader): Theme {
		val o = Theme()
		o.atlasPath = reader.string("atlasPath")!!
		o.bgColor = reader.color("bgColor")!!
		o.borderRadius = reader.float("borderRadius")!!
		o.buttonPad = reader.obj("buttonPad", PadSerializer)!!
		o.iconButtonGap = reader.float("iconButtonGap")!!
		o.controlBarBgColor = reader.color("controlBarBgColor")!!
		o.evenRowBgColor = reader.color("evenRowBgColor")!!
		o.fill = reader.color("fill")!!
		o.fillDisabled = reader.color("fillDisabled")!!
		o.fillHighlight = reader.color("fillHighlight")!!
		o.fillShine = reader.color("fillShine")!!
		o.formLabelColor = reader.color("formLabelColor")!!
		o.errorColor = reader.color("errorColor")!!
		o.warningColor = reader.color("warningColor")!!
		o.infoColor = reader.color("infoColor")!!
		o.headingColor = reader.color("headingColor")!!
		o.highlightedEvenRowBgColor = reader.color("highlightedEvenRowBgColor")!!
		o.highlightedOddRowBgColor = reader.color("highlightedOddRowBgColor")!!
		o.inputFill = reader.color("inputFill")!!
		o.oddRowBgColor = reader.color("oddRowBgColor")!!
		o.panelBgColor = reader.color("panelBgColor")!!
		o.stroke = reader.color("stroke")!!
		o.strokeDisabled = reader.color("strokeDisabled")!!
		o.strokeHighlight = reader.color("strokeHighlight")!!
		o.strokeThickness = reader.float("strokeThickness")!!
		o.strokeToggled = reader.color("strokeToggled")!!
		o.strokeToggledHighlight = reader.color("strokeToggledHighlight")!!
		o.textColor = reader.color("textColor")!!
		o.toggledEvenRowBgColor = reader.color("toggledEvenRowBgColor")!!
		o.toggledOddRowBgColor = reader.color("toggledOddRowBgColor")!!
		return o
	}

	override fun Theme.write(writer: Writer) {
		writer.string("atlasPath", atlasPath)
		writer.color("bgColor", bgColor)
		writer.float("borderRadius", borderRadius)
		writer.obj("buttonPad", buttonPad, PadSerializer)
		writer.float("iconButtonGap", iconButtonGap)
		writer.color("controlBarBgColor", controlBarBgColor)
		writer.color("evenRowBgColor", evenRowBgColor)
		writer.color("fill", fill)
		writer.color("fillDisabled", fillDisabled)
		writer.color("fillHighlight", fillHighlight)
		writer.color("fillShine", fillShine)
		writer.color("formLabelColor", formLabelColor)
		writer.color("errorColor", errorColor)
		writer.color("warningColor", warningColor)
		writer.color("infoColor", infoColor)
		writer.color("headingColor", headingColor)
		writer.color("highlightedEvenRowBgColor", highlightedEvenRowBgColor)
		writer.color("highlightedOddRowBgColor", highlightedOddRowBgColor)
		writer.color("inputFill", inputFill)
		writer.color("oddRowBgColor", oddRowBgColor)
		writer.color("panelBgColor", panelBgColor)
		writer.color("stroke", stroke)
		writer.color("strokeDisabled", strokeDisabled)
		writer.color("strokeHighlight", strokeHighlight)
		writer.float("strokeThickness", strokeThickness)
		writer.color("strokeToggled", strokeToggled)
		writer.color("strokeToggledHighlight", strokeToggledHighlight)
		writer.color("textColor", textColor)
		writer.color("toggledEvenRowBgColor", toggledEvenRowBgColor)
		writer.color("toggledOddRowBgColor", toggledOddRowBgColor)
	}
}