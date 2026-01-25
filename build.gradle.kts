plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.3.1"
    id("run-hytale")
}

group = findProperty("pluginGroup") as String? ?: "com.example"
version = findProperty("pluginVersion") as String? ?: "1.0.0"
description = findProperty("pluginDescription") as String? ?: "A Hytale plugin template"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Hytale Server API (provided by server at runtime)
    compileOnly(files("libs/hytale-server.jar"))
    
    // Common dependencies (will be bundled in JAR)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains:annotations:24.1.0")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Configure server testing
runHytale {
    jarUrl = ""
}

tasks {
    // Configure Java compilation
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
    }
    
    // Configure resource processing
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        
        // Replace placeholders in manifest.json
        val props = mapOf(
            "group" to project.group,
            "version" to project.version,
            "description" to project.description
        )
        inputs.properties(props)
        
        filesMatching("manifest.json") {
            expand(props)
        }
    }
    
    // Configure ShadowJar (bundle dependencies)
    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
        
        relocate("com.google.gson", "zurku.gravestones.libs.gson")
        minimize()
    }
    
    // Configure tests
    test {
        useJUnitPlatform()
    }
    
    // Make build depend on shadowJar
    build {
        dependsOn(shadowJar)
    }

    // Install plugin JAR to local Hytale Mods folder
    val installMods by registering(Copy::class) {
        dependsOn(shadowJar)
        val modsDir = file("C:/Users/Joshu/AppData/Roaming/Hytale/UserData/Mods")
        val jarFile = file("${buildDir}/libs/${rootProject.name}-${project.version}.jar")
        from(jarFile)
        into(modsDir)
        doLast {
            println("Installed plugin JAR into: $modsDir")
        }
    }

    // Collect runtime logs into the build folder for easy inspection
    val collectLogs by registering(Copy::class) {
        val userLogs = file("C:/Users/Joshu/AppData/Roaming/Hytale/UserData/Logs")
        val saveLogs = file("C:/Users/Joshu/AppData/Roaming/Hytale/UserData/Saves/New World/logs")
        from(userLogs) { include("**/*.log") }
        from(saveLogs) { include("**/*.log") }
        into(file("${buildDir}/hytale-logs"))
        doLast { println("Collected logs into ${buildDir}/hytale-logs") }
    }

    // Create a distribution ZIP containing the JAR (asset pack is bundled inside)
    val distZip by registering(Zip::class) {
        dependsOn(shadowJar)
        
        archiveBaseName.set("Zurku_Gravestones")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
        destinationDirectory.set(file("${buildDir}/dist"))
        
        from("${buildDir}/libs") {
            include("${rootProject.name}-${project.version}.jar")
        }
        
        doLast {
            println("Created distribution ZIP: ${archiveFile.get().asFile.absolutePath}")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}