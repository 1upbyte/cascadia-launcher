plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.cascadia.launcher"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "app.cascadia.launcher"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
        create("profile") {
            initWith(getByName("benchmark"))
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { abortOnError = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.startup:startup-runtime:1.2.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

// Enforce privacy on the actual merged APK manifest, including transitive libraries.
val verifyReleasePrivacy by tasks.registering {
    dependsOn("processReleaseManifest")
    doLast {
        val manifests = fileTree(layout.buildDirectory.dir("intermediates/merged_manifests/release")) {
            include("**/AndroidManifest.xml")
        }.files
        check(manifests.isNotEmpty()) { "Merged release manifest not found" }
        manifests.forEach { file ->
            val xml = file.readText()
            check(!Regex("android.permission.(INTERNET|ACCESS_NETWORK_STATE)").containsMatchIn(xml)) {
                "Network permission found in merged release manifest"
            }
        }
        val forbidden = configurations.getByName("releaseRuntimeClasspath")
            .resolvedConfiguration.resolvedArtifacts.filter {
                it.moduleVersion.id.group.startsWith("com.google.android.gms") ||
                    it.moduleVersion.id.group.startsWith("com.google.firebase")
            }
        check(forbidden.isEmpty()) { "Google services dependencies found: $forbidden" }
    }
}
tasks.named("check") { dependsOn(verifyReleasePrivacy) }
