import xyz.jpenilla.resourcefactory.bukkit.Permission
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml

plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.3.1"
    //id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("com.gradleup.shadow") version "9.4.0"
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
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("com.discordsrv:discordsrv:1.30.5")
    implementation("net.fellbaum:jemoji:1.7.6")
    implementation("org.apache.commons:commons-collections4:4.5.0")
    // Requirements for PackMaker (this is probably where the bulk of the Jar file comes from)
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.13.1")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.19")
    implementation("org.apache.xmlgraphics:batik-codec:1.19")
    implementation("commons-cli:commons-cli:1.10.0")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("org.bstats:bstats-bukkit:3.2.1")
}

group = "com.github.cinnamondev"
version = "1.4"
description = "Bringing emotes to minecraft!"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release = 25
    }
    shadowJar {
        configurations = project.configurations.runtimeClasspath.map { setOf(it) }

        dependencies {
            // Only merge bStats into the final jar, no other dependencies
            exclude { it.moduleGroup != "org.bstats" }
        }

        // Relocate bStats into the plugin's package to avoid conflicts with other
        // plugins using bStats
        relocate("org.bstats", project.group.toString())
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    runServer {
        downloadPlugins {
            modrinth("discordsrv", "1.30.5") //  for discordsrv integration testing.
            modrinth("luckperms", "v5.5.17-bukkit")
        }
        runDirectory.set(layout.buildDirectory.dir("run"))
        minecraftVersion("26.1.2")
    }
}


paperPluginYaml {
    main = "com.github.cinnamondev.minemoji.Minemoji"
    apiVersion = "26.1"
    website = "https://github.com/cinnamondev/minemoji"
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
        register("minemoji.tellraw") {
            description = "Easily emojify a tellraw snbt component"
            default = Permission.Default.OP
        }
        register("minemoji.reload") {
            description = "Reload emojis"
            default = Permission.Default.OP
        }
    }
}
