from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]

class NativeFruitingGeometry(unittest.TestCase):
    def test_cactus_fixture_hash_eligibility_is_bounded_and_not_rng(self):
        # Pinned CoordUtils hash mode 0 uses 4111/271/3067, shifts right once,
        # then xors coordXor. Only these low bits affect native shouldPlace's &6.
        # Exhaust all root residues and relevant salt bits; do not tune to one arena.
        def eligible(x, y, z, salt):
            return (((x * 4111 ^ y * 271 ^ z * 3067) >> 1) ^ salt) & 6 == 0
        for x in range(16):
            for y in range(16):
                for z in range(16):
                    for salt in range(0, 8, 2):
                        self.assertTrue(any(eligible(x+dx*d, y+h+1, z+dz*d, salt)
                            for dx, dz in ((0, 1), (-1, 0), (0, -1), (1, 0))
                            for d in range(1, 4) for h in range(2, 5)))
        self.assertTrue(any(not eligible(x, 3, 0, 0) for x in range(16)))
        fixture = (ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/CactusNativeFixture.java').read_text()
        self.assertIn('CactusBranchBlock.CactusThickness.BRANCH', fixture)
        self.assertIn('CoordUtils.coordHashCode(tip.above(), 0) & 6', fixture)
        consumer = (ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/OptionalTreeGameTests.java').read_text()
        for token in ('CactusNativeFixture.fruitingArm(level, pos, tree)', 'Invalid native geometry spent allowance',
                      'Empty allowance placed native product', 'Native placements and allowance debits disagree'):
            self.assertIn(token, consumer)
