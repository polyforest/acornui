package com.acornui.io.file

import org.junit.Ignore
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class ManifestUtilTest {

	@Ignore("KT-24463")
	@Test
	fun createManifest() {
		val manifest = ManifestUtil.createManifest(File("src/jvmTest/resources"))
		assertEquals(6, manifest.files.size)
		assertEquals(FilesManifest(files = listOf(
				ManifestEntry(path="testAssets/a.txt", modified=1564092620598, size=0, mimeType="text/plain"),
				ManifestEntry(path="testAssets/b.txt", modified=1564092671775, size=0, mimeType="text/plain"),
				ManifestEntry(path="testAssets/testA/a_2.txt", modified=1564092663774, size=0, mimeType="text/plain"),
				ManifestEntry(path="testAssets/testA/b_2.txt", modified=1564092654798, size=0, mimeType="text/plain"),
				ManifestEntry(path="testAssets/testA/testB/a_3.txt", modified=1564092663774, size=0, mimeType="text/plain"),
				ManifestEntry(path="testAssets/testA/testB/b_3.txt", modified=1564092654798, size=0, mimeType="text/plain")
		)), manifest)
	}
}