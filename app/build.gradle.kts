plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.coodex.codextts2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.coodex.codextts2"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    lint {
        warningsAsErrors = true
        abortOnError = true
        // Temporarily baseline some warnings that are not yet actionable
        disable += setOf(
            "ObsoleteSdkInt",
            "MissingTranslation",
            "OldTargetApi",
            "GradleDependency",
            "IconLauncherShape", // Placeholder icons; proper icons will replace these
        )
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/detekt.yml")
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.core)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

/**
 * Downloads pre-built sherpa-onnx native libraries from GitHub releases.
 * Only runs if jniLibs directory is empty or missing.
 */
tasks.register("downloadSherpaOnnxLibs") {
    val jniLibsDir = file("src/main/jniLibs")
    val markerFile = file("src/main/jniLibs/.downloaded")

    onlyIf { !markerFile.exists() }

    doLast {
        val version = "1.12.25"
        val url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v$version/sherpa-onnx-v$version-android.tar.bz2"
        val downloadDir = layout.buildDirectory.dir("tmp/sherpa-dl").get().asFile
        val archive = File(downloadDir, "sherpa-onnx-android.tar.bz2")

        downloadDir.mkdirs()

        if (!archive.exists()) {
            logger.lifecycle("Downloading sherpa-onnx v$version native libraries...")
            ant.invokeMethod("get", mapOf("src" to url, "dest" to archive))
        }

        logger.lifecycle("Extracting sherpa-onnx native libraries...")
        exec {
            commandLine("tar", "xjf", archive.absolutePath, "-C", downloadDir.absolutePath)
        }

        val extractedJniLibs = File(downloadDir, "jniLibs")
        if (extractedJniLibs.exists()) {
            extractedJniLibs.copyRecursively(jniLibsDir, overwrite = true)
            markerFile.createNewFile()
            logger.lifecycle("sherpa-onnx native libraries installed to ${jniLibsDir.absolutePath}")
        } else {
            throw GradleException("Expected jniLibs directory not found after extraction")
        }
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.contains("JniLibFolders") }.configureEach {
    dependsOn("downloadSherpaOnnxLibs")
}
