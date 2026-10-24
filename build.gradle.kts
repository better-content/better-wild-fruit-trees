import java.io.ByteArrayOutputStream
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
val lightQueueProofValue = providers.gradleProperty("lightQueueProof").orNull
require(lightQueueProofValue == null || lightQueueProofValue in setOf("true", "false")) { "lightQueueProof must be true or false" }
val lightQueueProofEnabled = lightQueueProofValue == "true"
require(!lightQueueProofEnabled || listOf("optionalGrowthCalibration", "withoutTConFixture", "withoutArsFixture", "withoutWildAddonsFixture").none { project.hasProperty(it) }) { "Queue proof requires the unchanged default present native fixture" }
val originalEventTraceValue = providers.gradleProperty("originalEventTrace").orNull
require(originalEventTraceValue == null || originalEventTraceValue in setOf("true", "false")) { "originalEventTrace must be true or false" }
val originalEventTrace = originalEventTraceValue == "true"
val originalEventTraceScopeValue = providers.gradleProperty("originalEventTraceScope").orNull
require(originalEventTraceScopeValue == null || originalEventTraceScopeValue in setOf("orange-v1", "kiwi-v1")) { "Unknown ordered observation scope" }
require(originalEventTraceScopeValue == null || originalEventTrace) { "Ordered scope requires explicit originalEventTrace=true" }
val originalEventTraceScope = originalEventTraceScopeValue ?: "orange-v1"
require(!lightQueueProofEnabled || !originalEventTrace) { "Queue proof and original ordered growth profile are separate experiments" }
val growthDiagnosticsValue = providers.gradleProperty("growthDiagnostics").orNull
require(growthDiagnosticsValue == null || growthDiagnosticsValue in setOf("true", "false")) { "growthDiagnostics must be true or false" }
require(growthDiagnosticsValue != "false" || (!originalEventTrace && !lightQueueProofEnabled)) { "Explicit diagnostic profiles cannot disable growthDiagnostics" }
val growthDiagnosticsEnabled = growthDiagnosticsValue == "true" || originalEventTrace || lightQueueProofEnabled
val calibrationEnabled = project.hasProperty("optionalGrowthCalibration")
require(originalEventTraceScope != "kiwi-v1" || !calibrationEnabled) { "Kiwi observation requires the original main26 fixture" }
val growthFixtureVersion = providers.gradleProperty("growthFixtureVersion").orNull ?: "v1"
val growthFixtureScope = providers.gradleProperty("growthFixtureScope").orNull ?: "full"
val growthFixtureControl = providers.gradleProperty("growthFixtureControl").orNull ?: "none"
require(growthFixtureVersion in setOf("v1", "v2")) { "Unknown growth fixture version" }
require(growthFixtureScope in setOf("full", "prototype")) { "Unknown growth fixture scope" }
require(growthFixtureControl in setOf("none", "zero-energy", "double-cadence")) { "Unknown growth fixture control" }
require(growthFixtureVersion == "v2" || (growthFixtureScope == "full" && growthFixtureControl == "none")) { "V2 controls/scope require v2" }
require(growthFixtureVersion != "v2" || (!calibrationEnabled && !lightQueueProofEnabled && !growthDiagnosticsEnabled &&
    listOf("growthDiagnostics", "originalEventTrace", "originalEventTraceScope", "lightQueueProof").none { project.hasProperty(it) })) { "V2 is separate from all original diagnostic scopes" }
