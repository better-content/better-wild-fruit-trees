"""Bounded standalone source gates; no modpack inputs or historical evidence required."""
import datetime
import json
from pathlib import Path
import re
import sys
import zipfile
from coconut_mapping import MIXIN as COCONUT_MIXIN, check_mapping as check_coconut_mapping, class_info

REFMAP = "better_wild_fruit_trees.refmap.json"
INVENTORY = json.loads(Path(__file__).with_name('native_required_tests.json').read_text())
REQUIRED_NATIVE_TESTS = INVENTORY['required_tests']
assert INVENTORY['schema'] == 'bc.wft_native_inventory.v2'
assert len(REQUIRED_NATIVE_TESTS) == len(set(REQUIRED_NATIVE_TESTS)) == 25


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
    if re.search(r"\[(?:main|Server thread)/WARN\] \[ne\.mi\.co\.ForgeConfigSpec/CORE\]: Configuration file (?:[^ ]*/)?(?:dynamictrees-(?:common|server)|fruitsdelight-(?:common|client)|farmersdelight-(?:common|server)|forge-server|patchouli-client|l2library-common)\.toml is not correct\. Correcting$", line):
        return True
    if re.search(r"\[main/WARN\] \[ne\.mi\.fm\.lo\.FMLConfig/CORE\]: Configuration file .*/config/fml\.toml is not correct\. Correcting$", line):
        return True
    return False


RECORD = re.compile(r'^\[([^\]]+)\] \[([^\]]+)/(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\] \[([^\]]+)\]: (.*)$')
FAILED_OPERATION = re.compile(
    r'(?:(?:Failed|Unable) to|Could not|Couldn\x27t) (?:load|save|decode|parse|start|execute|apply|create|read|write|reload)\b|'
    r'Error loading (?:recipe|tag|registry|datapack)\b|'
    r'^\s*(?:Caused by:\s*|Suppressed:\s*|Exception in thread "[^"]+" )?(?:[\w$]+\.)*\w*(?:Exception|Error)(?::|$)|'
    r'Exception in server tick loop|Encountered an unexpected exception|Tried to load a DUMMY|'
    r'\b(?:AssertionError|OutOfMemoryError|MixinApplyError|InjectionError)\b|\brequired tests? failed\b|result=FAIL\b', re.IGNORECASE)


# Same inspected Forge 47.4.13 optional FileNotFound catch and Minecraft 1.20.1
# Netty 4.1.82 caught-capability fallback as the Fonts native-input contract.
# This is exact record/continuation attribution, never a blanket DEBUG waiver.
OPTIONAL_JARJAR = re.compile(r'Failed to load resource META-INF/jarjar/metadata\.json from [^,\r\n]*, it does not contain dependency information\.')
STACK_FRAME = re.compile(r'\s+at [\w.$/<>]+\([^()\r\n]*\)(?: ~\[[^\]\r\n]*\])?')
NETTY_PROBES = {
    'direct buffer constructor: unavailable': (
        'java.lang.UnsupportedOperationException: Reflective setAccessible(true) disabled',
        'io.netty.util.internal.ReflectionUtil.trySetAccessible'),
    'jdk.internal.misc.Unsafe.allocateUninitializedArray(int): unavailable': (
        'java.lang.IllegalAccessException: class io.netty.util.internal.PlatformDependent0$7 '
        '(in module io.netty.common) cannot access class jdk.internal.misc.Unsafe '
        '(in module java.base) because module java.base does not export jdk.internal.misc to module io.netty.common',
        'io.netty.util.internal.PlatformDependent0$7.run'),
}


def native_probe_lines(lines):
    groups, group, proven = [], [], {}
    for number, line in enumerate(lines, 1):
        if RECORD.fullmatch(line) and group:
            groups.append(group)
            group = []
        group.append((number, line))
    if group:
        groups.append(group)
    for group in groups:
        parent, record = group[0]
        match = RECORD.fullmatch(record)
        if not match:
            continue
        stamp, thread, level, logger, message = match.groups()
        try:
            datetime.datetime.strptime(stamp, '%d%b%Y %H:%M:%S.%f')
        except ValueError:
            continue
        if thread != 'main' or level != 'DEBUG':
            continue
        number, category = parent, None
        if (logger == 'net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileDependencyLocator/'
                and OPTIONAL_JARJAR.fullmatch(message)
                and not FAILED_OPERATION.search(message.removeprefix('Failed to load resource '))):
            category = 'native-optional-jarjar-probe'
        probe = NETTY_PROBES.get(message) if logger == 'io.netty.util.internal.PlatformDependent0/' else None
        continuation = list(group[1:])
        while continuation and not continuation[-1][1].strip():
            continuation.pop()
        if probe and len(continuation) >= 2 and continuation[0][1] == probe[0]:
            frames = [text for _, text in continuation[1:]]
            if all(STACK_FRAME.fullmatch(text) for text in frames) and any(
                    re.match(r'\s+at ' + re.escape(probe[1]) + r'\(', text) for text in frames):
                number, category = continuation[0][0], 'native-netty-reflection-probe'
        if category:
            proven[number] = {'category': category, 'parent_line': parent, 'parent_record': record}
    return proven


