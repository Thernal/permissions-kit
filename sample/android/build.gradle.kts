plugins {
    alias(libs.plugins.permissionskit.android.application)
}

dependencies {
    implementation(projects.sample.shared)
    implementation(libs.androidx.activity.compose)
}
