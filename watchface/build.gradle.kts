plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.poketch.watchface"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.poketch.watchface"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
        }
    }
}
