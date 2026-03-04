plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "ua.retrogaming.gcac"
    compileSdk = 36

    defaultConfig {
        applicationId = "ua.retrogaming.gcac"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            output.outputFileName = "AdapterCompanion-${defaultConfig.versionName}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
    buildFeatures {
        compose = true
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
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.kotpref)
    implementation(libs.gson)
    implementation(libs.gson.support)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.livedata.support)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kpermissions)
    implementation(libs.kpermissions.rx3)
    implementation(libs.colorpicker.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.colorpicker)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.usb.serial.for1.android)
    implementation(libs.koin.android)

}