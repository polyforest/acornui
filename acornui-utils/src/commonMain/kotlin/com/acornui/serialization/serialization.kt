/*
 * Copyright 2015 Nicholas Bilyk
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

import com.acornui.collection.ArrayIterator
import com.acornui.collection.stringMapOf


interface Serializer<T> {

	fun read(data: T): Reader

	fun write(callback: (writer: Writer) -> Unit): T

	/**
	 * Creates a Reader for the specified data, and passes it to the <code>out</code> object.
	 * @return Returns the now-populated out object for chaining.
	 */
	fun <E> read(data: T, factory: From<E>): E {
		val reader = read(data)
		return reader.obj(factory)!!
	}

}

/**
 * Creates a Writer and passes it to the given To value.
 * Returns the data the Serializer produced.
 */
fun <E, T> Serializer<T>.write(value: E, to: To<E>): T {
	return write { it ->
		it.obj(true) { it2 ->
			to.write2(value, it2)
		}
	}
}

interface Reader {

	val isNull: Boolean

	fun contains(name: String): Boolean {
		return properties().contains(name)
	}

	fun contains(index: Int): Boolean {
		return index < elements().size
	}

	operator fun get(name: String): Reader? = properties()[name]
	operator fun get(index: Int): Reader? = elements()[index]

	fun byte(): Byte?
	fun bool(): Boolean?
	fun int(): Int?
	fun string(): String?
	fun short(): Short?
	fun long(): Long?
	fun float(): Float?
	fun double(): Double?
	fun char(): Char?
	fun byteArray(): ByteArray?

	fun properties(): Map<String, Reader>
	fun elements(): List<Reader>
}

inline fun Reader.contains(name: String, callback: (Reader) -> Unit) {
	if (contains(name)) {
		callback(get(name)!!)
	}
}

inline fun Reader.contains(index: Int, callback: (Reader) -> Unit) {
	if (contains(index)) {
		callback(get(index)!!)
	}
}

// Convenience methods for reading arrays and objects.

fun Reader.forEach(action: (key: String, child: Reader) -> Unit) {
	for (element in properties())
		action(element.key, element.value)
}

fun <T> Reader.obj(factory: From<T>): T? {
	if (isNull) return null
	return factory.read(this)
}

fun <T> Reader.map(itemFactory: From<T>): Map<String, T>? {
	val hashMap = stringMapOf<T>()
	forEach {
		propName, reader ->
		hashMap[propName] = itemFactory.read(reader)
	}
	return hashMap
}

fun Reader.boolArray(): BooleanArray? {
	if (isNull) return null
	val elements = elements()
	val arr = BooleanArray(elements.size)
	for (i in 0..elements.lastIndex) {
		arr[i] = elements[i].bool()!!
	}
	return arr
}

fun Reader.charArray(): CharArray? {
	if (isNull) return null
	val elements = elements()
	val arr = CharArray(elements.size)
	for (i in 0..elements.lastIndex) {
		arr[i] = elements[i].char()!!
	}
	return arr
}

fun Reader.intArray(): IntArray? {
	if (isNull) return null
	val elements = elements()
	val arr = IntArray(elements.size)
	for (i in 0..elements.lastIndex) {
		arr[i] = elements[i].int()!!
	}
	return arr
}

fun Reader.longArray(): LongArray? {
	if (isNull) return null
	val elements = elements()
	val arr = LongArray(elements.size)
	for (i in 0..elements.lastIndex) {
		arr[i] = elements[i].long()!!
	}
	return arr
}

fun Reader.floatArray(): FloatArray? {
	if (isNull) return null
	val elements = elements()
	val arr = FloatArray(elements.size)
	for (i in 0..elements.lastIndex) {
		arr[i] = elements[i].float()!!
	}
	return arr
}

fun Reader.doubleArray(): DoubleArray? {
	if (isNull) return null
	val elements = elements()
	val arr = DoubleArray(elements.size)
	for (i in 0..elements.lastIndex) {
		arr[i] = elements[i].double()!!
	}
	return arr
}

fun Reader.stringArray(): Array<String?>? {
	if (isNull) return null
	val elements = elements()
	return Array(elements.size) {
		val element = elements[it]
		if (element.isNull) null
		else element.string()
	}
}

fun Reader.shortArray(): ShortArray? {
	if (isNull) return null
	val elements = elements()
	val arr = ShortArray(elements.size)
	for (i in 0..elements.lastIndex) {
		arr[i] = elements[i].short()!!
	}
	return arr
}


/**
 * A simplified way to read an array from a reader, which uses a reified type parameter to construct the Array.
 */
inline fun <reified T> Reader.array2(name: String, itemFactory: From<T>): Array<T>? = get(name)?.array2(itemFactory)

inline fun <reified T> Reader.array2(itemFactory: From<T>): Array<T>? {
	val e = elements()
	return Array(e.size) {
		itemFactory.read(e[it])
	}
}

