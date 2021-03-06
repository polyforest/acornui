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

package com.acornui.math

import com.acornui.component.layout.Transform
import org.w3c.dom.DOMMatrixReadOnly

fun DOMMatrixReadOnly.toTransform() = Transform("matrix3d(${toFloat32Array()})")

fun scale(s: Double) = Transform("scale($s)")
fun scale(sX: Double, sY: Double) = Transform("scale($sX, $sY)")

fun rotate(value: Rotation) = Transform("rotate($value)")

fun skew(sX: Rotation, sY: Rotation) = Transform("skew($sX, $sY)")

fun skewX(sX: Rotation) = Transform("skewX($sX)")

fun skewY(sY: Rotation) = Transform("skewY($sY)")
