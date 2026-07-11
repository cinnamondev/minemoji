plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.4.0"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")

    }
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    implementation("net.fellbaum:jemoji:1.7.6")
    implementation("org.apache.commons:commons-collections4:4.5.0")
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
