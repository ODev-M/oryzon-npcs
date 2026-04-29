// OryzonNPCs — modern NPC plugin for Paper 1.20.6+
//
// Target Paper API 1.20.6: stable surface from 1.20.6 through 1.21.x. Java 21
// is mandatory (Paper requires it from 1.20.5 onwards). Build output lives in
// build/libs/oryzon-npcs-<version>.jar — that's the file users drop into
// plugins/.

plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "cv.oryzon.npcs"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    // PacketEvents: cross-version packet abstraction so we're not coupled to
    // any single NMS revision.
    maven("https://repo.codemc.io/repository/maven-releases/") {
        name = "codemc-releases"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.5.0")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        // Paper publishes API jars compiled to JDK 21; matching here keeps
        // bytecode portable across all 1.20.6+ servers.
        options.release.set(21)
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        // Relocate any third-party deps we end up bundling so we don't clash
        // with whatever else lives in plugins/.
        relocate("com.github.retrooper.packetevents", "cv.oryzon.npcs.libs.packetevents")
        relocate("io.github.retrooper.packetevents", "cv.oryzon.npcs.libs.packetevents_io")
    }

    build {
        dependsOn(shadowJar)
    }

    // Convenience: keep the unshaded jar from publishing — only the shaded
    // one is meant to be deployed.
    jar {
        enabled = false
    }
}
