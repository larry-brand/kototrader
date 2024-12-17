dependencies {
    implementation(project(":core:commons-general"))
    implementation(project(":core:trading-api"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${properties["coroutines_version"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${properties["coroutines_version"]}")

    implementation("org.slf4j:slf4j-api:${properties["slf4j_version"]}")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:${properties["log4j_version"]}")
    implementation("io.github.microutils:kotlin-logging-jvm:${properties["kolog_version"]}")

    testImplementation("org.junit.jupiter:junit-jupiter:${properties["junit_version"]}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${properties["mockito_kotlin_version"]}")
    testImplementation("org.mockito:mockito-inline:${properties["mockito_inline_version"]}")
    testImplementation("io.mockk:mockk:${properties["mockk_version"]}")
    testImplementation("io.kotest:kotest-assertions-core-jvm:${properties["kotest_version"]}")
}
