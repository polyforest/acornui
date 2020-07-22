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

package com.acornui.browser

data class PopUpSpecs(

		/**
		 * The height of the window. Min. value is 100
		 */
		val height: Int? = null,

		/**
		 * The left position of the window. Negative values not allowed
		 */
		val left: Int? = null,

		val menuBar: Boolean? = null,

		/**
		 * Whether or not to add a status bar
		 */
		val status: Boolean? = null,

		/**
		 * Whether or not to display the title bar. Ignored unless the calling application is an HTML Application or a trusted dialog box
		 */
		val titlebar: Boolean? = null,


		/**
		 * The top position of the window. Negative values not allowed
		 */
		val top: Int? = null,

		/**
		 * The width of the window. Min. value is 100
		 */
		val width: Int? = null) {


	fun toSpecsString(): String {
		val strs = ArrayList<String>()
		if (height != null) strs.add("height=$height")
		if (left != null) strs.add("left=$left")
		if (menuBar != null) strs.add("menuBar=$menuBar")
		if (status != null) strs.add("status=$status")
		if (titlebar != null) strs.add("titlebar=$titlebar")
		if (top != null) strs.add("top=$top")
		if (width != null) strs.add("width=$width")
		return strs.joinToString(",")
	}
}