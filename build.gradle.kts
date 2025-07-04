/***************
 * build.gradle.kts
 ***************/

plugins {
    // Kotlin JVM + Gradle DSL
    kotlin("jvm") version "1.9.23"

    // плагин Application → задачи run, installDist и т.д.
    application
}

repositories {
    mavenCentral()
}

/* ----------------------------  JavaFX  ---------------------------------- */

val javafxVersion = "21"

val platform = when {
    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> {
        val arch = System.getProperty("os.arch")
        if (arch == "aarch64") "mac-aarch64" else "mac"
    }
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "win"
    else -> "linux"
}

/* ----------------------------  Зависимости  ----------------------------- */

dependencies {
    implementation(kotlin("stdlib"))

    listOf("controls", "graphics", "base", "fxml").forEach { module ->
        implementation("org.openjfx:javafx-$module:$javafxVersion:$platform")
    }

    // Unit-тесты (JUnit 5)
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

/* ----------------------------  Kotlin /JVM  ----------------------------- */

kotlin {
    jvmToolchain(17)          // проект собирается под JDK 17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

/* ----------------------------  Application  ----------------------------- */

application {
    // имя файла с функцией fun main() — Main.kt  →  класс MainKt
    mainClass.set("MainKt")
}

/* ----------------------------  JAR с зависимостями  --------------------- */

tasks.withType<Jar>().configureEach {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    // fat-jar: внутрь кладём все runtime-зависимости
    from(
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    )
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE   // убираем дубли ресурсов
}

/* ----------------------------  Java / JavaFX Run  ----------------------- */

tasks.withType<JavaExec>().configureEach {
    val javafxModules = listOf("javafx.controls", "javafx.graphics", "javafx.fxml", "javafx.base")
        .joinToString(",")

    jvmArgs(
        "--module-path", configurations.runtimeClasspath.get().asPath,
        "--add-modules", javafxModules
    )
}

/* ----------------------------  JUnit 5  --------------------------------- */

tasks.test {
    useJUnitPlatform()
}