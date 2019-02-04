///*
// * Copyright 2015 Nicholas Bilyk
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.acornui.core.state
//
//import com.acornui.collection.peek
//import com.acornui.collection.pop
//import com.acornui.core.Drivable
//import com.acornui.core.LifecycleBase
//import com.acornui.core.Renderable
//import java.util.ArrayList
//
///**
// * @author nbilyk
// */
//open class StateStack<T : Drivable> : LifecycleBase(), Renderable {
//
//	protected val stateStack: MutableList<T> = ArrayList()
//	private var currentState: T? = null
//	private var isDirty: Boolean = false
//
//	fun contains(state: T): Boolean {
//		return stateStack.indexOf(state) != -1
//	}
//
//	fun push(state: T) {
//		stateStack.add(state)
//		isDirty = true
//	}
//
//	fun pop() {
//		if (stateStack.isEmpty()) return
//		// Remove the last state.
//		stateStack.pop()
//		isDirty = true
//	}
//
//	private fun setCurrentState(state: T?) {
//		if (currentState == state) return
//		if (currentState != null) {
//			stopState(currentState!!)
//			if (stateStack.indexOf(currentState!!) == -1) {
//				disposeState(currentState!!)
//			}
//			currentState = null
//		}
//		currentState = state
//		if (currentState != null) {
//			startState(currentState!!)
//		}
//	}
//
//	protected fun stopState(state: T) {
//		if (state.isActive) {
//			state.deactivate()
//		}
//	}
//
//	protected fun startState(state: T) {
//		if (!state.isActive) {
//			state.activate()
//		}
//	}
//
//	protected fun disposeState(state: T) {
//		if (state.isActive) {
//			state.deactivate()
//		}
//		if (!state.isDisposed) {
//			state.dispose()
//		}
//	}
//
//	override fun update() {
//		if (isDirty) {
//			isDirty = false
//			if (stateStack.isEmpty())
//				setCurrentState(null)
//			else
//				setCurrentState(stateStack.peek())
//		}
//		if (currentState != null) {
//			if (currentState!!.isActive) {
//				currentState!!.update()
//			}
//			if (currentState != null && currentState!!.isDisposed) {
//				isDirty = true
//				stateStack.remove(currentState!!)
//			}
//		}
//	}
//
//	override fun render() {
//		if (currentState != null) {
//			if (currentState!!.isActive) {
//				currentState!!.render()
//			}
//		}
//	}
//
//	override fun onActivated() {
//		setCurrentState(stateStack.first())
//	}
//
//	override fun onDeactivated() {
//		setCurrentState(null)
//	}
//
//	fun clear() {
//		isDirty = true
//		for (state in stateStack) {
//			disposeState(state)
//		}
//		stateStack.clear()
//	}
//
//	override fun dispose() {
//		super.dispose()
//		clear()
//		setCurrentState(null)
//	}
//}
