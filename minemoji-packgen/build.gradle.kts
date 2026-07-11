plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.4.0"

}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://nexus.scarsz.me/content/groups/public/") }
    maven { url = uri("https://repo1.maven.org/maven2/") }
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
}

dependencies {
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.13.1")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.19")
    implementation("org.apache.xmlgraphics:batik-codec:1.19")
    implementation("commons-cli:commons-cli:1.10.0")
    implementation("org.apache.commons:commons-lang3:3.20.0")

    implementation(project(":common"))
}

group = "com.github.cinnamondev"
version = "1.0"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release = 25
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

}