def classify_log(text):
    diagnostics = []
    lines = text.splitlines()
    probes = native_probe_lines(lines)
    for number, line in enumerate(lines, 1):
        match = RECORD.fullmatch(line)
        severity = match[3] if match else next((s for s in ('FATAL', 'ERROR', 'WARN') if re.search(r'\b' + s + r'\b', line)), None)
        state_failure = bool(match and (
            (match[4].rstrip('/') in ('net.minecraft.world.level.chunk.LevelChunk', 'minecraft/LevelChunk')
             and (re.search(r'^Tried to load a block entity for block .+ but failed at location ', match[5])
                  or re.search(r'^Tried to load a DUMMY block entity @ .+ but found not block entity block ', match[5])))
            or (match[4].rstrip('/') == 'com.ferreusveritas.dynamictrees.tree.species.Species'
                and match[5].startswith('Rooty Dirt block NOT FOUND for soil '))))
        failure = number not in probes and (state_failure or bool(FAILED_OPERATION.search(match[5] if match else line)))
        if severity not in ('WARN', 'ERROR', 'FATAL') and not failure and number not in probes:
            continue
        diagnostics.append({'line': number, 'record': line, 'severity': severity,
                            'thread': match[2] if match else None, 'logger': match[4] if match else None,
                            'native_probe': probes.get(number),
                            'category': probes[number]['category'] if number in probes else 'failed_operation' if failure else ('native_bootstrap' if expected_warning(line) else 'unclassified'),
                            'blocking': failure or severity in ('ERROR', 'FATAL')})
    return diagnostics


def check_log(text):
    findings = [item['record'] for item in classify_log(text) if item['blocking']]
    counts = re.findall(r"All (\d+) required tests passed", text)
    if not counts or int(counts[-1]) != len(REQUIRED_NATIVE_TESTS):
        findings.append("Missing complete functional native GameTest success (exactly twenty-five required tests; calibration retired)")
    return findings


def check_jar(archive):
    names = archive.namelist()
    assert len(names) == len(set(names)), "Duplicate archive entries"
    assert not any("/gametest/" in name or name.startswith("gameteststructures/") or "native-light" in name or "original-events" in name or name.endswith(".wftord") or "light-queue-proof" in name or name.endswith(".wftq") or "growth-v2" in name for name in names), "Shipping test fixture"
    assert not any(name.startswith(("slimeknights/", "com/ferreusveritas/dynamictreesplus/", "xueluoanping/dtnatures_spirit/",
                                    "net/hibiscus/naturespirit/", "org/violetmoon/")) for name in names), "Bundling optional upstream implementation"
    assert "com/bettercontent/betterwildfruittrees/compat/TConstructAxes.class" in names, "Missing optional typed tier adapter"
    manifest = archive.read("META-INF/MANIFEST.MF").decode()
    assert "MixinConfigs: better_wild_fruit_trees.mixins.json" in manifest, "Missing Mixin manifest"
    refmap = json.loads(archive.read(REFMAP))
    for name in ("FruitBlockMixin", "StaffMixin", "RecipeManagerMixin", "RootRemovalMixin", "CoconutFoodBlockMixin"):
        assert any(key.endswith("/" + name) and mapping for key, mapping in refmap["mappings"].items()), "Missing mapped target: " + name
    check_coconut_mapping(refmap, class_info(archive.read(COCONUT_MIXIN + '.class')))
    config = json.loads(archive.read("better_wild_fruit_trees.mixins.json"))
    assert config["required"] and config["injectors"]["defaultRequire"] == 1, "Fail-open injection policy"
    plugin = "com.bettercontent.betterwildfruittrees.compat.AddonMixins"
    assert config.get("plugin") == plugin and plugin.replace(".", "/") + ".class" in names, "Missing optional-addon mixin gating"
    required = {"MegaSubstanceMixin", "RootRemovalMixin", "StaticSpeciesSelectorMixin", "CactusFruitGenFeatureMixin",
                "PalmFruitGenFeatureMixin", "CactusHardnessMixin", "CoconutFoodBlockMixin", "CoconutReplacementMixin", "PodBlockMixin"}
    assert required.issubset(config["mixins"]), "Missing optional tree native path"
    for helper in ("RootLedger", "LedgerNetwork", "RareResources"):
        assert "com/bettercontent/betterwildfruittrees/compat/" + helper + ".class" in names, "Missing optional tree helper " + helper
    for name in config["mixins"]:
        assert config["package"].replace(".", "/") + "/" + name + ".class" in names, "Missing Mixin class"


if __name__ == "__main__":
    mode, path = sys.argv[1:]
    if mode == "log":
        text = Path(path).read_text()
        for diagnostic in classify_log(text):
            print(json.dumps(diagnostic))
        findings = check_log(text)
        if findings:
            raise SystemExit("Unclassified native findings:\n" + "\n".join(findings))
        print("Native GameTests and diagnostic gate passed")
    elif mode == "jar":
        with zipfile.ZipFile(path) as archive:
            check_jar(archive)
        print("Production archive/refmap/fixture-exclusion gate passed")
    else:
        raise SystemExit("Expected log or jar")
