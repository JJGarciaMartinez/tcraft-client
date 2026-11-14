import java.util.Properties

plugins {
    application
}

// version.properties is the single source of truth for name and version (see VERSION_GUIDE.md)
val versionProps = Properties().apply { rootProject.file("version.properties").inputStream().use(::load) }
val appName: String = versionProps.getProperty("app.name")
val numericVersion: String = versionProps.getProperty("app.version.numeric")
val vendor: String = versionProps.getProperty("app.vendor")
val winUpgradeUuid: String = versionProps.getProperty("app.win.upgrade.uuid")
// jpackage takes one to three numbers, e.g. 26.1.4
val numericPattern = Regex("""\d+(\.\d+){0,2}""")

version = versionProps.getProperty("app.version")

dependencies {
    implementation(project(":core"))
    implementation(project(":platform"))
    implementation("com.formdev:flatlaf:3.7.2")
}

application {
    mainClass = "hopsync.Main"
}

tasks.processResources {
    from(rootProject.file("version.properties"))
}

tasks.jar {
    archiveFileName = "$appName.jar"
    manifest {
        attributes("Main-Class" to application.mainClass)
    }
    // Self-contained JAR: java -jar and jpackage only ship this file
    from(configurations.runtimeClasspath.map { files -> files.map { if (it.isDirectory) it else zipTree(it) } })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("module-info.class", "META-INF/versions/*/module-info.class", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.withType<JavaCompile>().configureEach {
    // Swing components are never serialized; the lint only asks for boilerplate serialVersionUIDs
    options.compilerArgs.add("-Xlint:-serial")
}

// Packaging. jpackage only builds installers for the OS it runs on: the release workflow runs it on all three.

val packagingDir = rootProject.layout.projectDirectory.dir("packaging")
val jpackageInput = layout.buildDirectory.dir("jpackage/input")
val installerDir = layout.buildDirectory.dir("installer")
val osName = System.getProperty("os.name").lowercase()
val jpackageTool = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
    .map { it.metadata.installationPath.file(if (osName.startsWith("windows")) "bin/jpackage.exe" else "bin/jpackage") }

// jpackage bundles everything in --input, so it gets a folder with the JAR alone
val jpackageInputJar by tasks.registering(Sync::class) {
    from(tasks.jar)
    into(jpackageInput)
}

tasks.register<Exec>("jpackage") {
    group = "distribution"
    description = "Builds the installer of the current OS (.msi, .dmg or .deb) into desktop/build/installer."
    dependsOn(jpackageInputJar)
    inputs.dir(jpackageInput)
    inputs.dir(packagingDir)
    inputs.file(rootProject.file("version.properties"))
    outputs.dir(installerDir)

    val dest = installerDir.get().asFile
    args("--name", appName, "--app-version", numericVersion, "--vendor", vendor,
        "--description", "Keeps your Minecraft modpacks up to date",
        "--input", jpackageInput.get().asFile.path, "--main-jar", "$appName.jar", "--main-class", "hopsync.Main",
        "--dest", dest.path, "--java-options", "-Xmx1024m", "--java-options", "-Dfile.encoding=UTF-8")
    when {
        osName.startsWith("windows") -> args("--type", "msi", "--icon", packagingDir.file("icon.ico").asFile.path,
            // Keeps upgrades in place, also from the TCraftClient installers
            "--win-upgrade-uuid", winUpgradeUuid,
            "--win-menu", "--win-menu-group", appName, "--win-shortcut", "--win-dir-chooser")
        osName.contains("mac") -> args("--type", "dmg", "--icon", packagingDir.file("icon.icns").asFile.path,
            "--mac-package-name", appName, "--mac-package-identifier", "app.hopsync.client",
            "--java-options", "-Dapple.awt.application.name=$appName")
        else -> args("--type", "deb", "--icon", packagingDir.file("icon.png").asFile.path,
            "--linux-package-name", appName.lowercase(), "--linux-shortcut", "--linux-menu-group", "Game")
    }

    doFirst {
        if (!numericPattern.matches(numericVersion)) {
            throw GradleException("app.version.numeric=$numericVersion: jpackage needs one to three numbers, e.g. 26.1.4")
        }
        executable = jpackageTool.get().asFile.path
        // jpackage refuses to overwrite an installer left by a previous run
        dest.deleteRecursively()
    }
}

tasks.register<Zip>("portableZip") {
    group = "distribution"
    description = "Packs the JAR with double-click launchers into desktop/build/distributions, for Java 21 users."
    archiveFileName = "$appName-${project.version}-portable.zip"
    from(tasks.jar)
    from(packagingDir.dir("portable")) {
        filesMatching(listOf("*.sh", "*.command")) {
            permissions { unix("rwxr-xr-x") }
        }
    }
}

tasks.register("updateVersion") {
    group = "versioning"
    description = "Sets the version: ./gradlew updateVersion -PnewVersion=b26.2.0 [-Pnumeric=26.2.0]"
    val newVersion = providers.gradleProperty("newVersion")
    val numeric = providers.gradleProperty("numeric")
    val file = rootProject.file("version.properties")
    doLast {
        val display = newVersion.orNull
            ?: throw GradleException("Usage: ./gradlew updateVersion -PnewVersion=b26.2.0 [-Pnumeric=26.2.0]")
        // a26.1.0, b26.1.0 and rc26.1.0 are test builds of 26.1.0
        val number = numeric.orNull ?: display.replace(Regex("^[a-zA-Z]+-?"), "")
        if (!numericPattern.matches(number)) {
            throw GradleException("Numeric version $number: jpackage needs one to three numbers, e.g. 26.1.4")
        }
        file.writeText(file.readText()
            .replace(Regex("(?m)^app\\.version=.*$"), "app.version=$display")
            .replace(Regex("(?m)^app\\.version\\.numeric=.*$"), "app.version.numeric=$number"))
        logger.lifecycle("version.properties: app.version=$display, app.version.numeric=$number")
    }
}
