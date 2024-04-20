import java.net.URI
import java.net.URL

val rootPackage: String = project.properties["basePackage"].toString() + "." + project.name
val mainClassPath: String = rootPackage + "." + project.properties["mainClassName"].toString()

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    id("application")
    id("java")
}

repositories ({
	mavenCentral()
    this.maven({
        this.name = "m2-dv8tion"
        this.url = URI("https://m2.dv8tion.net/releases")
    })
})

application {
    mainClass.set(mainClassPath)
}

tasks.named<Jar>("jar") {
    this.manifest.attributes["Main-Class"] = mainClassPath
    this.archiveClassifier.set("no-libs")
}

tasks.named("build") {
    finalizedBy("fatJar")
    finalizedBy("javadoc")
}

tasks.register<Jar>("fatJar") {
    this.manifest.attributes["Main-Class"] = mainClassPath
    from(sourceSets.main.get().output)
    this.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

dependencies {
    this.implementation("com.amihaiemil.web:eo-yaml:6.0.0")
    this.implementation("org.slf4j:slf4j-simple:1.7.32")
    this.implementation("io.github.fragland:MineStat:3.0.5")
    this.implementation("org.json:json:20230227")
    this.implementation("net.dv8tion:JDA:4.4.1_353")
    this.implementation("com.vdurmont:emoji-java:5.1.1")
}

//Setup version
project.version = project.properties["version"]!!
