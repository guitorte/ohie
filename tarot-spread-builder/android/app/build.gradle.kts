plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tarot.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tarot.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// The web app (index.html, css/, js/, decks/) lives one directory up and is the
// single source of truth for the UI — copied into assets/www at build time so
// the ~12 MB of card art isn't duplicated inside the Android project in git.
val copyWebAssets = tasks.register<Copy>("copyWebAssets") {
    from(rootProject.projectDir.parentFile) {
        include("index.html", "css/**", "js/**", "decks/**")
    }
    into(layout.projectDirectory.dir("src/main/assets/www"))
}

tasks.named("preBuild") {
    dependsOn(copyWebAssets)
}
