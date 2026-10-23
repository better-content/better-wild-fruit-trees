from pathlib import Path
import subprocess
import unittest
import uuid

from check_calibration_log import FORMS, PROFILES, check, ordinals

ROOT = Path(__file__).resolve().parents[1]


def synthetic(profile):
    """Synthetic oracle controls only; these are NOT native measurements/evidence."""
    lines = [f'OPTIONAL_GROWTH_PROFILE profile={profile} namespace=better_wild_fruit_calibration samples={8 * len(PROFILES[profile])}',
             'OPTIONAL_NATIVE_REDWOOD_BINDING redwood=native_RedwoodLogic hemlock=unchanged_native_RedwoodLogic']
    for o in PROFILES[profile]:
        for s in range(8):
            endpoint = ('native_upturned_four_arm_v1' if o in (0, 3) else
                        'native_coconut_fruiting_crown_v1' if o == 1 else 'native_maximum_trunk_radius')
            lines += [f'OPTIONAL_GROWTH_INITIAL {FORMS[o]} sample={s} Geometry[branches=1, height=1, trunkRadius=1]',
                      f'OPTIONAL_GROWTH_SAMPLE {FORMS[o]} sample={s} pulses=8 formOrdinal={o} canonicalIndex={o*8+s} endpoint={endpoint} geometry=Geometry[branches=8, height=8, trunkRadius=8]']
        lines.append(f'OPTIONAL_GROWTH_MEAN {FORMS[o]} meanPulses=8.0')
    return '\n'.join(lines + ['1 GAME TESTS COMPLETE', 'All 1 required tests passed'])


