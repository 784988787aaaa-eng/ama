plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.mizanaldar.ptwqxs"
    minSdk = 24
    targetSdk = 36
    versionCode = 4
    versionName = "1.3"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  androidResources {
    localeFilters += listOf("ar", "en")
  }

  signingConfigs {
    create("release") {
      storeFile = file("mizan.keystore")
      storePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").getOrElse("123456")
      keyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").getOrElse("mizan")
      keyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").getOrElse("123456")
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // توحيد البصمة محلياً لتفادي كود 10 أثناء وضع التجربة والتطوير
      signingConfig = signingConfigs.getByName("release")
    }
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "/META-INF/INDEX.LIST"
      excludes += "/META-INF/DEPENDENCIES"
      excludes += "/META-INF/*.version"
      excludes += "/META-INF/*.kotlin_module"
      excludes += "/META-INF/licenses/**"
      excludes += "**/proto/**"
    }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
val envFile = rootProject.file(".env")
if (!envFile.exists()) {
  val googleClientId = System.getenv("GOOGLE_CLIENT_ID") ?: ""
  val googleClientSecret = System.getenv("GOOGLE_CLIENT_SECRET") ?: ""
  val geminiApiKey = System.getenv("GEMINI_API_KEY") ?: ""
  if (googleClientId.isNotEmpty() || googleClientSecret.isNotEmpty() || geminiApiKey.isNotEmpty()) {
    envFile.writeText("""
      GOOGLE_CLIENT_ID=$googleClientId
      GOOGLE_CLIENT_SECRET=$googleClientSecret
      GEMINI_API_KEY=$geminiApiKey
    """.trimIndent())
  }
}

// Sanitize the .env file to ensure no empty keys are present (which would cause compiler errors inBuildConfig.java)
if (envFile.exists()) {
  try {
    val lines = envFile.readLines().map { line ->
      if (line.contains("=")) {
        val parts = line.split("=", limit = 2)
        val key = parts[0].trim()
        val value = parts[1].trim()
        if (value.isEmpty()) {
          "$key=none"
        } else {
          line
        }
      } else {
        line
      }
    }
    envFile.writeText(lines.joinToString("\n"))
  } catch (e: Exception) {
    // Fail-safe
  }
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
  implementation(platform(libs.androidx.compose.bom))
  implementation("androidx.profileinstaller:profileinstaller:1.3.1")
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation("androidx.compose.ui:ui-text-google-fonts:1.6.3")
  implementation(libs.androidx.core.ktx)
  implementation("androidx.core:core-splashscreen:1.0.1")
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)
  implementation(libs.play.services.auth)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.paging.runtime)
  implementation(libs.androidx.paging.compose)
  implementation(libs.androidx.room.paging)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}




