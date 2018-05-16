/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.core

import com.acornui.core.time.time
import com.acornui.string.toRadix
import kotlin.math.abs

object UidUtil {

	fun createUid(): String {
		return abs(time.nowMs().toInt()).toRadix(36) + (Math.random() * INT_MAX_VALUE).toInt().toRadix(36)
	}

}