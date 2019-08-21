/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.serialization

import com.acornui.collection.stringMapOf
import com.acornui.io.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.Cbor

/**
 * A factory that provides a Reader and Writer for ReadBuffer<Byte>
 * @author nbilyk
 */
@Deprecated("use kotlinx serialization")
private object BinarySerializer : Serializer<ReadByteBuffer> {

	override fun read(data: ReadByteBuffer): Reader {
		data.rewind()
		readMarker(data)
		return BinaryReader(data, readPropertyIndex(data))
	}

	private fun readMarker(data: ReadByteBuffer) {
		if (data.getChar8() != 'A' || data.getChar8() != 'C' || data.getChar8() != 'O' || data.getChar8() != 'R' || data.getChar8() != 'N') throw Exception("Invalid file header")
	}

	private fun readPropertyIndex(data: ReadByteBuffer): List<String> {
		val size = data.getShort().toInt()
		val list = ArrayList<String>(size)
		for (i in 0..size - 1) {
			list.add(data.getString8())
		}
		return list
	}

	override fun write(callback: (Writer) -> Unit): NativeReadByteBuffer {
		val propertyIndex = ArrayList<String>()
		val binaryWriter = BinaryWriter(propertyIndex)
		callback(binaryWriter)

		val data = byteBuffer(calculateHeaderSize(propertyIndex) + binaryWriter.calculateSize())
		writeHeader(data, propertyIndex)
		binaryWriter.write(data)
		data.flip()
		return data
	}

	private fun writeHeader(data: WriteByteBuffer, propertyIndex: List<String>) {
		if (propertyIndex.size > Short.MAX_VALUE) throw Exception("Too many properties")
		writeMarker(data)
		data.putShort(propertyIndex.size.toShort())
		for (p in propertyIndex) {
			data.putString8(p)
		}
	}

	private const val MARKER_SIZE = 10

	private fun calculateHeaderSize(propertyIndex: List<String>): Int {
		var c = 2 + MARKER_SIZE
		for (p in propertyIndex) {
			c += p.length + 1
		}
		return c
	}

	private fun writeMarker(data: WriteByteBuffer) {
		data.putChar8('A')
		data.putChar8('C')
		data.putChar8('O')
		data.putChar8('R')
		data.putChar8('N')
	}
}

class BinaryReader(val data: ReadByteBuffer, propertyIndex: List<String>) : Reader {

	private val value: Any?

	override val isNull: Boolean
		get() = value == null

	init {
		val type = data.get()
		value = when (type) {
			BinaryType.NULL -> null
			BinaryType.BOOLEAN -> data.get() == 1.toByte()
			BinaryType.BYTE -> data.get()
			BinaryType.CHAR -> data.getChar16()
			BinaryType.SHORT -> data.getShort()
			BinaryType.INT -> data.getInt()
			BinaryType.FLOAT -> data.getFloat()
			BinaryType.LONG -> data.getLong()
			BinaryType.DOUBLE -> data.getDouble()
			BinaryType.STRING -> data.getString16()
			BinaryType.ARRAY -> {
				val size = data.getShort().toInt()
				val list = ArrayList<BinaryReader>(size)
				for (i in 0..size - 1) {
					list.add(BinaryReader(data, propertyIndex))
				}
				list
			}
			BinaryType.OBJECT -> {
				val size = data.getShort().toInt()
				val list = stringMapOf<BinaryReader>()
				for (i in 0..size - 1) {
					val j = data.getShort()
					val propertyName = propertyIndex[j.toInt()]
					list[propertyName] = BinaryReader(data, propertyIndex)
				}
				list
			}
			BinaryType.BYTE_ARRAY -> {
				val size = data.getInt()
				val byteArray = ByteArray(size)
				for (i in 0..size - 1) {
					byteArray[i] = data.get()
				}
			}
			else ->
				throw Exception("Unknown data type: $type")
		}
	}

