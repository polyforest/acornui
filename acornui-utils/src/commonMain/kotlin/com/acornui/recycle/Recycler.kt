package com.acornui.recycle

/**
 * A Recycler is an ObjectPool that executes [configure] on objects retrieved via [obtain], and [unconfigure] on
 * objects freed via [free].
 *
 * @param create Constructs the new object. This will be invoked when the pool is exhausted.
 * @param configure Invoked on objects retrieved via [obtain]. This includes newly constructed objects.
 * @param unconfigure Invoked on objects returned via [free].
 */
class Recycler<T>(

		/**
		 * Constructs a new element
		 */
		create: () -> T,
		private val configure: (T) -> Unit,
		private val unconfigure: (T) -> Unit
) : ObjectPool<T>(create) {

	override fun obtain(): T {
		val obj = super.obtain()
		configure(obj)
		return obj
	}

	override fun free(obj: T) {
		unconfigure(obj)
		super.free(obj)
	}
}