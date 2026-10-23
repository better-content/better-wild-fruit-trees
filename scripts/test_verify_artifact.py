import unittest
from unittest.mock import patch
from copy import deepcopy
from coconut_mapping import MIXIN, TARGET, METHODS, check_mapping


def synthetic_coconut_info():
    return {'owner': MIXIN, 'annotations': {'Lorg/spongepowered/asm/mixin/Mixin;': {'value': ['L' + TARGET + ';']}},
            'methods': [(name, desc, 1, {'Lorg/spongepowered/asm/mixin/injection/Inject;':
                {'method': [name], 'cancellable': 1, 'at': [('Lorg/spongepowered/asm/mixin/injection/At;', {'value': 'HEAD'})]}})
                for name, (_, desc) in METHODS.items()]}


def synthetic_coconut_mappings():
    return {name: 'L' + TARGET + ';' + srg + desc for name, (srg, desc) in METHODS.items()}
import io
import json
import zipfile
import verify_artifact as n
from verify_artifact import check_log, expected_warning, classify_log


class NativeOptionalProbeTest(unittest.TestCase):
    def record(self, message, logger='io.netty.util.internal.PlatformDependent0/'):
        return f'[18Oct2026 11:25:01.042] [main/DEBUG] [{logger}]: {message}'

    def jarjar(self):
        return self.record('Failed to load resource META-INF/jarjar/metadata.json from , it does not contain dependency information.',
                           'net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileDependencyLocator/')

    def stack(self, index=0):
        header, (heading, frame) = list(n.NETTY_PROBES.items())[index]
        return [self.record(header), heading, '\tat ' + frame + '(Native.java:31) ~[netty-common-4.1.82.Final.jar:4.1.82.Final]']

    def blocking(self, lines):
        return [row for row in n.classify_log('\n'.join(lines)) if row['blocking']]

    def test_exact_probes_retain_record_and_continuation_provenance(self):
        lines = [self.jarjar()] + self.stack() + self.stack(1) + ['']
        proof = n.native_probe_lines(lines)
        self.assertEqual([1, 3, 6], list(proof))
        self.assertEqual(lines[1], proof[3]['parent_record'])
        self.assertEqual(2, proof[3]['parent_line'])
        self.assertEqual([], self.blocking(lines))

    def test_changed_optional_resource_provenance_still_blocks(self):
        original = self.jarjar()
        for before, after in [('main/DEBUG', 'other/DEBUG'), ('main/DEBUG', 'main/INFO'),
                              ('main/DEBUG', 'main/ERROR'), ('main/DEBUG', 'main/FATAL'),
                              ('AbstractJarFileDependencyLocator/', 'Other/'),
                              ('metadata.json', 'required.json'), ('18Oct2026', '99Oct2026')]:
            with self.subTest(after=after):
                line = original.replace(before, after)
                self.assertEqual({}, n.native_probe_lines([line]))
                self.assertTrue(self.blocking([line]))
        for signal in ['AssertionError: invariant', 'OutOfMemoryError: heap', 'Failed to parse required recipe']:
            line = original.replace('from ,', 'from nativeFixture ' + signal + ',')
            self.assertEqual({}, n.native_probe_lines([line]))
            self.assertTrue(self.blocking([line]))
        self.assertTrue(self.blocking([original + ' result=FAIL']))
        self.assertTrue(self.blocking([original, 'java.lang.IllegalStateException: required resource failure']))

    def test_netty_context_cannot_hide_unexpected_exceptions_or_required_failures(self):
        for index in (0, 1):
            good = self.stack(index)
            cases = [good[1:], good[:2], [good[0], good[1], '\tat other.Class.run(Other.java:1)'],
                     [good[0].replace('main/DEBUG', 'Server thread/DEBUG'), *good[1:]],
                     [good[0].replace('main/DEBUG', 'main/ERROR'), *good[1:]],
                     [good[0].replace('main/DEBUG', 'main/FATAL'), *good[1:]],
                     [good[0].replace('PlatformDependent0/', 'other/'), *good[1:]],
                     [good[0], 'java.lang.IllegalStateException: unexpected', good[2]],
                     good + ['Caused by: java.lang.IllegalStateException: required load failed'],
                     good + ['Suppressed: java.lang.RuntimeException: required parse failed'],
                     good + ['not a stack frame'],
                     [good[0], self.record('different record', 'other/'), *good[1:]]]
            for lines in cases:
                with self.subTest(lines=lines):
                    self.assertEqual({}, n.native_probe_lines(lines))
                    self.assertTrue(self.blocking(lines))
        for message in ['Failed to load required data', 'Failed to parse recipe', 'AssertionError: broken',
                        'java.lang.IllegalStateException: uncaught', 'result=FAIL']:
            self.assertTrue(self.blocking([self.record(message)]))


