plugins {
    alias(libs.plugins.permissionskit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.permissions.api)
            }
        }
    }
}
