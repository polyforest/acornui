@file:JvmName("DataBindingCoreUtils")

package com.acornui.observe

import com.acornui.di.Owned
import com.acornui.di.own
import kotlin.jvm.JvmName

fun <T> Owned.dataBinding(initialValue: T): DataBindingImpl<T> {
	return own(DataBindingImpl(initialValue))
}