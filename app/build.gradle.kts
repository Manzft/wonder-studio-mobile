import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)
}

// Firma de release: lee keystore.properties si existe (no se commitea).
val keystoreProperties = Properties().apply {
	val file = rootProject.file("keystore.properties")
	if (file.exists()) file.inputStream().use { load(it) }
}

android {
	namespace = "com.manzft.wonderstudio"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.manzft.wonderstudio"
		minSdk = 26
		targetSdk = 35
		versionCode = 1
		versionName = "1.0"
	}

	signingConfigs {
		create("release") {
			if (keystoreProperties.getProperty("storeFile") != null) {
				storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
				storePassword = keystoreProperties.getProperty("storePassword")
				keyAlias = keystoreProperties.getProperty("keyAlias")
				keyPassword = keystoreProperties.getProperty("keyPassword")
			}
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			if (keystoreProperties.getProperty("storeFile") != null) {
				signingConfig = signingConfigs.getByName("release")
			}
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro",
			)
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlinOptions {
		jvmTarget = "17"
	}

	buildFeatures {
		compose = true
	}

	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.lifecycle.viewmodel.compose)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.material3)
	implementation(libs.androidx.documentfile)
	implementation(libs.kotlinx.serialization.json)
	debugImplementation(libs.androidx.ui.tooling)
}
