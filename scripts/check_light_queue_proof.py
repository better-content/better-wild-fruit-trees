"""SOURCE mechanism controls. Actual pinned SDK/AP only, never Minecraft/Gradle/cache writes."""
from pathlib import Path
import hashlib,json,re,subprocess,sys,uuid,zipfile,struct
from verify_artifact import classify_log
from coconut_mapping import class_info
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'src/lightQueueProof'
PKG='com/bettercontent/betterwildfruittrees/gametest/queueproof/mixin/'
FORGE_SHA='86ce0a0aa09201b566430dfec17f7a03fd2acbd16e04b17992b6ad5e78df4b0f'
HANDLERS={
 'ThreadedPipelineMixin':{'proof$issue':1,'proof$issued':1,'proof$staged':1,'proof$batch':1,'proof$solverBefore':1,'proof$solverAfter':1,'proof$task':2,'proof$retire':1,'proof$runEnd':1,'proof$send':1},
 'SorterAdmissionMixin':{'proof$sent':1,'proof$begin':1,'proof$priority':1,'proof$end':1},
 'PublicationMixin':{'proof$publication':1},'ChunkLifecycleMixin':{'proof$correct':1,'proof$unsaved':1},
 'TicketRemovalMixin':{'proof$removed':2},'TicketCallbackMixin':{'proof$sent':1},'MainCallbackMixin':{'proof$begin':1,'proof$end':1},'NotificationMixin':{'proof$sent':1},'PreparationInfoMixin':{'proof$scopeSpawn':1,'proof$scopeClear':1,'proof$clear':1},'PreparationSpawnMixin':{'proof$scope':1,'proof$clear':1}}

