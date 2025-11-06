import xyz.jpenilla.resourcefactory.bukkit.Permission
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml

plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.3.1"
    //id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("com.gradleup.shadow") version "9.2.2"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven { url = uri("https://nexus.scarsz.me/content/groups/public/") }
    maven { url = uri("https://repo1.maven.org/maven2/") }

    maven { url = uri("https://repo.maven.apache.org/maven2/") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly("com.discordsrv:discordsrv:1.30.2")
    implementation("net.fellbaum:jemoji:1.7.5")
    implementation("org.apache.commons:commons-collections4:4.4")
    // Requirements for PackMaker (this is probably where the bulk of the Jar file comes from)
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.12.0")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.19")
    implementation("org.apache.xmlgraphics:batik-codec:1.19")
    implementation("commons-cli:commons-cli:1.10.0")
    implementation("org.apache.commons:commons-lang3:3.19.0")
}

group = "com.github.cinnamondev"
version = "1.03"
description = "minemoji"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    compileJava {
        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release = 21
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    runServer {
        downloadPlugins {
            modrinth("discordsrv", "1.30.2") //  for discordsrv integration testing.
            modrinth("luckperms", "v5.5.17-bukkit")
        }
        runDirectory.set(layout.buildDirectory.dir("run"))
        minecraftVersion("1.21.10")
    }
}


paperPluginYaml {
    main = "com.github.cinnamondev.minemoji.Minemoji"
    apiVersion = "1.21"
    authors.add("cinnamondev")
    dependencies {
        server("DiscordSRV", PaperPluginYaml.Load.BEFORE, false)
    }
    permissions {
        register("minemoji.emoji") {
            description = "Base permission to use emojis"
            default = Permission.Default.TRUE
        }
        register("minemoji.list") {
            description = "List emojis using /minemoji list <pack>"
            default = Permission.Default.TRUE
        }
    }
}
