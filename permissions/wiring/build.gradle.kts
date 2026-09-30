plugins {
    alias(libs.plugins.permissionskit.compose)
    alias(libs.plugins.permissionskit.injection)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.permissions.api)
                implementation(projects.permissions.impl)
            }
        }
    }
}
