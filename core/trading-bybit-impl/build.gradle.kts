plugins {
    kotlin("plugin.serialization") version "1.8.21"
}

dependencies {
    implementation(project(":core:commons-general"))
    implementation(project(":core:trading-api"))

    implementation("org.apache.commons:commons-lang3:3.12.0")

    implementation("io.ktor:ktor-client-core:${properties["ktor_version"]}")
    implementation("io.ktor:ktor-client-json:${properties["ktor_version"]}")
    implementation("io.ktor:ktor-client-cio:${properties["ktor_version"]}")  // Используем CIO для асинхронных запросов
    implementation("io.ktor:ktor-client-serialization:${properties["ktor_version"]}")  // Для работы с JSON
    implementation("io.ktor:ktor-client-content-negotiation:${properties["ktor_version"]}")  // Для сериализации и десериализации
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")  // Для работы с JSON
    implementation("io.ktor:ktor-serialization-kotlinx-json:${properties["ktor_version"]}")

    implementation("org.slf4j:slf4j-api:${properties["slf4j_version"]}")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:${properties["log4j_version"]}")
    implementation("org.apache.logging.log4j:log4j-core:${properties["log4j_version"]}")
    implementation("io.github.microutils:kotlin-logging-jvm:${properties["kolog_version"]}")
}