plugins {
    kotlin("multiplatform")
    `maven-publish`
}

val KOTLIN_LANGUAGE_VERSION: String by extra
val KOTLIN_JVM_TARGET: String by extra
kotlin {
    js {
        compilations.named("test") {
            runtimeDependencyFiles
        }
        compilations.all {
            kotlinOptions {
                moduleKind = "amd"
                sourceMap = true
                sourceMapEmbedSources = "always"
                main = "noCall"
            }
        }
    }
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = KOTLIN_JVM_TARGET
            }
        }
    }
    targets.all {
        compilations.all {
            kotlinOptions {
                languageVersion = KOTLIN_LANGUAGE_VERSION
                apiVersion = KOTLIN_LANGUAGE_VERSION
                verbose = true
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(project(":acornui-core"))
                implementation(project(":acornui-utils"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        named("jvmMain") {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        named("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        named("jsMain") {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        named("jsTest") {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
