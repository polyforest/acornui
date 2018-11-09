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

package com.acornui.util

import java.io.File


fun main(args: Array<String>) {
	val replacements = ArrayList<Pair<String, String>>()
	replacements.add(Pair("width", "height"))
	replacements.add(Pair("height", "width"))
//	replacements.add(Pair(".x", ".y"))
//	replacements.add(Pair(".y", ".x"))
	replacements.add(Pair(" w ", " h "))
	replacements.add(Pair(" h ", " w "))
	replacements.add(Pair("top", "left"))
	replacements.add(Pair("right", "top"))
	replacements.add(Pair("bottom", "right"))
	replacements.add(Pair("left", "bottom"))
	replacements.add(Pair("vertical", "horizontal"))
	replacements.add(Pair("horizontal", "vertical"))
	replacements.add(Pair("HAlign", "VAlign"))
	replacements.add(Pair("center", "middle"))
	replacements.add(Pair("vgroup", "hgroup"))
	replacements.add(Pair("VDivider", "HDivider"))
	replacements.add(Pair("vDivider", "hDivider"))

	val verticalLayout = File("C:/Projects/kotlin/AcornUi/AcornUiCore/src/com/acornui/component/VScrollBar.kt")
	val str = verticalLayout.readText()
	val str2 = str.replaceList(replacements)
	println(str2)
}

private fun String.replaceList(replacements: ArrayList<Pair<String, String>>): String {
	var str = this
	for (i in 0..replacements.lastIndex) {
		val f = replacements[i].first
		str = str.replace(f, "##TEMP${i}##")
		str = str.replace(f.toUpperCase(), "##L_TEMP${i}##")
		str = str.replace(f.toFirstUpperCase(), "##F_TEMP${i}##")
	}
	for (i in 0..replacements.lastIndex) {
		val f = replacements[i].second
		str = str.replace("##TEMP${i}##", f)
		str = str.replace("##L_TEMP${i}##", f.toUpperCase())
		str = str.replace("##F_TEMP${i}##", f.toFirstUpperCase())
	}
	return str
}

private fun String.toFirstUpperCase(): String {
	return this[0].toUpperCase() + substring(1)
}