def check_sources(base=BASE):
 cfg=json.loads((base/'resources/better_wild_fruit_trees.light-queue-proof.mixins.json').read_text())
 assert cfg['required'] is True and cfg['injectors']['defaultRequire']==1
 assert cfg['package'].replace('.','/')==PKG.rstrip('/')
 sources=list((base/'java').rglob('*.java'))
 reserved={p.stem for p in sources if '/mixin/' in str(p)}
 assert reserved==set(cfg['mixins'])==set(HANDLERS)|{'MessageAccess','TicketAccess','PreparationCacheAccess','PreparationMapAccess'},'Reserved package pollution'
 for p in sources:
  t=p.read_text();assert '@Pseudo'not in t and 'java.lang.reflect'not in t and 'Class.forName'not in t
  if p.parent.name=='mixin':
   assert 'new 'not in t and 'System.out'not in t and 'getAsInt();'not in t.replace('original.getAsInt();',''), 'Hot allocation/I/O/requery'
 expected=[]
 for p in (ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest').glob('*.java'):
  t=p.read_text()
  if '@GameTestHolder(WildFruitTrees.MOD_ID)' in t:
   assert '@PrefixGameTestTemplate(false)' in t,'Native registry identity prefix drift'
   expected +=[n.lower()for n in re.findall(r'@GameTest\([^)]*\)\s+public static void (\w+)\(',t)]
 assert len(expected)==26,'Original mandatory inventory changed'
 expected+=['samechunkstablepriority','crosschunkmixedpriority','neighborpropagation']
 assert len(set(expected))==29,'Duplicate native registry name'
 assert (base/'resources/light-queue-proof-required-tests.txt').read_text().splitlines()==sorted(expected),'Original26/proof3 manifest drift'
 fixture=(base/'java/com/bettercontent/betterwildfruittrees/gametest/queueproof/LightQueueProofGameTests.java').read_text()
 assert fixture.count('@GameTest(')==3 and 'ordinal==0' in fixture and 'which*2+ordinal'in fixture
 assert '.join('not in fixture and '.managedBlock('not in fixture and '.runLightUpdates('not in fixture
 assert 'getChunkNow' in fixture and 'getTick()+100' in fixture and 'future.getNow(null)' in fixture

def check_classes(classes):
 for cls,handlers in HANDLERS.items():
  info=class_info((Path(classes)/(PKG+cls+'.class')).read_bytes());found={}
  for name,desc,_,annotations in info['methods']:
   for typ,values in annotations.items():
    if typ not in ('Lorg/spongepowered/asm/mixin/injection/Inject;','Lorg/spongepowered/asm/mixin/injection/Redirect;','Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;'):continue
    assert name in handlers and name not in found
    assert all(values.get(k)==handlers[name]for k in ('require','expect','allow')),'Weakened required anchor'
    assert values.get('remap',True)==(cls not in ('PreparationInfoMixin','PreparationSpawnMixin')) and not values.get('cancellable',False),'Unmapped/cancellable method'
    found[name]=values
  assert set(found)==set(handlers),'Missing handler'

def abi(forge,out):
 assert hashlib.sha256(Path(forge).read_bytes()).hexdigest()==FORGE_SHA,'SDK pin drift'
 texts={}
 for cls in ('ThreadedLevelLightEngine','ChunkTaskPriorityQueueSorter','DistanceManager','ChunkMap','ServerChunkCache','ServerChunkCache$MainThreadExecutor'):
  text=subprocess.check_output(['javap','-p','-c','-classpath',str(forge),'net.minecraft.server.level.'+cls],text=True);texts[cls]=text;(out/(cls+'.javap.txt')).write_text(text)
 with zipfile.ZipFile(forge) as archive:
  for owner,methods in {
   'net/minecraft/gametest/framework/TestFunction': [('getTestName','()Ljava/lang/String;'),('getStructureName','()Ljava/lang/String;'),('isRequired','()Z'),('getMaxAttempts','()I'),('getRequiredSuccesses','()I')],
   'net/minecraft/gametest/framework/GameTestRegistry': [('getAllTestFunctions','()Ljava/util/Collection;')],
   'net/minecraft/resources/ResourceLocation': [('getNamespace','()Ljava/lang/String;'),('tryParse','(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;')]
  }.items():
   declarations=class_info(archive.read(owner+'.class'))['methods']
   assert all(any(n==name and d==desc for n,d,_,_ in declarations) for name,desc in methods),'Actual registry ownership API drift'
 def method(cls,signature):return next(b for b in re.split(r'(?m)^  (?=\S)',texts[cls])if signature in b)
 run=method('ThreadedLevelLightEngine','void runUpdate()')
 for needle,n in [('Method java/lang/Math.min:(II)I',1),('InterfaceMethod java/lang/Runnable.run:()V',2),('Method net/minecraft/world/level/lighting/LevelLightEngine.runLightUpdates:()I',1),('InterfaceMethod it/unimi/dsi/fastutil/objects/ObjectListIterator.remove:()V',1)]:assert run.count(needle)==n,'runUpdate anchor drift '+needle
 stage=method('ThreadedLevelLightEngine','lambda$addTask$15(');assert stage.count('InterfaceMethod it/unimi/dsi/fastutil/objects/ObjectList.add:(Ljava/lang/Object;)Z')==1
 admission=method('ChunkTaskPriorityQueueSorter','lambda$submit$12(');assert admission.count('InterfaceMethod java/util/function/IntSupplier.getAsInt:()I')==1
 removal=method('DistanceManager','void removeTicket(long,');assert removal.count('Method net/minecraft/util/SortedArraySet.remove:(Ljava/lang/Object;)Z')==2
 # Both true/false arms have real lifecycle/ticket effects; none is a drain.
 light=method('ThreadedLevelLightEngine','lightChunk(');assert 'CompletableFuture' in light
 prep={}
 for cls in ('GameTestInfo','StructureUtils'):
  prep[cls]=subprocess.check_output(['javap','-p','-c','-classpath',str(forge),'net.minecraft.gametest.framework.'+cls],text=True);(out/(cls+'.javap.txt')).write_text(prep[cls])
 def pm(cls,signature):return next(b for b in re.split(r'(?m)^  (?=\S)',prep[cls])if signature in b)
 clear='Method net/minecraft/gametest/framework/StructureUtils.clearSpaceForStructure:'
 assert pm('GameTestInfo','void spawnStructure(').count('Method net/minecraft/gametest/framework/StructureUtils.spawnStructure:')==1
 assert pm('GameTestInfo','void clearStructure()').count(clear)==1
 spawn=pm('StructureUtils','StructureBlockEntity spawnStructure(')
 assert spawn.count('Method clearSpaceForStructure:')==1 and spawn.index('Method forceLoadChunks:')<spawn.index('Method clearSpaceForStructure:')<spawn.index('Method createStructureBlock:')
 clearbody=pm('StructureUtils','void clearSpaceForStructure(')
 for needle in ('Method net/minecraft/core/BlockPos.betweenClosedStream:','bipush        20','iconst_2','iconst_3'):assert needle in clearbody,'Native clear inflation drift'
 return texts

def sdk(forge,processor,srg,built_map=None,built_classes=None):
 check_sources();out=ROOT/'build/source-queue-proof-controls'/('sdk-'+str(uuid.uuid4()));out.mkdir(parents=True);(out/'classes').mkdir()
 abi(forge,out)
 lines=[]
 for l in Path(srg).read_text().splitlines():
  p=l.split()
  if p and p[0]=='MD:':lines.append(' '.join([p[0],p[3],p[4],p[1],p[2]]))
  elif p and p[0]in ('FD:','CL:'):lines.append(' '.join([p[0],p[2],p[1]]))
 (out/'input.srg').write_text('\n'.join(lines)+'\n')
 extras=list(Path('/home/dev/.gradle/caches/modules-2/files-2.1/io.github.llamalad7/mixinextras-common/0.5.4').rglob('mixinextras-common-0.5.4.jar'));assert len(extras)==1
 assert hashlib.sha256(extras[0].read_bytes()).hexdigest()=='e2de9240ce39af783a83bcd315445b84d675c01e4655f3ad55c163071ce15fee','Preparation API pin drift'
 runtime=list(Path('/home/dev/.gradle/caches/modules-2/files-2.1/io.github.llamalad7/mixinextras-forge/0.5.4').rglob('mixinextras-forge-0.5.4.jar'));assert len(runtime)==1 and hashlib.sha256(runtime[0].read_bytes()).hexdigest()=='7922899a121a27f63a69a9ffe57470d8719cc52d239dfa9408e15d32a7b4c264','Preparation runtime pin drift'
 cp=[Path(forge),Path(processor),extras[0],ROOT/'build/classes/java/main']+sorted(p for p in Path('/home/dev/.gradle/caches/modules-2/files-2.1').rglob('*.jar')if not p.name.endswith(('-sources.jar','-javadoc.jar')) and p!=Path(processor))
 args=['--release','17','-d',str(out/'classes'),'-classpath',':'.join(map(str,cp)),'-processorpath',str(processor),'-processor','org.spongepowered.tools.obfuscation.MixinObfuscationProcessorTargets,org.spongepowered.tools.obfuscation.MixinObfuscationProcessorInjection','-AreobfSrgFile='+str(out/'input.srg'),'-AoutRefMapFile='+str(out/'refmap.json')]+list(map(str,sorted((BASE/'java').rglob('*.java'))))
 (out/'javac.args').write_text('\n'.join(json.dumps(s)for s in args)+'\n');result=subprocess.run(['javac','@'+str(out/'javac.args')],capture_output=True,text=True,timeout=60);(out/'javac.log').write_text(result.stdout+result.stderr)
 print('RETAINED',out,flush=True)
 assert result.returncode==0,result.stdout+result.stderr
 warnings=re.findall(r'warning: ([^\n]+)',result.stderr)
 assert len(warnings)==4 and all('Cannot find target method "lambda$' in w and any(n in w for n in ('lambda$submit$12','lambda$addTask$15')) for w in warnings),'Unexpected AP warning; only bytecode-proven synthetic javac-model omissions allowed'
 # javac deliberately hides synthetic members. Validate every selector directly in the pinned classfile,
 # rather than pretending AP TypeElement lookup resolved those four warnings.
 with zipfile.ZipFile(forge) as archive:
  for cls in HANDLERS:
   info=class_info((out/'classes'/(PKG+cls+'.class')).read_bytes());ann=info['annotations']['Lorg/spongepowered/asm/mixin/Mixin;'];owner=(ann.get('value') or ann.get('targets'))[0].removeprefix('L').removesuffix(';').replace('.','/')
   declarations=class_info(archive.read(owner+'.class'))['methods']
   for name,desc,code,annotations in info['methods']:
    for key,values in annotations.items():
     if key in ('Lorg/spongepowered/asm/mixin/injection/Inject;','Lorg/spongepowered/asm/mixin/injection/Redirect;','Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;'):
      for selector in values['method']:
       selected=[(n,d)for n,d,_,_ in declarations if n==selector.split('(')[0] and ('('not in selector or d==selector[selector.index('('):])]
       assert len(selected)==1,'Pinned classfile selector not unique/present: '+selector
 check_classes(out/'classes');mapping=json.loads((out/'refmap.json').read_text())['mappings']
 assert any('m_9317_'in v for m in mapping.values()for v in m.values()),'Missing real private enum mapping'
 assert any('m_284002_'in v for m in mapping.values()for v in m.values()),'Missing real stage mapping'
 assert any('m_140595_'in v for m in mapping.values()for v in m.values()),'Missing real admission mapping'
 if built_map is not None:assert json.loads(Path(built_map).read_text())['mappings']==mapping,'Actual Gradle refmap differs from actual SDK AP control'
 if built_classes is not None:check_classes(built_classes)
 (out/'result.json').write_text(json.dumps({'actualSDK':FORGE_SHA,'actualAP':hashlib.sha256(Path(processor).read_bytes()).hexdigest(),'exit':0,'nativeApplication':'PENDING','globalDrain':'NOT_PROVEN','classAnnotations':'required exact counts','javacSyntheticWarnings':warnings,'syntheticAuthority':'actual pinned classfile selectors plus SRG mapping; not javac TypeElement'},indent=2)+'\n')
 print('Actual SDK/AP/required annotation queue controls passed')

def read_payload(file,expected_records):
 path=Path(file).absolute();assert not any(p.is_symlink() for p in (path,*path.parents)),'Symlink payload ancestry'
 assert path.name=='pipeline-v1.wftq' and path.parent.name=='light-queue-proof','Payload path/schema'
 attempt=path.parent.parent;assert str(uuid.UUID(attempt.name))==attempt.name,'Noncanonical nonce'
 assert path.stat().st_size<=20*1024*1024,'Payload bound'
 raw=path.read_bytes();magic,size=struct.unpack_from('>QI',raw);assert magic==0x5746545150523031 and 0<size<=65536
 meta={line.split('=',1)[0]:line.split('=',1)[1]for line in raw[12:12+size].decode('iso8859-1').splitlines()if '='in line and not line.startswith('#')}
 assert meta.get('schema')=='light-queue-proof-v1' and meta.get('profile')=='light-queue-proof-v1-required29'
 assert meta.get('nonce')==attempt.name and meta.get('attemptDirectory')==str(attempt)
 assert meta.get('sdkSha256')==FORGE_SHA and meta.get('sourceDirty')=='false' and re.fullmatch('[a-f0-9]{40}',meta.get('sourceCommit',''))
 assert meta.get('fixturePreparation')=='exact3-native-ticking-v1' and meta.get('preparationSdkSha256')=='e2de9240ce39af783a83bcd315445b84d675c01e4655f3ad55c163071ce15fee' and meta.get('preparationRuntimeSha256')=='7922899a121a27f63a69a9ffe57470d8719cc52d239dfa9408e15d32a7b4c264','Preparation binding mismatch'
 assert meta.get('globalDrainProven')=='false' and meta.get('incomplete')=='true' and meta.get('globalCoverageIncomplete')=='true'
 for key,name in [('classHashSetSha256','light-queue-proof-classes.sha256'),('nativeInputReceiptSha256','before-own-prepare.properties')]:assert meta.get(key)==hashlib.sha256((attempt/name).read_bytes()).hexdigest(),'Binding receipt mismatch '+key
 count=struct.unpack_from('>I',raw,12+size)[0];assert count==int(expected_records) and 0<count<=262144
 offset=16+size;assert len(raw)==offset+count*68,'Truncated/extra record bytes'
 issues={};acks={}
 for i in range(count):
  commit,*r=struct.unpack_from('>i8q',raw,offset+i*68);assert commit==i+1,'Uncommitted reservation'
  kind,slot,identity,pos,a,b,c,thread=r
  if kind==1:assert identity>0 and identity not in issues;issues[identity]=(slot,a)
  if kind==13:assert 0<=slot<6 and slot not in acks;acks[slot]=identity
 assert set(acks)==set(range(6)) and meta.get('observations')=='6','Missing/duplicate body acknowledgements'
 for slot,identity in acks.items():assert issues.get(identity)==(slot,1),'Acknowledgement is not bound to unique issued native POST'
 return meta

def check_log(text):
 counts=re.findall(r'All (\d+) required tests passed',text)
 assert counts and counts[-1]=='29','Original26 + proof3 must all pass (29 only in queue profile)'
 assert not any(item['blocking']for item in classify_log(text)),'Unchanged native diagnostic failure classification blocks'
 rows=re.findall(r'LIGHT_QUEUE_PROOF_OBSERVATION case=(\S+) ordinal=(\d+) schema=(\S+) ([^\r\n]*)',text)
 expected={(c,str(n))for c in ('SAME_CHUNK_STABLE_PRIORITY','CROSS_CHUNK_MIXED_PRIORITY','NEIGHBOR_PROPAGATION')for n in range(2)}
 assert len(rows)==6 and {(c,n)for c,n,_,_ in rows}==expected,'Exactly preregistered six rows, no retries/duplicates'
 for c,n,schema,rest in rows:assert schema=='light-queue-proof-v1' and 'structural=PASS'in rest and 'inference=INCONCLUSIVE_NO_GLOBAL_ORDERING_PROOF'in rest
 receipts=re.findall(r'LIGHT_QUEUE_PROOF_RECEIPT path=(\S+) sha256=([a-f0-9]{64}) records=(\d+) incomplete=true inference=INCONCLUSIVE_NO_GLOBAL_ORDERING_PROOF',text)
 assert len(receipts)==1,'Exactly one retained mechanism receipt required (global coverage intentionally incomplete)'
 path,digest,count=receipts[0];assert hashlib.sha256(Path(path).read_bytes()).hexdigest()==digest,'Receipt content mismatch'
 assert 0<int(count)<=262144
 read_payload(path,count)
 assert len(re.findall(r'LIGHT_QUEUE_PROOF_CASE case=\S+ observations=2 required=true',text))==3

if __name__=='__main__':
 if sys.argv[1]=='sdk':sdk(*sys.argv[2:])
 elif sys.argv[1]=='sources':check_sources()
 elif sys.argv[1]=='log':check_log(Path(sys.argv[2]).read_text(errors='replace'))
 else:raise SystemExit('expected sdk/sources/log')
