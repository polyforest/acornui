package com.acornui.jvm.loader

import com.acornui.async.Deferred
import com.acornui.async.Work
import com.acornui.async.async
import com.acornui.core.UserInfo
import com.acornui.core.assets.AssetLoader
import com.acornui.core.assets.AssetType
import com.acornui.jvm.asyncThread
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URL

abstract class JvmAssetLoaderBase<T>(
		override final val path: String,
		override final val type: AssetType<T>,
		protected val isAsync: Boolean
) : AssetLoader<T> {

	private var _bytesTotal: Int? = null
	private var _bytesLoaded: Int = 0

	val bytesTotal: Int
		get() = _bytesTotal ?: estimatedBytesTotal

	private lateinit var work: Deferred<T>

	private var initialized: Boolean = false

	protected fun init() {
		initialized = true
		if (path.startsWith("http:", ignoreCase = true) || path.startsWith("https:", ignoreCase = true)) {
			work = doWork {
				val connection = URL(path).openConnection()
				val fis = connection.inputStream
				_bytesTotal = connection.contentLength
				create(fis).also { _bytesLoaded = bytesTotal }
			}
		} else {
			val file = File(path)
			_bytesTotal = file.length().toInt()
			if (!file.exists()) throw FileNotFoundException(path)
			work = doWork {
				val fis = FileInputStream(file)
				create(fis).also { _bytesLoaded = bytesTotal }
			}
		}
	}

	abstract fun create(fis: InputStream): T

	override var estimatedBytesTotal: Int = 0

	override val secondsLoaded: Float
		get() {
			return _bytesLoaded.toFloat() * UserInfo.downBps
		}

	override val secondsTotal: Float
		get() {
			return bytesTotal.toFloat() * UserInfo.downBpsInv
		}


	private fun <T> doWork(work: Work<T>): Deferred<T> {
		return if (isAsync) asyncThread(work) else async(work)
	}

	suspend override fun await(): T {
		if (!initialized) throw Exception("Subclass must call init()")
		return work.await()
	}

	// TODO:
	override fun cancel() {}
}