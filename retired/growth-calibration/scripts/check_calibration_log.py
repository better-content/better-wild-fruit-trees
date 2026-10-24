"""Nonshipping finite diagnostic oracle, never a reference filler or main26 acceptance gate."""
import argparse
import json
from pathlib import Path
import re

from verify_artifact import classify_log

NAMESPACE = 'better_wild_fruit_calibration'
FORMS = ('dynamictreesplus:saguaro_cactus', 'dtnatures_spirit:coconut',
         'dtquark:ancient', 'dynamictreesplus:mega_cactus')
PROFILES = {'all': (0, 1, 2, 3), 'ancient': (2,), 'saguaro,saguaro_mega': (0, 3)}


def ordinals(profile):
    if profile not in PROFILES:
        raise ValueError('Unknown/empty/duplicate/out-of-order calibration profile')
    return PROFILES[profile]


def check(profile, text):
    selected = ordinals(profile)
    failures = []
    if not text.strip():
        return ['Missing/empty actual native log']
    failures.extend(d['record'] for d in classify_log(text) if d['blocking'])
    counts = re.findall(r'All (\d+) required tests passed', text)
    if counts != ['1'] or re.findall(r'(\d+) GAME TESTS COMPLETE', text) != ['1']:
        failures.append('Exactly one required diagnostic case and completion banner must complete, not zero/main26/extra cases')
    headers = [s.split('OPTIONAL_GROWTH_PROFILE ', 1)[1].strip() for s in text.splitlines() if 'OPTIONAL_GROWTH_PROFILE ' in s]
    expected_header = f'profile={profile} namespace={NAMESPACE} samples={8 * len(selected)}'
    if headers != [expected_header]:
        failures.append('Missing/extra/mislabelled profile or native namespace header')
    if sum('OPTIONAL_NATIVE_REDWOOD_BINDING redwood=native_RedwoodLogic hemlock=unchanged_native_RedwoodLogic' in s for s in text.splitlines()) != 1:
        failures.append('Missing actual native Redwood binding/control completion')
    expected = {(FORMS[o], s): (o, o * 8 + s) for o in selected for s in range(8)}
    samples = {}
    ordered = []
    initials = []
    means = []
    for line in text.splitlines():
        if 'OPTIONAL_GROWTH_INITIAL ' in line:
            m = re.search(r'OPTIONAL_GROWTH_INITIAL (\S+) sample=(\d+) Geometry\[', line)
            if not m:
                failures.append('Malformed initial geometry record')
            else:
                initials.append((m[1], int(m[2])))
        if 'OPTIONAL_GROWTH_SAMPLE ' in line:
            m = re.search(r'OPTIONAL_GROWTH_SAMPLE (\S+) sample=(\d+) pulses=(\d+) formOrdinal=(\d+) canonicalIndex=(\d+) endpoint=(\S+) geometry=Geometry\[', line)
            if not m:
                failures.append('Malformed/missing canonical sample fields')
                continue
            species, sample, pulse, ordinal, index, endpoint = m.groups()
            key = species, int(sample)
            ordered.append(key)
            if key in samples or key not in expected:
                failures.append('Duplicate/extra/unknown/mislabelled form sample')
            if expected.get(key) != (int(ordinal), int(index)) or not 1 <= int(pulse) <= 4096:
                failures.append('Canonical form ordinal/sample index/pulse deadline mismatch')
            target = ('native_upturned_four_arm_v1' if species in (FORMS[0], FORMS[3]) else
                      'native_coconut_fruiting_crown_v1' if species == FORMS[1] else 'native_maximum_trunk_radius')
            if endpoint != target:
                failures.append('Changed native structural/full-radius endpoint')
            samples[key] = int(pulse)
        if 'OPTIONAL_GROWTH_MEAN ' in line:
            m = re.search(r'OPTIONAL_GROWTH_MEAN (\S+) meanPulses=([0-9.]+)\s*$', line)
            if not m:
                failures.append('Malformed per-form mean')
            else:
                means.append((m[1], float(m[2])))
    if ordered != list(expected) or initials != list(expected):
        failures.append('Missing/extra/reordered initial/sample records; canonical ordering is mandatory')
    if [s for s, _ in means] != [FORMS[o] for o in selected]:
        failures.append('Missing/duplicate/extra/mislabelled independent per-form means')
    for species, mean in means:
        values = [samples.get((species, s)) for s in range(8)]
        if None in values or sum(values) / 8 != mean:
            failures.append('Mean does not equal that form\'s own eight actual samples')
    return failures


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--profile', required=True, choices=PROFILES)
    parser.add_argument('--latest', type=Path, required=True)
    parser.add_argument('--debug', type=Path, required=True)
    args = parser.parse_args()
    failed = []
    for path in (args.latest, args.debug):
        text = path.read_text() if path.is_file() else ''
        for item in classify_log(text):
            print(json.dumps(dict(log=path.name, **item)))
        failed.extend(f'{path.name}: {s}' for s in check(args.profile, text))
    if failed:
        raise SystemExit('\n'.join(failed))
    print('Finite diagnostic only: both logs/case/form records verified; no reference fill or full26 acceptance')
