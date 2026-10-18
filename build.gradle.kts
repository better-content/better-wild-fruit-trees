plugins {
    java
    id("net.minecraftforge.gradle") version "6.0.54"
    id("org.spongepowered.mixin") version "0.7.38"
}
group = "com.bettercontent"
version = property("mod_version") as String
base { archivesName.set("better-wild-fruit-trees") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
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
            workingDirectory(project.file("run-gametest"))
            property("forge.enabledGameTestNamespaces", "better_wild_fruit_trees")
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
    // Optional integration exercised by the default native fixture, never a runtime requirement.
    if (!project.hasProperty("withoutArsFixture")) {
        runtimeOnly(fg.deobf("curse.maven:ars-nouveau-401955:6688854"))
        runtimeOnly(fg.deobf("curse.maven:curios-309927:6418456"))
        runtimeOnly(fg.deobf("curse.maven:geckolib-388172:7553267"))
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
tasks.named<Jar>("jar") {
    exclude("com/bettercontent/betterwildfruittrees/gametest/**", "gameteststructures/**")
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
    into("run-gametest/gameteststructures")
}
val prepareGameTestFiles by tasks.registering(Copy::class) {
    from("src/gametest/fixture")
    into("run-gametest")
}
val resetGameTestWorld by tasks.registering {
    doLast {
        val world = file("run-gametest/world")
        if (world.exists()) {
            val retained = file("run-gametest/retained-worlds/${System.currentTimeMillis()}")
            retained.parentFile.mkdirs()
            check(world.renameTo(retained)) { "Could not retain previous GameTest world" }
        }
    }
}
tasks.matching { it.name.startsWith("prepareRunGameTestServer") }.configureEach {
    dependsOn(syncGameTestStructures, prepareGameTestFiles, resetGameTestWorld)
}
val verifyScripts by tasks.registering(Exec::class) {
    commandLine("python3", "-B", "-m", "unittest", "discover", "-s", "scripts", "-p", "test_*.py")
}
val verifyNativeLog by tasks.registering(Exec::class) {
    dependsOn("runGameTestServer")
    commandLine("python3", "-B", "scripts/verify_artifact.py", "log", "run-gametest/logs/latest.log")
}
val verifyRuntimeJar by tasks.registering(Exec::class) {
    dependsOn(stageRuntimeJar)
    commandLine("python3", "-B", "scripts/verify_artifact.py", "jar", "build/libs/${base.archivesName.get()}-$version.jar")
}
tasks.register("verifyFast") { dependsOn("check", verifyScripts) }
tasks.register("verifyFull") { dependsOn("verifyFast", verifyNativeLog, verifyRuntimeJar) }
