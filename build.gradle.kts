plugins {
    kotlin("jvm") version "1.9.23"
    application
}

repositories {
    mavenCentral()
}

val javafxVersion = "21"

// Автоматическое определение платформы
val platform = when {
    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> {
        val arch = System.getProperty("os.arch")
        if (arch == "aarch64") "mac-aarch64" else "mac"
    }
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "win"
    else -> "linux"
}

dependencies {
    implementation("org.openjfx:javafx-controls:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-fxml:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-base:$javafxVersion:$platform")
}

application {
    // Указываем главный класс (с функцией main)
    mainClass.set("WarshallVisualizerAppKt")
}

tasks.withType<JavaExec>().configureEach {
    val javafxModules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics", "javafx.base").joinToString(",")
    jvmArgs = listOf(
        "--module-path", classpath.asPath,
        "--add-modules", javafxModules
    )
}