package com.acornui.test

import com.acornui.asset.AssetLoader
import com.acornui.asset.AssetLoaderRo
import com.acornui.asset.AssetManager
import com.acornui.asset.AssetType
import com.acornui.async.Deferred
import com.acornui.signal.Signal
import com.acornui.signal.emptySignal

object MockAssetManager : AssetManager {
	override val currentLoadersChanged: Signal<() -> Unit> = emptySignal()
	override val currentLoaders: List<AssetLoaderRo<*>> = emptyList()

	override fun <T> load(path: String, type: AssetType<T>): AssetLoader<T> {
		@Suppress("UNCHECKED_CAST")
		return MockAssetLoader as AssetLoader<T>
	}

	override fun dispose() {
	}
}

object MockAssetLoader : AssetLoader<Any> {
	override val path: String = ""
	override val type: AssetType<*> = AssetType<Any>("mock")

	override suspend fun await(): Any {
		return Unit
	}

	override val status: Deferred.Status = Deferred.Status.SUCCESSFUL
	override val result: Any = Unit
	override val error: Throwable
		get() = Exception("Mock error")
	override val secondsLoaded: Float = 0f
	override val secondsTotal: Float = 0f

	override fun cancel() {
	}
}