@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        // JitPack serves every Maven Central address from a mirror that can lag new releases
        // by a day. PaperMC's repository proxies Central without that lag.
        maven("https://repo.papermc.io/repository/maven-public") {
            content { includeGroup("io.github.sendablemetatype.netty") }
        }
        maven("https://repo.opencollab.dev/maven-releases")
        maven("https://repo.opencollab.dev/maven-snapshots")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "protocol"

include("adventure", "bedrock-codec", "bedrock-connection", "common")
