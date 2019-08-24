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
}
""")
}