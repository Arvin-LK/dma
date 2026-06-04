plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.dma"
version = "1.1.0"

repositories {
    mavenCentral()
    // Local Maven repo for dma-core and dma-common
    mavenLocal()
}

dependencies {
    // DMA core engine (must be installed to local Maven first: ./mvnw install -DskipTests)
    implementation("com.dma:dma-core:1.0.0-SNAPSHOT")
    implementation("com.dma:dma-common:1.0.0-SNAPSHOT")
}

intellij {
    version.set("2024.1")
    type.set("IC") // IntelliJ IDEA Community Edition
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
