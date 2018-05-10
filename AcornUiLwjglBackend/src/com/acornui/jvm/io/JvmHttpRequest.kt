package com.acornui.jvm.io

import com.acornui.async.Deferred
import com.acornui.core.di.Injector
import com.acornui.core.request.*
import com.acornui.core.time.TimeDriver
import com.acornui.io.NativeBuffer
import com.acornui.io.toByteArray
import com.acornui.jvm.asyncThread
import com.acornui.logging.Log
import java.io.DataOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


abstract class JvmHttpRequest<out T>(timeDriver: TimeDriver, requestData: UrlRequestData) : Request<T> {

	// TODO: Seconds loaded / total.
	override val secondsLoaded = 0f
	override val secondsTotal = 0f

	private var work: Deferred<T>

	init {
		work = asyncThread(timeDriver) {
			// TODO: cookies
			val urlStr = if (requestData.method == UrlRequestMethod.GET && requestData.variables != null)
				requestData.url + "?" + requestData.variables!!.toQueryString() else requestData.url
			val url = URL(urlStr)
			val con = url.openConnection() as HttpURLConnection
			configure(con, requestData)

			var error: Throwable? = null
			var result: T? = null
			try {
				con.connect()
				val status = con.responseCode
				if (status == 200 || status == 304) {
					result = process(con.inputStream!!)
				} else {
					val errorMsg = con.errorStream?.readTextAndClose() ?: ""
					error = ResponseException(status.toShort(), "", errorMsg)
				}
			} catch (e: Throwable) {
				error = (e)
			} finally {
				con.disconnect()
			}
			if (error != null) throw error
			result!!
		}
	}

	private fun configure(con: HttpURLConnection, requestData: UrlRequestData) {
		if (requestData.user != null) {
			val userPass = "${requestData.user}:${requestData.password}"
			val basicAuth = "Basic " + Base64.getEncoder().encodeToString(userPass.toByteArray())
			con.setRequestProperty("Authorization", basicAuth)
		}
		con.requestMethod = requestData.method
		con.connectTimeout = requestData.timeout.toInt()
		for ((key, value) in requestData.headers) {
			con.setRequestProperty(key, value)
		}
		if (requestData.method != UrlRequestMethod.GET) {
			if (requestData.variables != null) {
				con.doOutput = true
				con.outputStream.writeTextAndClose(requestData.variables!!.toQueryString())
			} else if (requestData.formData != null) {
				con.doOutput = true
				val out = DataOutputStream(con.outputStream)
				val items = requestData.formData!!.items
				for (i in 0..items.lastIndex) {
					val item = items[i]
					if (i != 0) out.writeBytes("&")
					out.writeBytes("$item.name=")
					if (item is ByteArrayFormItem) {
						out.write(item.value.toByteArray())
					} else if (item is StringFormItem) {
						out.writeBytes(item.value)
					} else {
						Log.warn("Unknown form item type $item")
					}
				}

				out.flush()
				out.close()
			} else if (requestData.body != null) {
				con.doOutput = true
				con.outputStream.writeTextAndClose(requestData.body!!)
			}
		}
	}

	suspend override fun await(): T = work.await()

	override fun cancel() {}

	abstract fun process(inputStream: InputStream): T

}

object JvmRestServiceFactory : RestServiceFactory {
	override fun createTextRequest(injector: Injector, requestData: UrlRequestData): Request<String> {
		return object : JvmHttpRequest<String>(injector.inject(TimeDriver), requestData) {
			override fun process(inputStream: InputStream): String {
				return inputStream.readTextAndClose()
			}
		}
	}

	override fun createBinaryRequest(injector: Injector, requestData: UrlRequestData): Request<NativeBuffer<Byte>> {
		return object : JvmHttpRequest<NativeBuffer<Byte>>(injector.inject(TimeDriver), requestData) {
			override fun process(inputStream: InputStream): NativeBuffer<Byte> {
				TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
			}
		}
	}
}