/**
 * A simplified way to read an array from a reader, which uses a reified type parameter to construct the Array.
 */
inline fun <reified T> Reader.arrayWithNulls(name: String, itemFactory: From<T>): Array<T?>? = get(name)?.arrayWithNulls(itemFactory)

inline fun <reified T> Reader.arrayWithNulls(itemFactory: From<T>): Array<T?>? {
	val e = elements()
	return Array(e.size) {
		val itR = e[it]
		if (itR.isNull) null else itemFactory.read(itR)
	}
}

fun <E> Reader.arrayList(name: String, itemFactory: From<E>): ArrayList<E>? = get(name)?.arrayList(itemFactory)

fun <E> Reader.arrayList(itemFactory: From<E>): ArrayList<E> {
	val e = elements()
	val list = ArrayList<E>(e.size)
	for (i in 0..e.lastIndex) {
		list.add(itemFactory.read(e[i]))
	}
	return list
}

/**
 * Reads a sparse array in the following format:
 * [size, indexN, objectN, ...]
 */
inline fun <reified E : Any> Reader.sparseArray(itemFactory: From<E>): Array<E?>? {
	val e = elements()
	val n = e[0].int()!!
	val arr = arrayOfNulls<E?>(n)
	for (i in 1..e.lastIndex step 2) {
		val index = e[i].int()!!
		arr[index] = itemFactory.read(e[i + 1])
	}
	return arr
}

inline fun <reified E : Any> Reader.sparseArray(name: String, itemFactory: From<E>): Array<E?>? = get(name)?.sparseArray(itemFactory)

// Convenience methods for reading child properties:

fun Reader.bool(name: String): Boolean? = get(name)?.bool()
fun Reader.string(name: String): String? = get(name)?.string()
fun Reader.int(name: String): Int? = get(name)?.int()
fun Reader.long(name: String): Long? = get(name)?.long()
fun Reader.float(name: String): Float? = get(name)?.float()
fun Reader.double(name: String): Double? = get(name)?.double()
fun Reader.char(name: String): Char? = get(name)?.char()
fun Reader.byteArray(name: String): ByteArray? = get(name)?.byteArray()
fun Reader.boolArray(name: String): BooleanArray? = get(name)?.boolArray()
fun Reader.stringArray(name: String): Array<String?>? = get(name)?.stringArray()
fun Reader.shortArray(name: String): ShortArray? = get(name)?.shortArray()
fun Reader.intArray(name: String): IntArray? = get(name)?.intArray()
fun Reader.longArray(name: String): LongArray? = get(name)?.longArray()
fun Reader.floatArray(name: String): FloatArray? = get(name)?.floatArray()
fun Reader.doubleArray(name: String): DoubleArray? = get(name)?.doubleArray()
fun Reader.charArray(name: String): CharArray? = get(name)?.charArray()
fun <T> Reader.obj(name: String, factory: From<T>): T? = get(name)?.obj(factory)
fun <T> Reader.map(name: String, itemFactory: From<T>): Map<String, T>? = get(name)?.map(itemFactory)

interface Writer {

	fun property(name: String): Writer
	fun element(): Writer

	fun writeNull()
	fun byte(value: Byte?)
	fun bool(value: Boolean?)
	fun string(value: String?)
	fun int(value: Int?)
	fun long(value: Long?)
	fun float(value: Float?)
	fun double(value: Double?)
	fun char(value: Char?)
	fun byteArray(value: ByteArray?)

	fun obj(complex: Boolean, contents: (Writer) -> Unit)
	fun array(complex: Boolean, contents: (Writer) -> Unit)
}

// Convenience methods for writing arrays and objects.

fun <T> Writer.map(value: Map<String, T?>?, to: To<T>) {
	if (value == null) writeNull()
	else {
		obj(true) {
			for (entry in value) {
				val p = it.property(entry.key)
				if (entry.value == null) p.writeNull()
				else {
					p.obj(true) { it2 ->
						to.write2(entry.value!!, it2)
					}
				}
			}
		}
	}
}

fun <T> Writer.map(value: Map<String, T?>?, writeProp: (T, Writer) -> Unit) {
	if (value == null) writeNull()
	else {
		obj(true) {
			for (entry in value) {
				val p = it.property(entry.key)
				val v = entry.value
				if (v == null) p.writeNull()
				else {
					writeProp(v, p)
				}
			}
		}
	}
}

fun <T> Writer.obj(value: T?, to: To<T>) {
	if (value == null) writeNull()
	else {
		obj(true) {
			to.write2(value, it)
		}
	}
}

fun <T : Any> Writer.array(value: Array<out T?>?, to: To<T>) {
	if (value == null) writeNull()
	else array(ArrayIterator(value), to)
}

