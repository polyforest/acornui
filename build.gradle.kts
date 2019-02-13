val GRADLE_VERSION: String by extra
val PRODUCT_VERSION: String by extra
val PRODUCT_GROUP: String by extra
allprojects {
    repositories {
        jcenter()
    }
    tasks.withType<Wrapper> {
        gradleVersion = GRADLE_VERSION
        distributionType = Wrapper.DistributionType.ALL
    }
    version = PRODUCT_VERSION
    group = PRODUCT_GROUP
}
