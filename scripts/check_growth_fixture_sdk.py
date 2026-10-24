"""Pinned native v2 root-isolation/readiness ABI and actual AP/refmap/class controls."""
import hashlib,json
from pathlib import Path
import sys,zipfile
from coconut_mapping import class_info

ROOTY='com/ferreusveritas/dynamictrees/block/rooty/RootyBlock'
CHUNK='net/minecraft/server/level/ChunkMap'
RNG='net/minecraft/world/level/levelgen/LegacyRandomSource'
DESC='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V'
PINS={RNG:'015f279fdbb1a942b2d4bebfd1e74484f1369cb6bd3a9710fff479540362c430',ROOTY:'2e67e4eb4d0443f46ccfcd6ada944d94887c62dd5db3ccb4fce099f7f082f3a5',CHUNK:'0588dab076bd1558e48925d10fe499eedbb1fdd9e0c479d922daa31b32975f42',
      'net/minecraft/server/level/ChunkHolder':'379db8842e3d76df20399636436d389cb36086eac772e5a15655e5eb5d6d5852'}

def check_refmap(data):
    mappings=data['mappings'];assert data['data']['searge']==mappings
    prefix='com/bettercontent/betterwildfruittrees/gametest/growthv2/mixin/'
    assert set(mappings)=={prefix+'RootRandomTickIsolationMixin',prefix+'TickingHolderAccess',prefix+'LegacyRandomSeedAccess'}
    root=mappings[prefix+'RootRandomTickIsolationMixin'];holder=mappings[prefix+'TickingHolderAccess']
    assert root['randomTick'+DESC]=='L'+ROOTY+';m_213898_'+DESC
    # Invoker refmaps use an unqualified member descriptor; target owner is pinned
    # independently in the actual Mixin class annotation below (not guessed from text).
    assert holder['getVisibleChunkIfPresent']=='m_140327_(J)Lnet/minecraft/server/level/ChunkHolder;'
    assert mappings[prefix+'LegacyRandomSeedAccess']['seed']=='f_188575_:Ljava/util/concurrent/atomic/AtomicLong;'

def check_sdk(forge,dt,refmap,classes):
    for jar,owners in [(forge,[CHUNK,RNG,'net/minecraft/server/level/ChunkHolder']),(dt,[ROOTY])]:
        with zipfile.ZipFile(jar) as z:
            for owner in owners:
                raw=z.read(owner+'.class');assert hashlib.sha256(raw).hexdigest()==PINS[owner], 'Pinned native class drift '+owner
                methods=class_info(raw)['methods']
                if owner==ROOTY:assert any(x[0]=='randomTick' and x[1]==DESC and x[2]&1 for x in methods)
                if owner==CHUNK:assert any(x[0]=='getVisibleChunkIfPresent' and x[1]=='(J)Lnet/minecraft/server/level/ChunkHolder;' for x in methods)
                if owner.endswith('ChunkHolder'):assert any(x[0]=='getTickingChunkFuture' and x[1]=='()Ljava/util/concurrent/CompletableFuture;' and x[2]&1 for x in methods)
    check_refmap(json.loads(Path(refmap).read_text()))
    files=sorted(Path(classes).rglob('*.class'))
    assert {x.name for x in files}=={'RootRandomTickIsolationMixin.class','TickingHolderAccess.class','LegacyRandomSeedAccess.class'}
    for file in files:
        info=class_info(file.read_bytes())
        expected={'RootRandomTickIsolationMixin.class':ROOTY,'TickingHolderAccess.class':CHUNK,'LegacyRandomSeedAccess.class':RNG}[file.name]
        assert info['annotations']['Lorg/spongepowered/asm/mixin/Mixin;']['value']==['L'+expected+';']
    print('Pinned root isolation/TICKING ABI, actual helper classes and required AP/refmap PASS; not native delivery')

if __name__=='__main__':check_sdk(*sys.argv[1:])
