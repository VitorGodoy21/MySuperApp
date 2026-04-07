plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    kotlin("kapt")
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.vfdeginformatica.mysuperapp"
    compileSdk = 36

    // Lê a Maps API Key do local.properties de forma segura
    val localProperties = rootProject.file("local.properties")
    val mapsApiKey = if (localProperties.exists()) {
        localProperties.readLines()
            .firstOrNull { it.startsWith("MAPS_API_KEY=") }
            ?.substringAfter("=")
            ?: ""
    } else ""

    defaultConfig {
        applicationId = "com.vfdeginformatica.mysuperapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    flavorDimensions += "env"

    productFlavors {

        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("boolean", "IS_DEBUGGABLE", "true")
            resValue("string", "app_name", "DEV-MySuperApp")
        }

        create("prd") {
            dimension = "env"
            buildConfigField("boolean", "IS_DEBUGGABLE", "false")
        }
    }

    buildTypes {
        release {
            isDebuggable = true
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    androidComponents {
        beforeVariants(selector().withBuildType("debug")) {
            it.enable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.ui.tooling)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.firebase.appcheck.debug)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.ui.auth)
    implementation(libs.firebase.firestore)
    // App Check — Play Integrity em produção, Debug em dev
    implementation(libs.firebase.appcheck.playintegrity)
    //"devReleaseImplementation"(libs.firebase.appcheck.debug)

    //Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    //Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx.v284)

    //Icons Default
    implementation(libs.androidx.material.icons.extended)

    //Data Store Preferences
    implementation(libs.androidx.datastore.preferences)

    //Biometric
    implementation(libs.androidx.biometric)

    //ZXing for QR Code generation
    implementation(libs.zxing.core)

    //Gson for JSON serialization
    implementation(libs.gson)

    //Google Maps
    implementation(libs.google.maps.compose)
    implementation(libs.google.play.services.maps)
    implementation(libs.google.maps.utils)
}
