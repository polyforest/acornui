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

package com.acornui.persistence

import org.w3c.dom.Storage

/**
 * Sets the storage item if value is not null, otherwise, removes the item.
 */
fun Storage.setOrRemoveItem(key: String, value: String?) {
	if (value == null)
		removeItem(key)
	else
		setItem(key, value)
}

fun Storage.containsItem(key: String): Boolean = getItem(key) != null