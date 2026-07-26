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
    // No 26.2 builds of these optional development/compatibility mods are available yet.
    // Their data and resource compatibility remains bundled in this mod.
    add("testImplementation", "net.fabricmc:fabric-loader-junit:${property("fabric_loader_version")}")
}

tasks.withType<Test>().configureEach {
    // The loader helper disables standard tests by default; this project has
    // loader-aware JUnit regression tests that are part of the parity gate.
    setOnlyIf { true }
    useJUnitPlatform()
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
