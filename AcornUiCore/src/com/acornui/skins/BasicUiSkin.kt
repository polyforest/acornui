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
import com.acornui.core.assets.cachedGroup
import com.acornui.core.di.*
import com.acornui.core.focus.FocusManager
import com.acornui.core.focus.SimpleHighlight
import com.acornui.core.graphics.atlas
import com.acornui.core.graphics.contentsAtlas
import com.acornui.core.input.interaction.ContextMenuStyle
import com.acornui.core.input.interaction.ContextMenuView
import com.acornui.core.input.interaction.enableDownRepeat
import com.acornui.core.popup.PopUpManager
import com.acornui.core.userInfo
import com.acornui.component.ScrollRectStyle
import com.acornui.component.text.loadFontFromAtlas
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.*

open class BasicUiSkin(
		val target: UiComponent
) : Scoped {

	final override val injector = target.injector

	protected val theme = inject(Theme)

	open fun apply() {
		target.styleRules.clear()
		initTheme()

		target.populateButtonStyle(Button, { labelButtonSkin(theme, it) })
		target.populateButtonStyle(Checkbox, { checkboxSkin(theme, it) })
		target.populateButtonStyle(CollapseButton, { collapseButtonSkin(theme, it) })
		target.populateButtonStyle(RadioButton, { radioButtonSkin(theme, it) })
		target.populateButtonStyle(StyleSelectors.cbNoLabelStyle, { checkboxNoLabelSkin(theme, it) })
		target.populateButtonStyle(IconButton, { iconButtonSkin(theme, it) })

		stageStyle()
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
		optionsListStyle()
		dataGridStyle()
		rowsStyle()
		formStyle()
		treeStyle()
		contextMenuStyle()
		target.invalidateStyles()
	}

	open fun initTheme() {
	}

	protected open fun stageStyle() {
		val stageStyle = StageStyle()
		stageStyle.backgroundColor = theme.bgColor
		target.addStyleRule(stageStyle)
	}

	protected open fun popUpStyle() {
		val modalStyle = BoxStyle()
		modalStyle.backgroundColor = Color(0f, 0f, 0f, 0.7f)
		target.addStyleRule(modalStyle, PopUpManager.MODAL_STYLE)
	}

	protected open fun focusStyle() {
		val focusManager = inject(FocusManager)
		focusManager.highlight = null
		focusManager.highlight?.dispose()
		val focusHighlight = SimpleHighlight(target, theme.atlasPath, "FocusRect")
		focusHighlight.colorTint = theme.strokeToggled
		focusManager.highlight = focusHighlight
	}

	protected open fun textStyle() {
		target.addStyleRule(theme.textStyle)
		target.addStyleRule(theme.headingStyle, withAncestor(StyleSelectors.headingStyle))
		target.addStyleRule(theme.formLabelStyle, withAncestor(formLabelStyle))
		target.addStyleRule(charStyle { selectable = true }, withAncestor(TextInput) or withAncestor(TextArea))

		if (userInfo.isOpenGl)
			loadBitmapFonts()

		val textInputBoxStyle = BoxStyle()
		textInputBoxStyle.apply {
			backgroundColor = theme.inputFill
			borderColor = BorderColors(theme.stroke)
			borderThickness = Pad(theme.strokeThickness)
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
		errorMessageStyle.colorTint = Color.RED
		target.addStyleRule(errorMessageStyle, StyleSelectors.errorMessage)
	}

	protected open fun Scoped.loadBitmapFonts() {
		val group = cachedGroup()
		val fontStyle = theme.textStyle.toFontStyle()
		loadFontFromAtlas(fontStyle, "assets/uiskin/verdana_14.fnt", theme.atlasPath, group)
		loadFontFromAtlas(fontStyle.copy(bold = true), "assets/uiskin/verdana_14_bold.fnt", theme.atlasPath, group)
		loadFontFromAtlas(fontStyle.copy(italic = true), "assets/uiskin/verdana_14_italic.fnt", theme.atlasPath, group)
		loadFontFromAtlas(fontStyle.copy(bold = true, italic = true), "assets/uiskin/verdana_14_bold_italic.fnt", theme.atlasPath, group)
		loadFontFromAtlas(theme.headingStyle.toFontStyle(), "assets/uiskin/verdana_14_bold.fnt", theme.atlasPath, group)
	}

	protected open fun panelStyle() {
		val panelStyle = PanelStyle()
		panelStyle.background = {
			rect {
				style.apply {
					backgroundColor = theme.panelBgColor
					borderThickness = Pad(theme.strokeThickness)
					borderRadius = Corners(theme.borderRadius)
					borderColor = BorderColors(theme.stroke)
				}
			}
		}
		target.addStyleRule(panelStyle, Panel)
	}

	protected open fun windowPanelStyle() {
		val windowPanelStyle = WindowPanelStyle()
		windowPanelStyle.apply {
			background = {
				rect {
					style.apply {
						backgroundColor = theme.panelBgColor
						val borderRadius = Corners(theme.borderRadius)
						borderRadius.topLeft.clear()
						borderRadius.topRight.clear()
						this.borderRadius = borderRadius
						val borderThickness = Pad(theme.strokeThickness)
						borderThickness.top = 0f
						this.borderThickness = borderThickness
						borderColor = BorderColors(theme.stroke)
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
						this.borderRadius = borderRadius
						borderThickness = Pad(theme.strokeThickness)
						borderColor = BorderColors(theme.stroke)
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
				style.borderThickness = Pad(theme.strokeThickness)
				style.borderColor = BorderColors(theme.stroke)
				style.borderRadius = Corners(theme.borderRadius)
			}
		}
		headingGroupStyle.headingPadding.bottom = 0f

		headingGroupStyle.heading = {
			text {
				styleTags.add(StyleSelectors.headingStyle)
			}
		}

		target.addStyleRule(headingGroupStyle, HeadingGroup)
	}

	protected open fun themeRectStyle() {
		val themeRect = BoxStyle()
		themeRect.backgroundColor = theme.fill
		themeRect.borderColor = BorderColors(theme.stroke)
		themeRect.borderThickness = Pad(theme.strokeThickness)
		target.addStyleRule(themeRect, StyleSelectors.themeRect)
	}

	protected open fun tabNavigatorStyle() {
		val tabNavStyle = TabNavigatorStyle()
		tabNavStyle.background = { rect { styleTags.add(StyleSelectors.themeRect) } }
		target.addStyleRule(tabNavStyle, TabNavigator)

		target.populateButtonStyle(TabNavigator.DEFAULT_TAB_STYLE, { tabButtonSkin(theme, it) })
		target.populateButtonStyle(TabNavigator.DEFAULT_TAB_STYLE_FIRST, { tabButtonSkin(theme, it) })
		target.populateButtonStyle(TabNavigator.DEFAULT_TAB_STYLE_LAST, { tabButtonSkin(theme, it) })
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
		ruleStyle.borderColor = BorderColors(Color(1f, 1f, 1f, 0.7f))
		ruleStyle.backgroundColor = theme.stroke
		target.addStyleRule(ruleStyle)

		val vRuleStyle = RuleStyle()
		vRuleStyle.borderThickness = Pad().set(right = 1f)
		target.addStyleRule(vRuleStyle, Rule.VERTICAL_STYLE)

		val hRuleStyle = RuleStyle()
		hRuleStyle.borderThickness = Pad().set(bottom = 1f)
		target.addStyleRule(hRuleStyle, Rule.HORIZONTAL_STYLE)
	}

	protected open fun numericStepperStyle() {
		val stepperPad = Pad(left = 4f, right = 4f, top = 4f, bottom = 4f)
		target.populateButtonStyle(NumericStepper.STEP_UP_STYLE, { iconButtonSkin(it, "UpArrowStepper", Corners(topLeft = 0f, topRight = theme.borderRadius, bottomLeft = 0f, bottomRight = 0f), borderThickness = Pad(theme.strokeThickness), padding = stepperPad) })
		target.populateButtonStyle(NumericStepper.STEP_DOWN_STYLE, { iconButtonSkin(it, "DownArrowStepper", Corners(topLeft = 0f, topRight = 0f, bottomLeft = 0f, bottomRight = theme.borderRadius), borderThickness = Pad(theme.strokeThickness), padding = stepperPad) })
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
				populateButtonStyle(style, {
					{
						rect {
							style.backgroundColor = Color(0f, 0f, 0f, 0.6f)
							minWidth(size)
							minHeight(size)
						}
					}
				})
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
		s.borderColor = BorderColors(theme.stroke)
		s.borderRadius = Corners(0f)
		s.borderThickness = Pad(theme.strokeThickness)
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
					borderThickness = Pad(top = 0f, right = 0f, bottom = 0f, left = 4f)
					borderColor = BorderColors(Color(0f, 0f, 0f, 0.4f))
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
					borderThickness = Pad(top = 0f, right = 0f, bottom = 4f, left = 0f)
					borderColor = BorderColors(Color(0f, 0f, 0f, 0.4f))
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
					styleTags.add(StyleSelectors.themeRect)
					style.borderRadius = Corners(theme.borderRadius)
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
		colorSwatchStyle.borderRadius = Corners(theme.borderRadius)
		target.addStyleRule(colorSwatchStyle, ColorPicker.COLOR_SWATCH_STYLE)
	}

	protected open fun dataScrollerStyle() {
		val dataScrollerStyle = DataScrollerStyle()
		dataScrollerStyle.padding = Pad(theme.strokeThickness)
		dataScrollerStyle.background = {
			rect {
				style.apply {
					backgroundColor = theme.panelBgColor
					borderThickness = Pad(theme.strokeThickness)
					borderColor = BorderColors(theme.stroke)
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

	protected open fun optionsListStyle() {
		val optionsListStyle = OptionsListStyle().apply {
			downArrow = {
				atlas(theme.atlasPath, "OptionsListArrow")
			}
			padding = Pad(theme.strokeThickness)
			background = {
				rect {
					style.apply {
						backgroundColor = theme.inputFill
						borderThickness = Pad(theme.strokeThickness)
						borderRadius = Corners(0f)
						borderColor = BorderColors(theme.stroke)
					}
				}
			}
		}
		target.addStyleRule(optionsListStyle, OptionsList)

		val pad = Pad(top = 0f, right = theme.strokeThickness, bottom = theme.strokeThickness, left = theme.strokeThickness)
		val dataScrollerStyle = DataScrollerStyle().apply {
			padding = pad
			background = {
				rect {
					style.apply {
						backgroundColor = theme.panelBgColor
						borderThickness = pad
						borderRadius = Corners(0f, 0f, theme.borderRadius, theme.borderRadius)
						borderColor = BorderColors(theme.stroke)
					}
				}
			}
		}
		target.styleRules.add(StyleRule(dataScrollerStyle, withAncestor(OptionsList)))

		val scrollRectStyle = ScrollRectStyle().apply {
			borderRadius = Corners(0f, 0f, 0f, theme.borderRadius - theme.strokeThickness)
		}
		target.styleRules.add(StyleRule(scrollRectStyle, withAncestor(OptionsList)))

		val textInputBoxStyle = BoxStyle()
		textInputBoxStyle.apply {
			backgroundColor = Color.CLEAR
			borderThickness = Pad(0f)
		}
		target.addStyleRule(textInputBoxStyle, withAncestor(OptionsList) and TextInput)
	}

	protected open fun dataGridStyle() {
		val dataGridStyle = DataGridStyle()
		dataGridStyle.background = {
			rect {
				style.apply {
					backgroundColor = theme.fill
					borderThickness = Pad(theme.strokeThickness)
					borderColor = BorderColors(theme.stroke)
					borderRadius = Corners(theme.borderRadius)
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

		val headerCharStyle = CharStyle()
		headerCharStyle.bold = true
		headerCharStyle.selectable = false
		target.addStyleRule(headerCharStyle, withAncestor(TextField) andThen withAncestor(DataGrid.HEADER_CELL))

		val bodyCharStyle = CharStyle()
		bodyCharStyle.selectable = false
		target.addStyleRule(bodyCharStyle, withAncestor(TextField) andThen withAncestor(DataGrid.BODY_CELL))

		val headerFlowStyle = FlowLayoutStyle()
		headerFlowStyle.horizontalAlign = FlowHAlign.CENTER
		headerFlowStyle.multiline = false
		target.addStyleRule(headerFlowStyle, withAncestor(TextField) andThen withAncestor(DataGrid.HEADER_CELL))

		val groupHeaderCharStyle = CharStyle()
		groupHeaderCharStyle.bold = true
		groupHeaderCharStyle.selectable = false
		target.addStyleRule(groupHeaderCharStyle, withAncestor(TextField) andThen withAncestor(DataGridGroupHeader))

		val dataGridGroupHeaderStyle = DataGridGroupHeaderStyle()
		dataGridGroupHeaderStyle.collapseButton = { collapseButton { toggleOnClick = false } }
		dataGridGroupHeaderStyle.background = {
			rect {
				style.backgroundColor = theme.controlBarBgColor
				style.borderThickness = Pad(0f, 0f, 1f, 0f)
				style.borderColor = BorderColors(theme.stroke)
			}
		}
		dataGridGroupHeaderStyle.padding = Pad(6f)
		dataGridGroupHeaderStyle.gap = 2f
		dataGridGroupHeaderStyle.verticalAlign = VAlign.MIDDLE
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

fun iconButtonSkin(buttonState: ButtonState, icon: String, borderRadius: Corners, borderThickness: PadRo, padding: PadRo = Pad(5f)): Owned.() -> UiComponent = {
	val texture = buttonTexture(buttonState, borderRadius, borderThickness)
	val skinPart = IconButtonSkinPart(this, texture, padding)
	val theme = inject(Theme)
	skinPart.contentsAtlas(theme.atlasPath, icon)
	skinPart
}

fun labelButtonSkin(theme: Theme, buttonState: ButtonState): Owned.() -> UiComponent = {
	val texture = buttonTexture(buttonState, Corners(theme.borderRadius), Pad(theme.strokeThickness))
	LabelButtonSkinPart(this, texture)
}

fun tabButtonSkin(theme: Theme, buttonState: ButtonState): Owned.() -> UiComponent = {
	val texture = buttonTexture(buttonState, Corners(topLeft = theme.borderRadius, topRight = theme.borderRadius, bottomLeft = 0f, bottomRight = 0f), Pad(theme.strokeThickness), isTab = true)
	LabelButtonSkinPart(this, texture)
}

/**
 * A convenience function to create a button skin part.
 */
fun iconButtonSkin(theme: Theme, buttonState: ButtonState): Owned.() -> UiComponent = {
	val texture = buttonTexture(buttonState, Corners(theme.borderRadius), Pad(theme.strokeThickness))
	IconButtonSkinPart(this, texture)
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
			style.borderRadius = Corners(1000f)
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

fun Owned.buttonTexture(buttonState: ButtonState, borderRadius: CornersRo, borderThickness: PadRo, isTab: Boolean = false): CanvasLayoutContainer = canvas {
	val theme = inject(Theme)
	+rect {
		style.apply {
			backgroundColor = getButtonFillColor(buttonState)
			borderColor = BorderColors(getButtonStrokeColor(buttonState))
			val bT = borderThickness.copy()
			if (isTab && buttonState.toggled) {
				bT.bottom = 0f
			}
			this.borderThickness = bT
			this.borderRadius = borderRadius
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
					this.borderRadius = Corners(
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
					this.borderRadius = Corners(
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
		val padding: Pad = Pad(5f, 5f, 5f, 5f)
) : ElementContainerImpl<UiComponent>(owner), Labelable {

	val textField: TextField = text()

	init {
		textField.selectable = false
		+texture
		+textField

		textField.flowStyle.horizontalAlign = FlowHAlign.CENTER
	}

	override var label: String
		get() = textField.label
		set(value) {
			textField.label = value
		}

	override fun updateSizeConstraints(out: SizeConstraints) {
		texture.setSize(null, null)
		out.width.min = texture.width
		out.height.min = texture.height
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val textWidth = padding.reduceWidth(explicitWidth)
		textField.setSize(textWidth, null)
		textField.moveTo(padding.left, padding.top)
		val w = explicitWidth ?: maxOf(minWidth ?: 0f, textField.width + padding.left + padding.right)
		var h = maxOf(minHeight ?: 0f, textField.height + padding.top + padding.bottom)
		if (explicitHeight != null && explicitHeight > h) h = explicitHeight
		texture.setSize(w, h)
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

class Theme {
	var bgColor: ColorRo = Color(0xF1F2F3FF)
	var panelBgColor: ColorRo = Color(0xE7EDF1FF)

	private val brighten: ColorRo = Color(0x15151500)

	var fill: ColorRo = Color(0xE3E9EAFF)
	var fillHighlight: ColorRo = fill + brighten
	var fillDisabled: ColorRo = Color(0xCCCCCCFF)
	var fillShine: ColorRo = Color(1f, 1f, 1f, 0.9f)
	var inputFill: ColorRo = Color(0.97f, 0.97f, 0.97f, 1f)

	var stroke: ColorRo = Color(0x888888FF)
	var strokeThickness = 1f
	var strokeHighlight = stroke + brighten
	var strokeDisabled: ColorRo = Color(0x999999FF)

	var strokeToggled: ColorRo = Color(0x0235ACFF)
	var strokeToggledHighlight = strokeToggled + brighten

	var borderRadius = 8f

	var textStyle = charStyle {
		face = "Verdana"
		size = 14
		colorTint = Color(0x333333FF)
	}

	var headingStyle = charStyle {
		face = "Verdana"
		size = 14
		bold = true
		colorTint = Color(0x333333FF)
	}

	var subHeadingStyle = charStyle {
		face = "Verdana"
		size = 14
		bold = true
		colorTint = Color(0x333333FF)
	}

	var formLabelStyle = charStyle {
		colorTint = Color(0x555555FF)
		bold = true
	}

	var controlBarBgColor: ColorRo = Color(0xDAE5F0FF)

	var evenRowBgColor: ColorRo = bgColor + Color(0x03030300)
	var oddRowBgColor: ColorRo = bgColor - Color(0x03030300)

	var highlightedEvenRowBgColor: ColorRo = Color(0xFEFFD2FF)
	var highlightedOddRowBgColor: ColorRo = Color(0xFEFFD2FF)

	var toggledEvenRowBgColor: ColorRo = Color(0xFCFD7CFF)
	var toggledOddRowBgColor: ColorRo = Color(0xFCFD7CFF)

	var atlasPath = "assets/uiskin/uiskin.json"

	companion object : DKey<Theme> {
		override fun factory(injector: Injector): Theme? = Theme()
	}
}

object StyleSelectors {

	val cbNoLabelStyle = styleTag()
	val headingStyle = styleTag()
	val subHeadingStyle = styleTag()
	val themeRect = styleTag()
	val errorMessage = styleTag()
}

/**
 * A shortcut to creating a text field with the [StyleSelectors.headingStyle] tag.
 */
fun Owned.heading(text: String = "", init: ComponentInit<GlTextField> = {}): GlTextField {
	val t = GlTextField(this)
	t.styleTags.add(StyleSelectors.headingStyle)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [StyleSelectors.subHeadingStyle] tag.
 */
fun Owned.subHeading(text: String = "", init: ComponentInit<GlTextField> = {}): GlTextField {
	val t = GlTextField(this)
	t.styleTags.add(StyleSelectors.subHeadingStyle)
	t.text = text
	t.init()
	return t
}