import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    kotlin("jvm") version "2.3.20"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "org.katacr"
version = "2.0.3"

repositories {
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public/"){
        name = "Aliyun"
    }
    maven("https://maven.aliyun.com/repository/central"){
        name = "central"
    }
    maven("https://repo.alessiodp.com/releases/"){
        name = "libby"
    }
    maven("https://repo.codemc.org/repository/maven-public/") {
        name = "codemc"
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://repo.extendedclip.com/releases/") {
        name = "placeholderapi"
    }
    maven("https://jitpack.io") {
        name = "vaultapi"
    }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigot-snapshots"
    }
    maven("https://repo.opencollab.dev/main/") {
        name = "opencollab"
    }
    maven("https://repo.rosewooddev.io/repository/public/") {
        name = "rosewood"
    }
}

val spigotAdapter by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

val paperAdapter by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

kotlin.sourceSets.named(spigotAdapter.name) {
    kotlin.srcDir("src/spigot/kotlin")
}

kotlin.sourceSets.named(paperAdapter.name) {
    kotlin.srcDir("src/paper/kotlin")
}

dependencies {
    // Libby is the only bundled bootstrap dependency. KaMenu.onLoad uses it to
    // mount Kotlin and the remaining runtime libraries before Kotlin code runs.
    implementation("net.byteflux:libby-bukkit:1.3.0")
    compileOnly("org.bstats:bstats-bukkit:3.1.0")
    compileOnly(kotlin("stdlib"))
    // Adventure is mounted by Libby in KaMenu.onLoad instead of being bundled.
    compileOnly("net.kyori:adventure-api:4.26.1")
    compileOnly("net.kyori:adventure-key:4.26.1")
    compileOnly("net.kyori:adventure-text-minimessage:4.26.1")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.26.1")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.26.1")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.26.1")
    compileOnly("net.kyori:adventure-text-serializer-json:4.26.1")
    compileOnly("net.kyori:adventure-text-serializer-commons:4.26.1")
    compileOnly("net.kyori:adventure-text-serializer-bungeecord:4.4.1")
    // The shared runtime is compiled against the oldest public Bukkit API we support.
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.openjdk.nashorn:nashorn-core:15.3")
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")
    compileOnly("org.black_ixx:playerpoints:3.3.5")
    compileOnly("org.ow2.asm:asm:9.5")
    compileOnly("org.ow2.asm:asm-util:9.5")
    compileOnly("net.kyori:examination-api:1.3.0")
    compileOnly("net.kyori:examination-string:1.3.0")
    compileOnly("net.kyori:option:1.1.0")
    testImplementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    add(spigotAdapter.compileOnlyConfigurationName, "org.spigotmc:spigot-api:1.21.6-R0.1-SNAPSHOT")
    add(spigotAdapter.compileOnlyConfigurationName, "net.kyori:adventure-text-serializer-legacy:4.26.1")
    add(spigotAdapter.compileOnlyConfigurationName, "net.kyori:adventure-text-serializer-bungeecord:4.4.1")
    add(spigotAdapter.compileOnlyConfigurationName, "org.jetbrains.kotlin:kotlin-stdlib")

    // Paper Dialog and Folia are optional modern adapters merged into the same JAR.
    add(paperAdapter.compileOnlyConfigurationName, "io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT")
    add(paperAdapter.compileOnlyConfigurationName, "net.kyori:adventure-api:4.26.1")
    add(paperAdapter.compileOnlyConfigurationName, "net.kyori:adventure-key:4.26.1")
    add(paperAdapter.compileOnlyConfigurationName, "net.kyori:adventure-text-minimessage:4.26.1")
    add(paperAdapter.compileOnlyConfigurationName, "net.kyori:adventure-text-serializer-legacy:4.26.1")
    add(paperAdapter.compileOnlyConfigurationName, "net.kyori:adventure-text-serializer-plain:4.26.1")
    add(paperAdapter.compileOnlyConfigurationName, "net.kyori:adventure-text-serializer-gson:4.26.1")
    add(paperAdapter.compileOnlyConfigurationName, "net.kyori:adventure-text-serializer-json:4.26.1")
    add(paperAdapter.compileOnlyConfigurationName, "net.kyori:adventure-text-serializer-commons:4.26.1")
    add(paperAdapter.compileOnlyConfigurationName, "net.kyori:adventure-text-serializer-bungeecord:4.4.1")
    add(paperAdapter.compileOnlyConfigurationName, "net.kyori:examination-api:1.3.0")
    add(paperAdapter.compileOnlyConfigurationName, "net.kyori:examination-string:1.3.0")
    add(paperAdapter.compileOnlyConfigurationName, "net.kyori:option:1.1.0")
    add(paperAdapter.compileOnlyConfigurationName, "org.jetbrains.kotlin:kotlin-stdlib")
}

tasks {
    test {
        useJUnitPlatform()
    }

    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21")
    }
}

// The plugin bytecode must run on Minecraft 1.16.5's Java 16 runtime.
// The build itself uses the installed Java 21 toolchain because Gradle 8.8 does not run on Java 16.
val targetJavaVersion = 16
kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_16)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(targetJavaVersion)
}

// Modern Dialog adapters are compiled separately because their Paper/Bungee APIs
// require Java 21. Keep the shared runtime at Java 16 for 1.16.5 compatibility.
listOf(spigotAdapter, paperAdapter).forEach { adapterSourceSet ->
    configurations.named(adapterSourceSet.compileClasspathConfigurationName) {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
        }
    }
}

tasks.named<KotlinJvmCompile>("compileSpigotAdapterKotlin") {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

tasks.named<KotlinJvmCompile>("compilePaperAdapterKotlin") {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

tasks.named<JavaCompile>(spigotAdapter.compileJavaTaskName) {
    options.release.set(21)
}

tasks.named<JavaCompile>(paperAdapter.compileJavaTaskName) {
    options.release.set(21)
}

tasks.build {
    dependsOn("shadowJar")
}

// shadowJar intentionally uses the default artifact name. Disable the thin jar
// task so it cannot overwrite the dependency-bundled release artifact afterward.
tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")
    dependsOn(spigotAdapter.classesTaskName)
    dependsOn(paperAdapter.classesTaskName)
    from(spigotAdapter.output)
    from(paperAdapter.output)

    // bStats checks its own package at runtime. The project classes are rewritten
    // to this namespace while Libby applies the same relocation to downloaded jars.
    relocate("org.bstats", "org.katacr.kamenu.libs.bstats")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