fun <T : Any> Writer.array(value: Iterable<T?>?, to: To<T>) {
	if (value == null) writeNull()
	else {
		array(true) {
			for (v in value) {
				if (v == null) it.element().writeNull()
				else {
					it.element().obj(true) { it2 ->
						to.write2(v, it2)
					}
				}
			}
		}
	}
}

fun Writer.array(value: Iterable<String?>?) {
	if (value == null) writeNull()
	else {
		array(true) {
			for (v in value) {
				if (v == null) it.element().writeNull()
				else it.element().string(v)
			}
		}
	}
}

/**
 * Writes a sparse array in the following format:
 * [size, indexN, objectN, ...]
 */
fun <T : Any> Writer.sparseArray(value: Array<T?>?, to: To<T>) {
	if (value == null) writeNull()
	else {
		array(true) {
			it.element().int(value.size)
			for (i in 0..value.lastIndex) {
				val v = value[i]
				if (v != null) {
					it.element().int(i)
					it.element().obj(true) {
						to.write2(v, it)
					}
				}
			}
		}
	}
}

fun Writer.boolArray(value: BooleanArray?) {
	if (value == null) writeNull()
	else {
		array(false) {
			for (b in value) {
				it.element().bool(b)
			}
		}
	}
}

fun Writer.stringArray(value: Array<out String?>?) {
	if (value == null) writeNull()
	else {
		array(false) {
			for (i in value) {
				it.element().string(i)
			}
		}
	}
}

fun Writer.intArray(value: IntArray?) {
	if (value == null) writeNull()
	else {
		array(false) {
			for (i in value) {
				it.element().int(i)
			}
		}
	}
}

fun Writer.longArray(value: LongArray?) {
	if (value == null) writeNull()
	else {
		array(false) {
			for (i in value) {
				it.element().long(i)
			}
		}
	}
}

fun Writer.floatArray(value: FloatArray?) {
	if (value == null) writeNull()
	else {
		array(false) {
			for (i in value) {
				it.element().float(i)
			}
		}
	}
}

fun Writer.doubleArray(value: DoubleArray?) {
	if (value == null) writeNull()
	else {
		array(false) {
			for (i in value) {
				it.element().double(i)
			}
		}
	}
}

fun Writer.charArray(value: CharArray?) {
	if (value == null) writeNull()
	else {
		array(false) {
			for (i in value) {
				it.element().char(i)
			}
		}
	}
}

// Convenience methods for writing child properties:

fun Writer.enum(value: Enum<*>?) = if (value == null) writeNull() else string(value.name)
fun Writer.enum(name: String, value: Enum<*>?) = property(name).enum(value)
fun Writer.bool(name: String, value: Boolean?) = property(name).bool(value)
fun Writer.string(name: String, value: String?) = property(name).string(value)
fun Writer.int(name: String, value: Int?) = property(name).int(value)
fun Writer.long(name: String, value: Long?) = property(name).long(value)
fun Writer.float(name: String, value: Float?) = property(name).float(value)
fun Writer.double(name: String, value: Double?) = property(name).double(value)
fun Writer.char(name: String, value: Char?) = property(name).char(value)
fun Writer.byteArray(name: String, value: ByteArray?) = property(name).byteArray(value)
fun Writer.boolArray(name: String, value: BooleanArray?) = property(name).boolArray(value)
fun Writer.stringArray(name: String, value: Array<out String?>?) = property(name).stringArray(value)
fun Writer.intArray(name: String, value: IntArray?) = property(name).intArray(value)
fun Writer.longArray(name: String, value: LongArray?) = property(name).longArray(value)
fun Writer.floatArray(name: String, value: FloatArray?) = property(name).floatArray(value)
fun Writer.doubleArray(name: String, value: DoubleArray?) = property(name).doubleArray(value)
fun Writer.charArray(name: String, value: CharArray?) = property(name).charArray(value)

fun Writer.obj(name: String, complex: Boolean, contents: (Writer) -> Unit) = property(name).obj(complex, contents)
fun <T> Writer.obj(name: String, value: T?, to: To<T>) = property(name).obj(value, to)
fun <T : Any> Writer.array(name: String, value: Array<out T?>?, to: To<T>) = property(name).array(value, to)
fun <T : Any> Writer.array(name: String, value: Iterable<T?>?, to: To<T>) = property(name).array(value, to)
fun <T : Any> Writer.sparseArray(name: String, value: Array<T?>?, to: To<T>) = property(name).sparseArray(value, to)
fun <T> Writer.map(name: String, value: Map<String, T?>?, to: To<T>) = property(name).map(value, to)

/**
 * The serialization interface to read from a [Reader] object, producing a new instance.
 */
interface From<out T> {
	fun read(reader: Reader): T
}

/**
 * The serialization interface to write to a [Writer] object.
 */
interface To<in T> {

	fun write2(receiver: T, writer: Writer) = receiver.write(writer)

	fun T.write(writer: Writer)
}