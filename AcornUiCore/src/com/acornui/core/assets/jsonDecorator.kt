package com.acornui.core.assets

import com.acornui.action.Decorator
import com.acornui.async.Deferred
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.io.JSON_KEY
import com.acornui.serialization.From
import com.acornui.serialization.Serializer

fun <R> Scoped.jsonDecorator(factory: From<R>): Decorator<String, R> {
	return JsonDecorator(inject(JSON_KEY), factory)
}

/**
 * The JsonDecorator will deserialize a string. Its factory must be a singleton and produce no side-effects.
 */
class JsonDecorator<out R>(val serializer: Serializer<String>, val factory: From<R>) : Decorator<String, R> {
	override fun decorate(target: String): R {
		return serializer.read(target, factory)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		return hashCode() == other?.hashCode()
	}

	private val _hashCode: Int = run {
		var result = serializer.hashCode()
		result = 31 * result + factory.hashCode()
		result
	}

	override fun hashCode(): Int {
		return _hashCode
	}

}

fun <R> Scoped.loadAndCacheJson(path: String, factory: From<R>, group: CachedGroup): Deferred<R> {
	return loadAndCache(path, AssetType.TEXT, jsonDecorator(factory), group)
}