import unittest
from verify_artifact import check_log, expected_warning


class DiagnosticGateTest(unittest.TestCase):
    def test_missing_native_completion_blocks(self):
        self.assertTrue(check_log("All 7 required tests passed"))
        self.assertFalse(check_log("All 8 required tests passed"))

    def test_known_launcher_warning_only_at_warn(self):
        text = "[12:00:00] [main/WARN] [ne.mi.ja.se.JarSelector/]: Attempted to select two dependency jars from JarJar which have the same identification: Mod File:  and Mod File: . Using Mod File: "
        self.assertTrue(expected_warning(text))
        self.assertFalse(expected_warning(text.replace("/WARN]", "/ERROR]")))

    def test_unknown_warning_and_all_errors_block(self):
        for severity in ["WARN", "ERROR", "FATAL"]:
            text = f"[12:00:00] [Server thread/{severity}] [minecraft/Test]: unexpected\nAll 8 required tests passed"
            self.assertTrue(check_log(text))

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
