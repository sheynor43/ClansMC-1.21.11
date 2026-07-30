plugins {
    java
    id("com.gradleup.shadow") version "9.6.0"
}

group = property("group") as String
version = property("version") as String

val relocateBase = "io.github.sheynor43.clans.libs"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        name = "placeholderapi"
    }
    maven("https://repo.codemc.io/repository/maven-public/") {
        name = "codemc"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
}

dependencies {
    // Paper API — provided by the server at runtime.
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Soft dependencies — provided by other plugins at runtime, never shaded.
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")

    // Shaded (relocated into <plugin>.libs).
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.xerial:sqlite-jdbc:3.53.2.1")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.7")

    // Tests.
    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
        // Fail the build on the warning categories that matter for this project:
        // usage of deprecated API and unchecked/raw generics. Noisy categories that
        // are unavoidable with Brigadier/Bukkit (this-escape, serial) are left off.
        options.compilerArgs.addAll(listOf("-Xlint:deprecation,unchecked,rawtypes", "-Werror"))
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")

        relocate("com.zaxxer.hikari", "$relocateBase.hikari")
        relocate("org.sqlite", "$relocateBase.sqlite")
        relocate("org.mariadb.jdbc", "$relocateBase.mariadb")

        // Merge JDBC driver service files so both drivers register via ServiceLoader.
        // No minimize(): it strips driver service entries and reflectively-loaded classes.
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }

    // Disable the plain jar; only the shaded jar is a valid plugin.
    named<Jar>("jar") {
        enabled = false
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
