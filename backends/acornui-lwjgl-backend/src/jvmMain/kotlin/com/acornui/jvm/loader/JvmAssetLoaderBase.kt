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

package com.acornui.jvm.loader

import com.acornui.async.Deferred
import com.acornui.core.Bandwidth
import com.acornui.core.asset.AssetLoader
import com.acornui.core.asset.AssetType
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URL

abstract class JvmAssetLoaderBase<T>(
		final override val path: String,
		final override val type: AssetType<T>,
		protected val workScheduler: WorkScheduler<T>
) : AssetLoader<T> {

	private var _bytesTotal: Int? = null
	private var _bytesLoaded: Int = 0

	/**
	 * Like any good programmer, before we have any idea what the actual scope of the work is, we make a completely
	 * blind guess.
	 */
	val bytesTotal: Int
		get() = _bytesTotal ?: 100_000

	private lateinit var work: Deferred<T>

	private var initialized: Boolean = false

	protected fun init() {
		initialized = true
		if (path.startsWith("http:", ignoreCase = true) || path.startsWith("https:", ignoreCase = true)) {
			work = workScheduler {
				val connection = URL(path).openConnection()
				_bytesTotal = connection.contentLength
				create(connection.inputStream).also { _bytesLoaded = bytesTotal }
			}
		} else {
			val file = File(path)
			_bytesTotal = file.length().toInt()
			work = workScheduler {
				if (!file.exists())
					throw FileNotFoundException(path)
				create(FileInputStream(file)).also { _bytesLoaded = bytesTotal }
			}
		}
	}

	abstract fun create(inputStream: InputStream): T

	override val secondsLoaded: Float
		get() {
			return _bytesLoaded.toFloat() * Bandwidth.downBpsInv
		}

	override val secondsTotal: Float
		get() {
			return bytesTotal.toFloat() * Bandwidth.downBpsInv
		}

	override val status: Deferred.Status
		get() = work.status
	override val result: T
		get() = work.result
	override val error: Throwable
		get() = work.error


	override suspend fun await(): T {
		if (!initialized) throw Exception("Subclass must call init()")
		return work.await()
	}

	private var _canceled = false
	protected val canceled: Boolean
		get() = _canceled

	override fun cancel() {
		_canceled = true
	}
}

typealias WorkScheduler<T> = (work: () -> T) -> Deferred<T>
