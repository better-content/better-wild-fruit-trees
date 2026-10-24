"""Synthetic controller/schema controls only; no native geometry acceptance."""
from copy import deepcopy
from fractions import Fraction
import hashlib
from pathlib import Path
import subprocess
import unittest
import uuid
from check_growth_v2 import LEGACY, REFERENCES, validate_rows, read_rows

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest'
PROFILE='tcon=false:ars=false:wild=false'

def synthetic():
    rows=[]
    def emit(kind,data):rows.append(dict(type=kind,sequence=len(rows),data=data))
    binding=dict(fixtureVersion='v2',scope='prototype',control='none',profile=PROFILE,sourceDirty='false',sourceCommit='a'*40,rootCommit='b'*40,
                 classHashSetSha256='c'*64,inputHashSetSha256='d'*64,nativeInputReceiptSha256='e'*64,buildSha256='f'*64)
    emit('HEADER',dict(schema='bc.wft.growth.v2',species=['bayberry','kiwi'],bindings=binding,bindingSha256='1'*64,samples=8,pulseLimit=4096,setupLimitTicks=200,
                       testTimeoutTicks=13*8*(4096+200+8)+32,endpoint='native_maximum_trunk_radius',referenceProvenance='UNKNOWN'))
    tick=1
    for fruit in ['bayberry','kiwi']:
        reference=REFERENCES[fruit];floor=int(reference);extra=int((reference-floor)*8)
        for seed in range(8):
            sample=dict(species=fruit,seed=seed);pulses=floor+(seed<extra)
            emit('PREPARE',dict(sample=sample,tick=tick,day=6000,ticketCount=36,min=[4,160,4],max=[92,220,92]))
            emit('CONSTRUCT',dict(sample=sample,tick=tick+1,reference=reference,endpoint=8))
            emit('SETUP_CHECK',dict(tick=tick+2,day=6000,sunlight=True,nativeAdequateLight=True,tickingReady=True))
            emit('ADMIT',dict(sample=sample,tick=tick+2,day=6000,growthSeed=12345+104729*seed,worldSeed=54321+104729*seed,fertility=15,setupDurationNs=10))
            if fruit=='bayberry' and seed==0:
                emit('ISOLATION_CONTROL',dict(ownedCancelled=1,unrelatedOriginalReturned=1,measurementAlreadyEnded=True,gateDisarmed=True,tick=tick+3+pulses))
            emit('CLEANUP',dict(sample=sample,before=dict(plotOwned=True,ticketCount=36,ownedEntityCount=0,tickets=[],ownedEntityIds=[]),after=dict(plotOwned=False,ticketCount=0,ownedEntityCount=0,tickets=[],ownedEntityIds=[]),durationNs=5,failure='NONE'))
            emit('SAMPLE',dict(result=dict(sample=sample,status='PASS',pulses=pulses,radius=8,endpoint=8,startTick=tick,admissionTick=tick+2,endTick=tick+2+pulses,reason='NATIVE_MAX_TRUNK_RADIUS'),
                               position=[48,160,48],admissionDay=6000,terminalDay=6000,measurementDurationNs=20,cleanupDurationNs=5,
                               nativeCalls=dict(grow=pulses,age=pulses//16,worldSeedCalls=1,firstPulseTick=tick+3,lastPulseTick=tick+2+pulses,maximumGapTicks=1)))
            tick+=pulses+4
        emit('MEAN',dict(species=fruit,validSamples=8,measured=reference,reference=reference,deviation=0.,deviationNumerator=0,deviationDenominator=int(reference*8),pass_=True))
        rows[-1]['data']['pass']=rows[-1]['data'].pop('pass_')
    emit('TERMINAL',dict(complete=True,pass_=True));rows[-1]['data']['pass']=rows[-1]['data'].pop('pass_')
    return rows

class GrowthV2Test(unittest.TestCase):
    def test_actual_cleanup_reentrant_entity_release_and_partial_exception_controls(self):
        out=ROOT/'build/growth-cleanup-controls'/str(uuid.uuid4());out.mkdir(parents=True)
        p=subprocess.run(['javac','--release','17','-d',str(out),str(PKG/'GrowthPlotCleanup.java'),str(PKG/'GrowthOwnedResources.java'),str(ROOT/'scripts/GrowthCleanupSelfTest.java')],capture_output=True,text=True,timeout=30)
        self.assertEqual(0,p.returncode,p.stdout+p.stderr)
        p=subprocess.run(['java','-ea','-cp',str(out),'GrowthCleanupSelfTest'],capture_output=True,text=True,timeout=30)
        (out/'result.log').write_text(p.stdout+p.stderr);self.assertEqual(0,p.returncode,p.stdout+p.stderr)
        rows=synthetic();next(x for x in rows if x['type']=='CLEANUP')['data']['after']['ownedEntityCount']=1
        with self.assertRaises(AssertionError):validate_rows(rows,'prototype',PROFILE)
        rows=synthetic();next(x for x in rows if x['type']=='CLEANUP')['data']['failure']='dirty entities'
        with self.assertRaises(AssertionError):validate_rows(rows,'prototype',PROFILE)
        for target in ('PREPARE','ADMIT'):
            rows=synthetic();clean=next(x for x in rows if x['type']=='CLEANUP');rows.remove(clean)
            rows.insert(next(i for i,x in enumerate(rows) if x['type']==target),clean)
            with self.assertRaises(AssertionError):validate_rows(rows,'prototype',PROFILE)
        rows=synthetic();future=next(x for x in rows if x['type']=='CLEANUP' and x['data']['sample']['seed']==1);rows.remove(future)
        rows.insert(next(i for i,x in enumerate(rows) if x['type']=='CLEANUP'),future)
        with self.assertRaises(AssertionError):validate_rows(rows,'prototype',PROFILE)

    def test_controller_real_jdk_transitions_containment_and_independent_aggregation(self):
        out=ROOT/'build/growth-controller-controls'/str(uuid.uuid4());out.mkdir(parents=True)
        p=subprocess.run(['javac','--release','17','-d',str(out),str(PKG/'GrowthFixtureController.java'),str(PKG/'GrowthRootIsolation.java'),str(PKG/'GrowthTickingReadiness.java'),str(PKG/'GrowthOwnedResources.java'),str(ROOT/'scripts/GrowthControllerSelfTest.java')],capture_output=True,text=True,timeout=30)
        self.assertEqual(0,p.returncode,p.stdout+p.stderr)
        p=subprocess.run(['java','-ea','-cp',str(out),'com.bettercontent.betterwildfruittrees.gametest.GrowthControllerSelfTest'],capture_output=True,text=True,timeout=30)
        (out/'result.log').write_text(p.stdout+p.stderr);self.assertEqual(0,p.returncode,p.stdout+p.stderr);self.assertIn('14 controller scenarios plus root lease/forwarding and completed-LEFT identity controls PASS',p.stdout)

    def test_v1_registration_preserved_and_v2_native_contract_is_distinct(self):
        source=(PKG/'GrowthRateGameTests.java').read_text()
        self.assertIn('v2 ? GrowthFixturePlan.BATCH : "defaultBatch", GrowthFixturePlan.NAME',source)
        self.assertIn('WildFruitTrees.MOD_ID + ":growth", v2 ? GrowthFixtureController.MAX_TICKS : 2000, 0, true',source)
        self.assertIn('v2 ? GrowthFixtureV2::start : GrowthRateGameTests::nativeFreshGrowthCalibration',source)
        self.assertEqual(1,source.count('@net.minecraft.gametest.framework.GameTestGenerator'))
        native=(PKG/'GrowthFixtureV2.java').read_text();controller=(PKG/'GrowthFixtureController.java').read_text()
        for forbidden in ('getLightEngine(', 'runUpdates(', 'synchronized', '.sleep(', '.wait(', '.join(', 'OriginalLightTrace.'):
            self.assertNotIn(forbidden,native+controller)
        self.assertEqual(1,native.count('branch.growSignal('));self.assertEqual(1,native.count('TreeHelper.ageVolume('))
        self.assertIn('pulse % 16 == 0',native);self.assertEqual(1,native.count('level.random.setSeed('))
        for marker in ('isPositionTicking(', 'shouldTickBlocksAt(', 'areEntitiesLoaded(', 'hasAdequateLight(', 'removeRegionTicket(', 'RULE_DAYLIGHT', 'RULE_WEATHER_CYCLE'):
            self.assertIn(marker,native)
        self.assertIn('frequencyRate(rate)',source)
        build=(ROOT/'build.gradle.kts').read_text();self.assertIn('orNull ?: "v1"',build)
        self.assertIn('V2 is separate from all original diagnostic scopes',build)
        self.assertIn('"**/*growth-v2*"',build)
        # The original whole-file fixture oracle remains authoritative after EXACT reviewed migration.
        from test_native_light_probe import OriginalLightProbeTest
        OriginalLightProbeTest('test_only_observation_statements_added_to_original_fixture').test_only_observation_statements_added_to_original_fixture()

    def test_real_registration_and_plan_with_typed_native_constructor_stub(self):
        out=ROOT/'build/growth-registration-controls'/str(uuid.uuid4());out.mkdir(parents=True)
        source=(PKG/'GrowthRateGameTests.java').read_text().split('// VERSIONED_GROWTH_REGISTRATION_BEGIN',1)[1].split('// VERSIONED_GROWTH_REGISTRATION_END',1)[0]
        source=source.replace('@net.minecraft.gametest.framework.GameTestGenerator','')
        package='package com.bettercontent.betterwildfruittrees.gametest;'
        stubs={
            'net/minecraft/gametest/framework/GameTestHelper.java':'package net.minecraft.gametest.framework; public class GameTestHelper {public String tag;}',
            'net/minecraft/gametest/framework/TestFunction.java':'''package net.minecraft.gametest.framework; import java.util.function.Consumer; public record TestFunction(String batch,String name,String template,int ticks,long setup,boolean required,Consumer<GameTestHelper> body) {}''',
            'com/bettercontent/betterwildfruittrees/WildFruitTrees.java':'package com.bettercontent.betterwildfruittrees; public class WildFruitTrees {public static final String MOD_ID="better_wild_fruit_trees";}',
            'com/bettercontent/betterwildfruittrees/gametest/GrowthFixtureV2.java':package+'public class GrowthFixtureV2 {public static void start(net.minecraft.gametest.framework.GameTestHelper h){h.tag="V2";}}',
            'com/bettercontent/betterwildfruittrees/gametest/GrowthRateGameTests.java':package+'import com.bettercontent.betterwildfruittrees.WildFruitTrees; public class GrowthRateGameTests {'+source+'public static void nativeFreshGrowthCalibration(net.minecraft.gametest.framework.GameTestHelper h){h.tag="V1";}}',
            'com/bettercontent/betterwildfruittrees/gametest/RegistrationControl.java':package+'''public class RegistrationControl {public static void main(String[] a){
                String p=GrowthFixturePlan.PREFIX;
                for(String v:new String[]{"v1","v2"}){
                    System.setProperty(p+"growthFixtureVersion",v);var f=GrowthRateGameTests.growthCalibrationVersions();if(f.size()!=1)throw new AssertionError();var x=f.iterator().next();
                    if(!x.name().equals("nativefreshgrowthcalibration")||!x.template().equals("better_wild_fruit_trees:growth")||!x.required()||x.setup()!=0)throw new AssertionError();
                    if(x.ticks()!=(v.equals("v1")?2000:447648)||!x.batch().equals(v.equals("v1")?"defaultBatch":GrowthFixturePlan.BATCH))throw new AssertionError();
                    var h=new net.minecraft.gametest.framework.GameTestHelper();x.body().accept(h);if(!h.tag.equals(v.toUpperCase()))throw new AssertionError();
                }
                System.setProperty(p+"growthFixtureScope","prototype");if(!GrowthFixturePlan.species().equals(java.util.List.of("bayberry","kiwi")))throw new AssertionError();
                System.setProperty(p+"growthFixtureControl","double-cadence");if(GrowthFixturePlan.frequencyRate(.25F)!=.5F)throw new AssertionError();
                System.setProperty(p+"growthFixtureVersion","v1");try{GrowthFixturePlan.species();throw new AssertionError();}catch(IllegalArgumentException expected){}
                System.setProperty(p+"growthFixtureVersion","unknown");try{GrowthFixturePlan.version();throw new AssertionError();}catch(IllegalArgumentException expected){}
                System.out.println("Actual registration/production inventory/default/negative-input controls PASS, not native acceptance");
            }}'''}
        paths=[]
        for name,text in stubs.items():
            path=out/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text);paths.append(str(path))
        production=PKG.parent
        paths += [str(production/name) for name in ['WildFruitPolicy.java','TreeGrowthRates.java','ProductionRates.java']]
        paths += [str(PKG/name) for name in ['GrowthFixturePlan.java','GrowthFixtureController.java']]
        p=subprocess.run(['javac','--release','17','-d',str(out),*paths],capture_output=True,text=True,timeout=30)
        self.assertEqual(0,p.returncode,p.stdout+p.stderr)
        p=subprocess.run(['java','-ea','-cp',str(out),'com.bettercontent.betterwildfruittrees.gametest.RegistrationControl'],capture_output=True,text=True,timeout=30)
        (out/'result.log').write_text(p.stdout+p.stderr);self.assertEqual(0,p.returncode,p.stdout+p.stderr)

    def test_required_isolation_refmap_and_source_set_negatives(self):
        from check_growth_fixture_sdk import check_refmap, ROOTY, CHUNK, DESC
        prefix='com/bettercontent/betterwildfruittrees/gametest/growthv2/mixin/'
        mappings={prefix+'RootRandomTickIsolationMixin':{'randomTick'+DESC:'L'+ROOTY+';m_213898_'+DESC},
                  prefix+'TickingHolderAccess':{'getVisibleChunkIfPresent':'m_140327_(J)Lnet/minecraft/server/level/ChunkHolder;'}}
        good=dict(mappings=mappings,data=dict(searge=deepcopy(mappings)));check_refmap(good)
        for mutation in ('missing','wrong','namespace'):
            bad=deepcopy(good)
            if mutation=='missing':bad['mappings'].pop(prefix+'RootRandomTickIsolationMixin')
            elif mutation=='wrong':bad['mappings'][prefix+'RootRandomTickIsolationMixin']['randomTick'+DESC]='m_000_'
            else:bad['data']={'unsupported':bad['data']['searge']}
            with self.assertRaises((AssertionError,KeyError)):check_refmap(bad)
        build=(ROOT/'build.gradle.kts').read_text()
        block=build.split('sourceSets.create("growthFixtureV2") {',1)[1].split('} else null',1)[0]
        self.assertIn('runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath',block)
        self.assertNotIn('runtimeClasspath += sourceSets.main.get().compileClasspath',block)
        config=__import__('json').loads((ROOT/'src/growthFixtureV2/resources/better_wild_fruit_trees.growth-v2.mixins.json').read_text())
        self.assertTrue(config['required']);self.assertEqual(1,config['injectors']['defaultRequire'])
        self.assertEqual({'RootRandomTickIsolationMixin','TickingHolderAccess'},set(config['mixins']))
        self.assertNotIn('config("better_wild_fruit_trees.growth-v2.mixins.json")',build)
        self.assertIn('if (growthFixtureVersion == "v2") sourceSets.create("growthFixtureV2")',build)

    def test_receipt_complete_positive_and_fail_closed_negatives(self):
        rows=synthetic();result=validate_rows(rows,'prototype',PROFILE)
        self.assertEqual(16,result['sample_count']);self.assertFalse(result['full_growth_acceptance'])
        mutations=[]
        def mutate(fn):
            bad=deepcopy(rows);fn(bad);mutations.append(bad)
        mutate(lambda x:x[0]['data']['bindings'].update(fixtureVersion='v1'))
        mutate(lambda x:x[0]['data']['bindings'].update(profile='tcon=true:ars=true:wild=true'))
        mutate(lambda x:x[0]['data']['bindings'].update(sourceDirty='true'))
        mutate(lambda x:x[0]['data'].update(pulseLimit=4097))
        mutate(lambda x:x[0]['data'].update(testTimeoutTicks=2000))
        mutate(lambda x:x[0]['data'].update(species=['kiwi','bayberry']))
        mutate(lambda x:x[-1]['data'].update(complete=False))
        mutate(lambda x:x[-1]['data'].update(pass_=False,**{'pass':False}))
        sample=next(i for i,x in enumerate(rows) if x['type']=='SAMPLE');admit=next(i for i,x in enumerate(rows) if x['type']=='ADMIT')
        mean=next(i for i,x in enumerate(rows) if x['type']=='MEAN');check=next(i for i,x in enumerate(rows) if x['type']=='SETUP_CHECK')
        mutate(lambda x:x.pop(sample));mutate(lambda x:x.insert(sample,deepcopy(x[sample])))
        mutate(lambda x:x[sample]['data']['result'].update(status='INCOMPLETE'))
        mutate(lambda x:x[sample]['data']['result'].update(radius=7))
        mutate(lambda x:x[sample]['data']['nativeCalls'].update(grow=1))
        mutate(lambda x:x[sample]['data']['nativeCalls'].update(age=0))
        mutate(lambda x:x[sample]['data']['nativeCalls'].update(worldSeedCalls=2))
        mutate(lambda x:x[admit]['data'].update(worldSeed=0))
        mutate(lambda x:x[check]['data'].update(nativeAdequateLight=False))
        isolation=next(i for i,x in enumerate(rows) if x['type']=='ISOLATION_CONTROL')
        mutate(lambda x:x[isolation]['data'].update(ownedCancelled=0))
        mutate(lambda x:x[isolation]['data'].update(unrelatedOriginalReturned=0))
        mutate(lambda x:x[mean]['data'].update(reference=274.0))
        mutate(lambda x:x[mean]['data'].update(validSamples=7))
        mutate(lambda x:x[mean]['data'].update(measured=0.0))
        for bad in mutations:
            with self.assertRaises((AssertionError,KeyError,TypeError,ValueError)):validate_rows(bad,'prototype',PROFILE)
        with self.assertRaises(AssertionError):validate_rows(rows,'full',PROFILE)
        with self.assertRaises(AssertionError):validate_rows(rows,'prototype',PROFILE,'zero-energy')
        out=ROOT/'build/growth-receipt-controls'/str(uuid.uuid4());out.mkdir(parents=True)
        import json
        raw='\n'.join(json.dumps(x) for x in rows)+'\n';(out/'good.jsonl').write_text(raw);self.assertEqual(rows,read_rows(out/'good.jsonl'))
        for i,bad in enumerate((raw[:-1],raw.replace('"sequence": 1','"sequence": 0',1),raw.replace('"type": "HEADER"','"type":"HEADER","type":"HEADER"',1))):
            path=out/f'bad-{i}.jsonl';path.write_text(bad)
            with self.assertRaises((AssertionError,KeyError,ValueError)):read_rows(path)

if __name__=='__main__':unittest.main()
