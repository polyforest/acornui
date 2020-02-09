package com.acornui.observe

import com.acornui.di.Context
import com.acornui.di.own

fun <T> Context.dataBinding(initialValue: T): DataBindingImpl<T> {
	return own(DataBindingImpl(initialValue))
}