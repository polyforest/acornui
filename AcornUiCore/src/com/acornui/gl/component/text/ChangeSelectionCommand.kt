/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.gl.component.text

import com.acornui.core.mvc.Command
import com.acornui.core.mvc.CommandGroup
import com.acornui.core.mvc.CommandType
import com.acornui.core.mvc.StateCommand
import com.acornui.core.selection.SelectionRange

class ChangeSelectionCommand(
		val target: Any?,
		val oldSelection: List<SelectionRange>,
		val newSelection: List<SelectionRange>,
		override val group: CommandGroup?
) : StateCommand {

	override val type = Companion

	override fun reverse(): Command {
		return ChangeSelectionCommand(target, newSelection, oldSelection, group)
	}

	companion object : CommandType<ChangeSelectionCommand>
}