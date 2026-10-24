import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.Properties
import java.util.UUID

plugins {
    java
    id("net.minecraftforge.gradle") version "6.0.54"
    id("org.spongepowered.mixin") version "0.7.38"
}
// Explicit user retirement: no legacy/v2/optional adult-size calibration or its diagnostics.
// Reject old entry points BEFORE allocating an attempt, rather than silently skipping a profile.
val retiredGrowthProperties = setOf("optionalGrowthCalibration", "optionalGrowthCalibrationForms",
    "growthFixtureVersion", "growthFixtureScope", "growthFixtureControl", "growthDiagnostics",
    "originalEventTrace", "originalEventTraceScope", "lightQueueProof")
require(retiredGrowthProperties.none { project.hasProperty(it) }) {
    "RETIRED: adult-size/manual-growth-pulse calibration and associated diagnostics"
}
group = "com.bettercontent"
version = property("mod_version") as String
base { archivesName.set("better-wild-fruit-trees") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
val nativeAttempts = project.file("run-gametest/attempts").toPath()
require(!Files.isSymbolicLink(nativeAttempts) && !Files.isSymbolicLink(nativeAttempts.parent)) { "Native attempt ancestry must not be a symlink" }
Files.createDirectories(nativeAttempts)
val requestedAttempt = providers.gradleProperty("nativeGameTestAttemptId").orNull
val nativeAttemptId = requestedAttempt?.let {
    UUID.fromString(it).toString().also { canonical -> require(canonical == it) { "Noncanonical native attempt UUID" } }
} ?: UUID.randomUUID().toString()
val nativeAttempt = Files.createDirectory(nativeAttempts.resolve(nativeAttemptId)).toFile()
logger.lifecycle("Native GameTest fresh UUID admitted: $nativeAttemptId; existing world=false")
minecraft {
    mappings("official", "1.20.1")
    copyIdeResources = true
    runs {
        configureEach {
            workingDirectory(project.file("run"))
            property("forge.logging.console.level", "info")
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
            mods { create("better_wild_fruit_trees") { source(sourceSets.main.get()) } }
        }
        create("client")
        create("server") { arg("--nogui") }
        create("gameTestServer") {
            workingDirectory(nativeAttempt)
            property("forge.enabledGameTestNamespaces", "better_wild_fruit_trees")
            property("better_wild_fruit_trees.fixture.tconstruct", (!project.hasProperty("withoutTConFixture")).toString())
            property("better_wild_fruit_trees.fixture.ars_nouveau", (!project.hasProperty("withoutArsFixture")).toString())
            property("better_wild_fruit_trees.fixture.wild_addons", (!project.hasProperty("withoutWildAddonsFixture")).toString())
            arg("--nogui")
        }
    }
}
repositories {
    maven("https://maven.minecraftforge.net")
    maven("https://cursemaven.com") { content { includeGroup("curse.maven") } }
    mavenCentral()
}
val coconutNativeAbi by configurations.creating { isTransitive = false }
dependencies {
    add(coconutNativeAbi.name, "curse.maven:natures-spirit-1044992:6962783")
    minecraft("net.minecraftforge:forge:1.20.1-47.4.13")
    implementation(fg.deobf("curse.maven:dynamic-trees-252818:7502488"))
    implementation(fg.deobf("curse.maven:dynamic-trees-fruits-delight-1360918:7079680"))
    runtimeOnly(fg.deobf("curse.maven:fruits-delight-943774:8304691"))
    runtimeOnly(fg.deobf("curse.maven:farmers-delight-398521:8007609"))
    runtimeOnly(fg.deobf("curse.maven:patchouli-306770:7731017"))
    compileOnly(fg.deobf("curse.maven:tconstruct-74072:7449219"))
    compileOnly(fg.deobf("curse.maven:mantle-74924:7563777"))
    if (!project.hasProperty("withoutTConFixture")) {
        runtimeOnly(fg.deobf("curse.maven:tconstruct-74072:7449219"))
        runtimeOnly(fg.deobf("curse.maven:mantle-74924:7563777"))
    }
    if (!project.hasProperty("withoutArsFixture")) {
        runtimeOnly(fg.deobf("curse.maven:ars-nouveau-401955:6688854"))
        runtimeOnly(fg.deobf("curse.maven:curios-309927:6418456"))
        runtimeOnly(fg.deobf("curse.maven:geckolib-388172:7553267"))
    }
    compileOnly(fg.deobf("curse.maven:natures-spirit-1044992:6962783"))
    compileOnly(fg.deobf("curse.maven:dynamic-trees-plus-478155:7502535"))
    compileOnly(fg.deobf("curse.maven:dynamic-trees-natures-spirit-1049917:6735910"))
    if (!project.hasProperty("withoutWildAddonsFixture")) {
        runtimeOnly(fg.deobf("curse.maven:dynamic-trees-plus-478155:7502535"))
        runtimeOnly(fg.deobf("curse.maven:dynamic-trees-natures-spirit-1049917:6735910"))
        runtimeOnly(fg.deobf("curse.maven:natures-spirit-1044992:6962783"))
        runtimeOnly(fg.deobf("curse.maven:terrablender-563928:6290448"))
        runtimeOnly(fg.deobf("curse.maven:dynamic-trees-quark-386753:5511673"))
        runtimeOnly(fg.deobf("curse.maven:quark-243121:6427817"))
        runtimeOnly(fg.deobf("curse.maven:zeta-968868:7335229"))
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}
mixin {
    add(sourceSets.main.get(), "better_wild_fruit_trees.refmap.json")
    config("better_wild_fruit_trees.mixins.json")
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.isIncremental = false
}
tasks.named<JavaCompile>("compileJava") { outputs.file(layout.buildDirectory.file("tmp/compileJava/compileJava-refmap.json")) }
val prepareMixinResources by tasks.registering(Copy::class) {
    dependsOn("compileJava")
    from(layout.buildDirectory.file("tmp/compileJava/compileJava-refmap.json"))
    into(layout.buildDirectory.dir("generated/mixinResources"))
    rename { "better_wild_fruit_trees.refmap.json" }
}
sourceSets.main { resources.srcDir(prepareMixinResources) }
tasks.test { useJUnitPlatform() }
tasks.named<Jar>("jar") {
    exclude("com/bettercontent/betterwildfruittrees/gametest/**", "gameteststructures/**", "**/*native-light*", "**/*original-events*", "**/*.wftord", "**/*light-queue-proof*", "**/*.wftq", "**/*growth-v2*")
    filesMatching("better_wild_fruit_trees.refmap.json") { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
    manifest { attributes("Implementation-Version" to project.version) }
    finalizedBy("reobfJar")
}
val stageRuntimeJar by tasks.registering(Copy::class) {
    dependsOn("reobfJar")
    from(layout.buildDirectory.file("reobfJar/output.jar"))
    into(layout.buildDirectory.dir("libs"))
    rename { "${base.archivesName.get()}-$version.jar" }
}
tasks.named("assemble") { dependsOn(stageRuntimeJar) }
val syncGameTestStructures by tasks.registering(Copy::class) {
    from("src/main/resources/gameteststructures")
    into(nativeAttempt.resolve("gameteststructures"))
}
val prepareGameTestFiles by tasks.registering(Copy::class) {
    from("src/gametest/fixture")
    into(nativeAttempt)
}
val resetGameTestWorld by tasks.registering {
    doLast { check(!nativeAttempt.resolve("world").exists()) { "Refusing existing native world; retain the entire prior attempt" } }
}
val configureGameTestWorldSeed by tasks.registering {
    dependsOn(prepareGameTestFiles)
    doLast {
        val properties = nativeAttempt.resolve("server.properties")
        val seedLine = "level-seed=8675309"
        val lines = if (properties.isFile) properties.readLines() else emptyList()
        val updated = lines.map { if (it.startsWith("level-seed=")) seedLine else it }.toMutableList()
        if (lines.none { it.startsWith("level-seed=") }) updated += seedLine
        properties.writeText(updated.joinToString("\n", postfix = "\n"))
    }
}
val verifyNativeRunConfigGuard by tasks.registering {
    doLast {
        minecraft.runs.forEach { run ->
            val native = run.name == "gameTestServer"
            val expectedSources = mutableSetOf(sourceSets.main.get())
            val nativeTimingEnabled = native && System.getenv("BC_TIMING_NATIVE") == "true"
            if (nativeTimingEnabled) {
                check(project.extensions.extraProperties.has("bcNativeTimingInstalled") &&
                    project.extensions.extraProperties.get("bcNativeTimingInstalled") == true) { "Native timing requires owning Root adapter" }
                val timing = sourceSets.getByName("bcNativeTiming")
                check(timing.java.srcDirs.size == 1 && timing.resources.srcDirs.size == 1 &&
                    timing.java.srcDirs.single().resolve("bc/nonshipping/nativetiming/NativeTimingObserver.java").isFile &&
                    timing.resources.srcDirs.single().resolve("bc_native_timing.mixins.json").isFile &&
                    timing.output.classesDirs.files.intersect(sourceSets.main.get().output.classesDirs.files).isEmpty()) { "Native timing source isolation drift" }
                expectedSources.add(timing)
            }
            check(run.allSources.toSet() == expectedSources && run.mods.getByName("better_wild_fruit_trees").sources.toSet() == expectedSources) { "Native/ordinary classpath drift" }
            check(run.properties.keys.none { key -> retiredGrowthProperties.any { key == "better_wild_fruit_trees.$it" } || key == "better_wild_fruit_trees.calibration.forms" }) { "Retired calibration property injected" }
            if (native) {
                check(file(run.workingDirectory).canonicalFile == nativeAttempt.canonicalFile) { "Native directory drift" }
                check(run.properties["forge.enabledGameTestNamespaces"] == "better_wild_fruit_trees") { "Native namespace drift" }
                val pinnedSdkArgs = listOf("--launchTarget", "forgegametestserveruserdev", "--gameDir", ".",
                    "--fml.forgeVersion", "47.4.13", "--fml.mcVersion", "1.20.1", "--fml.forgeGroup",
                    "net.minecraftforge", "--fml.mcpVersion", "20230612.114412")
                val expectedArgs = pinnedSdkArgs + listOf("--nogui") +
                    (if (nativeTimingEnabled) listOf("--mixin.config", "bc_native_timing.mixins.json") else emptyList()) +
                    listOf("--mixin.config", "better_wild_fruit_trees.mixins.json")
                check(run.args.map { it.toString() } == expectedArgs) { "Native effective SDK/authored args override: ${run.args}" }
                check(!nativeAttempt.resolve("world").exists() && !nativeAttempt.resolve("server.properties").exists()) { "Native input existed before own prepare" }
                val receipt = Properties()
                receipt.setProperty("phase", "BEFORE_OWN_PREPARE_OR_ENGINE")
                receipt.setProperty("attempt", nativeAttemptId)
                receipt.setProperty("actualRunConfigDirectory", file(run.workingDirectory).canonicalPath)
                receipt.setProperty("actualNamespace", "better_wild_fruit_trees")
                receipt.setProperty("actualArgs", run.args.joinToString(" "))
                receipt.setProperty("profile", "functional25-calibration-retired-v1")
                receipt.setProperty("worldAbsent", "true")
                receipt.setProperty("propertiesAbsent", "true")
                receipt.setProperty("customProviderBindingConsumed", "false")
                listOf("tconstruct", "ars_nouveau", "wild_addons").forEach { name ->
                    receipt.setProperty("actualFixture.$name", run.properties["better_wild_fruit_trees.fixture.$name"].toString())
                }
                Files.newBufferedWriter(nativeAttempt.resolve("before-own-prepare.properties").toPath(), StandardOpenOption.CREATE_NEW).use {
                    receipt.store(it, "Actual functional native RunConfig; calibration retired, not an acceptance verdict")
                }
            } else check(file(run.workingDirectory).canonicalFile == file("run").canonicalFile) { "Ordinary directory drift" }
        }
    }
}
listOf(syncGameTestStructures, prepareGameTestFiles, resetGameTestWorld, configureGameTestWorldSeed).forEach {
    it.configure { dependsOn(verifyNativeRunConfigGuard) }
}
tasks.matching { it.name.startsWith("prepareRunGameTestServer") }.configureEach {
    dependsOn(syncGameTestStructures, prepareGameTestFiles, resetGameTestWorld, configureGameTestWorldSeed)
}
val verifyScripts by tasks.registering(Exec::class) {
    commandLine("python3", "-B", "-m", "unittest", "discover", "-s", "scripts", "-p", "test_*.py")
}
val verifyNativeLog by tasks.registering(Exec::class) {
    dependsOn("runGameTestServer")
    commandLine("python3", "-B", "scripts/verify_artifact.py", "log", nativeAttempt.resolve("logs/latest.log").absolutePath)
}
val verifyCoconutMapping by tasks.registering {
    doLast {
        val mapped = configurations.compileClasspath.get().files.single { it.name.startsWith("natures-spirit-1044992-6962783_mapped_") && it.extension == "jar" }
        val processor = configurations.annotationProcessor.get().files.single { it.name == "mixin-0.8.5-processor.jar" }
        exec { commandLine("python3", "-B", "scripts/coconut_mapping.py", "native", coconutNativeAbi.singleFile.absolutePath, mapped.absolutePath) }
        exec { commandLine("python3", "-B", "scripts/check_coconut_ap.py", processor.absolutePath) }
    }
}
val verifyRuntimeJar by tasks.registering(Exec::class) {
    dependsOn(stageRuntimeJar, verifyCoconutMapping)
    commandLine("python3", "-B", "scripts/verify_artifact.py", "jar", "build/libs/${base.archivesName.get()}-$version.jar")
}
tasks.register("verifyFast") { dependsOn("check", verifyScripts, verifyNativeRunConfigGuard) }
tasks.register("verifyFull") { dependsOn("verifyFast", verifyNativeLog, verifyRuntimeJar) }
