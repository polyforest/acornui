package com.acornui.build

import com.acornui.collection.setTo
import com.acornui.logging.ArrayTarget
import com.acornui.logging.Logger
import com.acornui.logging.Log
import com.acornui.test.assertListEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TaskKtTest {

	private val taskOutput = arrayListOf<Any>()
	private val arrayTarget = ArrayTarget()
	private val logWarnOutput: List<String>
		get() = arrayTarget.list.filter { it.level == Logger.WARN }.map { it.message }

	init {
		Log.targets.setTo(listOf(arrayTarget))
	}

	@Before
	fun before() {
		taskOutput.clear()
		arrayTarget.clear()
	}

	@Test
	fun duplicateSet() {

		class Config {

			@ConfigProp
			var foo by Freezable("foo")

		}

		class Model
		class Tasks

		val c = Config()
		runCommands(arrayOf("--foo=Test", "--foo=Test2"), { c }, { Model() }, { Tasks() })
		assertEquals("Test", c.foo)
		assertEquals(1, logWarnOutput.size)

		AssertionLevels.unknownProperty = AssertionLevel.ERROR
		assertFailsWith<CliException> {
			runCommands(arrayOf("--foo=Test", "--foo=Test"), { Config() }, { Model() }, { Tasks() })
		}
	}

	@Test
	fun suspendingTask() {

		class Model
		class Tasks {

			@Task
			suspend fun hello() {
				taskOutput.add("Hello")
			}
		}

		runCommands(arrayOf("hello"), { Model() }, { Tasks() })
		assertListEquals(listOf("Hello"), taskOutput)
	}

	@Test
	fun deepConfig() {

		class B {

			@ConfigProp
			var foo by Freezable("foo")

			@ConfigProp
			var baz by Freezable(3)
		}

		class A {

			@ConfigProp
			var foo by Freezable("foo")

			@ConfigProp
			var bar by Freezable(3)

			@ConfigProp("Properties for B")
			val b = B()
		}

		class Model
		class Tasks

		val a = A()
		runCommands(arrayOf("--b.foo=Test"), { a }, { Model() }, { Tasks() })
		assertEquals("Test", a.b.foo)
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

		assertFailsWith<ConfigurationException> {
			runCommands(arrayOf(""), { Config() }, { Model() }, { Tasks() })
		}
	}

	@Test
	fun missingSubject() {
		class Config {

			@ConfigProp
			var foo by Freezable("foo")

			@ConfigProp
			var bar by Freezable(3)
		}

		class Model
		class Tasks {

			@Task
			fun String.hello() {}
		}

		AssertionLevels.unknownSubject = AssertionLevel.ERROR
		assertFailsWith<CliException> {
			runCommands(arrayOf("hello", ":foo"), { Config() }, { Model() }, { Tasks() })
		}
	}

	@Test
	fun duplicateSubjectAlias() {

		class Config {

			@ConfigProp("Sets the Foo level", alias = "duplicate")
			var foo by Freezable("foo")

			@ConfigProp("Sets the Bar level", alias = "duplicate")
			var bar by Freezable(3)
		}

		class Model
		class Tasks

		assertFailsWith<ConfigurationException> {
			runCommands(arrayOf(""), { Config() }, { Model() }, { Tasks() })
		}
	}

	/**
	 * Assert that objects are deep-frozen when calling [freeze].
	 */
	@Test
	fun deepFreezeTest() {

		class A {

			val _foo = Freezable("foo")
			@ConfigProp("Sets the Foo level")
			var foo by _foo
		}

		class B {

			val _foo = Freezable("foo")
			@ConfigProp("Sets the Foo level")
			var foo by _foo

			val _bar = Freezable(3)
			@ConfigProp("Sets the Bar level")
			var bar by _bar

			val _baz = Freezable(true, required = true)
			@ConfigProp("Sets the Baz level")
			var baz by _baz

			val _a = Freezable(A())
			@ConfigProp("Sets the A level")
			var a by _a
		}

		val c = B()
		c.baz = true
		c.freeze()
		assertTrue(c._foo.frozen)
		assertTrue(c._bar.frozen)
		assertTrue(c._baz.frozen)
		assertTrue(c.a._foo.frozen)

		assertFails {
			c.foo = "test"
		}
		assertFails {
			c.a.foo = "test"
		}
	}

	/**
	 * Check that freezing before required properties are set causes an error.
	 */
	@Test
	fun freezeBeforeRequired() {

		class A {

			val _foo = Freezable("foo")
			@ConfigProp("Sets the Foo level")
			var foo by _foo

			val _baz = Freezable(true, required = true)
			@ConfigProp("Sets the Baz level")
			var baz by _baz
		}

		class B {

			val _foo = Freezable("foo")
			@ConfigProp("Sets the Foo level")
			var foo by _foo

			val _bar = Freezable(3)
			@ConfigProp("Sets the Bar level")
			var bar by _bar

			val _baz = Freezable(true, required = true)
			@ConfigProp("Sets the Baz level")
			var baz by _baz

			val _a = Freezable(A())
			@ConfigProp("Sets the A level")
			var a by _a
		}

		val c = B()
		assertFailsWith<CliException> {
			c.freeze()
		}
	}

	@Test
	fun duplicateTaskParameterAliases() {

		class Tasks {

			@Task
			fun foo1(@TaskArgument(alias = "duplicate") arg1: Int, @TaskArgument(alias = "duplicate") arg2: Int) {}
		}

		assertFailsWith<ConfigurationException> {
			runCommands(tasksProvider = { Tasks() })
		}
	}

	/**
	 * Test that model subjects are matched to the most-specific receiver possible.
	 */
	@Test
	fun subjectPriority() {

		open class SuperType(val name: String)
		class SubType(name: String) : SuperType(name)

		class Model {
			@ModelProp
			val subjectA = SubType("subjectA")

			@ModelProp
			val subjectB = SuperType("subjectB")
		}

		class Tasks {

			// First list SubType before SuperType

			@Task
			fun SubType.testA() {
				taskOutput.add("SubType.testA $name")
			}

			@Task
			fun SuperType.testA() {
				taskOutput.add("SuperType.testA $name")
			}

			// Then SuperType before SubType

			@Task
			fun SuperType.testB() {
				taskOutput.add("SuperType.testB $name")
			}

			@Task
			fun SubType.testB() {
				taskOutput.add("SubType.testB $name")
			}
		}

		runCommands(
				args = listOf("testA", "testB"),
				modelProvider = { Model() },
				tasksProvider = { Tasks() }
		)

		assertListEquals(listOf(
				"SubType.testA subjectA",
				"SuperType.testA subjectB",
				"SubType.testB subjectA",
				"SuperType.testB subjectB"
		), taskOutput)


	}

	@Test
	fun runCommandsTest() {

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

//		runCommands(arrayOf("--foo=Hello World", "--enumTest=BYE", "--bool", "hello"), Config::class, Model::class, Tasks::class)

		val cliArgs = arrayOf("--foo=Hello World", "--enumTest=BYE", "--bool", "hello", "-lang=de_DE")

		runCommands(
				cliArgs,
				{ Config() },
				{ Model(it) },
				{ Tasks(it) }
		)


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

	@Test
	fun idempotence() = runBlocking {

		class Tasks {

			suspend fun hello(msg: String) = idempotent {
				taskOutput.add(msg)
			}

			suspend fun Any.hello(msg: String) = idempotent {
				taskOutput.add(msg + "$this")
			}

			suspend fun bye(arg0: Int, arg1: String): Int {
				taskOutput.add("bye non-idempotent $arg0 $arg1")
				return idempotent {
					taskOutput.add("bye idempotent $arg0 $arg1")
					arg0 + 1
				}
			}
		}

		val t = Tasks()

		t.hello("Hi")
		t.hello("Hi")
		t.hello("Bye")
		t.hello("Bye")
		t.hello("Hi")
		t.hello("Bye")

		assertListEquals(listOf("Hi", "Bye"), taskOutput)
		taskOutput.clear()

		class Foo(val m: String) {
			override fun toString(): String = m
		}

		t.apply {
			val f = Foo("5")
			3.hello("Hi")
			4.hello("Hi")
			3.hello("Hi")
			3.hello("Hi")
			4.hello("Hi")
			f.hello("Hi")
			f.hello("Hi")
			f.hello("Hi")
			assertListEquals(listOf("Hi3", "Hi4", "Hi5"), taskOutput)
		}

		taskOutput.clear()
		assertEquals(1, t.bye(0, "hi"))
		assertEquals(2, t.bye(1, "hi"))
		assertEquals(2, t.bye(1, "hi"))
		assertEquals(1, t.bye(0, "hi"))
		assertEquals(1, t.bye(0, "bye"))

		assertListEquals(listOf(
				"bye non-idempotent 0 hi",
				"bye idempotent 0 hi",
				"bye non-idempotent 1 hi",
				"bye idempotent 1 hi",
				"bye non-idempotent 1 hi",
				"bye non-idempotent 0 hi",
				"bye non-idempotent 0 bye",
				"bye idempotent 0 bye"
		), taskOutput)

	}

	@Test
	fun idempotenceSameArgs() = runBlocking {

		class Tasks {

			suspend fun a() = idempotent {
				taskOutput.add(1)
			}

			suspend fun b() = idempotent {
				taskOutput.add(2)
			}
		}

		val t = Tasks()
		t.a()
		t.a()
		t.b()
		t.b()

		assertListEquals(listOf(1, 2), taskOutput)
	}

}

enum class Foo {
	HI,
	BYE
}