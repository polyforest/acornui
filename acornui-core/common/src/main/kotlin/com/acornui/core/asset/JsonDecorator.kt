package com.acornui.core.asset

import com.acornui.action.Decorator
import com.acornui.async.Deferred
import com.acornui.core.di.Scoped
import com.acornui.serialization.From
import com.acornui.serialization.fromJson

fun <R> jsonDecorator(factory: From<R>): Decorator<String, R> {
	return JsonDecorator(factory)
}

/**
 * The JsonDecorator will deserialize a string. Its factory must be a singleton and produce no side-effects.
 */
class JsonDecorator<out R>(val factory: From<R>) : Decorator<String, R> {
	override fun decorate(target: String): R {
		return fromJson(target, factory)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		return hashCode() == other?.hashCode()
	}

	private val _hashCode: Int = run {
		factory.hashCode()
	}

	override fun hashCode(): Int {
		return _hashCode
	}

}

fun <R> Scoped.loadAndCacheJson(path: String, factory: From<R>, group: CachedGroup): Deferred<R> {
	return loadAndCache(path, AssetType.TEXT, jsonDecorator(factory), group)
}