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

package com.acornui.component

import com.acornui.component.input.TextInput
import com.acornui.di.Context
import com.acornui.dom.createElement
import org.w3c.dom.HTMLDataListElement
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class DataList(owner: Context) : UiComponentImpl<HTMLDataListElement>(owner, createElement("datalist"))

var TextInput.list: DataList?
	get() = dom.list?.host as DataList?
	set(value) {
		dom.setAttribute("list", value?.id ?: "")
	}

inline fun Context.dataList(init: ComponentInit<DataList> = {}): DataList {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return DataList(this).apply(init)
}