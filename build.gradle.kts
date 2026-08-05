import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources

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

val collarColors =
    listOf(
        "white",
        "orange",
        "magenta",
        "light_blue",
        "yellow",
        "lime",
        "pink",
        "gray",
        "light_gray",
        "cyan",
        "purple",
        "blue",
        "brown",
        "green",
        "red",
        "black",
        "maroon",
        "rose",
        "coral",
        "indigo",
        "navy",
        "slate",
        "olive",
        "amber",
        "beige",
        "teal",
        "mint",
        "aqua",
        "verdant",
        "forest",
        "ginger",
        "tan",
    )
val catCollarVariants =
    listOf(
        "tabby",
        "black",
        "red",
        "siamese",
        "british_shorthair",
        "calico",
        "persian",
        "ragdoll",
        "white",
        "jellie",
        "all_black",
    )
val wolfCollarVariants =
    linkedMapOf(
        "pale" to "wolf",
        "spotted" to "wolf_spotted",
        "snowy" to "wolf_snowy",
        "black" to "wolf_black",
        "ashen" to "wolf_ashen",
        "rusty" to "wolf_rusty",
        "woods" to "wolf_woods",
        "chestnut" to "wolf_chestnut",
        "striped" to "wolf_striped",
    )
val generatedCollarVariants = layout.buildDirectory.dir("generated/resources/collar-variants")
val generateCollarVariants by tasks.registering {
    inputs.property("colors", collarColors)
    inputs.property("catVariants", catCollarVariants)
    inputs.property("wolfVariants", wolfCollarVariants)
    outputs.dir(generatedCollarVariants)

    doLast {
        val root = generatedCollarVariants.get().asFile
        require(
            root.toPath().startsWith(
                layout.buildDirectory
                    .get()
                    .asFile
                    .toPath(),
            ),
        )
        project.delete(root)
        catCollarVariants.forEach { variant ->
            collarColors.forEach { color ->
                val texture = "dye_depot:entity/cat/collar/minecraft/$variant/$color"
                val target =
                    root.resolve(
                        "data/dye_depot/cat_variant/polymer/collar/cat/minecraft/$variant/$color.json",
                    )
                target.parentFile.mkdirs()
                target.writeText(
                    """{"asset_id":"$texture","baby_asset_id":"${texture}_baby","spawn_conditions":[]}""",
                )
            }
        }
        wolfCollarVariants.forEach { (variant, source) ->
            collarColors.forEach { color ->
                val texture = "dye_depot:entity/wolf/collar/minecraft/$variant/$color"
                val target =
                    root.resolve(
                        "data/dye_depot/wolf_variant/polymer/collar/wolf/minecraft/$variant/$color.json",
                    )
                target.parentFile.mkdirs()
                target.writeText(
                    """{"assets":{"wild":"minecraft:entity/wolf/$source","tame":"$texture/tame","angry":"minecraft:entity/wolf/${source}_angry"},"baby_assets":{"wild":"minecraft:entity/wolf/${source}_baby","tame":"$texture/tame_baby","angry":"minecraft:entity/wolf/${source}_angry_baby"},"spawn_conditions":[]}""",
                )
            }
        }
    }
}

sourceSets.named("main") {
    resources.srcDir(generatedCollarVariants)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateCollarVariants)
}

tasks.named("sourcesJar") {
    dependsOn(generateCollarVariants)
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
