"""Distinct kiwi-v1 observation controls; never default/native qualification or a fixture fence."""
from pathlib import Path
import subprocess
import unittest
import uuid

ROOT = Path(__file__).resolve().parents[1]


class KiwiTraceScopeTest(unittest.TestCase):
    def test_explicit_native_only_scope_and_unchanged_original_fixture(self):
        build = (ROOT/'build.gradle.kts').read_text()
        self.assertIn('originalEventTraceScopeValue in setOf("orange-v1", "kiwi-v1")', build)
        self.assertIn('originalEventTraceScopeValue == null || originalEventTrace', build)
        self.assertIn('originalEventTraceScope != "kiwi-v1" || !calibrationEnabled', build)
        self.assertIn('if (native && originalEventTrace) originalEventTraceScope else null', build)
        self.assertIn('receipt.setProperty("originalEventTraceScope", if (originalEventTrace) originalEventTraceScope else "NONE")', build)
        self.assertIn('binding.setProperty("traceScope", originalEventTraceScope)', build)
        self.assertIn(':traceScope=kiwi-v1', build)
        profile = (ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/OriginalTraceProfile.java').read_text()
        self.assertIn('Boolean.getBoolean("better_wild_fruit_trees.originalEventTrace")', profile)
        self.assertIn('"kiwi-v1".equals(System.getProperty(', profile)
        # The independent existing whole stripped-file SHA gate still pins ALL original callbacks/body.
        from test_native_light_probe import OriginalLightProbeTest
        OriginalLightProbeTest().test_only_observation_statements_added_to_original_fixture()

    def test_distinct_dictionary_not_silent_orange_widening(self):
        adapter = (ROOT/'src/nativeProbe/java/com/bettercontent/betterwildfruittrees/gametest/NativeStateCodes.java').read_text()
        self.assertIn('public static final String SCOPE = "original-orange-blocks-v1";', adapter)
        self.assertIn('private static final Set<String> NATIVE_KEYS = Set.of("dynamictrees:rooty_dirt", "dynamictrees:oak_branch", "dtfruitsdelight:orange_leaves");', adapter)
        self.assertIn('private static final Set<String> KIWI_KEYS = Set.of("dynamictrees:rooty_dirt", "dynamictrees:jungle_branch", "dtfruitsdelight:kiwi_leaves");', adapter)
        self.assertIn('OriginalTraceProfile.kiwi() ? KIWI_KEYS : NATIVE_KEYS', adapter)
        self.assertNotIn('"dynamictrees:trunk_shell"', adapter)
        self.assertIn('new StateCodebook.Builder<BlockState>(131072)', adapter)
        comparator = (ROOT/'scripts/KiwiOriginalEventCompare.java').read_text()
        for key in ['original-kiwi-blocks-v1', 'observedSpecies', 'traceScope', 'OriginalEventCompare.compare(a, b)', '"kiwi"']:
            self.assertIn(key, comparator)

    def test_actual_jdk_kiwi_receipt_scope_and_cross_profile_negatives(self):
        out = ROOT/'build/source-kiwi-trace-controls'/str(uuid.uuid4())
        classes = out/'classes'
        classes.mkdir(parents=True)
        names = ['OriginalEventCompare.java', 'KiwiOriginalEventCompare.java', 'OrderedEventsSelfTest.java', 'KiwiCompletionReceiptSelfTest.java']
        sources = [ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/OrderedEvents.java', *[ROOT/'scripts'/n for n in names]]
        p = subprocess.run(['javac', '--release', '17', '-d', str(classes), *map(str, sources)], capture_output=True, text=True, timeout=30)
        (out/'javac.log').write_text(p.stdout+p.stderr)
        self.assertEqual(0, p.returncode, p.stderr)
        p = subprocess.run(['java', '-cp', str(classes), 'com.bettercontent.betterwildfruittrees.gametest.KiwiCompletionReceiptSelfTest', str(out/'evidence')], capture_output=True, text=True, timeout=30)
        (out/'controls.log').write_text(p.stdout+p.stderr)
        self.assertEqual(0, p.returncode, p.stdout+p.stderr)


if __name__ == '__main__':
    unittest.main()
