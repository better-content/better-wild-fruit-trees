"""Observation-only source/isolation contracts and pure stub JDK controls, not native acceptance."""
import hashlib
import json
from pathlib import Path
import unittest
import uuid
from copy import deepcopy
from check_native_light_probe import controls, expected_refmap, check_refmap, check_reserved_sources, check_reserved_classes

ROOT = Path(__file__).resolve().parents[1]


class OriginalLightProbeTest(unittest.TestCase):
    def test_only_observation_statements_added_to_original_fixture(self):
        path = ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/GrowthRateGameTests.java'
        source = path.read_text()
        additions = {
            'OriginalLightTrace.begin(fruit, sample);',
            'try { // ORIGINAL_LIGHT_OBSERVATION_SCOPE',
            'OriginalLightTrace.pulse(pulse);',
            '} finally { OriginalLightTrace.finish(); } // ORIGINAL_LIGHT_OBSERVATION_SCOPE',
        }
        self.assertTrue(all(source.count(line) == 1 for line in additions))
        stripped = ''.join(line for line in source.splitlines(keepends=True) if line.strip() not in additions)
        # Entire 8d4c441 original file including prior witnesses. This pins every original seed,
        # root/clock/delay, test/count, 4096-loop/16-age condition, endpoint/reference/tolerance.
        self.assertEqual('ffda01e8d7fe56cb80094d5c17232089d95f119cd913df1413e5098526199b26',
                         hashlib.sha256(stripped.encode()).hexdigest())

    def test_native_only_required_configuration_and_no_gameplay_queries(self):
        config_name = 'better_wild_fruit_trees.native-light.mixins.json'
        config = json.loads((ROOT/'src/nativeProbe/resources'/config_name).read_text())
        self.assertIs(True, config['required'])
        self.assertEqual(1, config['injectors']['defaultRequire'])
        self.assertEqual({'LeavesLightTraceMixin', 'SkyStorageTraceMixin', 'LightPublicationTraceMixin', 'ChunkWriteTraceMixin'}, set(config['mixins']))
        build = (ROOT/'build.gradle.kts').read_text()
        self.assertIn('val nativeProbe = sourceSets.create("nativeProbe")', build)
        native_run = build.split('create("gameTestServer") {', 1)[1].split('\n    }\n}', 1)[0]
        diagnostic_block = native_run.split('if (growthDiagnosticsEnabled) {', 1)[1].split('\n            }', 1)[0]
        self.assertIn('source(nativeProbe)', diagnostic_block)
        self.assertIn('args("--mixin.config", "'+config_name+'")', diagnostic_block)
        self.assertIn('property("better_wild_fruit_trees.growthDiagnostics", "true")', diagnostic_block)
        self.assertIn('growthDiagnosticsValue == null || growthDiagnosticsValue in setOf("true", "false")', build)
        self.assertIn('growthDiagnosticsValue != "false" || (!originalEventTrace && !lightQueueProofEnabled)', build)
        expression = build.split('val growthDiagnosticsEnabled = ', 1)[1].splitlines()[0].replace('||', 'or')
        for manual in (None, 'false', 'true'):
            for ordered, queue in ((False, False), (True, False), (False, True)):
                effective = eval(expression, {}, dict(growthDiagnosticsValue=manual, originalEventTrace=ordered, lightQueueProofEnabled=queue))
                self.assertEqual(manual == 'true' or ordered or queue, effective)
                if manual in (None, 'false') and not ordered and not queue:self.assertFalse(effective)
        self.assertIn('if (native && growthDiagnosticsEnabled) expectedSources.add(nativeProbe)', build)
        self.assertIn('run.allSources.toSet() == expectedSources', build)
        self.assertIn('run.mods.getByName("better_wild_fruit_trees").sources.toSet() == expectedSources', build)
        self.assertIn('val diagnosticArgs = if (growthDiagnosticsEnabled)', build)
        self.assertIn('receipt.setProperty("growthDiagnostics", growthDiagnosticsEnabled.toString())', build)
        self.assertEqual(1, build.count('source(nativeProbe)'))
        self.assertNotIn('config("'+config_name+'")', build)
        self.assertIn('"**/*native-light*"', build)
        self.assertFalse((ROOT/'src/main/resources'/config_name).exists())
        ledger = (ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/OriginalLightTrace.java').read_text()
        self.assertLess(ledger.index('if (!Boolean.getBoolean("better_wild_fruit_trees.growthDiagnostics")) return;'), ledger.index('Session s = new Session(sample);'))
        witness = (ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/LegacyGrowthWitness.java').read_text()
        self.assertIn('return Boolean.getBoolean("better_wild_fruit_trees.growthDiagnostics")', witness)
        for method, query in (('static void sample(', 'h.getLevel()'), ('public static void joined(', 'event.getLevel()'), ('public static void left(', 'event.getLevel()')):
            body = witness.split(method, 1)[1]
            self.assertLess(body.index('if (!enabled()) return;'), body.index(query))
        self.assertNotIn('net.minecraft', ledger)
        self.assertNotIn('net.minecraftforge', ledger)
        self.assertIn('kind == WRITE ? mix(hashes[p][kind], pos)', ledger)
        before_print = ledger.split('        void print() {')[0]
        for forbidden in ('System.out', 'getBlockState(', 'setBlock(', 'getChunk(', 'getBrightness(', 'getLightValue(',
                          'nextInt(', 'nextFloat(', 'setSeed(', 'synchronized', '.join(', '.wait(', '.sleep(', 'ThreadLocal', 'Map<'):
            self.assertNotIn(forbidden, before_print)
        adapter = (ROOT/'src/nativeProbe/java/com/bettercontent/betterwildfruittrees/gametest/NativeStateCodes.java').read_text()
        self.assertIn('@SubscribeEvent\n    public static void starting(ServerStartingEvent ignored)', adapter)
        self.assertIn('new StateCodebook.Builder<BlockState>(131072)', adapter)
        self.assertLess(adapter.index('if (!selected(name)) continue;'), adapter.index('block.getStateDefinition().getPossibleStates()'))
        self.assertIn('original-orange-blocks-v1', adapter)
        self.assertIn('selectedBlockKeys=', adapter)
        hot = adapter.split('public static long code(BlockState state)', 1)[1]
        for forbidden in ('BuiltInRegistries', 'getValues(', 'getStateDefinition(', 'starting(', 'new ', 'System.out', 'synchronized'):
            self.assertNotIn(forbidden, hot)
        for source in (ROOT/'src/nativeProbe/java').rglob('*Mixin.java'):
            text = source.read_text()
            for forbidden in ('getBlockState(', 'getChunk(', 'getBlock(', 'Block.getId(', 'setReturnValue(', '.cancel(', 'cancellable = true', 'System.out', '@GameTest'):
                self.assertNotIn(forbidden, text)

    def test_required_refmap_missing_wrong_and_namespace_negatives(self):
        out = ROOT/'build/source-native-light-controls'/str(uuid.uuid4())
        out.mkdir(parents=True)
        good = {'mappings': expected_refmap(), 'data': {'searge': expected_refmap()}}
        (out/'good.json').write_text(json.dumps(good))
        check_refmap(out/'good.json')
        for index, (cls, values) in enumerate(expected_refmap().items()):
            for mutation in ('missing', 'wrong_srg', 'wrong_namespace'):
                bad = deepcopy(good)
                if mutation == 'missing': del bad['mappings'][cls][next(iter(values))]
                elif mutation == 'wrong_srg': bad['mappings'][cls][next(iter(values))] = 'm_00000_'
                else: bad['data'] = {'unsupported': bad['data']['searge']}
                path = out/f'{index}-{mutation}.json'
                path.write_text(json.dumps(bad))
                with self.assertRaises(AssertionError): check_refmap(path)

    def test_reserved_mixin_namespace_rejects_every_ordinary_class_and_descendant(self):
        reserved = 'fixture.reserved'
        config = {'package': reserved, 'mixins': ['RealMixin']}
        good_sources = {'RealMixin.java': 'package fixture.reserved; @Mixin(Target.class) class RealMixin {}',
                        'Helper.java': 'package fixture; class Helper {}'}
        check_reserved_sources(good_sources, config)
        for package, name, annotation in ((reserved, 'Helper', ''), (reserved+'.child', 'Bootstrap', ''),
                                           (reserved, 'RealMixin', ''), (reserved+'.child', 'FakeMixin', '@Mixin(Target.class)')):
            with self.assertRaises(AssertionError):
                check_reserved_sources({name+'.java': 'package '+package+'; '+annotation+' class '+name+' {}'}, config)
        good_info = {'owner': 'fixture/reserved/RealMixin', 'annotations': {'Lorg/spongepowered/asm/mixin/Mixin;': {}}}
        check_reserved_classes([good_info, {'owner': 'fixture/Helper', 'annotations': {}}], config)
        for owner in ('fixture/reserved/Helper', 'fixture/reserved/child/Bootstrap', 'fixture/reserved/RealMixin$Helper'):
            with self.assertRaises(AssertionError): check_reserved_classes([good_info, {'owner': owner, 'annotations': {}}], config)
        with self.assertRaises(AssertionError): check_reserved_classes([{'owner': good_info['owner'], 'annotations': {}}], config)

    def test_pure_forward_once_result_exception_and_bounded_ledger(self):
        controls()


if __name__ == '__main__':
    unittest.main()
