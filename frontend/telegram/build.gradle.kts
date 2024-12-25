plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.1.1"
}

application {
    mainClass.set("org.cryptolosers.telegrambot.TradingTelegramBotMainKt")
}

dependencies {
    implementation(project(":core:commons-general"))
    implementation(project(":core:history-service"))
    implementation(project(":core:trading-api"))
    implementation(project(":core:simulator"))
    implementation(project(":core:trading-transaq-impl"))
    implementation(project(":core:trading-bybit-impl"))
    implementation(project(":core:indicators"))

    implementation("org.slf4j:slf4j-api:${properties["slf4j_version"]}")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:${properties["log4j_version"]}")
    implementation("io.github.microutils:kotlin-logging-jvm:${properties["kolog_version"]}")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${properties["coroutines_version"]}")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${properties["coroutines_version"]}")
    implementation("com.google.code.gson:gson:${properties["gson_version"]}")
    implementation("org.telegram:telegrambots:6.9.7.1") {
        exclude("org.slf4j")
        exclude("com.google.code.gson")
    }
}