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

package com.acornui.input

import com.acornui.system.Platform
import com.acornui.system.userInfo
import org.w3c.dom.events.KeyboardEvent

/**
 * Returns true if [KeyboardEvent.altKey], [KeyboardEvent.ctrlKey], [KeyboardEvent.metaKey], or
 * [KeyboardEvent.shiftKey] is true.
 */
val KeyboardEvent.hasAnyModifier: Boolean
	get() = altKey || ctrlKey || metaKey || shiftKey

/**
 * The command key on mac os, otherwise, the ctrl key.
 */
val KeyboardEvent.commandPlat: Boolean
	get() = if (userInfo.platform == Platform.APPLE) metaKey else ctrlKey

val KeyboardEvent.isEnterOrReturn: Boolean
	get() = keyCode == Ascii.ENTER || keyCode == Ascii.RETURN
