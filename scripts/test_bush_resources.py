"""Authored resource contracts; native placement/harvest/break checks live in GameTests."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'


class BushResources(unittest.TestCase):
    def test_explicit_patch_inventory_and_optional_safe_codec(self):
        patches = {
            'minecraft:patch_berry_bush': 'minecraft:sweet_berry_bush',
            'fruitsdelight:blueberry_bush': 'fruitsdelight:blueberry_bush',
            'fruitsdelight:cranberry_bush': 'fruitsdelight:cranberry_bush',
            'ars_nouveau:patch_berry': 'ars_nouveau:sourceberry_bush',
        }
        for key, block in patches.items():
            namespace, name = key.split(':')
            data = json.loads((ROOT / 'data' / namespace / 'worldgen/configured_feature' / (name + '.json')).read_text())
            self.assertEqual(data, {'type': 'better_wild_fruit_trees:bush_field', 'config': {'block': block}})
        actual = list((ROOT / 'data').glob('*/worldgen/configured_feature/*.json'))
        self.assertEqual(len(actual), len(patches), 'Do not override trees or non-bush plants')
        self.assertFalse(list((ROOT / 'data').glob('*/worldgen/placed_feature/*.json')), 'Keep native frequency and biome placement')

    def test_juvenile_ages_use_full_foliage_not_seedling_models(self):
        for namespace, name, count, model in [
            ('minecraft', 'sweet_berry_bush', 4, 'minecraft:block/sweet_berry_bush_stage2'),
            ('fruitsdelight', 'blueberry_bush', 5, 'fruitsdelight:block/blueberry_bush'),
            ('fruitsdelight', 'cranberry_bush', 5, 'fruitsdelight:block/cranberry_bush_1'),
            ('ars_nouveau', 'sourceberry_bush', 4, 'ars_nouveau:block/sourceberry/sourceberry2'),
        ]:
            variants = json.loads((ROOT / 'assets' / namespace / 'blockstates' / (name + '.json')).read_text())['variants']
            self.assertEqual(set(variants), {f'age={age}' for age in range(count)})
            for age in range(3):
                self.assertEqual(variants[f'age={age}']['model'], model)
            self.assertNotEqual(variants[f'age={count-1}']['model'], model, 'Keep ripe fruit visible')


if __name__ == '__main__':
    unittest.main()
