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

package com.acornui.math

inline class Rotation(private val value: String) {
	override fun toString(): String = value
}

val Double.deg: Rotation
	get() = Rotation("${this}deg")

val Double.rad: Rotation
	get() = Rotation("${this}rad")

val Double.grad: Rotation
	get() = Rotation("${this}grad")

val Double.turn: Rotation
	get() = Rotation("${this}turn")

val Int.deg: Rotation
	get() = Rotation("${this}deg")

val Int.rad: Rotation
	get() = Rotation("${this}rad")

val Int.grad: Rotation
	get() = Rotation("${this}grad")

val Int.turn: Rotation
	get() = Rotation("${this}turn")
