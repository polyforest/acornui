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

package com.acornui.input

import com.acornui.di.Context

interface Clipboard {

	/**
	 * Copies the given string.
	 * For browser back-ends, this method may only be invoked as a result of a user interaction.
	 *
	 * @param str The string to copy.
	 * @return Returns true if the copy was successful.
	 */
	fun copy(str: String): Boolean

	/**
	 * Triggers a copy event for the currently focused target.
	 * For browser back-ends, this method may only be invoked as a result of a user interaction.
	 * @return Returns true if the copy interaction was successfully invoked.
	 * @see com.acornui.focus.FocusManager.focus
	 */
	fun triggerCopy(): Boolean

	companion object : Context.Key<Clipboard>
}
