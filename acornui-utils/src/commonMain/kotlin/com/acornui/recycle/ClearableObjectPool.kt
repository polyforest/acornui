package com.acornui.recycle

/**
 * An interface for objects that can be reset to their original state.
 */
interface Clearable {

	/**
	 * Clears this instance back to its original state.
	 */
	fun clear()
}

/**
 * An ObjectPool implementation that resets the objects as they go back into the pool.
 * @author nbilyk
 */
open class ClearableObjectPool<T : Clearable>(initialCapacity: Int, capacity: Int, create: () -> T) : ObjectPool<T>(initialCapacity, capacity, create) {

	constructor(create: () -> T) : this(8, 20000, create)

	override fun free(obj: T) {
		obj.clear()
		super.free(obj)
	}
}