val calibrationForms = providers.gradleProperty("optionalGrowthCalibrationForms").orNull ?: "all"
require(calibrationForms in setOf("all", "ancient", "saguaro,saguaro_mega")) { "Unknown/empty/duplicate calibration profile" }
require(!project.hasProperty("optionalGrowthCalibrationForms") || calibrationEnabled) { "Form selection requires the diagnostic profile" }
require(!calibrationEnabled || !project.hasProperty("withoutWildAddonsFixture")) { "Calibration requires the unchanged present-addon SDK" }
group = "com.bettercontent"
version = property("mod_version") as String
base { archivesName.set("better-wild-fruit-trees") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
// Required observation hooks exist only on the native GameTest classpath, never ordinary launches/JARs.
val nativeProbe = sourceSets.create("nativeProbe") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}
configurations[nativeProbe.annotationProcessorConfigurationName].extendsFrom(configurations.annotationProcessor.get())
val growthFixtureV2 = if (growthFixtureVersion == "v2") sourceSets.create("growthFixtureV2") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
} else null
growthFixtureV2?.let { configurations[it.annotationProcessorConfigurationName].extendsFrom(configurations.annotationProcessor.get()) }
// Absent entirely, including its Mixin registration, unless explicitly requested.
val lightQueueProof = if (lightQueueProofEnabled) sourceSets.create("lightQueueProof") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
} else null
lightQueueProof?.let { configurations[it.annotationProcessorConfigurationName].extendsFrom(configurations.annotationProcessor.get()) }
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
            growthFixtureV2?.let {
                mods.named("better_wild_fruit_trees") { source(it) }
                args("--mixin.config", "better_wild_fruit_trees.growth-v2.mixins.json")
            }
            if (growthDiagnosticsEnabled) {
                mods.named("better_wild_fruit_trees") { source(nativeProbe) }
                args("--mixin.config", "better_wild_fruit_trees.native-light.mixins.json")
                property("better_wild_fruit_trees.growthDiagnostics", "true")
            }
            if (lightQueueProofEnabled) {
                mods.named("better_wild_fruit_trees") { source(lightQueueProof!!) }
                property("better_wild_fruit_trees.lightQueueProof", "true")
            }
            workingDirectory(nativeAttempt)
            if (originalEventTrace) {
                property("better_wild_fruit_trees.originalEventTrace", "true")
                property("better_wild_fruit_trees.originalEventTraceScope", originalEventTraceScope)
            }
            property("better_wild_fruit_trees.growthFixtureVersion", growthFixtureVersion)
            property("better_wild_fruit_trees.growthFixtureScope", growthFixtureScope)
            property("better_wild_fruit_trees.growthFixtureControl", growthFixtureControl)
            property("forge.enabledGameTestNamespaces", if (calibrationEnabled)
                "better_wild_fruit_calibration" else "better_wild_fruit_trees")
            if (calibrationEnabled) property("better_wild_fruit_trees.calibration.forms", calibrationForms)
            property("better_wild_fruit_trees.fixture.tconstruct", (!project.hasProperty("withoutTConFixture")).toString())
            property("better_wild_fruit_trees.fixture.ars_nouveau", (!project.hasProperty("withoutArsFixture")).toString())
            property("better_wild_fruit_trees.fixture.wild_addons", (!project.hasProperty("withoutWildAddonsFixture")).toString())
            arg("--nogui")
            if (lightQueueProofEnabled) args("--mixin.config", "better_wild_fruit_trees.light-queue-proof.mixins.json")
        }
    }
}
repositories {
    maven("https://maven.minecraftforge.net")
    maven("https://cursemaven.com") { content { includeGroup("curse.maven") } }
    mavenCentral()
}
// Raw pinned bytes are an independent production-ABI oracle, not a deobfuscated runtime fixture.
val coconutNativeAbi by configurations.creating { isTransitive = false }
dependencies {
    add(coconutNativeAbi.name, "curse.maven:natures-spirit-1044992:6962783")
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
    // Only the explicit SOURCE proof needs finally-safe native preparation wrappers.
    lightQueueProof?.let {
        add(it.compileOnlyConfigurationName, "io.github.llamalad7:mixinextras-common:0.5.4")
        add(it.runtimeOnlyConfigurationName, "io.github.llamalad7:mixinextras-forge:0.5.4")
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}
mixin {
    add(sourceSets.main.get(), "better_wild_fruit_trees.refmap.json")
    add(nativeProbe, "better_wild_fruit_trees.native-light.refmap.json")
    growthFixtureV2?.let { add(it, "better_wild_fruit_trees.growth-v2.refmap.json") }
    lightQueueProof?.let { add(it, "better_wild_fruit_trees.light-queue-proof.refmap.json") }
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
val prepareNativeProbeMixinResources by tasks.registering(Copy::class) {
    dependsOn("compileNativeProbeJava")
    from(layout.buildDirectory.file("tmp/compileNativeProbeJava/compileNativeProbeJava-refmap.json"))
    into(layout.buildDirectory.dir("generated/nativeProbeMixinResources"))
    rename { "better_wild_fruit_trees.native-light.refmap.json" }
}
nativeProbe.resources.srcDir(prepareNativeProbeMixinResources)
val verifyNativeLightProbe by tasks.registering(Exec::class) {
    dependsOn(nativeProbe.classesTaskName, "createSrgToMcp")
    doFirst {
        val classpath = configurations.compileClasspath.get().files
        val forge = classpath.single { it.name == "forge-1.20.1-47.4.13_mapped_official_1.20.1.jar" }
        val dt = classpath.single { it.name == "dynamic-trees-252818-7502488_mapped_official_1.20.1.jar" }
        val processor = configurations.annotationProcessor.get().files.single { it.name == "mixin-0.8.5-processor.jar" }
        commandLine("python3", "-B", "scripts/check_native_light_probe.py", forge.absolutePath, dt.absolutePath,
            "build/createSrgToMcp/output.srg", processor.absolutePath,
            "build/tmp/compileNativeProbeJava/compileNativeProbeJava-refmap.json", "build/classes/java/nativeProbe")
    }
}
tasks.matching { it.name == "runGameTestServer" }.configureEach { dependsOn(verifyNativeLightProbe) }
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
    // MixinGradle may contribute registered-source-set refmaps to Jar specs independently
    // of source-set outputs. Explicitly exclude the native-only namespace as well as classes.
    exclude("com/bettercontent/betterwildfruittrees/gametest/**", "gameteststructures/**", "**/*native-light*", "**/*original-events*", "**/*.wftord", "**/*light-queue-proof*", "**/*.wftq", "**/*growth-v2*")
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
    if (lightQueueProofEnabled) commandLine("python3", "-B", "scripts/check_light_queue_proof.py", "log", nativeAttempt.resolve("logs/latest.log").absolutePath)
    else commandLine("python3", "-B", "scripts/verify_artifact.py", "log", nativeAttempt.resolve("logs/latest.log").absolutePath)
}
val verifyCoconutMapping by tasks.registering {
    doLast {
        val mapped = configurations.compileClasspath.get().files.single {
            it.name.startsWith("natures-spirit-1044992-6962783_mapped_") && it.extension == "jar"
        }
        val processor = configurations.annotationProcessor.get().files.single { it.name == "mixin-0.8.5-processor.jar" }
        exec { commandLine("python3", "-B", "scripts/coconut_mapping.py", "native", coconutNativeAbi.singleFile.absolutePath, mapped.absolutePath) }
        exec { commandLine("python3", "-B", "scripts/check_coconut_ap.py", processor.absolutePath) }
    }
}
val verifyRuntimeJar by tasks.registering(Exec::class) {
    dependsOn(stageRuntimeJar, verifyCoconutMapping)
    commandLine("python3", "-B", "scripts/verify_artifact.py", "jar", "build/libs/${base.archivesName.get()}-$version.jar")
}
val verifyCalibrationRunConfigGuard by tasks.registering {
    doLast {
        minecraft.runs.forEach { run ->
            val native = run.name == "gameTestServer"
            val expectedNamespace = if (calibrationEnabled) "better_wild_fruit_calibration" else "better_wild_fruit_trees"
            listOf("growthFixtureVersion" to growthFixtureVersion, "growthFixtureScope" to growthFixtureScope,
                "growthFixtureControl" to growthFixtureControl).forEach { (key, value) ->
                check(run.properties["better_wild_fruit_trees.$key"] == if (native) value else null) { "Growth fixture selection escaped native RunConfig" }
            }
            check((run.properties["better_wild_fruit_trees.calibration.forms"] != null) == (native && calibrationEnabled)) { "Calibration property escaped native diagnostic RunConfig" }
            check(run.properties["better_wild_fruit_trees.lightQueueProof"] == if (native && lightQueueProofEnabled) "true" else null) { "Queue proof flag escaped native RunConfig" }
            check(run.properties["better_wild_fruit_trees.originalEventTrace"] == if (native && originalEventTrace) "true" else null) { "Ordered observation flag escaped native RunConfig" }
            check(run.properties["better_wild_fruit_trees.originalEventTraceScope"] == if (native && originalEventTrace) originalEventTraceScope else null) { "Ordered observation scope escaped native RunConfig" }
            check(run.properties["better_wild_fruit_trees.growthDiagnostics"] == if (native && growthDiagnosticsEnabled) "true" else null) { "Growth diagnostics escaped native opt-in RunConfig" }
            val expectedSources = mutableSetOf(sourceSets.main.get())
            if (native && growthDiagnosticsEnabled) expectedSources.add(nativeProbe)
            if (native && growthFixtureV2 != null) expectedSources.add(growthFixtureV2)
            if (native && lightQueueProofEnabled) expectedSources.add(lightQueueProof!!)
            val nativeTimingEnabled = native && System.getenv("BC_TIMING_NATIVE") == "true"
            if (nativeTimingEnabled) {
                check(project.extensions.extraProperties.has("bcNativeTimingInstalled") &&
                    project.extensions.extraProperties.get("bcNativeTimingInstalled") == true) { "Native timing flag requires the owning Root adapter" }
                val timing = sourceSets.getByName("bcNativeTiming")
                check(timing.java.srcDirs.size == 1 && timing.resources.srcDirs.size == 1 &&
                    timing.java.srcDirs.single().resolve("bc/nonshipping/nativetiming/NativeTimingObserver.java").isFile &&
                    timing.resources.srcDirs.single().resolve("bc_native_timing.mixins.json").isFile &&
                    timing.output.classesDirs.files.intersect(sourceSets.main.get().output.classesDirs.files).isEmpty()) {
                    "Native timing must remain the separate nonshipping Root source set"
                }
                expectedSources.add(timing)
            }
            check(run.allSources.toSet() == expectedSources) { "Native/ordinary source classpath drift: ${run.allSources.map { it.name }}" }
            check(run.mods.getByName("better_wild_fruit_trees").sources.toSet() == expectedSources) { "Diagnostic mod source escaped explicit opt-in" }
            if (native) {
                check(file(run.workingDirectory).canonicalFile == nativeAttempt.canonicalFile) { "Native RunConfig directory drift" }
                check(run.properties["forge.enabledGameTestNamespaces"] == expectedNamespace) { "Native namespace drift" }
                if (calibrationEnabled) check(run.properties["better_wild_fruit_trees.calibration.forms"] == calibrationForms) { "Native profile drift" }
                val effectiveArgs = run.args.map { it.toString() }
                val pinnedSdkArgs = listOf("--launchTarget", "forgegametestserveruserdev", "--gameDir", ".",
                    "--fml.forgeVersion", "47.4.13", "--fml.mcVersion", "1.20.1", "--fml.forgeGroup",
                    "net.minecraftforge", "--fml.mcpVersion", "20230612.114412")
                val diagnosticArgs = if (growthDiagnosticsEnabled) listOf("--mixin.config", "better_wild_fruit_trees.native-light.mixins.json") else emptyList()
                val v2Args = if (growthFixtureV2 != null) listOf("--mixin.config", "better_wild_fruit_trees.growth-v2.mixins.json") else emptyList()
                val originalExpectedArgs = pinnedSdkArgs + diagnosticArgs + v2Args + listOf("--nogui", "--mixin.config", "better_wild_fruit_trees.mixins.json")
                val fixtureExpectedArgs = if (lightQueueProofEnabled) originalExpectedArgs.dropLast(2) + listOf("--mixin.config", "better_wild_fruit_trees.light-queue-proof.mixins.json") + originalExpectedArgs.takeLast(2) else originalExpectedArgs
                // Root attaches before MixinGradle appends the final authored config.
                // Admit exactly that observation pair at that boundary; every original
                // SDK/authored argument and its order remain exact, arbitrary extras fail.
                val expectedArgs = if (nativeTimingEnabled) fixtureExpectedArgs.dropLast(2) +
                    listOf("--mixin.config", "bc_native_timing.mixins.json") + fixtureExpectedArgs.takeLast(2) else fixtureExpectedArgs
                check(effectiveArgs == expectedArgs) {
                    "Native effective SDK/authored args override: $effectiveArgs"
                }
                check(!nativeAttempt.resolve("world").exists() && !nativeAttempt.resolve("server.properties").exists()) { "Native input existed before own prepare" }
                // Canonical release passes its stage directory to every ordinary source gate.
                // This mod declares no local custom providers and does not consume that binding.
                // Isolated calibration diagnostics must still reject any provider override.
                val providerBindingAbsent = System.getenv("BC_CUSTOM_MOD_JAR_DIR").isNullOrBlank()
                if (calibrationEnabled) check(providerBindingAbsent) { "Unexpected custom provider override" }
                if (growthFixtureVersion == "v2" && (growthFixtureScope != "full" || growthFixtureControl != "none"))
                    check(providerBindingAbsent) { "V2 diagnostics cannot consume custom provider overrides" }
                val receipt = Properties()
                receipt.setProperty("phase", "BEFORE_OWN_PREPARE_OR_ENGINE")
                receipt.setProperty("attempt", nativeAttemptId)
                receipt.setProperty("actualRunConfigDirectory", file(run.workingDirectory).canonicalPath)
                receipt.setProperty("actualNamespace", run.properties["forge.enabledGameTestNamespaces"].toString())
                receipt.setProperty("actualArgs", run.args.joinToString(" "))
                receipt.setProperty("growthDiagnostics", growthDiagnosticsEnabled.toString())
                receipt.setProperty("growthFixtureVersion", growthFixtureVersion)
                receipt.setProperty("growthFixtureScope", growthFixtureScope)
                receipt.setProperty("growthFixtureControl", growthFixtureControl)
                receipt.setProperty("originalEventTraceScope", if (originalEventTrace) originalEventTraceScope else "NONE")
                receipt.setProperty("actualAllSourceSets", run.allSources.map { it.name }.distinct().sorted().joinToString(","))
                receipt.setProperty("actualDiagnosticMixin", if (growthDiagnosticsEnabled) "better_wild_fruit_trees.native-light.mixins.json" else "NONE")
                receipt.setProperty("profile", if (lightQueueProofEnabled) "light-queue-proof-v1-required29" else if (calibrationEnabled) calibrationForms else "main26")
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
if (lightQueueProof != null) {
    val proofSet = lightQueueProof
    val proofResources = tasks.register<Copy>("prepareLightQueueProofMixinResources") {
        dependsOn("compileLightQueueProofJava")
        from(layout.buildDirectory.file("tmp/compileLightQueueProofJava/compileLightQueueProofJava-refmap.json"))
        into(layout.buildDirectory.dir("generated/lightQueueProofMixinResources"))
        rename { "better_wild_fruit_trees.light-queue-proof.refmap.json" }
    }
    proofSet.resources.srcDir(proofResources)
    val proofAbi = tasks.register<Exec>("verifyLightQueueProofAbi") {
        dependsOn(proofSet.classesTaskName, "createSrgToMcp")
        doFirst {
            val forge = configurations.compileClasspath.get().files.single { it.name == "forge-1.20.1-47.4.13_mapped_official_1.20.1.jar" }
            val processor = configurations.annotationProcessor.get().files.single { it.name == "mixin-0.8.5-processor.jar" }
            commandLine("python3", "-B", "scripts/check_light_queue_proof.py", "sdk", forge.absolutePath, processor.absolutePath,
                "build/createSrgToMcp/output.srg", "build/tmp/compileLightQueueProofJava/compileLightQueueProofJava-refmap.json", "build/classes/java/lightQueueProof")
        }
    }
    val proofBindings = tasks.register("prepareLightQueueProofBindings") {
        dependsOn(verifyCalibrationRunConfigGuard, proofAbi, nativeProbe.classesTaskName, sourceSets.main.get().classesTaskName)
        doLast {
            fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
            fun git(vararg args: String): String { val out = ByteArrayOutputStream(); exec { commandLine(listOf("git") + args); standardOutput = out }; return out.toString("UTF-8").trim() }
            check(git("status", "--porcelain").isEmpty()) { "Queue proof requires clean committed Source" }
            val lines = listOf("main" to sourceSets.main.get(), "nativeProbe" to nativeProbe, "lightQueueProof" to proofSet).flatMap { (name, set) ->
                set.output.classesDirs.files.flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "class" }.map { cls -> "$name/${cls.relativeTo(root).invariantSeparatorsPath} ${sha(cls.readBytes())}" }.toList() }
            }.sorted()
            val manifest = (lines.joinToString("\n") + "\n").toByteArray(Charsets.UTF_8)
            Files.write(nativeAttempt.resolve("light-queue-proof-classes.sha256").toPath(), manifest, StandardOpenOption.CREATE_NEW)
            val binding = Properties()
            binding.setProperty("nonce", nativeAttemptId); binding.setProperty("attemptDirectory", nativeAttempt.canonicalPath)
            binding.setProperty("profile", "light-queue-proof-v1-required29"); binding.setProperty("sourceCommit", git("rev-parse", "HEAD")); binding.setProperty("sourceDirty", "false")
            binding.setProperty("classHashSetSha256", sha(manifest))
            val forge = configurations.compileClasspath.get().files.single { it.name == "forge-1.20.1-47.4.13_mapped_official_1.20.1.jar" }
            binding.setProperty("sdkSha256", sha(forge.readBytes()))
            binding.setProperty("fixturePreparation", "exact3-native-ticking-v1")
            val extras = configurations[proofSet.compileClasspathConfigurationName].files.single { it.name == "mixinextras-common-0.5.4.jar" }
            check(sha(extras.readBytes()) == "e2de9240ce39af783a83bcd315445b84d675c01e4655f3ad55c163071ce15fee") { "Preparation wrapper SDK drift" }
            binding.setProperty("preparationSdkSha256", sha(extras.readBytes()))
            val extrasRuntime = configurations[proofSet.runtimeClasspathConfigurationName].files.single { it.name == "mixinextras-forge-0.5.4.jar" }
            check(sha(extrasRuntime.readBytes()) == "7922899a121a27f63a69a9ffe57470d8719cc52d239dfa9408e15d32a7b4c264") { "Preparation runtime SDK drift" }
            binding.setProperty("preparationRuntimeSha256", sha(extrasRuntime.readBytes()))
            binding.setProperty("nativeInputReceiptSha256", sha(nativeAttempt.resolve("before-own-prepare.properties").readBytes()))
            Files.newBufferedWriter(nativeAttempt.resolve("light-queue-proof-bindings.properties").toPath(), StandardOpenOption.CREATE_NEW).use { binding.store(it, "SOURCE mechanism experiment, not a fence or original growth qualification") }
        }
    }
    tasks.matching { it.name == "runGameTestServer" }.configureEach { dependsOn(proofBindings) }
}
val prepareOriginalEventBindings by tasks.registering {
    dependsOn(verifyCalibrationRunConfigGuard, nativeProbe.classesTaskName, sourceSets.main.get().classesTaskName)
    onlyIf { originalEventTrace }
    doLast {
        fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        fun git(vararg args: String): String {
            val output = ByteArrayOutputStream()
            exec { commandLine(listOf("git") + args); standardOutput = output; isIgnoreExitValue = true }
            return output.toString("UTF-8").trim()
        }
        val lines = listOf("main" to sourceSets.main.get(), "nativeProbe" to nativeProbe).flatMap { (name, set) ->
            set.output.classesDirs.files.flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "class" }.map { cls ->
                "$name/${cls.relativeTo(root).invariantSeparatorsPath} ${sha(cls.readBytes())}"
            }.toList() }
        }.sorted()
        val manifest = (lines.joinToString("\n") + "\n").toByteArray(Charsets.UTF_8)
        Files.write(nativeAttempt.resolve("original-events-classes.sha256").toPath(), manifest, StandardOpenOption.CREATE_NEW)
        val binding = Properties()
        binding.setProperty("nonce", nativeAttemptId)
        binding.setProperty("attemptDirectory", nativeAttempt.canonicalPath)
        binding.setProperty("profile", (if (calibrationEnabled) calibrationForms else "main26") + ":tcon=" + !project.hasProperty("withoutTConFixture") + ":ars=" + !project.hasProperty("withoutArsFixture") + ":wild=" + !project.hasProperty("withoutWildAddonsFixture") + if (originalEventTraceScope == "kiwi-v1") ":traceScope=kiwi-v1" else "")
        binding.setProperty("traceScope", originalEventTraceScope)
        binding.setProperty("observedSpecies", if (originalEventTraceScope == "kiwi-v1") "kiwi" else "orange")
        binding.setProperty("sourceCommit", git("rev-parse", "HEAD"))
        binding.setProperty("sourceDirty", (git("status", "--porcelain").isNotEmpty()).toString())
        binding.setProperty("classHashSetSha256", sha(manifest))
        val receipt = nativeAttempt.resolve("before-own-prepare.properties")
        if (receipt.isFile) binding.setProperty("nativeInputReceiptSha256", sha(receipt.readBytes()))
        Files.newBufferedWriter(nativeAttempt.resolve("original-events-bindings.properties").toPath(), StandardOpenOption.CREATE_NEW).use { binding.store(it, "Postcompile pre-run native-only ordered observation binding") }
    }
}
tasks.matching { it.name == "runGameTestServer" }.configureEach { dependsOn(prepareOriginalEventBindings) }
if (growthFixtureV2 != null) {
    val fixture = growthFixtureV2
    val resources = tasks.register<Copy>("prepareGrowthFixtureMixinResources") {
        dependsOn(fixture.compileJavaTaskName)
        from(layout.buildDirectory.file("tmp/compileGrowthFixtureV2Java/compileGrowthFixtureV2Java-refmap.json"))
        into(layout.buildDirectory.dir("generated/growthFixtureV2MixinResources"))
        rename { "better_wild_fruit_trees.growth-v2.refmap.json" }
    }
    fixture.resources.srcDir(resources)
    val isolation = tasks.register<Exec>("verifyGrowthFixtureIsolation") {
        dependsOn(fixture.classesTaskName, "createSrgToMcp")
        doFirst {
            val cp = configurations.compileClasspath.get().files
            val forge = cp.single { it.name == "forge-1.20.1-47.4.13_mapped_official_1.20.1.jar" }
            val dt = cp.single { it.name == "dynamic-trees-252818-7502488_mapped_official_1.20.1.jar" }
            commandLine("python3", "-B", "scripts/check_growth_fixture_sdk.py", forge.absolutePath, dt.absolutePath,
                "build/tmp/compileGrowthFixtureV2Java/compileGrowthFixtureV2Java-refmap.json", "build/classes/java/growthFixtureV2")
        }
    }
    tasks.matching { it.name == "runGameTestServer" || it.name == "verifyFast" }.configureEach { dependsOn(isolation) }
}
val prepareGrowthFixtureBindings by tasks.registering {
    dependsOn(verifyCalibrationRunConfigGuard, sourceSets.main.get().classesTaskName)
    dependsOn(provider { minecraft.runs.getByName("gameTestServer").allSources.map { it.classesTaskName } })
    onlyIf { growthFixtureVersion == "v2" }
    doLast {
        fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        fun git(directory: File, vararg args: String): String {
            val out = ByteArrayOutputStream()
            exec { workingDir(directory); commandLine(listOf("git") + args); standardOutput = out }
            return out.toString("UTF-8").trim()
        }
        check(git(projectDir, "status", "--porcelain").isEmpty()) { "V2 native requires frozen clean committed Source" }
        val root = file("../../better-content-modpack")
        check(git(root, "status", "--porcelain").isEmpty()) { "V2 native requires clean Root" }
        val classLines = minecraft.runs.getByName("gameTestServer").allSources.flatMap { set ->
            set.output.classesDirs.files.flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "class" }
                .map { "${set.name}/${it.relativeTo(dir).invariantSeparatorsPath} ${sha(it.readBytes())}" }.toList() }
        }.sorted()
        val manifest = (classLines.joinToString("\n") + "\n").toByteArray(Charsets.UTF_8)
        Files.write(nativeAttempt.resolve("growth-v2-classes.sha256").toPath(), manifest, StandardOpenOption.CREATE_NEW)
        val inputs = sourceSets.main.get().runtimeClasspath.files.filter { it.isFile }.map { "${it.name} ${sha(it.readBytes())}" }.sorted()
        val inputManifest = (inputs.joinToString("\n") + "\n").toByteArray(Charsets.UTF_8)
        Files.write(nativeAttempt.resolve("growth-v2-inputs.sha256").toPath(), inputManifest, StandardOpenOption.CREATE_NEW)
        val binding = Properties()
        binding.setProperty("fixtureVersion", growthFixtureVersion); binding.setProperty("scope", growthFixtureScope)
        binding.setProperty("control", growthFixtureControl); binding.setProperty("sourceDirty", "false")
        binding.setProperty("sourceCommit", git(projectDir, "rev-parse", "HEAD")); binding.setProperty("rootCommit", git(root, "rev-parse", "HEAD"))
        binding.setProperty("nonce", nativeAttemptId); binding.setProperty("attemptDirectory", nativeAttempt.canonicalPath)
        binding.setProperty("profile", "tcon=" + !project.hasProperty("withoutTConFixture") + ":ars=" + !project.hasProperty("withoutArsFixture") + ":wild=" + !project.hasProperty("withoutWildAddonsFixture"))
        binding.setProperty("classHashSetSha256", sha(manifest)); binding.setProperty("inputHashSetSha256", sha(inputManifest))
        binding.setProperty("nativeInputReceiptSha256", sha(nativeAttempt.resolve("before-own-prepare.properties").readBytes()))
        listOf("TreeGrowthRates.java", "gametest/GrowthFixturePlan.java", "gametest/GrowthFixtureController.java", "gametest/GrowthFixtureV2.java", "gametest/GrowthRateGameTests.java").forEach { path ->
            binding.setProperty("sourceSha256.$path", sha(file("src/main/java/com/bettercontent/betterwildfruittrees/$path").readBytes()))
        }
        binding.setProperty("buildSha256", sha(file("build.gradle.kts").readBytes()))
        Files.newBufferedWriter(nativeAttempt.resolve("growth-v2-bindings.properties").toPath(), StandardOpenOption.CREATE_NEW).use { binding.store(it, "V2 frozen inputs; not acceptance") }
    }
}
tasks.matching { it.name == "runGameTestServer" }.configureEach { dependsOn(prepareGrowthFixtureBindings) }
if (growthFixtureVersion == "v2") verifyNativeLog.configure {
    doLast { exec { commandLine("python3", "-B", "scripts/check_growth_v2.py", "--attempt", nativeAttempt.absolutePath,
        "--scope", growthFixtureScope, "--profile", "tcon=" + !project.hasProperty("withoutTConFixture") + ":ars=" + !project.hasProperty("withoutArsFixture") + ":wild=" + !project.hasProperty("withoutWildAddonsFixture")) } }
}
tasks.register("verifyFast") { dependsOn("check", verifyScripts, verifyCalibrationRunConfigGuard) }
tasks.register("verifyFull") { dependsOn("verifyFast", verifyNativeLog, verifyRuntimeJar) }
