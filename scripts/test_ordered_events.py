"""Pure JDK controls only; retains every class, payload and receipt under build/."""
import subprocess
import unittest
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class OrderedEventsTest(unittest.TestCase):
    def test_jdk_roundtrip_order_bounds_and_failure_preservation(self):
        out = ROOT/'build/source-ordered-controls'/str(uuid.uuid4())
        classes = out/'classes'
        classes.mkdir(parents=True)
        sources = [ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/OrderedEvents.java', ROOT/'scripts/OriginalEventCompare.java', ROOT/'scripts/OrderedEventsSelfTest.java']
        compile = subprocess.run(['javac', '--release', '17', '-d', str(classes), *map(str, sources)], capture_output=True, text=True)
        (out/'javac.log').write_text(compile.stdout+compile.stderr)
        self.assertEqual(0, compile.returncode, compile.stderr)
        run = subprocess.run(['java', '-cp', str(classes), 'com.bettercontent.betterwildfruittrees.gametest.OrderedEventsSelfTest', str(out/'evidence')], capture_output=True, text=True)
        (out/'controls.log').write_text(run.stdout+run.stderr)
        self.assertEqual(0, run.returncode, run.stdout+run.stderr)

    def test_opt_in_native_only_binding_and_head_guards(self):
        build = (ROOT/'build.gradle.kts').read_text()
        self.assertIn('val originalEventTrace = originalEventTraceValue == "true"', build)
        self.assertEqual(1, build.count('property("better_wild_fruit_trees.originalEventTrace", "true")'))
        self.assertIn('onlyIf { originalEventTrace }', build)
        self.assertIn('dependsOn(verifyCalibrationRunConfigGuard, nativeProbe.classesTaskName, sourceSets.main.get().classesTaskName)', build)
        self.assertIn('original-events-classes.sha256', build)
        self.assertIn('Ordered observation flag escaped native RunConfig', build)
        source = (ROOT/'src/nativeProbe/java/com/bettercontent/betterwildfruittrees/gametest/nativeprobe/ChunkWriteTraceMixin.java').read_text()
        self.assertIn('@At("HEAD"), require = 1, expect = 1, allow = 1', source)
        self.assertIn('@At("RETURN"), require = 4, expect = 4, allow = 4', source)
        core = (ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/OrderedEvents.java').read_text()
        for name in ('queryBegin', 'queryEnd', 'writeHead', 'writeReturn', 'sky', 'adequate', 'publication'):
            body = core.split('public static void '+name+'(', 1)[1].split('\n    }', 1)[0]
            for forbidden in ('new ', 'Files.', 'System.out', 'synchronized', '.wait(', '.join(', 'getBlockState', 'nextInt'):
                self.assertNotIn(forbidden, body)
        self.assertIn('StandardOpenOption.CREATE_NEW', core)
