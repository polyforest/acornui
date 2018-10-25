package com.acornui.test


import com.acornui.core.toUnderscoreCase
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

object SerializationCodeHelper {

	fun print(kClass: KClass<*>, jsonUsesUndercase: Boolean = false) {

		val simpleName = kClass.simpleName

		val simpleMap = mapOf(
				Pair("kotlin.Boolean", "bool"),
				Pair("kotlin.Int", "int"),
				Pair("kotlin.String", "string"),
				Pair("kotlin.Long", "long"),
				Pair("kotlin.Float", "float"),
				Pair("kotlin.Double", "double"),
				Pair("kotlin.Char", "char"),
				Pair("kotlin.Int", "int"),
				Pair("kotlin.BoolArray", "boolArray"),
				Pair("kotlin.IntArray", "intArray"),
				Pair("kotlin.ShortArray", "shortArray"),
				Pair("kotlin.LongArray", "longArray"),
				Pair("kotlin.FloatArray", "floatArray"),
				Pair("kotlin.DoubleArray", "doubleArray"),
				Pair("kotlin.CharArray", "charArray"),
				Pair("com.acornui.math.Vector2", "vector2"),
				Pair("com.acornui.math.Vector2Ro", "vector2"),
				Pair("com.acornui.math.Vector3", "vector3"),
				Pair("com.acornui.math.Vector3Ro", "vector3"),
				Pair("com.acornui.graphics.Color", "color"),
				Pair("com.acornui.graphics.ColorRo", "color")
		)

		val params = kClass.primaryConstructor?.parameters
		val paramNames = ArrayList<String>()
		if (params != null) {
			for (param in kClass.primaryConstructor!!.parameters) {
				paramNames.add(param.name ?: "")
			}
		}

		var writeProperties = ""
		var readProperties = ""
		var constructorParams = ""
		for (i in kClass.declaredMemberProperties) {
			var type = i.returnType.toString()
			var isNullable = false
			if (type.endsWith("?")) {
				isNullable = true
				type = type.substring(0, type.length - 1)
			}

			val kotlinName = i.name
			val jsonName = if (jsonUsesUndercase) i.name.toUnderscoreCase() else i.name
			val isConstructorParam = paramNames.contains(kotlinName)
			val writeStr: String
			var readStr: String
			if (simpleMap.containsKey(type)) {
				writeStr =  "writer.${simpleMap[type]}(\"$jsonName\", $kotlinName)"
				readStr = "$kotlinName = reader.${simpleMap[type]}(\"$jsonName\")"
			} else if (type.startsWith("kotlin.Array")) {
				val eType = type.substring("kotlin.Array".length + 1, type.length - 1)
				val eSimpleType = eType.substringAfterLast('.').trimEnd('?')
				writeStr =  "writer.array(\"$jsonName\", $kotlinName, ${eSimpleType}Serializer)"
				readStr = "$kotlinName = reader.array2(\"$jsonName\", ${eSimpleType}Serializer)"
			} else if (type.startsWith("java.util.ArrayList")) {
				val eType = type.substring("java.util.ArrayList".length + 1, type.length - 1)
				val eSimpleType = eType.substringAfterLast('.').trimEnd('?')
				writeStr =  "writer.array(\"$jsonName\", $kotlinName, ${eSimpleType}Serializer)"
				readStr = "$kotlinName = reader.arrayList(\"$jsonName\", ${eSimpleType}Serializer)"
			} else if (type.startsWith("kotlin.collections.MutableList")) {
				val eType = type.substring("kotlin.collections.MutableList".length + 1, type.length - 1)
				val eSimpleType = eType.substringAfterLast('.').trimEnd('?')
				writeStr =  "writer.array(\"$jsonName\", $kotlinName, ${eSimpleType}Serializer)"
				readStr = "$kotlinName = reader.arrayList(\"$jsonName\", ${eSimpleType}Serializer)"
			} else {
				var t = type
				val genericI = t.indexOf("<")
				if (genericI != -1) {
					t = t.substring(0, genericI)
				}
				t = t.substringAfterLast('.').trimEnd('?')
				writeStr =  "writer.obj(\"$jsonName\", $kotlinName, ${t}Serializer)"
				readStr = "$kotlinName = reader.obj(\"$jsonName\", ${t}Serializer)"
			}
			if (!isNullable)
				readStr += "!!"
			if (isConstructorParam) {
				if (constructorParams.isNotEmpty()) constructorParams += ","
				constructorParams += "\n\t\t\t$readStr"
			} else {
				readProperties += "\t\to.$readStr\n"
			}
			writeProperties += "\t\t$writeStr\n"
		}
		if (constructorParams.isNotEmpty()) constructorParams += "\n\t\t"
		readProperties = readProperties.trim()
		writeProperties = writeProperties.trim()

		println("""
object ${simpleName}Serializer : To<$simpleName>, From<$simpleName> {

	override fun read(reader: Reader): $simpleName {
		val o = $simpleName($constructorParams)
		$readProperties
		return o
	}

	override fun $simpleName.write(writer: Writer) {
		$writeProperties
	}
}
""")


	}
}