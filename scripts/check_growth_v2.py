"""Fail-closed V2 receipt reader. Diagnostic subset/control is never full/canonical acceptance."""
import argparse
from fractions import Fraction
import hashlib
import json
from pathlib import Path
import re

LEGACY = ('apple', 'bayberry', 'durian', 'fig', 'hawberry', 'kiwi', 'lychee', 'mango', 'mangosteen', 'orange', 'peach', 'pear', 'persimmon')
REFERENCES = dict(zip(LEGACY, (204.375,273.625,241.5,209.75,303.625,396.375,428.375,420.75,287.75,395.5,326.5,219.25,282.125)))
HEX = re.compile(r'[a-f0-9]{64}')

def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()

def properties(path):
    result = {}
    for row in path.read_text().splitlines():
        if not row.strip() or row.startswith(('#','!')): continue
        # Java Properties emitted here have simple literal keys and escaped scalar values only.
        match = re.fullmatch(r'([^=]+)=(.*)', row)
        assert match, 'Malformed property'
        key, value = match.groups()
        assert key not in result, 'Duplicate property'
        value = re.sub(r'\\([:=#! ])', r'\1', value)
        assert '\\' not in value, 'Unexpected escaped binding'
        result[key] = value
    return result

def read_rows(path):
    data = path.read_bytes()
    assert data and data.endswith(b'\n') and len(data) < 16*1024*1024, 'Partial/unbounded receipt'
    def unique(pairs):
        result = {}
        for key, value in pairs:
            assert key not in result, 'Duplicate JSON key'
            result[key] = value
        return result
    rows = [json.loads(x, object_pairs_hook=unique) for x in data.splitlines()]
    assert [x['sequence'] for x in rows] == list(range(len(rows))), 'Missing/duplicate/reordered receipt'
    assert rows[0]['type'] == 'HEADER' and rows[-1]['type'] == 'TERMINAL', 'Partial receipt'
    assert sum(x['type']=='HEADER' for x in rows)==1 and sum(x['type']=='TERMINAL' for x in rows)==1
    assert all(x['type'] in {'HEADER','PREPARE','CONSTRUCT','SETUP_CHECK','ADMIT','SAMPLE','MEAN','ISOLATION_CONTROL','TERMINAL'} for x in rows)
    return rows