class CalibrationProfiles(unittest.TestCase):
    def test_pure_java_original_canonical_indices_and_rng_recipes(self):
        out = ROOT/'build/source-calibration-controls'/str(uuid.uuid4())
        out.mkdir(parents=True)
        sdk = Path('/home/dev/.sdkman/candidates/java/current/bin')
        sources = [ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/CalibrationPlan.java',
                   ROOT/'scripts/CalibrationPlanSelfTest.java',
                   ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/CoconutArchitecture.java',
                   ROOT/'scripts/CoconutArchitectureSelfTest.java']
        compiled = subprocess.run([str(sdk/'javac'), '-proc:none', '-d', str(out), *map(str, sources)], capture_output=True, text=True)
        (out/'javac.log').write_text(compiled.stdout + compiled.stderr)
        self.assertEqual(compiled.returncode, 0, compiled.stderr)
        ran = subprocess.run([str(sdk/'java'), '-cp', str(out), 'com.bettercontent.betterwildfruittrees.gametest.CalibrationPlanSelfTest'], capture_output=True, text=True)
        (out/'recipes.log').write_text(ran.stdout + ran.stderr)
        self.assertEqual(ran.returncode, 0, ran.stderr)
        rows = [line.split() for line in ran.stdout.splitlines()]
        self.assertEqual(len(rows), 32 + 8 + 16)
        for profile in PROFILES:
            expected = [(FORMS[o], str(o*8+s), str(s), str(12345+104729*s), str(54321+104729*s))
                        for o in PROFILES[profile] for s in range(8)]
            self.assertEqual([tuple(x[1:]) for x in rows if x[0] == profile], expected)
        coconut = subprocess.run([str(sdk/'java'), '-cp', str(out), 'com.bettercontent.betterwildfruittrees.gametest.CoconutArchitectureSelfTest'], capture_output=True, text=True)
        (out/'coconut-endpoint-controls.log').write_text(coconut.stdout + coconut.stderr)
        self.assertEqual(coconut.returncode, 0, coconut.stderr)
        print('Retained pure JDK profile controls:', out)

    def test_valid_synthetic_profiles_are_distinct_not_native_acceptance(self):
        for profile in PROFILES:
            self.assertEqual(check(profile, synthetic(profile)), [])
        self.assertTrue(check('all', synthetic('all').replace('native_coconut_fruiting_crown_v1', 'native_maximum_trunk_radius')))

    def test_invalid_empty_duplicate_unknown_or_unapproved_selector(self):
        for selector in ('', ' ', 'unknown', 'ancient,ancient', 'saguaro', 'coconut', 'saguaro_mega,saguaro', 'all,ancient'):
            with self.assertRaises(ValueError):
                ordinals(selector)

    def test_missing_extra_duplicate_mislabelled_samples_or_means(self):
        good = synthetic('ancient')
        mutations = [
            good.replace('OPTIONAL_GROWTH_SAMPLE dtquark:ancient sample=0', 'MISSING sample=0'),
            good + '\n' + next(s for s in good.splitlines() if 'OPTIONAL_GROWTH_SAMPLE' in s),
            good.replace('canonicalIndex=16', 'canonicalIndex=0'),
            good.replace('formOrdinal=2', 'formOrdinal=0'),
            good.replace('sample=7', 'sample=6'),
            good.replace('dtquark:ancient', 'unknown:ancient'),
            good.replace('OPTIONAL_GROWTH_MEAN', 'MISSING_MEAN'),
            good.replace('meanPulses=8.0', 'meanPulses=26.75'),
            good.replace('native_maximum_trunk_radius', 'plateau'),
            good.replace('pulses=8', 'pulses=4097'),
        ]
        for text in mutations:
            self.assertTrue(check('ancient', text))
        cactus = synthetic('saguaro,saguaro_mega')
        self.assertTrue(check('saguaro,saguaro_mega', cactus.replace(FORMS[3], FORMS[0])))

    def test_namespace_zero_case_extra_case_header_and_absence_controls(self):
        good = synthetic('ancient')
        for text in ('', ' ', good.replace('All 1', 'All 0'), good.replace('All 1', 'All 2'), good.replace('1 GAME TESTS COMPLETE', '0 GAME TESTS COMPLETE'),
                     good + '\nAll 1 required tests passed',
                     good.replace('better_wild_fruit_calibration', 'better_wild_fruit_trees'),
                     good.replace('profile=ancient', 'profile=all'),
                     good.replace('samples=8', 'samples=32'),
                     good.replace('OPTIONAL_NATIVE_REDWOOD_BINDING', 'MISSING_BINDING')):
            self.assertTrue(check('ancient', text))

    def test_actual_failed_operation_patterns_at_warn_debug_error(self):
        good = synthetic('ancient')
        for severity, logger, message in (
            ('WARN', 'net.minecraft.world.level.chunk.LevelChunk/', 'Tried to load a block entity for block Block{supplementaries:urn} but failed at location BlockPos{x=0, y=0, z=0}'),
            ('WARN', 'minecraft/LevelChunk', 'Tried to load a DUMMY block entity @ BlockPos{x=0} but found not block entity block Block{minecraft:air}'),
            ('WARN', 'net.minecraftforge.common.loot.LootModifierManager/', 'Could not decode GlobalLootModifier with json id goety:missing - error: null'),
            ('WARN', 'com.ferreusveritas.dynamictrees.tree.species.Species/', 'Rooty Dirt block NOT FOUND for soil minecraft:seagrass'),
            ('DEBUG', 'example/', 'Failed to apply native operation'),
            ('ERROR', 'example/', 'Registry operation rejected'),
        ):
            record = f'[21Oct2026 13:30:00.000] [main/{severity}] [{logger}]: {message}'
            self.assertTrue(check('ancient', good + '\n' + record))

    def test_effective_sdk_plus_authored_args_source_controls(self):
        # Pure argument-contract controls, NOT an actual Gradle RunConfig reconstruction.
        sdk = ['--launchTarget', 'forgegametestserveruserdev', '--gameDir', '.',
               '--fml.forgeVersion', '47.4.13', '--fml.mcVersion', '1.20.1',
               '--fml.forgeGroup', 'net.minecraftforge', '--fml.mcpVersion', '20230612.114412']
        existing_mixin = ['--mixin.config', 'better_wild_fruit_trees.mixins.json']
        expected = sdk + ['--nogui'] + existing_mixin
        valid = lambda args: args == expected
        self.assertTrue(valid(expected))
        self.assertFalse(valid(['--nogui'] + sdk))
        for i in range(len(sdk)):
            self.assertFalse(valid(sdk[:i] + ['--nogui'] + sdk[i:]))
        wrong_path = sdk.copy(); wrong_path[3] = '/other/world'
        wrong_placeholder = sdk.copy(); wrong_placeholder[3] = '{game_directory}'
        for args in (['--nogui'], sdk, sdk[:-1] + ['--nogui'], sdk + ['--nogui', '--nogui'],
                     wrong_path + ['--nogui'], wrong_placeholder + ['--nogui'],
                     sdk + ['--nogui', '--launchTarget', 'forgegametestserveruserdev'],
                     sdk + ['--nogui', '--world', 'other'],
                     ['--nogui'] + list(reversed(sdk)),
                     [a.replace('47.4.13', '47.4.14') for a in sdk] + ['--nogui']):
            self.assertFalse(valid(args))
        for path in ('build.gradle.kts', 'gradle/finite-calibration-model.init.gradle'):
            text = (ROOT/path).read_text()
            for token in ('pinnedSdkArgs', 'forgegametestserveruserdev', '20230612.114412'):
                self.assertIn(token, text)
        init = (ROOT/'gradle/finite-calibration-model.init.gradle').read_text()
        for args in (sdk + ['--nogui'], expected + existing_mixin,
                     sdk + ['--nogui', '--mixin.config', 'wrong.mixins.json'],
                     sdk + ['--mixin.config', '--nogui', 'better_wild_fruit_trees.mixins.json'],
                     sdk + ['--nogui', '--mixin.config', '/other/better_wild_fruit_trees.mixins.json']):
            self.assertFalse(valid(args))
        self.assertIn('ACTUAL_MODEL_MISMATCH.json', init)
        self.assertIn('ACTUAL_ENGINE_MODEL_MISMATCH.json', init)
        self.assertIn('nativeTask.workingDir.canonicalFile != directory', init)
        self.assertIn("nativeTask.mainClass.orNull != 'cpw.mods.bootstraplauncher.BootstrapLauncher'", init)
        self.assertIn('StandardOpenOption.CREATE_NEW', init)

    def test_main_matrix_and_native_physics_guards_remain(self):
        source = (ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/OptionalGrowthCalibration.java').read_text()
        self.assertIn('probe(h, CalibrationPlan.select("all"), 0, true)', source)
        self.assertIn('timeoutTicks = 2000', source)
        for text in ('pulse <= 4096', 'new MegaSubstance().apply(level, root)', 'CactusNativeFixture.fresh(level, root.above(), source)',
                     'level.setDayTime(6000); level.updateSkyBrightness()',
                     'TreeHelper.ageVolume(level, root.above(), 20, 48, 1, SafeChunkBounds.ANY)',
                     'RandomSource.create(selected.growthSeed())', 'level.random.setSeed(selected.levelSeed())',
                     'branch.growSignal(level, root.above(), new GrowSignal(tree, root, tree.getEnergy(level, root), random, Direction.UP))'):
            self.assertIn(text, source)
        coconut = (ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/CoconutNativeFixture.java').read_text()
        consumer = (ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/OptionalTreeGameTests.java').read_text()
        self.assertIn('static ItemStack fallingDrop(Block block, FallingBlockEntity entity)', coconut)
        self.assertIn('return ((IFallingFruit)block).getDropOnFallItems(entity);', coconut)
        self.assertIn('CoconutNativeFixture.fallingDrop(block, entity)', consumer)
        self.assertIn('falling.is(product) && falling.getCount() == 8', consumer)
        build = (ROOT/'build.gradle.kts').read_text()
        self.assertIn('"saguaro", "coconut", "ancient", "saguaro_mega"', build)
        self.assertIn('nativeAttempt.resolve("logs/debug.log")', build)
        self.assertIn('Files.createDirectory(nativeAttempts.resolve(nativeAttemptId))', build)
        self.assertIn('BEFORE_OWN_PREPARE_OR_ENGINE', build)
        self.assertIn('verifyCalibrationRunConfigGuard', build)
        self.assertIn('actualFixture.$name', build)
        self.assertIn('dependsOn("reobfJar", verifyGrowthReferences)', build)

    def test_release_binding_is_unused_and_calibration_remains_isolated(self):
        build = (ROOT/'build.gradle.kts').read_text()
        self.assertEqual(1, build.count('System.getenv("BC_CUSTOM_MOD_JAR_DIR")'))
        self.assertIn('if (calibrationEnabled) check(providerBindingAbsent)', build)
        self.assertIn('receipt.setProperty("customProviderOverrideAbsent", providerBindingAbsent.toString())', build)
        self.assertIn('receipt.setProperty("customProviderBindingConsumed", "false")', build)


if __name__ == '__main__':
    unittest.main()
