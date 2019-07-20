plugins {
	java
}

group = "com.acornui.skins"

sourceSets {
	main {
		resources {
			setSrcDirs(listOf("resources"))
		}
	}
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			groupId = group.toString()
			artifactId = "basic"
			version = version.toString()

			from(components["java"])
		}
	}
}