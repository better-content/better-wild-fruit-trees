"""Authored retirement boundary controls, not historical/native acceptance."""
from pathlib import Path
import json
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
PKG = ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest'


def registered(sources):
    result=[]
    for name, text in sources.items():
        for match in re.finditer(r'@GameTest\([^)]*\)\s+public static void (\w+)\(', text):
            result.append((name+'.'+match[1]).lower())
    return sorted(result)


class NativeRetirement(unittest.TestCase):
    def test_actual_functional_registration_matches_authored_inventory(self):
        inventory=json.loads((ROOT/'scripts/native_required_tests.json').read_text())
        sources={p.stem:p.read_text() for p in PKG.glob('*.java')}
        actual=registered(sources)
        self.assertEqual(25,len(actual));self.assertEqual(len(actual),len(set(actual)))
        self.assertEqual(actual,inventory['required_tests'])
        self.assertTrue(all('@GameTestGenerator' not in s and 'framework.GameTestGenerator' not in s for s in sources.values()))
        self.assertFalse(any('freshgrowthcalibration' in n or 'freshgrowthdiagnostic' in n for n in actual))
        # Equal counts cannot hide replacing a retained functional case with calibration.
        bad=dict(sources);bad['GrowthRateGameTests']=bad['GrowthRateGameTests'].replace(
            'nativeGrowthFrequencyIsConstantWithoutAgeOrSizeTransitions','nativeFreshGrowthCalibration')
        self.assertEqual(25,len(registered(bad)));self.assertNotEqual(actual,registered(bad))

    def test_native_cadence_rate_energy_persistence_and_optional_functionality_remain(self):
        growth=(PKG/'GrowthRateGameTests.java').read_text()
        optional=(PKG/'OptionalTreeGameTests.java').read_text()
        for token in ['draw<4096','4096*rate*config.get()*folding.get()', 'Native growth pulse cadence ignored normalized rate',
                      'Age/calendar introduced a rate transition','Reload changed growth rate','Growth saved an age ledger',
                      'Ordinary growth rate changed','branch.growSignal(']:self.assertIn(token,growth)
        for token in ['optionalNativePulseCadenceUsesEachMeasuredReference','draw < 4096','4096 * rate * .5 * 2',
                      'Optional full-size energy missing','CoconutNativeFixture.fallingDrop(block, entity)',
                      'falling.is(product) && falling.getCount() == 8']:self.assertIn(token,optional)
        for text in [growth,optional]:
            self.assertNotIn('verifyWoodReferences(',text);self.assertNotIn('firstMax',text)
            self.assertNotIn('GrowthFixturePlan',text);self.assertNotIn('TreeHelper.ageVolume',text)

    def test_retired_entrypoints_fail_before_attempt_allocation_without_observer_or_reference_hardgates(self):
        build=(ROOT/'build.gradle.kts').read_text()
        self.assertLess(build.index('require(retiredGrowthProperties.none'),build.index('Files.createDirectories(nativeAttempts)'))
        for flag in ['optionalGrowthCalibration','optionalGrowthCalibrationForms','growthFixtureVersion','growthFixtureScope',
                     'growthFixtureControl','growthDiagnostics','originalEventTrace','originalEventTraceScope','lightQueueProof']:
            self.assertIn('"'+flag+'"',build)
        for forbidden in ['sourceSets.create(', 'verifyGrowthReferences', 'verifyNativeLightProbe', 'prepareGrowthFixtureBindings',
                          'verifyCalibrationRunConfigGuard', 'check_growth_v2.py','check_calibration_log.py']:
            self.assertNotIn(forbidden,build)
        self.assertIn('dependsOn("verifyFast", verifyNativeLog, verifyRuntimeJar)',build)
        self.assertIn('Native effective SDK/authored args override',build)

    def test_historical_code_is_retained_not_discovered_or_compiled(self):
        retired=ROOT/'retired/growth-calibration'
        self.assertTrue((retired/'java/GrowthFixtureV2.java').is_file())
        self.assertTrue((retired/'java/OptionalGrowthCalibration.java').is_file())
        self.assertTrue((retired/'scripts/test_growth_v2.py').is_file())
        self.assertFalse((retired/'scripts/__init__.py').exists())
        for forbidden in ['GrowthFixtureV2.java','OptionalGrowthCalibration.java','LegacyGrowthWitness.java','CalibrationBirths.java']:
            self.assertFalse((PKG/forbidden).exists())
