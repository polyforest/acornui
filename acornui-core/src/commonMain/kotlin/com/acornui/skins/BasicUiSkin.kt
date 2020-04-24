/*
 * Copyright 2019 Poly Forest, LLC
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

@file:Suppress("MemberVisibilityCanBePrivate")

package com.acornui.skins

import com.acornui.collection.AlwaysFilter
import com.acornui.collection.and
import com.acornui.collection.not
import com.acornui.collection.or
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
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.filter.BlurQuality
import com.acornui.focus.FocusableStyle
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.input.SoftKeyboardView
import com.acornui.input.interaction.ContextMenuStyle
import com.acornui.input.interaction.ContextMenuView
import com.acornui.math.*
import com.acornui.validation.FormInputStyle
import kotlin.random.Random

open class BasicUiSkin(
		protected val target: UiComponent,
		protected val theme: Theme = Theme()
) : ContextImpl(target) {

	open fun apply() {
		target.styleRules.clear()
		WindowScalingAttachment.attach(target)

		target.addStyleRule(buttonStyle { basicLabelButtonSkin(theme) }, ButtonImpl)
		target.addStyleRule(buttonStyle { basicCheckboxSkin(theme) }, CheckboxImpl)
		target.addStyleRule(buttonStyle { collapseButtonSkin(theme) }, CollapseButton)
		target.addStyleRule(buttonStyle { basicRadioButtonSkin(theme) }, RadioButtonImpl)
		target.addStyleRule(buttonStyle { basicIconButtonSkin(theme) }, IconButton)

		stageStyle()
		iconStyle()
		focusStyle()
		textFontStyle()
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
		treeStyle()
		contextMenuStyle()
		calendarStyle()
		htmlComponentStyle()
		tooltipStyle()
		imageButtonStyle()
		formStyle()
		softKeyboardStyle()
		dropShadowStyle()
		itemRendererStyle()
	}

	protected open fun stageStyle() {
		val stageStyle = StageStyle().apply {
			backgroundColor = theme.bgColor
		}
		target.addStyleRule(stageStyle)
	}

	protected open fun iconStyle() {
		val iconStyle = IconStyle().apply {
			iconColor = theme.iconColor
		}
		target.addStyleRule(iconStyle)
	}

	protected open fun focusStyle() {
		val focusableStyle = FocusableStyle().apply {
			highlight = {
				simpleHighlight(theme.atlasPaths, "HighlightRect") {
					colorTint = theme.focusHighlightColor
				}
			}
			junk = theme.focusHighlightColor
		}
		target.addStyleRule(focusableStyle)
	}
//
//	protected open fun validationStyle() {
//		target.getAttachment<Highlighter>(Highlighter)?.dispose()
//		val focusHighlighter = HighlighterImpl(target, simpleHighlight(theme.atlasPaths, "HighlightRect") {
//			colorTint = theme.focusHighlightColor
//		})
//		target.setAttachment(Highlighter, focusHighlighter)
//		val focusableStyle = ValidationStyle().apply {
//			highlighter = focusHighlighter
//		}
//		target.addStyleRule(focusableStyle)
//	}

	protected open fun textFontStyle() {
		theme.bodyFont.addStyles()
		theme.headingFont.addStyles(withAncestor(TextStyleTags.heading))
		theme.formLabelFont.addStyles(withAncestor(formLabelStyleTag))
	}

	protected open fun textStyle() {
		target.addStyleRule(charStyle {
			fontSizes = theme.fontSizes
		})
		target.addStyleRule(charStyle { selectable = theme.selectableText }, not(withAncestor(TextInput) or withAncestor(TextArea)))
		val textInputStyle = TextInputStyle().apply {
			background = {
				rect {
					style.apply {
						backgroundColor = theme.inputFill
						borderColors = BorderColors(theme.stroke)
						borderRadii = Corners(theme.inputCornerRadius)
						borderThicknesses = Pad(theme.strokeThickness)
					}
				}
			}
			padding = Pad(theme.strokeThickness + 2f)
		}
		target.addStyleRule(textInputStyle, TextInput.filter or TextArea.filter)

		val textInputFlowStyle = TextFlowStyle()
		textInputFlowStyle.multiline = false
		target.addStyleRule(textInputFlowStyle, withAncestor(TextInput))

		val textAreaStyle = TextFlowStyle()
		textAreaStyle.multiline = true
		target.addStyleRule(textAreaStyle, withAncestor(TextArea))

		target.addStyleRule(charStyle {
			colorTint = theme.errorColor
		}, withAncestor(TextStyleTags.error))

		target.addStyleRule(charStyle {
			colorTint = theme.warningColor
		}, withAncestor(TextStyleTags.warning))

		target.addStyleRule(charStyle {
			colorTint = theme.infoColor
		}, withAncestor(TextStyleTags.info))
	}

	protected open fun panelStyle() {
		val panelStyle = PanelStyle().apply {
			background = {
				rect {
					style.backgroundColor = theme.panelBgColor
					style.borderColors = BorderColors(theme.stroke)
					style.borderRadii = Corners(theme.borderRadius)
					style.borderThicknesses = Pad(theme.strokeThickness)
				}
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
				iconImageButton(theme.atlasPaths, "ic_clear_white_18dp") {
					style.overState = colorTransformation { tint(Color.RED) }
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
				styleTags.add(TextStyleTags.large)
			}
		}

		target.addStyleRule(headingGroupStyle, HeadingGroup)
	}

	protected fun ThemeFontVo.addStyles(filter: StyleFilter = AlwaysFilter) {
		target.addStyleRule(charStyle {
			colorTint = color
			fontFamily = family
			fontSize = size
			fontWeight = weight
			fontStyle = style
		}, filter)

		target.addStyleRule(charStyle { fontWeight = strongWeight }, filter and withAncestor(TextStyleTags.strong))

		target.addStyleRule(charStyle { fontStyle = emphasisStyle }, filter and withAncestor(TextStyleTags.emphasis))
		target.addStyleRule(charStyle { fontSize = FontSize.relativeSize(size, -2) }, filter and withAncestor(TextStyleTags.extraSmall))
		target.addStyleRule(charStyle { priority = 100f; colorTint = Color.RED; fontSize = FontSize.relativeSize(size, -1) }, AlwaysFilter)
		target.addStyleRule(charStyle { fontSize = size; priority = 1f }, filter and withAncestor(TextStyleTags.regular))
		target.addStyleRule(charStyle { fontSize = FontSize.relativeSize(size, 1) }, filter and withAncestor(TextStyleTags.large))
		target.addStyleRule(charStyle { fontSize = FontSize.relativeSize(size, 2) }, filter and withAncestor(TextStyleTags.extraLarge))
	}

	protected open fun themeRectStyle() {
		val themeRect = BoxStyle()
		themeRect.backgroundColor = theme.fill
		themeRect.borderColors = BorderColors(theme.stroke)
		themeRect.borderThicknesses = Pad(theme.strokeThickness)
		target.addStyleRule(themeRect, CommonStyleTags.themeRect)
	}

	protected open fun tabNavigatorStyle() {
		val tabNavStyle = TabNavigatorStyle().apply {
			tabBarPadding = Pad(0f, 0f, -theme.strokeThickness, 0f)
			contentsPadding = Pad(theme.strokeThickness)
			background = {
				rect {
					style.apply {
						backgroundColor = theme.panelBgColor
						borderColors = BorderColors(theme.stroke)
						borderThicknesses = Pad(theme.strokeThickness)
					}
				}
			}
		}
		target.addStyleRule(tabNavStyle, TabNavigator)

		target.addStyleRule(buttonStyle { basicTabSkin(theme) }, TabNavigator.DEFAULT_TAB_STYLE)
	}

	protected open fun dividerStyle() {
		val hDividerStyle = DividerStyle()
		hDividerStyle.handle = { atlas(theme.atlasPaths, "HDividerHandle") }
		hDividerStyle.divideBar = { atlas(theme.atlasPaths, "HDividerBar") }
		target.addStyleRule(hDividerStyle, HDivider)

		val vDividerStyle = DividerStyle()
		vDividerStyle.handle = { atlas(theme.atlasPaths, "VDividerHandle") }
		vDividerStyle.divideBar = { atlas(theme.atlasPaths, "VDividerBar") }
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
		val stepperPad = Pad(4f)
		stepperPad.right = 5f

		target.addStyleRule(buttonStyle {
			val texture = basicButtonSkin(theme, Corners(topLeft = 0f, topRight = maxOf(4f, theme.borderRadius), bottomRight = 0f, bottomLeft = 0f))
			val skinPart = basicIconButtonSkin(texture) {
				layoutStyle.padding = stepperPad
			}
			val theme = theme
			skinPart.element = atlas(theme.atlasPaths, "ArrowUpSm") {
				colorTint = theme.iconColor
			}
			skinPart
		}, NumericStepper.STEP_UP_STYLE)

		target.addStyleRule(buttonStyle {
			val texture = basicButtonSkin(theme, Corners(topLeft = 0f, topRight = 0f, bottomRight = maxOf(4f, theme.borderRadius), bottomLeft = 0f))
			val skinPart = basicIconButtonSkin(texture) {
				layoutStyle.padding = stepperPad
			}
			val theme = theme
			skinPart.element = atlas(theme.atlasPaths, "ArrowDownSm") {
				colorTint = theme.iconColor
			}
			skinPart
		}, NumericStepper.STEP_DOWN_STYLE)
	}

	protected open fun scrollAreaStyle() {
		// Scroll area (used in GL versions)
		val scrollAreaStyle = scrollAreaStyle {
			corner = {
				rect {
					style.backgroundColor = theme.strokeDisabled
				}
			}
		}
		target.addStyleRule(scrollAreaStyle, ScrollArea)
	}

	protected open fun scrollBarStyle() {
		val scrollBarStyle = scrollBarStyle {
			thumb = {
				button {
					focusEnabled = false
					style.skin = rectButtonSkin { Color(0f, 0f, 0f, 0.6f) }
				} layout { fill() }
			}
			track = {
				rect {
					style.backgroundColor = Color(1f, 1f, 1f, 0.5f)
				} layout { fill() }
			}
			inactiveAlpha = 0.2f
		}
		target.addStyleRule(scrollBarStyle, ScrollBar.filter)
	}

	private fun progressBarStyle() {
		val s = ProgressBarRectStyle().apply {
			borderColors = BorderColors(theme.stroke)
			borderRadii = Corners(0f)
			borderThicknesses = Pad(theme.strokeThickness)
			fillColor = theme.fill
			bgColor = Color(0f, 0f, 0f, 0.2f)
		}
		target.addStyleRule(s, ProgressBarRect)
	}

	protected open fun sliderStyle() {
		val sliderStyle = scrollBarStyle {
			inactiveAlpha = 1f
			decrementButton = { spacer() }
			incrementButton = { spacer() }
			pageMode = false
			thumb = {
				atlas(theme.atlasPaths, "SliderPuck") {
					colorTint = theme.stroke
				}
			}
		}
		target.addStyleRule(sliderStyle, VSlider.filter or HSlider.filter)

		val vSliderStyle = scrollBarStyle {
			naturalHeight = 200f
			naturalWidth = 18f
			track = {
				rect {
					style.apply {
						backgroundColor = theme.stroke
						margin = Pad(left = 0f, top = 6f, right = 0f, bottom = 6f)
					}
				} layout {
					width = 2f
					heightPercent = 1f
				}
			}
		}
		target.addStyleRule(vSliderStyle, VSlider)

		val hSliderStyle = scrollBarStyle {
			naturalWidth = 200f
			naturalHeight = 18f
			track = {
				rect {
					style.apply {
						backgroundColor = theme.stroke
						margin = Pad(left = 6f, top = 0f, right = 6f, bottom = 0f)
					}
				} layout {
					widthPercent = 1f
					height = 2f
				}
			}
		}
		target.addStyleRule(hSliderStyle, HSlider)
	}

	protected open fun colorPickerStyle() {
		val colorPaletteStyle = ColorPaletteStyle().apply {
			background = {
				rect {
					styleTags.add(CommonStyleTags.themeRect)
					style.borderRadii = Corners(theme.borderRadius)
				}
			}
			hueSaturationIndicator = {
				atlas(theme.atlasPaths, "Picker")
			}
			sliderArrow = {
				atlas(theme.atlasPaths, "SliderArrowRight")
			}
		}
		target.addStyleRule(colorPaletteStyle, ColorPalette)

		val colorPickerStyle = ColorPickerStyle().apply {
			background = {
				button {
					focusEnabled = false
					style.skin = { basicButtonSkin(theme, Corners(32f), Pad(theme.strokeThickness)) }
				}
			}
			colorSwatch = {
				rect {
					style.backgroundColor = Color.WHITE
					style.borderColors = BorderColors(Color.WHITE * 0.8f)
					style.borderRadii = Corners(32f)
					style.borderThicknesses = Pad(1f)
				}
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
				iconAtlas(theme.atlasPaths, "ic_expand_more_white_24dp")
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
				shadowRect {
					style.backgroundColor = theme.panelBgColor
					style.borderThicknesses = pad
					style.borderRadii = Corners(0f, 0f, theme.borderRadius, theme.borderRadius)
					style.borderColors = BorderColors(theme.stroke)
				}
			}
			borderRadii = Corners(0f, 0f, theme.borderRadius, theme.borderRadius)
			filter = withAncestor(OptionList)
		}
		target.styleRules.add(dataScrollerStyle)

		val scrollRectStyle = ScrollRectStyle().apply {
			borderRadii = Corners(0f, 0f, 0f, theme.borderRadius - theme.strokeThickness)
			filter = withAncestor(OptionList)
		}
		target.styleRules.add(scrollRectStyle)

		val textInputStyle = TextInputStyle().apply {
			background = noSkinOptional
		}
		target.addStyleRule(textInputStyle, withAncestor(OptionList) and TextInput.filter)
	}

	protected open fun dataGridStyle() {
		val dataGridStyle = DataGridStyle().apply {
			background = {
				rect {
					style.apply {
						backgroundColor = theme.fill
						borderThicknesses = Pad(theme.strokeThickness)
						borderColors = BorderColors(theme.stroke)
						borderRadii = Corners(theme.borderRadius)
					}
				}
			}
			cellPadding = Pad(theme.strokeThickness + 2f)
			resizeHandleWidth = 8f
			sortDownArrow = { atlas(theme.atlasPaths, "ArrowDownMed") { colorTint = theme.iconColor } }
			sortUpArrow = { atlas(theme.atlasPaths, "ArrowUpMed") { colorTint = theme.iconColor } }
			borderRadii = Corners(theme.borderRadius)
			borderThicknesses = Pad(theme.strokeThickness + 1f)
			cellFocusHighlight = {
				SimpleHighlight(target, theme.atlasPaths, "HighlightRect").apply { colorTint = theme.focusHighlightColor }
			}
			headerCellBackground = {
				button {
					style.skin = { basicButtonSkin(theme, Corners(0f), Pad(0f)) }
				}
			}
		}

		target.addStyleRule(dataGridStyle, DataGrid)

		target.addStyleRule(charStyle { selectable = false }, withAncestor(TextField) and withAncestor(DataGrid.BODY_CELL))

		val headerFlowStyle = FlowLayoutStyle()
		headerFlowStyle.horizontalAlign = FlowHAlign.CENTER
		headerFlowStyle.multiline = false
		target.addStyleRule(headerFlowStyle, withAncestor(TextField) and withAncestor(DataGrid.HEADER_CELL))

		target.addStyleRule(charStyle { selectable = false }, withAncestor(TextField) and (withAncestor(DataGridGroupHeader) or withAncestor(DataGrid.HEADER_CELL)))

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

	protected open fun treeStyle() {
		val itemRendererStyle = DefaultTreeItemRendererStyle()
		itemRendererStyle.openedFolderIcon = {
			atlas(theme.atlasPaths, "folder-horizontal-open.png")
		}
		itemRendererStyle.closedFolderIcon = {
			atlas(theme.atlasPaths, "folder-horizontal.png")
		}
		itemRendererStyle.leafIcon = {
			atlas(theme.atlasPaths, "document.png")
		}
		target.addStyleRule(itemRendererStyle, DefaultTreeItemRenderer)
		val horizontalLayoutStyle = HorizontalLayoutStyle()
		horizontalLayoutStyle.verticalAlign = VAlign.MIDDLE
		target.addStyleRule(horizontalLayoutStyle, withParent(DefaultTreeItemRenderer))

		target.addStyleRule(charStyle { selectable = false }, withParent(DefaultTreeItemRenderer))
	}

	protected open fun contextMenuStyle() {
		val contextMenuStyle = ContextMenuStyle()
		contextMenuStyle.rightArrow = {
			atlas(theme.atlasPaths, "ArrowRightMed")
		}
		target.addStyleRule(contextMenuStyle, ContextMenuView)
	}

	protected open fun calendarStyle() {
		val datePickerStyle = DatePickerStyle().apply {
			downArrow = {
				iconAtlas(theme.atlasPaths, "ic_date_range_white_24dp")
			}
			padding = Pad(theme.strokeThickness, theme.strokeThickness + 2f, theme.strokeThickness, theme.strokeThickness)
			background = {
				rect {
					style.apply {
						backgroundColor = theme.inputFill
						borderThicknesses = Pad(theme.strokeThickness)
						borderColors = BorderColors(theme.stroke)
					}
				}
			}
		}
		target.addStyleRule(datePickerStyle, DatePicker)

		val calendarPanelStyle = PanelStyle().apply {
			background = {
				shadowRect {
					style.backgroundColor = theme.panelBgColor
					style.borderColors = BorderColors(theme.stroke)
					style.borderRadii = Corners(bottomLeft = vec2(theme.borderRadius, theme.borderRadius), bottomRight = vec2(theme.borderRadius, theme.borderRadius))
					style.borderThicknesses = Pad(theme.strokeThickness)
				}
			}
		}
		target.addStyleRule(calendarPanelStyle, Panel.filter and withAncestor(Calendar))

		val calendarStyle = CalendarStyle().apply {
			monthDecButton = {
				iconImageButton {
					element = atlas(theme.atlasPaths, "ic_chevron_left_white_24dp")
				}
			}
			monthIncButton = {
				iconImageButton {
					element = atlas(theme.atlasPaths, "ic_chevron_right_white_24dp")
				}
			}
		}
		target.addStyleRule(calendarStyle)

		val inactiveCalendarItemRendererStyle = CalendarItemRendererStyle().apply {
			disabledColor = Color(0.5f, 0.5f, 0.5f, 0.3f)
			upColor = Color(1f, 1f, 1f, 0.3f)
			overColor = Color(1f, 1f, 0.5f, 0.3f)
			downColor = Color(0.6f, 0.6f, 0.5f, 0.3f)
			toggledUpColor = Color(1f, 1f, 0f, 0.2f)
			toggledOverColor = Color(1f, 1f, 0f, 0.3f)
			toggledDownColor = Color(1f, 1f, 0f, 0.2f)
		}
		target.addStyleRule(inactiveCalendarItemRendererStyle, withAncestor(CalendarItemRendererImpl) and withAncestor(CalendarItemRendererImpl.INACTIVE))

		val calendarTextFlowStyle = TextFlowStyle().apply {
			horizontalAlign = FlowHAlign.CENTER
		}
		target.addStyleRule(calendarTextFlowStyle, withAncestor(CalendarItemRendererImpl))

		val textInputStyle = TextInputStyle()
		textInputStyle.apply {
			background = noSkinOptional
		}
		target.addStyleRule(textInputStyle, withAncestor(DatePicker) and TextInput.filter)
	}

	protected open fun htmlComponentStyle() {
		val boxStyle = BoxStyle()
		boxStyle.backgroundColor = Color.CLEAR
		target.addStyleRule(boxStyle, HtmlComponent)
	}

	protected open fun tooltipStyle() {
		val tooltipStyle = PanelStyle().apply {
			background = {
				shadowRect {
					style.backgroundColor = theme.panelBgColor
					style.borderColors = BorderColors(theme.stroke)
					style.borderRadii = Corners(theme.borderRadius)
					style.borderThicknesses = Pad(theme.strokeThickness)
				}
			}
		}
		target.addStyleRule(tooltipStyle, TooltipView)
	}

	protected open fun imageButtonStyle() {
		val imageButtonStyle = ImageButtonStyle().apply {
			upState = colorTransformation {
				tint(theme.iconColor)
			}
			overState = colorTransformation {
				tint(theme.iconColor)
				offset = Color(0.1f, 0.1f, 0.1f, 0.0f)
			}
			downState = colorTransformation {
				tint(theme.iconColor * 0.9f)
				offset = Color(-0.1f, -0.1f, -0.1f, 0.0f)
			}

			toggledUpState = colorTransformation {
				tint(theme.toggledIconColor)
			}
			toggledOverState = colorTransformation {
				tint(theme.toggledIconColor)
				offset = Color(0.1f, 0.1f, 0.1f, 0.0f)
			}
			toggledDownState = colorTransformation {
				tint(theme.toggledIconColor * 0.9f)
				offset = Color(-0.1f, -0.1f, -0.1f, 0.0f)
			}

			disabledState = colorTransformation {
				tint(0.2f, 0.2f, 0.2f, 0.5f)
				grayscale()
				offset = Color(-0.1f, -0.1f, -0.1f, 0.0f)
			}
		}
		target.addStyleRule(imageButtonStyle, ImageButton.ICON_IMAGE)
	}

	protected open fun formStyle() {
		val formStyle = GridLayoutStyle().apply {
			horizontalGap = 10f
			columns = listOf(
					GridColumn(
							hAlign = HAlign.RIGHT,
							widthPercent = 0.4f
					),
					GridColumn(
							widthPercent = 0.6f
					)
			)
		}
		target.addStyleRule(formStyle, formStyleTag)

		val formInputStyle = FormInputStyle().apply {
			errorIcon = {
				atlas(theme.atlasPaths, "ic_error_outline_white_24dp") {
					colorTint = theme.errorColor
					baselineOverride = 15f
				}
			}
			warningIcon = {
				atlas(theme.atlasPaths, "ic_warning_white_24dp") {
					colorTint = theme.warningColor
					baselineOverride = 15f
				}
			}
		}
		target.addStyleRule(formInputStyle)
	}

	protected open fun softKeyboardStyle() {
		val panelStyle = panelStyle {
			background = {
				rect {
					style.backgroundColor = theme.panelBgColor
					style.borderColors = BorderColors(theme.stroke)
					style.borderRadii = Corners(0f)
					style.borderThicknesses = Pad(theme.strokeThickness)
				}
			}
		}
		target.addStyleRule(panelStyle, SoftKeyboardView)
	}

	protected open fun dropShadowStyle() {
		val dropShadowStyle = GlowBoxStyle().apply {
			quality = BlurQuality.NORMAL
			blurX = 3f
			blurY = 3f
			offset = vec3(4f, 4f)
			colorTransform = ColorTransformation().apply {
				tint(Color(a = 0.3f))
			}
		}
		target.addStyleRule(dropShadowStyle, shadowRectStyleTag)
	}

	protected fun rectButtonSkin(stateToColor: (ButtonState) -> ColorRo): Context.() -> ButtonSkin = {
		RectButtonSkin(this, stateToColor)
	}

	protected open fun itemRendererStyle() {
		val charStyle = charStyle {
			selectable = false
		}
		target.addStyleRule(charStyle, withAncestor(ItemRenderer))
	}

}

private class RectButtonSkin(owner: Context, private val stateToColor: (ButtonState) -> ColorRo) : Rect(owner), ButtonSkin {

	override var label: String = ""

	override var buttonState: ButtonState = ButtonState.UP
		set(value) {
			field = value
			colorTint = stateToColor(value)
		}

	init {
		style.backgroundColor = Color.WHITE
		colorTint = stateToColor(ButtonState.UP)
	}
}