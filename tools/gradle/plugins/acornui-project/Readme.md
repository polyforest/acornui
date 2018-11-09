# How to get plugin on classpath
```
buildscript {
	val acornConfig: Map<String, String> = gradle.startParameter.projectProperties
	val ACORNUI_GROUP by acornConfig

	repositories {
		jcenter()
	}

	dependencies {
		classpath("$ACORNUI_GROUP:acornui-project:1.0")
		classpath("$ACORNUI_GROUP:acornui-texturepack:1.0")
	}
}
```

# How to apply plugin
```
plugins {
	id("com.polyforest.acornui-project")
	id("com.polyforest.acornui-texturepack")
}
```