class DiagnosticGateTest(unittest.TestCase):
    def test_missing_native_completion_blocks(self):
        self.assertTrue(check_log("All 7 required tests passed"))
        self.assertTrue(check_log("All 14 required tests passed"))
        self.assertTrue(check_log("All 15 required tests passed"))
        self.assertTrue(check_log("All 17 required tests passed"))
        self.assertTrue(check_log("All 18 required tests passed"))
        self.assertTrue(check_log("All 19 required tests passed"))
        self.assertTrue(check_log("All 25 required tests passed"))
        self.assertFalse(check_log("All 26 required tests passed"))

    def test_known_launcher_warning_only_at_warn(self):
        text = "[12:00:00] [main/WARN] [ne.mi.ja.se.JarSelector/]: Attempted to select two dependency jars from JarJar which have the same identification: Mod File:  and Mod File: . Using Mod File: "
        self.assertTrue(expected_warning(text))
        self.assertFalse(expected_warning(text.replace("/WARN]", "/ERROR]")))

    def test_unknown_warnings_never_gate_by_count_but_errors_block(self):
        for severity in ['WARN', 'ERROR', 'FATAL']:
            record = f'[12:00:00] [Server thread/{severity}] [minecraft/Test]: unexpected'
            text = (record + '\n') * 20 + 'All 26 required tests passed'
            self.assertEqual(severity != 'WARN', bool(check_log(text)))
            rows = classify_log(text)
            self.assertEqual(20, len(rows))
            self.assertEqual(record, rows[0]['record'])
            self.assertEqual('minecraft/Test', rows[0]['logger'])
            self.assertEqual(20, rows[-1]['line'])

    def test_failed_operations_at_info_warn_and_errors_always_block(self):
        for severity in ['INFO', 'WARN', 'ERROR', 'FATAL']:
            for message in ['Failed to save chunk', 'Could not decode recipe', 'AssertionError: test failed',
                            "Couldn't load tag", 'Unable to reload resources', 'java.lang.IllegalStateException: crash']:
                text = f'[12:00:00] [Server thread/{severity}] [any/Logger]: {message}\nAll 26 required tests passed'
                self.assertTrue(check_log(text))
                self.assertTrue(classify_log(text)[0]['blocking'])
        self.assertTrue(check_log('ERROR unstructured crash\nAll 26 required tests passed'))

    def test_warning_label_never_applies_to_error_or_other_logger(self):
        record = '[12:00:00] [main/WARN] [ne.mi.ja.se.JarSelector/]: Attempted to select two dependency jars from JarJar which have the same identification: Mod File:  and Mod File: . Using Mod File: '
        self.assertEqual('native_bootstrap', classify_log(record)[0]['category'])
        self.assertEqual('unclassified', classify_log(record.replace('ne.mi.ja.se.JarSelector/', 'other/'))[0]['category'])
        error = record.replace('/WARN]', '/ERROR]')
        self.assertTrue(check_log(error + '\nAll 26 required tests passed'))
        self.assertNotEqual('native_bootstrap', classify_log(error)[0]['category'])

    def test_canonical_file_logger_is_equivalent_but_unrelated_class_is_not(self):
        text = "[12:00:00] [main/WARN] [net.minecraftforge.jarjar.selection.JarSelector/]: Attempted to select two dependency jars from JarJar which have the same identification: Mod File:  and Mod File: . Using Mod File: "
        self.assertTrue(expected_warning(text))
        self.assertFalse(expected_warning(text.replace("[net.minecraftforge", "[other.net.minecraftforge")))
        self.assertFalse(expected_warning(text.replace("/WARN]", "/ERROR]")))

    def test_only_null_config_bootstrap_is_classified(self):
        text = "[12:00:00] [Server thread/WARN] [ne.mi.co.ForgeConfigSpec/CORE]: Incorrect key trees.growthMultiplier was corrected from null to its default, 0.5. "
        self.assertTrue(expected_warning(text))
        self.assertFalse(expected_warning(text.replace("from null", "from 3.0")))
        self.assertFalse(expected_warning(text.replace("/WARN]", "/ERROR]")))


