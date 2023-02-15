plugins {
    id("com.github.ben-manes.versions") version "0.44.0"
    kotlin("multiplatform") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    // KSP support
    id("com.google.devtools.ksp") version "1.7.20-1.0.6"
//    id("kotlin2js") version "1.7.21"
}

repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") // new repository here
}

val fritz2Version = "1.0-SNAPSHOT"

group = "cz.glubo.glubot.backoffice"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvm()
    js(IR) {
        browser()
    }.binaries.executable()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("dev.fritz2:core:$fritz2Version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
                // implementation("dev.fritz2:headless:$fritz2Version") // optional
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
        val jsMain by getting {
            dependencies {
            }
        }
    }
}

/**
 * KSP support - start
 */
dependencies {
    add("kspCommonMainMetadata", "dev.fritz2:lenses-annotation-processor:$fritz2Version")
//    implementation(kotlin("stdlib-js"))
}
kotlin.sourceSets.commonMain { kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin") }
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
    if (name != "kspCommonMainKotlinMetadata") dependsOn("kspCommonMainKotlinMetadata")
}
/**
 * KSP support - end
 */
