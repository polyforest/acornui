package com.acornui.build

import org.junit.Before
import org.junit.Test
import kotlin.test.assertFails

class TaskKtTest {

	private val outputLines = arrayListOf<String>()

	@Before
	fun before() {
		clear()
	}

	@Test
	fun duplicateConfigAliases() {

		class Config {

			@ConfigProp("Sets the Foo level", alias = "duplicate")
			var foo by Freezable("foo")

			@ConfigProp("Sets the Bar level", alias = "duplicate")
			var bar by Freezable(3)
		}

		class Model
		class Tasks

		assertFails {
			runCommands(arrayOf(""), Config::class, Model::class, Tasks::class)
		}
	}

	@Test
	fun duplicateTaskParameterAliases() {

		class Config
		class Model
		class Tasks {

			@Task
			fun foo1(@TaskArgument(alias = "duplicate") arg1: Int, @TaskArgument(alias = "duplicate") arg2: Int) {}
		}

		assertFails {
			runCommands(arrayOf(""), Config::class, Model::class, Tasks::class)
		}
	}

	@Test
	fun runCommands() {

		class Config {

			@ConfigProp("Sets the Foo level")
			var foo by Freezable("foo")

			@ConfigProp("Sets the Bar level")
			var bar by Freezable(3)

			@ConfigProp("Sets the enum level")
			var enumTest by Freezable(Foo.BYE)

			@ConfigProp("A bool test")
			var bool by Freezable(false)
		}

		class Model(private val config: Config) {

			@ModelProp("The testA model")
			val testA = 3

			@ModelProp("The testB model")
			val testB = 4
		}

		class Tasks(private val config: Config) {

			@Task(description = "Says hello.")
			fun hello(@TaskArgument(description = "The language to use.", alias = "lang") language: String = "en_US") {
				val greeting = when (language) {
					"en_US" -> "Hello Acorn no receiver"
					"de_DE" -> "Hallo Acorn no receiver"
					"fr_FR" -> "Bonjour Acorn no receiver"
					else -> throw Exception("Unknown language")
				}
				println(greeting)
			}

			@Task(description = "Says hello.")
			fun String.hello(@TaskArgument(description = "The language to use.", alias = "lang") language: String = "en_US") {
				val greeting = when (language) {
					"en_US" -> "Hello Acorn"
					"de_DE" -> "Hallo Acorn"
					"fr_FR" -> "Bonjour Acorn"
					else -> throw Exception("Unknown language")
				}
				println("$this $greeting")
			}
		}

//		runCommands(arrayOf("--foo=Hello World", "--enum-test=BYE", "--bool", "hello"), Config::class, Model::class, Tasks::class)
		runCommands(arrayOf("--foo=Hello World", "--enum-test=BYE", "--bool", "hello", "-lang=de_DE"), Config::class, Model::class, Tasks::class)


		//assertEquals("Foo Test", props.foo)
//		val freeArgs = arrayListOf("--foo=Test")
//		props.configure(freeArgs)
	}

	@Test
	fun tasks() {

		class Tasks {

			@Task(description = "Says hello")
			fun hello(@TaskArgument(description = "The language to use", alias = "lang") language: String = "en_US") {
				val greeting = when (language) {
					"en_US" -> "Hello Acorn"
					"de_DE" -> "Hallo Acorn"
					"fr_FR" -> "Bonjour Acorn"
					else -> throw Exception("Unknown language")
				}
				println(greeting)
			}
		}

	}

	private fun addLine(s: String) {
		outputLines.add(s)
	}

	private fun clear() {
		outputLines.clear()
	}
}

var last: (() -> Unit)? = null

fun idempotent(inner: () -> Unit) {
	if (last != null) {
		val lastR = inner::class.java.declaredFields
		val innerR = inner::class.java.declaredFields
		println(lastR[1].get(inner))
		println("" + lastR + " : " + innerR)

	}

	last = inner
	print("invoke inner ")
	inner()
}

enum class Foo {
	HI,
	BYE
}