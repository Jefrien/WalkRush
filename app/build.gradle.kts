import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Read local.properties
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

// Helper function to get property with fallback
fun getProperty(key: String, defaultValue: String): String {
    return localProperties.getProperty(key)
        ?: System.getenv(key)
        ?: defaultValue
}

android {
    namespace = "dev.jefrien.walkrush"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "dev.jefrien.walkrush"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // BuildConfig fields from local.properties
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${getProperty("SUPABASE_URL", "https://placeholder.supabase.co")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${getProperty("SUPABASE_ANON_KEY", "placeholder-key")}\""
        )
        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            "\"${getProperty("OPENAI_API_KEY", "")}\""
        )

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release uses environment variables or CI/CD secrets
            buildConfigField(
                "String",
                "SUPABASE_URL",
                "\"${System.getenv("SUPABASE_URL") ?: getProperty("SUPABASE_URL", "")}\""
            )
            buildConfigField(
                "String",
                "SUPABASE_ANON_KEY",
                "\"${System.getenv("SUPABASE_ANON_KEY") ?: getProperty("SUPABASE_ANON_KEY", "")}\""
            )
            buildConfigField(
                "String",
                "OPENAI_API_KEY",
                "\"${System.getenv("OPENAI_API_KEY") ?: getProperty("OPENAI_API_KEY", "")}\""
            )
        }
        debug {
            isDebuggable = true
            // Debug uses local.properties
            buildConfigField("String", "SUPABASE_URL", "\"${getProperty("SUPABASE_URL", "")}\"")
            buildConfigField(
                "String",
                "SUPABASE_ANON_KEY",
                "\"${getProperty("SUPABASE_ANON_KEY", "")}\""
            )
            buildConfigField(
                "String",
                "OPENAI_API_KEY",
                "\"${getProperty("OPENAI_API_KEY", "")}\""
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.transport.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Dependency Injection - Koin
    implementation(libs.bundles.koin)
    implementation(libs.koin.android)

    // Serialization
    implementation(libs.kotlinx.serialization)

    // Supabase (with Ktor)
    implementation(platform(libs.supabase.bom))
    implementation(libs.bundles.supabase)
    implementation(libs.bundles.ktor)

    // Health Connect (replaces Google Fit)
    implementation(libs.health.connect.client)

    implementation(libs.multiplatform.settings)


}