	override fun contains(name: String): Boolean {
		return properties().contains(name)
	}

	override fun contains(index: Int): Boolean {
		return index < elements().size
	}

	override fun bool(): Boolean? = value as Boolean?

	override fun byte(): Byte? = value as Byte?

	override fun int(): Int? = value as Int?

	override fun string(): String? = value as String?

	override fun char(): Char? = value as Char?

	override fun short(): Short? = value as Short?

	override fun long(): Long? = value as Long?

	override fun float(): Float? = value as Float?

	override fun double(): Double? = value as Double?

	override fun byteArray(): ByteArray? = value as ByteArray?

	@Suppress("UNCHECKED_CAST")
	override fun properties(): Map<String, BinaryReader> = value as Map<String, BinaryReader>

	@Suppress("UNCHECKED_CAST")
	override fun elements(): List<BinaryReader> = value as List<BinaryReader>
}

class BinaryWriter(val propertyIndex: MutableList<String>) : Writer {

	private var type: Byte = BinaryType.UNKNOWN
	private var _value: Any? = null

	fun calculateSize(): Int {
		// Add 1 byte for the type
		return 1 + when (type) {
			BinaryType.NULL -> 0
			BinaryType.BOOLEAN -> 1
			BinaryType.BYTE -> 1
			BinaryType.CHAR -> 2
			BinaryType.SHORT -> 2
			BinaryType.INT -> 4
			BinaryType.FLOAT -> 4
			BinaryType.LONG -> 8
			BinaryType.DOUBLE -> 8
			BinaryType.STRING -> (_value as String).length * 2 + 2 // Double-byte unicode, + Stop char
			BinaryType.ARRAY -> {
				@Suppress("UNCHECKED_CAST")
				val list = _value as List<BinaryWriter>
				var c = 2 // The array size
				for (i in 0..list.lastIndex)
					c += list[i].calculateSize()
				c
			}
			BinaryType.OBJECT -> {
				@Suppress("UNCHECKED_CAST")
				val list = (_value as Map<String, BinaryWriter>).values
				// Two bytes for the array size + each property name is stored as a Short for the property index
				var c = 2 + list.size * 2
				for (writer in list)
					c += writer.calculateSize()
				c
			}
			BinaryType.BYTE_ARRAY -> {
				4 + (_value as ByteArray).size
			}
			else -> throw Exception("Unknown data type: $type")
		}
	}

	override fun property(name: String): Writer {
		if (type != BinaryType.OBJECT) throw Exception("Writer is not type OBJECT")
		@Suppress("UNCHECKED_CAST")
		val map = _value as MutableMap<String, BinaryWriter>
		val newWriter = BinaryWriter(propertyIndex)
		map[name] = newWriter
		if (!propertyIndex.contains(name))
			propertyIndex.add(name)
		return newWriter
	}

	override fun element(): Writer {
		if (type != BinaryType.ARRAY) throw Exception("Writer is not type OBJECT")
		@Suppress("UNCHECKED_CAST")
		val list = _value as ArrayList<BinaryWriter>
		val newWriter = BinaryWriter(propertyIndex)
		list.add(newWriter)
		return newWriter
	}

	override fun writeNull() {
		type = BinaryType.NULL
		_value = null
	}

	override fun byte(value: Byte?) {
		type = BinaryType.BYTE
		_value = value
	}

	override fun bool(value: Boolean?) {
		if (value == null) return writeNull()
		type = BinaryType.BOOLEAN
		_value = value
	}

	override fun string(value: String?) {
		if (value == null) return writeNull()
		type = BinaryType.STRING
		_value = value
	}

	override fun int(value: Int?) {
		if (value == null) return writeNull()
		type = BinaryType.INT
		_value = value
	}

	override fun long(value: Long?) {
		if (value == null) return writeNull()
		type = BinaryType.LONG
		_value = value
	}

	override fun float(value: Float?) {
		if (value == null) return writeNull()
		type = BinaryType.FLOAT
		_value = value
	}

