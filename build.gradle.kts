import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.2.71"
}

group = "io.github.dimonchik0036.${project.name}"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/ktor")
}

dependencies {
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("io.ktor:ktor-server-netty:0.9.5")
    compile(kotlin("stdlib-jdk8"))
}


kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

task<Jar>("fatJar") {
    baseName = "${project.name}-fat"
    manifest {
        attributes(
            mapOf(
                "Implementation-Version" to project.version,
                "Main-Class" to "${project.group}.MainKt"
            )
        )
    }

    from(configurations.runtime.map { if (it.isDirectory) it else zipTree(it) })
    with(tasks["jar"] as CopySpec)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}