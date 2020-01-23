package com.acornui.observe

import com.acornui.di.Owned
import com.acornui.di.own

fun <T> Owned.dataBinding(initialValue: T): DataBindingImpl<T> {
	return own(DataBindingImpl(initialValue))
}