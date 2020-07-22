/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.css

import org.intellij.lang.annotations.Language

fun v(v: Length): String = v.toString()

fun pad(all: Length): String = all.toString()

fun pad(topAndBottom: Length, rightAndLeft: Length): String =
	"$topAndBottom $rightAndLeft"

fun pad(top: Length, rightAndLeft: Length, bottom: Length): String =
	"$top $rightAndLeft $bottom"

fun pad(top: Length, right: Length, bottom: Length, left: Length): String =
	"$top $right $bottom $left"

fun margin(all: Length): String =
	all.toString()

fun margin(topAndBottom: Length, rightAndLeft: Length): String =
	"$topAndBottom $rightAndLeft"

fun margin(top: Length, rightAndLeft: Length, bottom: Length): String =
	"$top $rightAndLeft $bottom"

fun margin(top: Length, right: Length, bottom: Length, left: Length): String =
	"$top $right $bottom $left"

fun prefix(@Language("CSS", prefix = "* { ", suffix = ": 0; }") prop: String, @Language("CSS", prefix = "* { --some-property: ", suffix = "; }") value: String, prefixes: List<String> = listOf("-moz-", "-webkit-", "-ms-", "")): String {
	var str = ""
	for (prefix in prefixes) {
		str += "$prefix$prop: $value;"
	}
	return str
}