// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}

tasks.register("verifyPoketchReleaseReadiness") {
    group = "verification"
    description = "Runs all unit tests across all subprojects and builds dual release AAB bundles for Mobile and Wear OS."

    dependsOn(
        ":core-domain:test",
        ":core-data:testDebugUnitTest",
        ":core-sync:testDebugUnitTest",
        ":app-mobile:testDebugUnitTest",
        ":app-wear:testDebugUnitTest",
        ":app-mobile:bundleRelease",
        ":app-wear:bundleRelease"
    )
}
