plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.hexa.launcher"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.hexa.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = 11
        versionName = "1.1"
    }
}
