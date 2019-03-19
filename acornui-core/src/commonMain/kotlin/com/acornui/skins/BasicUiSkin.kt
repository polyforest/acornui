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

package com.acornui.skins

import com.acornui.async.launch
import com.acornui.component.*
import com.acornui.component.datagrid.DataGrid
import com.acornui.component.datagrid.DataGridGroupHeader
import com.acornui.component.datagrid.DataGridGroupHeaderStyle
import com.acornui.component.datagrid.DataGridStyle
import com.acornui.component.layout.DataScroller
import com.acornui.component.layout.DataScrollerStyle
import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.*
import com.acornui.component.layout.algorithm.virtual.VirtualHorizontalLayoutStyle
import com.acornui.component.layout.algorithm.virtual.VirtualVerticalLayoutStyle
import com.acornui.component.layout.spacer
import com.acornui.component.scroll.*
import com.acornui.component.style.*
import com.acornui.component.text.*
import com.acornui.core.AppConfig
import com.acornui.core.asset.cachedGroup
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.focus.FocusManager
import com.acornui.core.focus.SimpleHighlight
import com.acornui.core.input.interaction.ContextMenuStyle
import com.acornui.core.input.interaction.ContextMenuView
import com.acornui.core.input.interaction.enableDownRepeat
import com.acornui.core.popup.PopUpManager
import com.acornui.core.userInfo
import com.acornui.graphic.Color
import com.acornui.math.Corners
import com.acornui.math.Pad
import com.acornui.math.Vector2

open class BasicUiSkin(
		val target: UiComponent,
		private val skinPartFactory: SkinPartFactory = BasicSkinPartFactory()
) : Scoped, SkinPartFactory by skinPartFactory {

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

		target.addStyleRule(ButtonStyle().set { labelButtonSkin(theme, it) }, Button)
		target.addStyleRule(ButtonStyle().set { checkboxSkin(theme, it) }, Checkbox)
		target.addStyleRule(ButtonStyle().set { collapseButtonSkin(theme, it) }, CollapseButton)
		target.addStyleRule(ButtonStyle().set { radioButtonSkin(theme, it) }, RadioButton)
		target.addStyleRule(ButtonStyle().set { checkboxNoLabelSkin(theme, it) }, Checkbox.NO_LABEL)
		target.addStyleRule(ButtonStyle().set { iconButtonSkin(theme, it) }, IconButton)

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
		htmlComponentStyle()
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
		focusHighlight.colorTint = theme.focusHighlightColor
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

		val charStyle = CharStyle()
		charStyle.selectable = false
		target.addStyleRule(charStyle, withAncestor(Button))
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
		val tabNavStyle = TabNavigatorStyle().apply {
			vGap = -theme.strokeThickness
			contentsPadding = Pad(theme.strokeThickness)
			background = { rect {
				style.apply {
					backgroundColor = theme.panelBgColor
					borderColors = BorderColors(theme.stroke)
					borderThicknesses = Pad(theme.strokeThickness)
				}
			} }
		}
		target.addStyleRule(tabNavStyle, TabNavigator)

		target.addStyleRule(ButtonStyle().set { tabButtonSkin(theme, it) }, TabNavigator.DEFAULT_TAB_STYLE)
		target.addStyleRule(ButtonStyle().set { tabButtonSkin(theme, it) }, TabNavigator.DEFAULT_TAB_STYLE_FIRST)
		target.addStyleRule(ButtonStyle().set { tabButtonSkin(theme, it) }, TabNavigator.DEFAULT_TAB_STYLE_LAST)
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
		target.addStyleRule(ButtonStyle().set { iconButtonSkin(it, "UpArrowStepper", padding = stepperPad) }, NumericStepper.STEP_UP_STYLE)
		target.addStyleRule(ButtonStyle().set { iconButtonSkin(it, "DownArrowStepper", padding = stepperPad) }, NumericStepper.STEP_DOWN_STYLE)
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

		val thumb: SkinPart = {
			button {
				focusEnabled = false
				style.set {
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

		val track: SkinPart = {
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
			sliderArrow = {
				atlas(theme.atlasPath, "SliderArrowRight")
			}
		}
		target.addStyleRule(colorPaletteStyle, ColorPalette)

		val colorPickerStyle = ColorPickerStyle()
		colorPickerStyle.apply {
			background = {
				button { focusEnabled = false }
			}
			colorSwatch = {
				curvedRect(Color.WHITE, Color(1f, 1f, 1f, 0.5f))
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
			resizeHandleWidth = if (userInfo.isTouchDevice) 16f else 8f
			sortDownArrow = { atlas(theme.atlasPath, "DownArrow") { colorTint = theme.iconColor } }
			sortUpArrow = { atlas(theme.atlasPath, "UpArrow") { colorTint = theme.iconColor } }
			borderRadius = Corners(theme.borderRadius)
			borderThickness = Pad(theme.strokeThickness)
			cellFocusHighlight = {
				SimpleHighlight(target, theme.atlasPath, "FocusRect").apply { colorTint = theme.strokeToggled }
			}
			headerCellBackground = {
				button {
					style.set { buttonState ->
						{ buttonTexture(buttonState, Corners(0f), Pad(0f)) }
					}
				}
			}
		}


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
						borderColors = BorderColors(theme.stroke)
					}
				}
			}
		}
		target.addStyleRule(datePickerStyle, DatePicker)

		val calendarPanelStyle = PanelStyle().apply {
			background = {
				rect {
					style.backgroundColor = theme.panelBgColor
					style.borderColors = BorderColors(theme.stroke)
					style.borderRadii = Corners(bottomLeft = Vector2(theme.borderRadius, theme.borderRadius), bottomRight = Vector2(theme.borderRadius, theme.borderRadius))
					style.borderThicknesses = Pad(theme.strokeThickness)
				}
			}
		}
		target.addStyleRule(calendarPanelStyle, Panel and withAncestor(Calendar))

		target.addStyleRule(ButtonStyle().set { iconButtonSkin(it, "ArrowLeft") }, Calendar.MONTH_DEC_STYLE)
		target.addStyleRule(ButtonStyle().set { iconButtonSkin(it, "ArrowRight") }, Calendar.MONTH_INC_STYLE)

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

		val textInputBoxStyle = BoxStyle()
		textInputBoxStyle.apply {
			backgroundColor = Color.CLEAR
			borderThicknesses = Pad(0f)
		}
		target.addStyleRule(textInputBoxStyle, withAncestor(DatePicker) and TextInput)
	}

	protected open fun htmlComponentStyle() {
		val boxStyle = BoxStyle()
		boxStyle.backgroundColor = Color.CLEAR
		target.addStyleRule(boxStyle, HtmlComponent)
	}
}