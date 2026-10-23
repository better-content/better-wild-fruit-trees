from pathlib import Path
import json,subprocess,unittest,uuid,hashlib,zipfile,struct,shutil
from unittest.mock import patch
import check_light_queue_proof as q

class LightQueueProofControls(unittest.TestCase):
 def test_source_inventory_reserved_namespace(self):
  q.check_sources()
  out=q.ROOT/'build/source-queue-proof-controls'/('source-negatives-'+str(uuid.uuid4()));out.mkdir(parents=True)
  for mode in ('required','require','reserved','extra'):
   copy=out/mode;shutil.copytree(q.BASE,copy);cfg=copy/'resources/better_wild_fruit_trees.light-queue-proof.mixins.json';data=json.loads(cfg.read_text())
   if mode=='required':data['required']=False
   elif mode=='require':data['injectors']['defaultRequire']=0
   elif mode=='reserved':data['package']+='pollution'
   else:
    p=copy/'java/com/bettercontent/betterwildfruittrees/gametest/queueproof/mixin/descendant/Ordinary.java';p.parent.mkdir();p.write_text('class Ordinary {}')
   cfg.write_text(json.dumps(data))
   with self.assertRaises(AssertionError):q.check_sources(copy)
 def test_jdk_bounded_provenance_and_incomplete_negatives(self):
  out=q.ROOT/'build/source-queue-proof-controls'/('jdk-'+str(uuid.uuid4()));out.mkdir(parents=True);classes=out/'classes';classes.mkdir()
  source=q.BASE/'java/com/bettercontent/betterwildfruittrees/gametest/queueproof/QueueProofLedger.java'
  r=subprocess.run(['javac','--release','17','-d',str(classes),str(source),str(q.ROOT/'scripts/QueueProofLedgerSelfTest.java')],capture_output=True,text=True);(out/'javac.log').write_text(r.stdout+r.stderr);self.assertEqual(0,r.returncode,r.stderr)
  for mode in ('normal','duplicate','unknown-stage','pending','throwable','overflow','binding'):
   r=subprocess.run(['java','-cp',str(classes),'QueueProofLedgerSelfTest',mode,str(out/str(uuid.uuid4()))],capture_output=True,text=True,timeout=30);(out/(mode+'.log')).write_text(r.stdout+r.stderr);self.assertEqual(0,r.returncode,r.stdout+r.stderr)
 def test_new_native_profile_default_inert_and_archives(self):
  t=(q.ROOT/'build.gradle.kts').read_text()
  for s in ('else null','lightQueueProof?.let','source(lightQueueProof!!)','better_wild_fruit_trees.lightQueueProof','Queue proof requires the unchanged default present native fixture','withoutTConFixture','withoutArsFixture','withoutWildAddonsFixture','Queue proof and original ordered growth profile are separate experiments','check(effectiveArgs == expectedArgs)','prepareLightQueueProofBindings','**/*light-queue-proof*','**/*.wftq'):
   self.assertIn(s,t)
  import verify_artifact
  body=(q.ROOT/'scripts/verify_artifact.py').read_text();self.assertIn('"light-queue-proof" in name',body);self.assertIn('name.endswith(".wftq")',body)
  self.assertIn('run.properties["better_wild_fruit_trees.lightQueueProof"] == if (native && lightQueueProofEnabled) "true" else null',t)
 def test_log_exact_six_and_no_original_test_waiver(self):
  out=q.ROOT/'build/source-queue-proof-controls'/('log-'+str(uuid.uuid4()));out.mkdir(parents=True);attempt=out/str(uuid.uuid4());attempt.mkdir();directory=attempt/'light-queue-proof';directory.mkdir();file=directory/'pipeline-v1.wftq'
  (attempt/'light-queue-proof-classes.sha256').write_text('authored primitive control, not native classes\n');(attempt/'before-own-prepare.properties').write_text('authored primitive binding control, not Native acceptance\n')
  meta={'schema':'light-queue-proof-v1','profile':'light-queue-proof-v1-required29','nonce':attempt.name,'attemptDirectory':str(attempt),'sdkSha256':q.FORGE_SHA,'sourceDirty':'false','sourceCommit':'a'*40,'globalDrainProven':'false','incomplete':'true','globalCoverageIncomplete':'true','observations':'6'}
  for key,name in [('classHashSetSha256','light-queue-proof-classes.sha256'),('nativeInputReceiptSha256','before-own-prepare.properties')]:meta[key]=hashlib.sha256((attempt/name).read_bytes()).hexdigest()
  header='\n'.join(k+'='+v for k,v in meta.items()).encode();records=[]
  for slot in range(6):records.extend([(1,slot,slot+1,0,1,0,0,1),(13,slot,slot+1,0,0,0,0,1)])
  payload=struct.pack('>QI',0x5746545150523031,len(header))+header+struct.pack('>I',len(records))+b''.join(struct.pack('>i8q',i+1,*r)for i,r in enumerate(records));file.write_bytes(payload);digest=hashlib.sha256(payload).hexdigest()
  rows=['LIGHT_QUEUE_PROOF_OBSERVATION case='+c+' ordinal='+str(n)+' schema=light-queue-proof-v1 structural=PASS inference=INCONCLUSIVE_NO_GLOBAL_ORDERING_PROOF'for c in ('SAME_CHUNK_STABLE_PRIORITY','CROSS_CHUNK_MIXED_PRIORITY','NEIGHBOR_PROPAGATION')for n in range(2)]
  cases=['LIGHT_QUEUE_PROOF_CASE case='+c+' observations=2 required=true'for c in ('SAME_CHUNK_STABLE_PRIORITY','CROSS_CHUNK_MIXED_PRIORITY','NEIGHBOR_PROPAGATION')]
  receipt='LIGHT_QUEUE_PROOF_RECEIPT path='+str(file)+' sha256='+digest+' records=12 incomplete=true inference=INCONCLUSIVE_NO_GLOBAL_ORDERING_PROOF'
  text='\n'.join(rows+cases+[receipt,'All 29 required tests passed'])
  q.check_log(text)
  for bad in (text.replace('All 29','All 26'),text.replace('All 29','All 3'),text+'\n'+rows[0],text.replace(rows[0],''),text+'\n[00:00:01] [Server thread/ERROR] [test/]: Native original failure',text.replace(digest,'0'*64),text.replace('ordinal=1','ordinal=2'),text.replace('structural=PASS','structural=TIMEOUT'),text+'\n'+receipt):
   with self.assertRaises(AssertionError):q.check_log(bad)

if __name__=='__main__':unittest.main()