def validate_rows(rows, scope, profile, control='none', require_success=True):
    assert scope in ('full','prototype') and control in ('none','zero-energy','double-cadence')
    species = list(LEGACY) if scope=='full' else ['bayberry','kiwi']
    header=rows[0]['data']; binding=header['bindings']
    assert header['schema']=='bc.wft.growth.v2' and header['species']==species
    assert header['samples']==8 and header['pulseLimit']==4096 and header['setupLimitTicks']==200
    assert header['testTimeoutTicks']==13*8*(200+4096+8)+32
    assert header['endpoint']=='native_maximum_trunk_radius' and header['referenceProvenance']=='UNKNOWN'
    assert binding['fixtureVersion']=='v2' and binding['scope']==scope and binding['control']==control
    assert binding['profile']==profile and binding['sourceDirty']=='false'
    assert re.fullmatch('[a-f0-9]{40}', binding['sourceCommit']) and re.fullmatch('[a-f0-9]{40}', binding['rootCommit'])
    for key in ('classHashSetSha256','inputHashSetSha256','nativeInputReceiptSha256','buildSha256'):
        assert HEX.fullmatch(binding[key]), 'Missing/malformed input binding'
    assert HEX.fullmatch(header['bindingSha256'])
    expected=[(s,i) for s in species for i in range(8)]
    samples=[x['data'] for x in rows if x['type']=='SAMPLE']
    assert [(x['result']['sample']['species'],x['result']['sample']['seed']) for x in samples]==expected, 'Missing/duplicate sample coverage'
    prepared={};constructed={};admitted={};checks=[];current=None;finished=set();mean_order=[];isolation_rows=[]
    for row in rows[1:-1]:
        kind,d=row['type'],row['data']
        if kind in ('PREPARE','CONSTRUCT','ADMIT'):
            key=(d['sample']['species'],d['sample']['seed']); assert key in expected and key not in finished
            table={'PREPARE':prepared,'CONSTRUCT':constructed,'ADMIT':admitted}[kind]
            assert key not in table, 'Duplicate lifecycle record'
            if kind=='PREPARE':
                assert current is None and key==expected[len(finished)]; current=key; checks=[]
                assert d['ticketCount']==36 and d['min']==[4,160,4] and d['max']==[92,220,92]
            else:
                assert current==key and key in prepared
                if kind=='CONSTRUCT':
                    assert d['reference']==REFERENCES[key[0]] and d['endpoint']>1
                else:
                    assert key in constructed and checks and all(checks[-1][x] is True for x in ('sunlight','nativeAdequateLight','tickingReady'))
                    assert d['growthSeed']==12345+104729*key[1] and d['worldSeed']==54321+104729*key[1] and d['fertility']==15
                    assert 0<d['tick']-prepared[key]['tick']<=200 and d['day']==6000 and d['setupDurationNs']>=0
            table[key]=d
        elif kind=='SETUP_CHECK':
            assert current in constructed; checks.append(d)
        elif kind=='SAMPLE':
            r=d['result'];key=(r['sample']['species'],r['sample']['seed']);assert key not in finished;finished.add(key)
            assert r['status'] in ('PASS','ASSERTION_FAILURE','INVALID_SETUP','INCOMPLETE')
            if r['status'] in ('PASS','ASSERTION_FAILURE'):
                assert current==key and key in admitted and r['startTick']==prepared[key]['tick'] and r['admissionTick']==admitted[key]['tick']
                assert 1<=r['pulses']<=4096 and r['endpoint']==constructed[key]['endpoint'] and r['endTick']==r['admissionTick']+r['pulses']
                assert d['position']==[48,160,48] and d['admissionDay']==d['terminalDay']==6000
                calls=d['nativeCalls'];assert calls['grow']==r['pulses'] and calls['age']==r['pulses']//16 and calls['worldSeedCalls']==1
                assert calls['firstPulseTick']==r['admissionTick']+1 and calls['lastPulseTick']==r['endTick']
                if r['pulses']>1:assert calls['maximumGapTicks']==1
                if r['status']=='PASS':assert r['radius']==r['endpoint'] and r['reason']=='NATIVE_MAX_TRUNK_RADIUS'
                else:assert r['pulses']==4096 and r['radius']!=r['endpoint']
            assert d['measurementDurationNs']>=0 and d['cleanupDurationNs']>=0
            current=None;checks=[]
        elif kind=='MEAN':
            assert current is None and len(mean_order)<len(species) and d['species']==species[len(mean_order)]
            assert len(finished)==(len(mean_order)+1)*8
            mean_order.append(d['species'])
        elif kind=='ISOLATION_CONTROL':
            assert current==expected[0] and current in admitted and not isolation_rows
            isolation_rows.append(d)
    means=[x['data'] for x in rows if x['type']=='MEAN'];terminal=rows[-1]['data']
    if terminal['complete']:
        isolation=[x['data'] for x in rows if x['type']=='ISOLATION_CONTROL']
        assert len(isolation)==1 and isolation[0]['ownedCancelled']==1 and isolation[0]['unrelatedOriginalReturned']==1
        assert isolation[0]['measurementAlreadyEnded'] is True and isolation[0]['gateDisarmed'] is True
        assert isolation[0]['tick']>samples[0]['result']['endTick']
        assert [x['species'] for x in means]==species
        for mean in means:
            batch=[x['result'] for x in samples if x['result']['sample']['species']==mean['species']]
            count=sum(x['status']=='PASS' for x in batch);assert mean['validSamples']==count and mean['reference']==REFERENCES[mean['species']]
            if count==8:
                measured=Fraction(sum(x['pulses'] for x in batch),8);ref=Fraction(str(mean['reference']));deviation=abs(measured/ref-1)
                assert mean['measured']==float(measured) and mean['deviation']==float(deviation)
                assert Fraction(mean['deviationNumerator'],mean['deviationDenominator'])==deviation
                assert mean['pass']==(deviation<Fraction(1,5))
            else:assert mean['measured'] is None and mean['deviation'] is None and mean['deviationNumerator'] is None and not mean['pass']
        assert terminal['pass']==all(x['pass'] for x in means)
    else:assert not terminal['pass'] and any(x['result']['status'] in ('INVALID_SETUP','INCOMPLETE') for x in samples)
    if require_success:
        assert control=='none', 'Fault controls cannot qualify'
        assert terminal['pass'] and terminal['complete'] and all(x['result']['status']=='PASS' for x in samples), 'Native fixture did not pass'
    return dict(scope=scope,profile=profile,control=control,sample_count=len(samples),native_fixture_pass=terminal['pass'],complete=terminal['complete'],full_growth_acceptance=scope=='full' and control=='none' and terminal['pass'] and terminal['complete'])

def validate_attempt(attempt, scope, profile, control='none', require_success=True):
    attempt=Path(attempt).absolute()
    assert not any(p.is_symlink() for p in (attempt,*attempt.parents)), 'Symlink native attempt ancestry'
    p=attempt/'growth-v2-bindings.properties'; binding=properties(p)
    assert binding['attemptDirectory']==str(attempt) and binding['nonce']==attempt.name
    for path,key in [('growth-v2-classes.sha256','classHashSetSha256'),('growth-v2-inputs.sha256','inputHashSetSha256'),('before-own-prepare.properties','nativeInputReceiptSha256')]:
        assert sha(attempt/path)==binding[key], 'Changed bound input '+path
    before=properties(attempt/'before-own-prepare.properties')
    assert before['attempt']==attempt.name and before['growthFixtureVersion']=='v2' and before['growthFixtureScope']==scope and before['growthFixtureControl']==control
    assert before['worldAbsent']=='true' and before['propertiesAbsent']=='true' and before['authoredSeed']=='8675309'
    assert before['growthDiagnostics']=='false' and before['originalEventTraceScope']=='NONE'
    assert before['actualNamespace']=='better_wild_fruit_trees' and before['profile']=='main26'
    rows=read_rows(attempt/'growth-v2-results.jsonl')
    assert rows[0]['data']['bindings']==binding and rows[0]['data']['bindingSha256']==sha(p)
    return validate_rows(rows,scope,profile,control,require_success)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--attempt',required=True);p.add_argument('--scope',required=True);p.add_argument('--profile',required=True)
    args=p.parse_args();print(json.dumps(validate_attempt(args.attempt,args.scope,args.profile)))
