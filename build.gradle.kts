plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.20"
    id("java-library")
    id("antlr")
    id("idea")
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("maven-publish")
}

project.group = "com.strumenta"
project.version = rootProject.file("src/main/resources/com/strumenta/entity/parser/version.txt").readText().trim()

val jvmVersion = rootProject.file(".java-version").readText().trim()

repositories {
    mavenLocal()
    mavenCentral()
}

val antlrVersion = project.properties["antlrVersion"]
val kotlinVersion = project.properties["kotlinVersion"]
val kolasuVersion = project.properties["kolasuVersion"]

dependencies {
    antlr("org.antlr:antlr4:$antlrVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.antlr:antlr4-runtime:$antlrVersion")
    implementation("com.strumenta.kolasu:kolasu-core:$kolasuVersion")
    implementation("com.strumenta.kolasu:kolasu-emf:$kolasuVersion")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.slf4j:slf4j-api:2.0.1")
    implementation("org.slf4j:slf4j-simple:2.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}

val generatedMain = File(project.layout.buildDirectory.asFile.get(), "generated-src${File.separator}antlr${File.separator}main")

sourceSets.getByName("main") {
    java.srcDir("src${File.separator}main${File.separator}java")
    java.srcDir(generatedMain)
}

tasks {
    named("runKtlintCheckOverMainSourceSet") {
        dependsOn("generateGrammarSource")
    }
    named("compileKotlin") {
        dependsOn("generateGrammarSource")
    }
    named("compileTestKotlin") {
        dependsOn("generateTestGrammarSource")
    }
}

tasks.generateGrammarSource {
    inputs.files(fileTree("src/main/antlr").include("*.g4"))
    maxHeapSize = "256m"
    arguments.addAll(listOf("-package", "com.strumenta.entity.parser"))
    arguments.addAll(listOf("-listener", "-visitor"))
    outputDirectory = file("$projectDir/build/generated-src/antlr/main/com/strumenta/entity/parser")
}

tasks.wrapper {
    gradleVersion = "8.8"
    distributionType = Wrapper.DistributionType.ALL
}
