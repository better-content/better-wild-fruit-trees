import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID

plugins {
    java
    id("net.minecraftforge.gradle") version "6.0.54"
    id("org.spongepowered.mixin") version "0.7.38"
}
val calibrationEnabled = project.hasProperty("optionalGrowthCalibration")
val calibrationForms = providers.gradleProperty("optionalGrowthCalibrationForms").orNull ?: "all"
require(calibrationForms in setOf("all", "ancient", "saguaro,saguaro_mega")) { "Unknown/empty/duplicate calibration profile" }
require(!project.hasProperty("optionalGrowthCalibrationForms") || calibrationEnabled) { "Form selection requires the diagnostic profile" }
require(!calibrationEnabled || !project.hasProperty("withoutWildAddonsFixture")) { "Calibration requires the unchanged present-addon SDK" }
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
            property("forge.enabledGameTestNamespaces", if (calibrationEnabled)
                "better_wild_fruit_calibration" else "better_wild_fruit_trees")
            if (calibrationEnabled) property("better_wild_fruit_trees.calibration.forms", calibrationForms)
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
dependencies {
    minecraft("net.minecraftforge:forge:1.20.1-47.4.13")
    implementation(fg.deobf("curse.maven:dynamic-trees-252818:7502488"))
    implementation(fg.deobf("curse.maven:dynamic-trees-fruits-delight-1360918:7079680"))
    runtimeOnly(fg.deobf("curse.maven:fruits-delight-943774:8304691"))
    runtimeOnly(fg.deobf("curse.maven:farmers-delight-398521:8007609"))
    // Development fixture: DT's optional guide recipe references Patchouli's book item.
    runtimeOnly(fg.deobf("curse.maven:patchouli-306770:7731017"))
    // Optional public API integration. Keep these exact pins aligned with the native fixture.
    compileOnly(fg.deobf("curse.maven:tconstruct-74072:7449219"))
    compileOnly(fg.deobf("curse.maven:mantle-74924:7563777"))
    if (!project.hasProperty("withoutTConFixture")) {
        runtimeOnly(fg.deobf("curse.maven:tconstruct-74072:7449219"))
        runtimeOnly(fg.deobf("curse.maven:mantle-74924:7563777"))
    }
    // Optional integration exercised by the default native fixture, never a runtime requirement.
    if (!project.hasProperty("withoutArsFixture")) {
        runtimeOnly(fg.deobf("curse.maven:ars-nouveau-401955:6688854"))
        runtimeOnly(fg.deobf("curse.maven:curios-309927:6418456"))
        runtimeOnly(fg.deobf("curse.maven:geckolib-388172:7553267"))
    }
    // Compile-time target validation for mapped vanilla-method injections on the optional native food block.
    compileOnly(fg.deobf("curse.maven:natures-spirit-1044992:6962783"))
    // Typed public native APIs in nonshipping optional fixture adapters; never private discovery.
    compileOnly(fg.deobf("curse.maven:dynamic-trees-plus-478155:7502535"))
    compileOnly(fg.deobf("curse.maven:dynamic-trees-natures-spirit-1049917:6735910"))
    // Optional addon native fixture; production signatures do not link optional addon classes.
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
    options.isIncremental = false // Required Mixins must always receive a complete annotation-processing pass.
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
// Diagnostic only: the default remains all32; admitted subsets require their own exact form records.
// Actual namespaces, canonical ordinals, independent means and BOTH nonempty logs are fail-closed.
if (calibrationEnabled) {
    tasks.matching { it.name == "runGameTestServer" }.configureEach {
        doLast {
            exec {
                commandLine("python3", "-B", "scripts/check_calibration_log.py", "--profile", calibrationForms,
                    "--latest", nativeAttempt.resolve("logs/latest.log").absolutePath,
                    "--debug", nativeAttempt.resolve("logs/debug.log").absolutePath)
            }
        }
    }
}
tasks.named<Jar>("jar") {
    exclude("com/bettercontent/betterwildfruittrees/gametest/**", "gameteststructures/**")
    filesMatching("better_wild_fruit_trees.refmap.json") { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
    manifest { attributes("Implementation-Version" to project.version) }
    finalizedBy("reobfJar")
}
// A source-only partial implementation must not accidentally become an activatable artifact.
// Native pulse samples are intentionally absent until separately authorized calibration supplies them.
val verifyGrowthReferences by tasks.registering {
    val source = layout.projectDirectory.file("src/main/java/com/bettercontent/betterwildfruittrees/TreeGrowthRates.java")
    inputs.file(source)
    doLast {
        val measured = Regex("Map\\.entry\\(\\\"([^\\\"]+)\\\", new Reference\\(").findAll(source.asFile.readText())
            .map { it.groupValues[1] }.toSet()
        val missing = setOf("saguaro", "coconut", "ancient", "saguaro_mega") - measured
        check(missing.isEmpty()) { "NOT READY: unmeasured native growth references: $missing" }
    }
}
val stageRuntimeJar by tasks.registering(Copy::class) {
    dependsOn("reobfJar", verifyGrowthReferences)
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
    doLast {
        check(!nativeAttempt.resolve("world").exists()) { "Refusing existing native world; retain the entire prior attempt" }
    }
}
val configureGameTestWorldSeed by tasks.registering {
    dependsOn(prepareGameTestFiles)
    doLast {
        val properties = nativeAttempt.resolve("server.properties")
        val seedLine = "level-seed=8675309"
        val lines = if (properties.isFile) properties.readLines() else emptyList()
        val updated = lines.map { if (it.startsWith("level-seed=")) seedLine else it }.toMutableList()
        if (lines.none { it.startsWith("level-seed=") }) updated += seedLine
        properties.parentFile.mkdirs()
        properties.writeText(updated.joinToString("\n", postfix = "\n"))
    }
}
tasks.matching { it.name.startsWith("prepareRunGameTestServer") }.configureEach {
    dependsOn(syncGameTestStructures, prepareGameTestFiles, resetGameTestWorld, configureGameTestWorldSeed)
}
listOf(syncGameTestStructures, prepareGameTestFiles, resetGameTestWorld, configureGameTestWorldSeed).forEach {
    it.configure { dependsOn("verifyCalibrationRunConfigGuard") }
}
val verifyScripts by tasks.registering(Exec::class) {
    commandLine("python3", "-B", "-m", "unittest", "discover", "-s", "scripts", "-p", "test_*.py")
}
val verifyNativeLog by tasks.registering(Exec::class) {
    dependsOn("runGameTestServer")
    commandLine("python3", "-B", "scripts/verify_artifact.py", "log", nativeAttempt.resolve("logs/latest.log").absolutePath)
}
val verifyRuntimeJar by tasks.registering(Exec::class) {
    dependsOn(stageRuntimeJar)
    commandLine("python3", "-B", "scripts/verify_artifact.py", "jar", "build/libs/${base.archivesName.get()}-$version.jar")
}
val verifyCalibrationRunConfigGuard by tasks.registering {
    doLast {
        minecraft.runs.forEach { run ->
            val native = run.name == "gameTestServer"
            val expectedNamespace = if (calibrationEnabled) "better_wild_fruit_calibration" else "better_wild_fruit_trees"
            check((run.properties["better_wild_fruit_trees.calibration.forms"] != null) == (native && calibrationEnabled)) { "Calibration property escaped native diagnostic RunConfig" }
            if (native) {
                check(file(run.workingDirectory).canonicalFile == nativeAttempt.canonicalFile) { "Native RunConfig directory drift" }
                check(run.properties["forge.enabledGameTestNamespaces"] == expectedNamespace) { "Native namespace drift" }
                if (calibrationEnabled) check(run.properties["better_wild_fruit_trees.calibration.forms"] == calibrationForms) { "Native profile drift" }
                val effectiveArgs = run.args.map { it.toString() }
                val pinnedSdkArgs = listOf("--launchTarget", "forgegametestserveruserdev", "--gameDir", ".",
                    "--fml.forgeVersion", "47.4.13", "--fml.mcVersion", "1.20.1", "--fml.forgeGroup",
                    "net.minecraftforge", "--fml.mcpVersion", "20230612.114412")
                check(effectiveArgs == pinnedSdkArgs + listOf("--nogui", "--mixin.config", "better_wild_fruit_trees.mixins.json")) {
                    "Native effective SDK/authored args override: $effectiveArgs"
                }
                check(!nativeAttempt.resolve("world").exists() && !nativeAttempt.resolve("server.properties").exists()) { "Native input existed before own prepare" }
                // Canonical release passes its stage directory to every ordinary source gate.
                // This mod declares no local custom providers and does not consume that binding.
                // Isolated calibration diagnostics must still reject any provider override.
                val providerBindingAbsent = System.getenv("BC_CUSTOM_MOD_JAR_DIR").isNullOrBlank()
                if (calibrationEnabled) check(providerBindingAbsent) { "Unexpected custom provider override" }
                val receipt = Properties()
                receipt.setProperty("phase", "BEFORE_OWN_PREPARE_OR_ENGINE")
                receipt.setProperty("attempt", nativeAttemptId)
                receipt.setProperty("actualRunConfigDirectory", file(run.workingDirectory).canonicalPath)
                receipt.setProperty("actualNamespace", run.properties["forge.enabledGameTestNamespaces"].toString())
                receipt.setProperty("actualArgs", run.args.joinToString(" "))
                receipt.setProperty("profile", if (calibrationEnabled) calibrationForms else "main26")
                receipt.setProperty("worldAbsent", "true")
                receipt.setProperty("propertiesAbsent", "true")
                receipt.setProperty("authoredSeed", "8675309")
                receipt.setProperty("customProviderOverrideAbsent", providerBindingAbsent.toString())
                receipt.setProperty("customProviderBindingConsumed", "false")
                listOf("tconstruct", "ars_nouveau", "wild_addons").forEach { name ->
                    receipt.setProperty("actualFixture.$name", run.properties["better_wild_fruit_trees.fixture.$name"].toString())
                }
                listOf("src/gametest/fixture/server.properties", "build.gradle.kts",
                    "src/main/java/com/bettercontent/betterwildfruittrees/gametest/CalibrationPlan.java",
                    "src/main/java/com/bettercontent/betterwildfruittrees/gametest/OptionalGrowthCalibration.java").forEach { path ->
                    val hash = MessageDigest.getInstance("SHA-256").digest(file(path).readBytes())
                    receipt.setProperty("sourceSha256.$path", hash.joinToString("") { "%02x".format(it.toInt() and 255) })
                }
                val receiptPath = nativeAttempt.resolve("before-own-prepare.properties").toPath()
                Files.newBufferedWriter(receiptPath, StandardOpenOption.CREATE_NEW).use { receipt.store(it, "Actual native RunConfig; not a Native acceptance verdict") }
            } else check(file(run.workingDirectory).canonicalFile == file("run").canonicalFile) { "Ordinary RunConfig changed" }
        }
    }
}
tasks.register("verifyFast") { dependsOn("check", verifyScripts, verifyCalibrationRunConfigGuard) }
tasks.register("verifyFull") { dependsOn("verifyFast", verifyNativeLog, verifyRuntimeJar) }
