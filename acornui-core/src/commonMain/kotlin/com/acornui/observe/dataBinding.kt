@file:JvmName("DataBindingUtils")

package com.acornui.observe

import com.acornui.di.Owned
import com.acornui.di.own
import com.acornui.observe.DataBindingImpl
import kotlin.jvm.JvmName

fun <T> Owned.dataBinding(initialValue: T): DataBindingImpl<T> {
	return own(DataBindingImpl(initialValue))
}