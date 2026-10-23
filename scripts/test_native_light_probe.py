"""Observation-only source/isolation contracts and pure stub JDK controls, not native acceptance."""
import hashlib
import json
from pathlib import Path
import unittest
import uuid
from copy import deepcopy
from check_native_light_probe import controls, expected_refmap, check_refmap

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
        self.assertIn('source(nativeProbe)', native_run)
        self.assertIn('args("--mixin.config", "'+config_name+'")', native_run)
        self.assertEqual(1, build.count('source(nativeProbe)'))
        self.assertNotIn('config("'+config_name+'")', build)
        self.assertIn('"**/*native-light*"', build)
        self.assertFalse((ROOT/'src/main/resources'/config_name).exists())
        ledger = (ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/OriginalLightTrace.java').read_text()
        self.assertNotIn('net.minecraft', ledger)
        self.assertNotIn('net.minecraftforge', ledger)
        self.assertIn('kind == WRITE ? mix(hashes[p][kind], pos)', ledger)
        before_print = ledger.split('        void print() {')[0]
        for forbidden in ('System.out', 'getBlockState(', 'setBlock(', 'getChunk(', 'getBrightness(', 'getLightValue(',
                          'nextInt(', 'nextFloat(', 'setSeed(', 'synchronized', '.join(', '.wait(', '.sleep(', 'ThreadLocal', 'Map<'):
            self.assertNotIn(forbidden, before_print)
        for source in (ROOT/'src/nativeProbe/java').rglob('*.java'):
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

    def test_pure_forward_once_result_exception_and_bounded_ledger(self):
        controls()


if __name__ == '__main__':
    unittest.main()
