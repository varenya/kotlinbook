plugins {
    kotlin("jvm") version "1.8.21"
    application
}



group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-server-core:2.1.2")
    implementation("io.ktor:ktor-server-netty:2.1.2")
    implementation("com.google.code.gson:gson:2.10")
    implementation("ch.qos.logback:logback-classic:1.4.4")
    implementation("org.slf4j:slf4j-api:2.0.3")
    implementation("io.ktor:ktor-server-status-pages:2.1.2")
    implementation("com.typesafe:config:1.4.2")
}

application {
    mainClass.set("kotlinbook.MainKt")
}


tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}