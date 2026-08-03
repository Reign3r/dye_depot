import org.gradle.api.tasks.testing.Test

plugins {
    id("com.possible-triangle.fabric")
}

fabric {
    accessWidener()
    dataGen {
        splitSourceSet()

        existing("supplementaries")
        existing("suppsquared")
    }
}

fabricApi {
    configureTests {
        createSourceSet.set(true)
        modId.set("dye_depot_gametest")
        enableGameTests.set(true)
        enableClientGameTests.set(true)
        eula.set(true)
    }
}

repositories {
    maven("https://maven.nucleoid.xyz")

    maven {
        url = uri("https://maven.blamejared.com/")
        content {
            includeGroup("mezz.jei")
        }
    }

    nexus {
        content {
            includeGroup("io.github.fabricators_of_create.Porting-Lib")
            includeGroup("com.possible-triangle")
        }
    }
}

dependencies {
    modImplementation("eu.pb4:polymer-core:${property("polymer_version")}")
    modImplementation("eu.pb4:polymer-blocks:${property("polymer_version")}")
    modImplementation("eu.pb4:polymer-resource-pack:${property("polymer_version")}")
    modImplementation("eu.pb4:polymer-resource-pack-extras:${property("polymer_version")}")
    modImplementation("eu.pb4:polymer-virtual-entity:${property("polymer_version")}")

    // No 26.2 builds of these optional development/compatibility mods are available yet.
    // Their data and resource compatibility remains bundled in this mod.
    add("testImplementation", "net.fabricmc:fabric-loader-junit:${property("fabric_loader_version")}")
}

val testRuntimeDirectory =
    layout.buildDirectory
        .dir("test-runtime")
        .get()
        .asFile

tasks.withType<Test>().configureEach {
    // The loader helper disables standard tests by default; this project has
    // loader-aware JUnit regression tests that are part of the parity gate.
    setOnlyIf { true }
    useJUnitPlatform()
    // The production mod is server-only; run loader-aware JUnit through the
    // same environment so its mixins and initializer are exercised.
    systemProperty("fabric.side", "server")
    // Loader-aware tests create server config/log directories. Keep those
    // transient files under build instead of polluting the repository root.
    workingDir(testRuntimeDirectory)
    doFirst { testRuntimeDirectory.mkdirs() }
}

tasks.named("compileTestJava") {
    setOnlyIf { true }
}

val (version, type) = mod.version.get().split("-")

mod.version = version

upload.maven {
    name = "${mod.id.get()}-$type"
    artifactVersion = "${mod.minecraftVersion.get()}-${mod.version.get()}"

    nexus()
}

enableSpotless()
