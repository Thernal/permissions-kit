plugins {
    alias(libs.plugins.permissionskit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.permissions.api)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.lifecycle.common)
                implementation(libs.lifecycle.runtime.compose)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core)
            }
        }
    }
}
