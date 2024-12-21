import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version ("1.8.22")
}
val compileKotlin: KotlinCompile by tasks
val compileTestKotlin: KotlinCompile by tasks

//compileKotlin.kotlinOptions {
//    jvmTarget = "1.11"
//}
//compileTestKotlin.kotlinOptions {
//    jvmTarget = "1.11"
//}

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "kotlin")

    repositories {
        mavenCentral()
        jcenter()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging.showStandardStreams = true
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}