	override fun double(value: Double?) {
		if (value == null) return writeNull()
		type = BinaryType.DOUBLE
		_value = value
	}

	override fun char(value: Char?) {
		if (value == null) return writeNull()
		type = BinaryType.CHAR
		_value = value
	}

	override fun obj(complex: Boolean, contents: (Writer) -> Unit) {
		type = BinaryType.OBJECT
		_value = stringMapOf<BinaryWriter>()
		contents(this)
	}

	override fun array(complex: Boolean, contents: (Writer) -> Unit) {
		type = BinaryType.ARRAY
		_value = ArrayList<BinaryWriter>()
		contents(this)
	}

	override fun byteArray(value: ByteArray?) {
		if (value == null) return writeNull()
		type = BinaryType.BYTE_ARRAY
		_value = value
	}

	fun write(data: WriteByteBuffer) {
		data.put(type)
		when (type) {
			BinaryType.NULL -> {
			}
			BinaryType.BOOLEAN -> {
				data.put(if (_value == true) 1 else 0)
			}
			BinaryType.BYTE -> data.put(_value as Byte)
			BinaryType.CHAR -> data.putChar16(_value as Char)
			BinaryType.SHORT -> data.putShort(_value as Short)
			BinaryType.INT -> data.putInt(_value as Int)
			BinaryType.FLOAT -> data.putFloat(_value as Float)
			BinaryType.LONG -> data.putLong(_value as Long)
			BinaryType.DOUBLE -> data.putDouble(_value as Double)
			BinaryType.STRING -> data.putString16(_value as String)
			BinaryType.ARRAY -> {
				@Suppress("UNCHECKED_CAST")
				val list = _value as List<BinaryWriter>
				data.putShort(list.size.toShort())
				for (i in 0..list.lastIndex)
					list[i].write(data)
			}
			BinaryType.OBJECT -> {
				@Suppress("UNCHECKED_CAST")
				val map = (_value as Map<String, BinaryWriter>)
				data.putShort(map.size.toShort())
				for (entry in map) {
					val i = propertyIndex.indexOf(entry.key)
					if (i == -1) throw Exception("${entry.key} not found in the property index")
					data.putShort(i.toShort())
					entry.value.write(data)
				}
			}
			BinaryType.BYTE_ARRAY -> {
				val bytes = _value as ByteArray
				data.putInt(bytes.size)
				for (i in 0..bytes.lastIndex) {
					data.put(bytes[i])
				}
			}
			else -> throw Exception("Unknown data type: $type")
		}
	}
}

@Deprecated("use kotlinx serialization")
object BinaryType {
	const val UNKNOWN: Byte = 0
	const val NULL: Byte = 1
	const val BOOLEAN: Byte = 2
	const val BYTE: Byte = 3
	const val CHAR: Byte = 4
	const val SHORT: Byte = 5
	const val INT: Byte = 6
	const val FLOAT: Byte = 7
	const val LONG: Byte = 8
	const val DOUBLE: Byte = 9
	const val STRING: Byte = 10
	const val ARRAY: Byte = 11
	const val OBJECT: Byte = 12
	const val BYTE_ARRAY: Byte = 13
}

@Deprecated("use kotlinx serialization")
fun <T> binaryParse(binary: ReadByteBuffer, factory: From<T>): T {
	return BinarySerializer.read(binary, factory)
}

@Deprecated("use kotlinx serialization")
fun <T> toBinary(value: T, factory: To<T>): ReadByteBuffer {
	return BinarySerializer.write(value, factory)
}

private val cbor = Cbor(context = dataModule)

fun <T> binaryParse(deserializer: DeserializationStrategy<T>, binary: ByteArray): T {
	return cbor.load(deserializer, binary)
}

fun <T> binaryDump(serializer: SerializationStrategy<T>, value: T): ByteArray {
	return cbor.dump(serializer, value)
}