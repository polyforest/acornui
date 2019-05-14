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

package com.acornui.core.asset

import com.acornui.action.Decorator
import com.acornui.async.Deferred
import com.acornui.core.di.Scoped
import com.acornui.serialization.From
import com.acornui.serialization.json

/**
 * The JsonDecorator will deserialize a string. Its factory must be a singleton and produce no side-effects.
 */
class JsonDecorator<out R>(val factory: From<R>) : Decorator<String, R> {
	override fun decorate(target: String): R {
		return json.read(target, factory)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		return hashCode() == other?.hashCode()
	}

	private val _hashCode: Int = run {
		31 * factory.hashCode()
	}

	override fun hashCode(): Int {
		return _hashCode
	}

}

fun <R> jsonDecorator(factory: From<R>): Decorator<String, R> {
	return JsonDecorator(factory)
}


fun <R> Scoped.loadAndCacheJson(path: String, factory: From<R>, group: CachedGroup): Deferred<R> {
	return loadAndCache(path, AssetType.TEXT, jsonDecorator(factory), group)
}
