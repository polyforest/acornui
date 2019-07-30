package com.acornui

/**
 * Applies workarounds to KT-15101 and KT-16379
 */
actual fun kotlinBugFixes() {
	js( // language=JS
			"""
if (Function.prototype.uncachedBind === undefined) {
	Function.prototype.uncachedBind = Function.prototype.bind;
	/**
	 * Workaround to kotlin member references not being equal. KT-15101
	 */
	Function.prototype.bind = function() {
		if (arguments.length !== 2 || arguments[0] !== null || arguments[1] === null) return this.uncachedBind.apply(this, arguments);
		var receiver = arguments[1];
		if (!receiver.__bindingCache) receiver.__bindingCache = {};
		var existing = receiver.__bindingCache[this];
		if (existing !== undefined) return existing;
		var newBind = this.uncachedBind.apply(this, arguments);
		receiver.__bindingCache[this] = newBind;
		return newBind;
	};
	
	Kotlin.uncachedIsType = Kotlin.isType;
	Kotlin.isType = function(object, klass) {
		if (klass === Object) {
		  switch (typeof object) {
			case 'string':
			case 'number':
			case 'boolean':
			case 'function':
			  return true;
			default:return object instanceof Object;
		  }
		}
		if (object == null || klass == null || (typeof object !== 'object' && typeof object !== 'function')) {
		  return false;
		}
		if (typeof klass === 'function' && object instanceof klass) {
		  return true;
		}
	
		if (!object.__typeCache) object.__typeCache = {};
		var existing = object.__typeCache[klass];
		if (existing !== undefined) return existing;
		var typeCheck = Kotlin.uncachedIsType.apply(this, arguments);
		object.__typeCache[klass] = typeCheck;
		return typeCheck;
	};				
}
""")
}