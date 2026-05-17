plugins {
    id("com.android.library")
}

group = "com.application.dynamic_app_icon_flutter_plus"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.application.dynamic_app_icon_flutter_plus"
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin")
        }

        getByName("test") {
            kotlin.srcDirs("src/test/kotlin")
        }
    }

    defaultConfig {
        minSdk = 24
    }

    testOptions {
        unitTests {
            all {
                it.useJUnitPlatform()

                it.testLogging {
                    events(
                        "passed",
                        "skipped",
                        "failed",
                        "standardOut",
                        "standardError"
                    )

                    showStandardStreams = true
                }

                it.outputs.upToDateWhen { false }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.mockito:mockito-core:5.23.0")
}
