group = "dev.local.mixplorer"

patches {
    about {
        name = "MiXplorer Sharing Fix"
        description = "Fixes unreadable filesystem paths in externally shared file metadata."
        author = "MiXplorer Sharing Fix contributors"
        source = "https://github.com/ak800i/mixplorer-patches-for-morphe"
        contact = "https://github.com/ak800i/mixplorer-patches-for-morphe/issues"
        website = "https://github.com/ak800i/mixplorer-patches-for-morphe"
        license = "Not specified"
    }
}

dependencies {
    testImplementation(libs.morphe.patcher)
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}