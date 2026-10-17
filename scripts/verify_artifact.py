"""Bounded standalone source gates; no modpack inputs or historical evidence required."""
import json
from pathlib import Path
import re
import sys
import zipfile

REFMAP = "better_wild_fruit_trees.refmap.json"


def expected_warning(line):
    # File logs retain canonical logger names; the console abbreviates these exact classes.
    for canonical, console in {
        "net.minecraftforge.fml.loading.moddiscovery.ModFileParser/LOADING": "ne.mi.fm.lo.mo.ModFileParser/LOADING",
        "net.minecraftforge.jarjar.selection.JarSelector/": "ne.mi.ja.se.JarSelector/",
        "net.minecraft.server.packs.VanillaPackResourcesBuilder/": "minecraft/VanillaPackResourcesBuilder",
        "net.minecraftforge.common.ForgeConfigSpec/CORE": "ne.mi.co.ForgeConfigSpec/CORE",
        "net.minecraftforge.fml.loading.FMLConfig/CORE": "ne.mi.fm.lo.FMLConfig/CORE",
    }.items():
        line = line.replace("[" + canonical + "]", "[" + console + "]")
    # Native userdev launcher/library diagnostics, not gameplay error exemptions.
    if re.search(r"\[main/WARN\] \[ne\.mi\.fm\.lo\.mo\.ModFileParser/LOADING\]: Mod file .*/(?:javafmllanguage|lowcodelanguage|mclanguage|fmlcore)-1\.20\.1-47\.4\.13\.jar is missing mods\.toml file$", line):
        return True
    if re.search(r"\[main/WARN\] \[ne\.mi\.ja\.se\.JarSelector/\]: Attempted to select two dependency jars from JarJar which have the same identification: Mod File:  and Mod File: \. Using Mod File: ?$", line):
        return True
    if re.search(r"\[main/WARN\] \[minecraft/VanillaPackResourcesBuilder\]: Assets URL 'union:.*!/(?:assets|data)/\.mcassetsroot' uses unexpected schema$", line):
        return True
    # Fresh pinned Forge configs report creation as correction. Never accept a correction
    # from an existing non-null value or an ERROR, nor any other logger's diagnostics.
    if re.search(r"\[(?:main|Server thread)/WARN\] \[ne\.mi\.co\.ForgeConfigSpec/CORE\]: Incorrect key [A-Za-z0-9_.]+ was corrected from null to its default, .+\. ?$", line):
        return True
    if re.search(r"\[(?:main|Server thread)/WARN\] \[ne\.mi\.co\.ForgeConfigSpec/CORE\]: Configuration file (?:[^ ]*/)?(?:dynamictrees-(?:common|server)|fruitsdelight-(?:common|client)|farmersdelight-(?:common|server)|forge-server|patchouli-client)\.toml is not correct\. Correcting$", line):
        return True
    if re.search(r"\[main/WARN\] \[ne\.mi\.fm\.lo\.FMLConfig/CORE\]: Configuration file .*/config/fml\.toml is not correct\. Correcting$", line):
        return True
    return False


def check_log(text):
    findings = [line for line in text.splitlines()
                if re.search(r"/(?:WARN|ERROR|FATAL)\]", line) and not expected_warning(line)]
    counts = re.findall(r"All (\d+) required tests passed", text)
    if not counts or int(counts[-1]) != 8:
        findings.append("Missing complete native GameTest success (exactly eight required tests)")
    return findings


def check_jar(archive):
    names = archive.namelist()
    assert len(names) == len(set(names)), "Duplicate archive entries"
    assert not any("/gametest/" in name or name.startswith("gameteststructures/") for name in names), "Shipping test fixture"
    manifest = archive.read("META-INF/MANIFEST.MF").decode()
    assert "MixinConfigs: better_wild_fruit_trees.mixins.json" in manifest, "Missing Mixin manifest"
    refmap = json.loads(archive.read(REFMAP))
    for name in ("FruitBlockMixin", "StaffMixin", "RecipeManagerMixin"):
        assert any(key.endswith("/" + name) and mapping for key, mapping in refmap["mappings"].items()), "Missing mapped target: " + name
    config = json.loads(archive.read("better_wild_fruit_trees.mixins.json"))
    assert config["required"] and config["injectors"]["defaultRequire"] == 1, "Fail-open injection policy"
    for name in config["mixins"]:
        assert config["package"].replace(".", "/") + "/" + name + ".class" in names, "Missing Mixin class"


if __name__ == "__main__":
    mode, path = sys.argv[1:]
    if mode == "log":
        findings = check_log(Path(path).read_text())
        if findings:
            raise SystemExit("Unclassified native findings:\n" + "\n".join(findings))
        print("Native GameTests and diagnostic gate passed")
    elif mode == "jar":
        with zipfile.ZipFile(path) as archive:
            check_jar(archive)
        print("Production archive/refmap/fixture-exclusion gate passed")
    else:
        raise SystemExit("Expected log or jar")
