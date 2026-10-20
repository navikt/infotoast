rootProject.name = "infotoast"


val ktorVersion = "3.6.0"
val tsmKtorVersion = "1.3.6"

dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") {
            content {
                includeGroup("no.nav.tsm")
                includeGroup("no.nav.tsm.sykmelding")
            }
        }
        maven("https://jitpack.io") {
            content {
                includeGroup("dev.hayden")
            }
        }
        mavenCentral {
            content {
                excludeGroup("no.nav.tsm.sykmelding")
                excludeGroup("no.nav.tsm")
                excludeGroup("dev.hayden")
            }
        }

    }

    versionCatalogs {
        create("ktorLibs").from("io.ktor:ktor-version-catalog:${ktorVersion}")
        create("tsmKtorLibs").from("no.nav.tsm:ktor-version-catalog:${tsmKtorVersion}")
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("io.github.ben-manes.versions.settings") version "0.66.0"
}
