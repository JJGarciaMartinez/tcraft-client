import java.util.Properties

plugins {
    application
}

// version.properties is the single source of truth for name and version (see VERSION_GUIDE.md)
val versionProps = Properties().apply { rootProject.file("version.properties").inputStream().use(::load) }
val appName: String = versionProps.getProperty("app.name")

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
