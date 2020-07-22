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
import com.acornui.input.interaction.ContextMenuStyle
import com.acornui.input.interaction.ContextMenuView
import com.acornui.math.*
import com.acornui.validation.FormInputStyle

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
		scrollbarStyle()
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
		}
		target.addStyleRule(focusableStyle)
	}

	protected open fun textFontStyle() {
		theme.bodyFont.addStyles(name = "CharStyle_body")
		theme.headingFont.addStyles(withAncestor(TextStyleTags.heading), name = "CharStyle_heading")
		theme.formLabelFont.addStyles(withAncestor(formLabelStyleTag), name = "CharStyle_formLabel")
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
			padding = Pad(theme.strokeThickness + 2.0)
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
						borderThickness.top = 0.0
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
		headingGroupStyle.headingPadding.bottom = 0.0

		headingGroupStyle.heading = {
			text {
				addClass(TextStyleTags.large)
			}
		}

		target.addStyleRule(headingGroupStyle, HeadingGroup)
	}

	protected fun ThemeFontVo.addStyles(filter: StyleFilter = AlwaysFilter, name: String? = null) {
		target.addStyleRule(charStyle {
			this.name = name + "_general"
			colorTint = color
			fontFamily = family
			fontSize = size
			fontWeight = weight
			fontStyle = style
		}, filter)

		target.addStyleRule(charStyle { fontWeight = strongWeight }, filter and withAncestor(TextStyleTags.strong))

		target.addStyleRule(charStyle { fontStyle = emphasisStyle }, filter and withAncestor(TextStyleTags.emphasis))
		target.addStyleRule(charStyle { fontSize = FontSize.relativeSize(size, -2) }, filter and withAncestor(TextStyleTags.extraSmall))
		target.addStyleRule(charStyle { fontSize = FontSize.relativeSize(size, -1) }, filter and withAncestor(TextStyleTags.small))
		target.addStyleRule(charStyle { fontSize = size; priority = 1.0 }, filter and withAncestor(TextStyleTags.regular))
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
			tabBarPadding = Pad(0.0, 0.0, 0.0, -theme.strokeThickness)
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
		ruleStyle.thickness = 2.0
		ruleStyle.borderColors = BorderColors(Color(1.0, 1.0, 1.0, 0.7))
		ruleStyle.backgroundColor = theme.stroke
		target.addStyleRule(ruleStyle)

		val vRuleStyle = RuleStyle()
		vRuleStyle.borderThicknesses = Pad().set(right = 1.0)
		target.addStyleRule(vRuleStyle, Rule.VERTICAL_STYLE)

		val hRuleStyle = RuleStyle()
		hRuleStyle.borderThicknesses = Pad().set(bottom = 1.0)
		target.addStyleRule(hRuleStyle, Rule.HORIZONTAL_STYLE)
	}

	protected open fun numericStepperStyle() {
		val stepperPad = Pad(4.0)
		stepperPad.right = 5.0

		target.addStyleRule(buttonStyle {
			val texture = basicButtonSkin(theme, Corners(topLeft = 0.0, topRight = maxOf(4.0, theme.borderRadius), bottomRight = 0.0, bottomLeft = 0.0))
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
			val texture = basicButtonSkin(theme, Corners(topLeft = 0.0, topRight = 0.0, bottomRight = maxOf(4.0, theme.borderRadius), bottomLeft = 0.0))
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

	protected open fun scrollbarStyle() {
		val scrollbarStyle = scrollbarStyle {
			thumb = {
				button {
					focusEnabled = false
					style.skin = rectButtonSkin { Color(0.0, 0.0, 0.0, 0.6) }
				} layout { fill() }
			}
			track = {
				rect {
					style.backgroundColor = Color(1.0, 1.0, 1.0, 0.5)
				} layout { fill() }
			}
			inactiveAlpha = 0.2
		}
		target.addStyleRule(scrollbarStyle, Scrollbar.filter)
	}

	private fun progressBarStyle() {
		val s = ProgressBarRectStyle().apply {
			borderColors = BorderColors(theme.stroke)
			borderRadii = Corners(0.0)
			borderThicknesses = Pad(theme.strokeThickness)
			fillColor = theme.fill
			bgColor = Color(0.0, 0.0, 0.0, 0.2)
		}
		target.addStyleRule(s, ProgressBarRect)
	}

	protected open fun sliderStyle() {
		val sliderStyle = scrollbarStyle {
			inactiveAlpha = 1.0
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

		val vSliderStyle = scrollbarStyle {
			naturalHeight = 200.0
			naturalWidth = 18.0
			track = {
				rect {
					style.apply {
						backgroundColor = theme.stroke
						margin = Pad(left = 0.0, top = 6.0, right = 0.0, bottom = 6.0)
					}
				} layout {
					width = 2.0
					heightPercent = 1.0
				}
			}
		}
		target.addStyleRule(vSliderStyle, VSlider)

		val hSliderStyle = scrollbarStyle {
			naturalWidth = 200.0
			naturalHeight = 18.0
			track = {
				rect {
					style.apply {
						backgroundColor = theme.stroke
						margin = Pad(left = 6.0, top = 0.0, right = 6.0, bottom = 0.0)
					}
				} layout {
					widthPercent = 1.0
					height = 2.0
				}
			}
		}
		target.addStyleRule(hSliderStyle, HSlider)
	}

	protected open fun colorPickerStyle() {
		val colorPaletteStyle = ColorPaletteStyle().apply {
			background = {
				rect {
					addClass(CommonStyleTags.themeRect)
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
					style.skin = { basicButtonSkin(theme, Corners(32.0), Pad(theme.strokeThickness)) }
				}
			}
			colorSwatch = {
				rect {
					style.backgroundColor = Color.WHITE
					style.borderColors = BorderColors(Color.WHITE * 0.8)
					style.borderRadii = Corners(32.0)
					style.borderThicknesses = Pad(1.0)
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
		verticalLayoutStyle.padding = Pad(left = 5.0, top = 0.0, right = 5.0, bottom = 0.0)
		target.addStyleRule(verticalLayoutStyle, withParent(DataScroller))

		val horizontalLayoutStyle = VirtualHorizontalLayoutStyle()
		horizontalLayoutStyle.padding = Pad(left = 0.0, top = 5.0, right = 0.0, bottom = 5.0)
		target.addStyleRule(horizontalLayoutStyle, withParent(DataScroller))
	}

	protected open fun optionListStyle() {
		val optionListStyle = OptionListStyle().apply {
			downArrow = {
				iconAtlas(theme.atlasPaths, "ic_expand_more_white_24dp")
			}
			padding = Pad(theme.strokeThickness, theme.strokeThickness, theme.strokeThickness + 2.0, theme.strokeThickness)
			background = {
				rect {
					style.apply {
						backgroundColor = theme.inputFill
						borderThicknesses = Pad(theme.strokeThickness)
						borderRadii = Corners(0.0)
						borderColors = BorderColors(theme.stroke)
					}
				}
			}
		}
		target.addStyleRule(optionListStyle, OptionList)

		val pad = Pad(left = theme.strokeThickness, top = 0.0, right = theme.strokeThickness, bottom = theme.strokeThickness)
		val dataScrollerStyle = DataScrollerStyle().apply {
			padding = pad
			background = {
				shadowRect {
					style.backgroundColor = theme.panelBgColor
					style.borderThicknesses = pad
					style.borderRadii = Corners(0.0, 0.0, theme.borderRadius, theme.borderRadius)
					style.borderColors = BorderColors(theme.stroke)
				}
			}
			borderRadii = Corners(0.0, 0.0, theme.borderRadius, theme.borderRadius)
			filter = withAncestor(OptionList)
		}
		target.styleRules.add(dataScrollerStyle)

		val scrollRectStyle = ScrollRectStyle().apply {
			borderRadii = Corners(0.0, 0.0, 0.0, theme.borderRadius - theme.strokeThickness)
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
			cellPadding = Pad(theme.strokeThickness + 2.0)
			resizeHandleWidth = 8.0
			sortDownArrow = { atlas(theme.atlasPaths, "ArrowDownMed") { colorTint = theme.iconColor } }
			sortUpArrow = { atlas(theme.atlasPaths, "ArrowUpMed") { colorTint = theme.iconColor } }
			borderRadii = Corners(theme.borderRadius)
			borderThicknesses = Pad(theme.strokeThickness + 1.0)
			cellFocusHighlight = {
				SimpleHighlight(target, theme.atlasPaths, "HighlightRect").apply { colorTint = theme.focusHighlightColor }
			}
			headerCellBackground = {
				button {
					style.skin = { basicButtonSkin(theme, Corners(0.0), Pad(0.0)) }
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
				style.borderThicknesses = Pad(0.0, 0.0, 0.0, 1.0)
				style.borderColors = BorderColors(theme.stroke)
			}
		}
		target.addStyleRule(dataGridGroupHeaderStyle, DataGridGroupHeader)

		val dataGridGroupHeaderLayoutStyle = HorizontalLayoutStyle()
		dataGridGroupHeaderLayoutStyle.padding = Pad(6.0)
		dataGridGroupHeaderLayoutStyle.gap = 2.0
		dataGridGroupHeaderLayoutStyle.verticalAlign = VAlign.MIDDLE
		target.addStyleRule(dataGridGroupHeaderStyle, DataGridGroupHeader)

		val columnMoveIndicatorStyle = BoxStyle()
		columnMoveIndicatorStyle.backgroundColor = Color(0.5, 0.5, 0.5, 0.5)
		target.addStyleRule(columnMoveIndicatorStyle, DataGrid.COLUMN_MOVE_INDICATOR)

		val columnInsertionIndicatorStyle = RuleStyle()
		columnInsertionIndicatorStyle.thickness = 4.0
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
			padding = Pad(theme.strokeThickness, theme.strokeThickness, theme.strokeThickness + 2.0, theme.strokeThickness)
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
			disabledColor = Color(0.5, 0.5, 0.5, 0.3)
			upColor = Color(1.0, 1.0, 1.0, 0.3)
			overColor = Color(1.0, 1.0, 0.5, 0.3)
			downColor = Color(0.6, 0.6, 0.5, 0.3)
			toggledUpColor = Color(1.0, 1.0, 0.0, 0.2)
			toggledOverColor = Color(1.0, 1.0, 0.0, 0.3)
			toggledDownColor = Color(1.0, 1.0, 0.0, 0.2)
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
				offset = Color(0.1, 0.1, 0.1, 0.0)
			}
			downState = colorTransformation {
				tint(theme.iconColor * 0.9)
				offset = Color(-0.1, -0.1, -0.1, 0.0)
			}

			toggledUpState = colorTransformation {
				tint(theme.toggledIconColor)
			}
			toggledOverState = colorTransformation {
				tint(theme.toggledIconColor)
				offset = Color(0.1, 0.1, 0.1, 0.0)
			}
			toggledDownState = colorTransformation {
				tint(theme.toggledIconColor * 0.9)
				offset = Color(-0.1, -0.1, -0.1, 0.0)
			}

			disabledState = colorTransformation {
				tint(0.2, 0.2, 0.2, 0.5)
				grayscale()
				offset = Color(-0.1, -0.1, -0.1, 0.0)
			}
		}
		target.addStyleRule(imageButtonStyle, ImageButton.ICON_IMAGE)
	}

	protected open fun formStyle() {
		val formStyle = GridLayoutStyle().apply {
			horizontalGap = 10.0
			columns = listOf(
					GridColumn(
							hAlign = HAlign.RIGHT,
							widthPercent = 0.4
					),
					GridColumn(
							widthPercent = 0.6
					)
			)
		}
		target.addStyleRule(formStyle, formStyleTag)

		val formInputStyle = FormInputStyle().apply {
			errorIcon = {
				atlas(theme.atlasPaths, "ic_error_outline_white_24dp") {
					colorTint = theme.errorColor
					baselineOverride = 15.0
				}
			}
			warningIcon = {
				atlas(theme.atlasPaths, "ic_warning_white_24dp") {
					colorTint = theme.warningColor
					baselineOverride = 15.0
				}
			}
		}
		target.addStyleRule(formInputStyle)
	}

	protected open fun dropShadowStyle() {
		val dropShadowStyle = GlowBoxStyle().apply {
			quality = BlurQuality.NORMAL
			blurX = 3.0
			blurY = 3.0
			offset = vec3(4.0, 4.0)
			colorTransform = ColorTransformation().apply {
				tint(Color(a = 0.3))
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