class OptionalArchiveContractTest(unittest.TestCase):
    """Synthetic archive-shape contracts, not a compiled/native artifact acceptance substitute."""
    def entries(self):
        package = 'com.bettercontent.betterwildfruittrees'
        mapped = {'FruitBlockMixin', 'StaffMixin', 'RecipeManagerMixin', 'RootRemovalMixin', 'CoconutFoodBlockMixin'}
        mixins = mapped | {'MegaSubstanceMixin', 'StaticSpeciesSelectorMixin', 'CactusFruitGenFeatureMixin',
                           'PalmFruitGenFeatureMixin', 'CactusHardnessMixin', 'CoconutReplacementMixin', 'PodBlockMixin'}
        config = {'required': True, 'injectors': {'defaultRequire': 1}, 'package': package + '.mixin',
                  'plugin': package + '.compat.AddonMixins', 'mixins': sorted(mixins)}
        entries = {'META-INF/MANIFEST.MF': 'MixinConfigs: better_wild_fruit_trees.mixins.json\n',
                   'better_wild_fruit_trees.mixins.json': json.dumps(config),
                   n.REFMAP: json.dumps({'mappings': {package.replace('.', '/') + '/mixin/' + name: {'method': 'mapped'} for name in mapped}})}
        refmap = json.loads(entries[n.REFMAP])
        refmap['mappings'][MIXIN] = synthetic_coconut_mappings()
        entries[n.REFMAP] = json.dumps(refmap)
        for name in mixins:
            entries[package.replace('.', '/') + '/mixin/' + name + '.class'] = b'synthetic'
        for name in ['AddonMixins', 'RootLedger', 'LedgerNetwork', 'RareResources', 'TConstructAxes']:
            entries[package.replace('.', '/') + '/compat/' + name + '.class'] = b'synthetic'
        return entries

    def check(self, entries):
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, 'w') as archive:
            for name, value in entries.items():
                archive.writestr(name, value)
        buffer.seek(0)
        # These existing tests are archive-shape controls. Real class parsing/AP behavior
        # is independently tested by check_coconut_ap.py against actual compiled stub classes.
        with zipfile.ZipFile(buffer) as archive, patch.object(n, 'class_info', return_value=synthetic_coconut_info()):
            n.check_jar(archive)

    def test_coconut_exact_mapping_and_annotation_negatives(self):
        good = {'mappings': {MIXIN: synthetic_coconut_mappings()}}
        check_mapping(good, synthetic_coconut_info())
        for name in METHODS:
            for mutation in ('missing', 'wrong_srg', 'wrong_descriptor', 'wrong_owner', 'unmapped'):
                bad = deepcopy(good)
                values = bad['mappings'][MIXIN]
                if mutation == 'missing': del values[name]
                elif mutation == 'wrong_srg': values[name] = values[name].replace(METHODS[name][0], 'm_00000_')
                elif mutation == 'wrong_descriptor': values[name] = values[name].replace('BlockPos;', 'BlockPos;I')
                elif mutation == 'wrong_owner': values[name] = values[name].replace(TARGET, 'other/Target')
                else: values[name] = name + METHODS[name][1]
                with self.assertRaises(AssertionError): check_mapping(bad, synthetic_coconut_info())
        for mutation in ('pseudo', 'remap_false', 'string_target', 'missing_injector', 'require_zero', 'not_cancellable', 'wrong_at'):
            info = synthetic_coconut_info()
            if mutation == 'pseudo': info['annotations']['Lorg/spongepowered/asm/mixin/Pseudo;'] = {}
            elif mutation == 'remap_false': info['annotations']['Lorg/spongepowered/asm/mixin/Mixin;']['remap'] = 0
            elif mutation == 'string_target': info['annotations']['Lorg/spongepowered/asm/mixin/Mixin;'] = {'targets': [TARGET.replace('/', '.')]}
            elif mutation == 'missing_injector': info['methods'].pop()
            else:
                inject = info['methods'][0][3]['Lorg/spongepowered/asm/mixin/injection/Inject;']
                if mutation == 'require_zero': inject['require'] = 0
                elif mutation == 'not_cancellable': inject['cancellable'] = 0
                else: inject['at'][0][1]['value'] = 'RETURN'
            with self.assertRaises(AssertionError): check_mapping(good, info)
        bad = deepcopy(good)
        bad['data'] = {'searge': {MIXIN: {}}}
        with self.assertRaises(AssertionError): check_mapping(bad, synthetic_coconut_info())

    def test_complete_synthetic_shape_is_accepted(self):
        self.check(self.entries())

    def test_missing_optional_mapped_targets_or_plugin_blocks(self):
        for name in ['RootRemovalMixin', 'CoconutFoodBlockMixin']:
            entries = self.entries()
            mapping = json.loads(entries[n.REFMAP])
            mapping['mappings'] = {key: value for key, value in mapping['mappings'].items() if not key.endswith('/' + name)}
            entries[n.REFMAP] = json.dumps(mapping)
            with self.assertRaises(AssertionError): self.check(entries)
        entries = self.entries()
        del entries['com/bettercontent/betterwildfruittrees/compat/AddonMixins.class']
        with self.assertRaises(AssertionError): self.check(entries)

    def test_optional_native_path_cannot_be_omitted_from_registration(self):
        entries = self.entries()
        config = json.loads(entries['better_wild_fruit_trees.mixins.json'])
        config['mixins'].remove('PodBlockMixin')
        entries['better_wild_fruit_trees.mixins.json'] = json.dumps(config)
        with self.assertRaises(AssertionError): self.check(entries)

    def test_fixture_or_optional_upstream_implementation_must_not_ship(self):
        for path in ['better_wild_fruit_trees.native-light.mixins.json',
                     'better_wild_fruit_trees.native-light.refmap.json',
                     'com/bettercontent/betterwildfruittrees/gametest/nativeprobe/LeavesLightTraceMixin.class',
                     'com/bettercontent/betterwildfruittrees/gametest/nativeprobe/NativeStateCodes.class',
                     'com/bettercontent/betterwildfruittrees/gametest/nativeprobe/StateCodebook.class',
                     'com/bettercontent/betterwildfruittrees/gametest/OriginalLightTrace.class',
                     'com/bettercontent/betterwildfruittrees/gametest/OptionalTreeGameTests.class',
                     'com/ferreusveritas/dynamictreesplus/tree/CactusSpecies.class',
                     'net/hibiscus/naturespirit/blocks/CoconutBlock.class',
                     'xueluoanping/dtnatures_spirit/systems/featuregen/PalmFruitGenFeature.class',
                     'org/violetmoon/quark/content/world/item/AncientFruitItem.class']:
            entries = self.entries(); entries[path] = b'synthetic'
            with self.assertRaises(AssertionError): self.check(entries)
