import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
    application
}
group = "ru.senin.kotlin.wiki"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    application {
        mainClass.set("ru.senin.kotlin.wiki.MainKt")
    }
}

dependencies {
    implementation("com.apurebase:arkenv:3.1.0")
    implementation("org.apache.commons:commons-compress:1.20")
    implementation(kotlin("reflect"))
    implementation("com.ximpleware", "vtd-xml", "2.11")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation(kotlin("test-junit5"))
}

tasks.withType<Test>().all {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin.sourceSets.all {
    languageSettings.apply {
        useExperimentalAnnotation("kotlin.time.ExperimentalTime")
    }
}

application {
    applicationDefaultJvmArgs += listOf("-Xmx12g")
}
