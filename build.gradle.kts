plugins {
    kotlin("jvm") version "1.9.23"
    application
    id("org.jetbrains.dokka") version "1.9.10"
}

repositories {
    mavenCentral()
}

val javafxVersion = "21"
val platform = when {
    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> {
        val arch = System.getProperty("os.arch")
        if (arch == "aarch64") "mac-aarch64" else "mac"
    }
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "win"
    else -> "linux"
}

dependencies {
    implementation(kotlin("stdlib"))
    listOf("controls", "graphics", "base", "fxml").forEach { module ->
        implementation("org.openjfx:javafx-$module:$javafxVersion:$platform")
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

application {
    mainClass.set("MainKt")
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    from(
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    )
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<JavaExec>().configureEach {
    val javafxModules = listOf("javafx.controls", "javafx.graphics", "javafx.fxml", "javafx.base")
        .joinToString(",")
    jvmArgs(
        "--module-path", configurations.runtimeClasspath.get().asPath,
        "--add-modules", javafxModules
    )
}

tasks.test {
    useJUnitPlatform()
}

// === Dokka: только настройка существующей задачи! ===
tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml") {
    outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
}