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

package com.acornui

import com.acornui.component.TreeNode
import com.acornui.test.assertListEquals
import kotlin.test.BeforeTest
import kotlin.test.Test


/**
 * @author nbilyk
 */
class HierarchyTest {

	private lateinit var tree1: TestNode
	private lateinit var tree2: TestNode

	@BeforeTest fun before() {
		tree1 = node("a") {
			+node("a.a") {
				+node("a.a.a")
				+node("a.a.b")
			}
			+node("a.b") {
				+node("a.b.a")
				+node("a.b.b")
			}
			+node("a.c") {
				+node("a.c.a")
				+node("a.c.b")
				+node("a.c.c")
			}
		}

		tree2 = node("f") {
			+node("b") {
				+node("a")
				+node("d") {
					+node("c")
					+node("e")
				}
			}
			+node("g") {
				+node("i") {
					+node("h")
				}
			}
		}
	}

	@Test fun childWalkLevelOrder() {
		val list = ArrayList<String>()
		tree1.childWalkLevelOrder {
			it as TestNode
			list.add(it.id)
			TreeWalk.CONTINUE
		}
		assertListEquals(arrayOf("a", "a.a", "a.b", "a.c", "a.a.a", "a.a.b", "a.b.a", "a.b.b", "a.c.a", "a.c.b", "a.c.c"), list)

		list.clear()
		tree1.childWalkLevelOrder {
			child ->
			child as TestNode
			list.add(child.id)
			if (child.id == "a.c") {
				TreeWalk.SKIP
			} else {
				TreeWalk.CONTINUE
			}
		}
		assertListEquals(arrayOf("a", "a.a", "a.b", "a.c", "a.a.a", "a.a.b", "a.b.a", "a.b.b"), list)

		list.clear()
		tree1.childWalkLevelOrder {
			child ->
			child as TestNode
			list.add(child.id)
			if (child.id == "a.b") {
				TreeWalk.ISOLATE
			} else {
				TreeWalk.CONTINUE
			}
		}
		assertListEquals(arrayOf("a", "a.a", "a.b", "a.b.a", "a.b.b"), list)

		list.clear()
		tree2.childWalkLevelOrder {
			it as TestNode
			list.add(it.id)
			TreeWalk.CONTINUE
		}
		assertListEquals(arrayOf("f", "b", "g", "a", "d", "i", "c", "e", "h"), list)
	}

	@Test fun childWalkLevelOrderReversed() {
		val list = ArrayList<String>()
		tree1.childWalkLevelOrderReversed {
			child ->
			child as TestNode
			list.add(child.id)
			TreeWalk.CONTINUE
		}
		assertListEquals(arrayOf("a", "a.c", "a.b", "a.a", "a.c.c", "a.c.b", "a.c.a", "a.b.b", "a.b.a", "a.a.b", "a.a.a"), list)

		list.clear()
		tree1.childWalkLevelOrderReversed {
			child ->
			child as TestNode
			list.add(child.id)
			if (child.id == "a.c") {
				TreeWalk.SKIP
			} else {
				TreeWalk.CONTINUE
			}
		}
		assertListEquals(arrayOf("a", "a.c", "a.b", "a.a", "a.b.b", "a.b.a", "a.a.b", "a.a.a"), list)

		list.clear()
		tree1.childWalkLevelOrderReversed {
			child ->
			child as TestNode
			list.add(child.id)
			if (child.id == "a.b") {
				TreeWalk.ISOLATE
			} else {
				TreeWalk.CONTINUE
			}
		}
		assertListEquals(arrayOf("a", "a.c", "a.b", "a.b.b", "a.b.a"), list)

		list.clear()
		tree2.childWalkLevelOrderReversed {
			it as TestNode
			list.add(it.id)
			TreeWalk.CONTINUE
		}
		assertListEquals(arrayOf("f", "g", "b", "i", "d", "a", "h", "e", "c"), list)
	}

	@Test fun childWalkPreOrder() {
		val list = ArrayList<String>()
		tree2.childWalkPreorder {
			it as TestNode
			list.add(it.id)
			TreeWalk.CONTINUE
		}
		assertListEquals(arrayOf("f", "b", "a", "d", "c", "e", "g", "i", "h"), list)
	}


}

private class TestNode(val id: String) : NodeRo {

	/**
	 * Syntax sugar for addChild.
	 */
	operator fun TestNode.unaryPlus(): TestNode {
		this@TestNode.addChild(this@TestNode.children.size, this)
		return this
	}

	override var parent: TestNode? = null

	private val _children = ArrayList<TestNode>()
	override val children: List<TestNode>
		get() = _children

	fun addChild(index: Int, child: TestNode): TestNode {
		_children.add(index, child)
		child.parent = this
		return child
	}
	override fun hashCode(): Int {
		return 31 + id.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		return id == (other as? TestNode)?.id
	}
}

private fun node(id: String, init: TestNode.() -> Unit = {}): TestNode {
	val node = TestNode(id)
	node.init()
	return node
}