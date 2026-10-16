plugins {
    id("nexalloy.bundle")
}

android {
    namespace = "io.github.nexalloy.patches.a092devs"

    defaultConfig {
        applicationId = "io.github.nexalloy.patches.a092devs"
        versionCode = 4
        versionName = "1.3.0"
    }
}

nexalloyBundle {
    entry = "io.github.nexalloy.a092devs.A092DevsPatches"
    author = "a092devs"
    descriptionResource = "bundle_description"
    resourcePackageId = 0x4e
}

dependencies {
    compileOnly(libs.nexalloy.patcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.junit.jupiter.params)
}
