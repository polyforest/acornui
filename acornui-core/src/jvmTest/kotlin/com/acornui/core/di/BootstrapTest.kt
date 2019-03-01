package com.acornui.core.di

import com.acornui.async.launch
import kotlin.test.Test
import kotlin.test.assertNotNull

class BootstrapTest {
	@Test fun set() {
		val b = Bootstrap()
		launch {
			b.set(Dep2, Dep2(b.get(Dep1)))
			assertNotNull(b.createInjector().injectOptional(Dep1))
			assertNotNull(b.createInjector().injectOptional(Dep2))
		}
		b.set(Dep1, Dep1())
	}

}

private class Dep1 {
	companion object : DKey<Dep1>
}

private class Dep2(dep1: Dep1) {
	companion object : DKey<Dep2>
}

private class Dep3(dep1: Dep1, dep2: Dep2) {
	companion object : DKey<Dep3>
}