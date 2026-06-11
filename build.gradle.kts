import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "com.italiarevenge"
version = project.property("pluginVersion") as String
description = "Minecraft buy-order marketplace plugin for PaperMC 1.21.11+"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        mergeServiceFiles()
        relocate("org.sqlite", "com.italiarevenge.iROrders.libs.sqlite")
        relocate("org.xerial", "com.italiarevenge.iROrders.libs.xerial")
        minimize()
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        exclude("META-INF/versions/*/module-info.class")
    }
    build {
        dependsOn("shadowJar")
    }
    runServer {
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
    }
    processResources {
        val props = mapOf("version" to version)
        // Use classic plugin.yml (softdepend gives Vault classloader access).
        // Exclude paper-plugin.yml so Paper's new isolated classloader is NOT used.
        filesMatching("plugin.yml") {
            expand(props)
        }
        exclude("paper-plugin.yml")
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
        options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
    }
}
