/*
 * Copyright 2018 Poly Forest
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

package com.acornui.function

val (() -> Unit).as1: (Any?) -> Unit
	get() {
		return this.asDynamic()
	}

val (() -> Unit).as2: (Any?, Any?) -> Unit
	get() {
		return this.asDynamic()
	}

val (() -> Unit).as3: (Any?, Any?, Any?) -> Unit
	get() {
		return this.asDynamic()
	}

val (() -> Unit).as4: (Any?, Any?, Any?, Any?) -> Unit
	get() {
		return this.asDynamic()
	}

val (() -> Unit).as5: (Any?, Any?, Any?, Any?, Any?) -> Unit
	get() {
		return this.asDynamic()
	}

val (() -> Unit).as6: (Any?, Any?, Any?, Any?, Any?, Any?) -> Unit
	get() {
		return this.asDynamic()
	}