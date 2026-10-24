"""Pinned bytecode oracle + authored-stub pure JDK/AP controls; never starts Minecraft."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import sys
import uuid
import zipfile
from coconut_mapping import class_info

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'com/bettercontent/betterwildfruittrees/gametest/nativeprobe/'
FORGE_SHA = '86ce0a0aa09201b566430dfec17f7a03fd2acbd16e04b17992b6ad5e78df4b0f'
DT_SHA = 'ca21e8d1796bdd66b4fad163822761492262354181788b31783497b0ae322315'
LEVEL = 'net/minecraft/world/level/'
BLOCKPOS = '(Lnet/minecraft/core/BlockPos;)Z'
BRIGHT = '(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I'
WRITE = '(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;'
MAP = [
    (LEVEL+'LevelReader/canSeeSkyFromBelowWater', BLOCKPOS, LEVEL+'LevelReader/m_46861_'),
    (LEVEL+'BlockAndTintGetter/getBrightness', BRIGHT, LEVEL+'BlockAndTintGetter/m_45517_'),
    (LEVEL+'lighting/SkyLightSectionStorage/getLightValue', '(J)I', LEVEL+'lighting/SkyLightSectionStorage/m_6181_'),
    (LEVEL+'lighting/LayerLightSectionStorage/swapSectionMap', '()V', LEVEL+'lighting/LayerLightSectionStorage/m_75790_'),
    (LEVEL+'chunk/LevelChunk/setBlockState', WRITE, LEVEL+'chunk/LevelChunk/m_6978_'),
]
FIELD = LEVEL+'lighting/LayerLightSectionStorage/visibleSectionData'
FIELD_SRG = LEVEL+'lighting/LayerLightSectionStorage/f_75731_'


def native_abi(forge, dt, mcp):
    for path, expected in ((forge, FORGE_SHA), (dt, DT_SHA)):
        assert hashlib.sha256(Path(path).read_bytes()).hexdigest() == expected, 'Pinned native light probe ABI archive drift'
    def method(jar, cls, signature):
        text = subprocess.check_output(['javap', '-p', '-c', '-classpath', str(jar), cls], text=True)
        return next(block for block in re.split(r'(?m)^  (?=\S)', text) if signature in block)
    adequate = method(dt, 'com.ferreusveritas.dynamictrees.block.leaves.DynamicLeavesBlock', 'boolean hasAdequateLight(')
    for name, descriptor in [('canSeeSkyFromBelowWater', BLOCKPOS), ('getBrightness', BRIGHT)]:
        needle = 'InterfaceMethod ' + LEVEL + 'LevelAccessor.' + name + ':' + descriptor
        assert adequate.count(needle) == 1, 'Original light read owner/descriptor/count drift'
    assert adequate.count('ireturn') == 3, 'Adequate-light return site drift'
    publication = method(forge, 'net.minecraft.world.level.lighting.LayerLightSectionStorage', 'void swapSectionMap()')
    writes = [line for line in publication.splitlines() if 'putfield' in line]
    assert len(writes) == 1 and 'Field visibleSectionData:Lnet/minecraft/world/level/lighting/DataLayerStorageMap;' in writes[0], 'Publication field write drift'
    sky = method(forge, 'net.minecraft.world.level.lighting.SkyLightSectionStorage', 'int getLightValue(long);')
    assert sky.count('ireturn') == 1 and 'Method getLightValue:(JZ)I' in sky, 'Skylight entrypoint drift'
    chunk = method(forge, 'net.minecraft.world.level.chunk.LevelChunk', 'BlockState setBlockState(')
    assert chunk.count('areturn') == 4, 'Original chunk write return site drift'
    assert chunk.count('Code:\n       0:') == 1, 'Original chunk write entrypoint drift'
    mapping = Path(mcp).read_text()
    for name, descriptor, srg in MAP:
        assert f'MD: {srg} {descriptor} {name} {descriptor}' in mapping, 'Pinned MCP/SRG method drift'
    assert f'FD: {FIELD_SRG} {FIELD}' in mapping, 'Pinned publication field mapping drift'
    with zipfile.ZipFile(forge) as archive:
        for owner, name, descriptor in (
            (LEVEL+'block/Block', 'getStateDefinition', '()Lnet/minecraft/world/level/block/state/StateDefinition;'),
            (LEVEL+'block/state/StateDefinition', 'getPossibleStates', '()Lcom/google/common/collect/ImmutableList;'),
            (LEVEL+'block/state/StateHolder', 'getValues', '()Lcom/google/common/collect/ImmutableMap;'),
            (LEVEL+'block/state/properties/Property', 'getName', '()Ljava/lang/String;'),
            (LEVEL+'block/state/properties/Property', 'getName', '(Ljava/lang/Comparable;)Ljava/lang/String;')):
            declarations = class_info(archive.read(owner+'.class'))['methods']
            assert any(n == name and d == descriptor for n, d, _, _ in declarations), 'Native immutable-state bootstrap API drift'
    startup = method(forge, 'net.minecraft.gametest.framework.GameTestServer', 'boolean initServer()')
    assert startup.index('Method loadLevel:()V') < startup.index('ServerLifecycleHooks.handleServerStarting:'), 'Native codebook startup ordering drift'
    tick = method(forge, 'net.minecraft.gametest.framework.GameTestServer', 'void tickServer(')
    assert 'Method startTests:' in tick, 'Native test startup moved'
    print('Pinned original read/return/PUTFIELD/descriptor/SRG/startup ABI controls passed')


def check_required_annotations(classes):
    counts = {'LeavesLightTraceMixin': {'trace$canSee': 1, 'trace$brightness': 1, 'trace$adequate': 3},
              'SkyStorageTraceMixin': {'trace$enter': 1, 'trace$return': 1},
              'LightPublicationTraceMixin': {'trace$published': 1}, 'ChunkWriteTraceMixin': {'trace$writeHead': 1, 'trace$write': 4}}
    for cls, handlers in counts.items():
        info = class_info((Path(classes)/(PACKAGE+cls+'.class')).read_bytes())
        assert info['owner'] == PACKAGE + cls, 'Wrong observation class'
        targets = {'LeavesLightTraceMixin': 'com/ferreusveritas/dynamictrees/block/leaves/DynamicLeavesBlock',
                   'SkyStorageTraceMixin': LEVEL+'lighting/SkyLightSectionStorage',
                   'LightPublicationTraceMixin': LEVEL+'lighting/LayerLightSectionStorage',
                   'ChunkWriteTraceMixin': LEVEL+'chunk/LevelChunk'}
        assert info['annotations']['Lorg/spongepowered/asm/mixin/Mixin;']['value'] == ['L'+targets[cls]+';'], 'Wrong observation target'
        found = {}
        for name, _, _, annotations in info['methods']:
            for annotation, values in annotations.items():
                if annotation not in ('Lorg/spongepowered/asm/mixin/injection/Inject;', 'Lorg/spongepowered/asm/mixin/injection/Redirect;'): continue
                assert name in handlers and name not in found, 'Unknown/duplicate observation handler'
                n = handlers[name]
                assert all(values.get(key) == n for key in ('require', 'expect', 'allow')), 'Weakened native required-apply count'
                assert not values.get('cancellable', False), 'Observation must never cancel'
                found[name] = values
        assert set(found) == set(handlers), 'Missing required observation hook'
        if cls == 'ChunkWriteTraceMixin':
            assert found['trace$writeHead']['at'][0][1]['value'] == 'HEAD', 'Write request must precede original mutation/nested calls'
            assert found['trace$write']['at'][0][1]['value'] == 'RETURN', 'Preserve four original write RETURN sites'
        if cls == 'LightPublicationTraceMixin':
            at = found['trace$published']['at'][0][1]
            assert at['value'] == 'FIELD' and at['opcode'] == 181, 'Not observing original PUTFIELD'
            assert at['shift'][1] == 'AFTER', 'Publication observation must follow original write'


def check_reserved_sources(sources, config):
    reserved = config['package']
    allowed = {reserved+'.'+name for name in config['mixins']}
    for path, text in sources.items():
        package = re.search(r'(?m)^package\s+([\w.]+)\s*;', text)
        assert package, 'Missing explicit Java package: '+str(path)
        package = package.group(1)
        if package == reserved or package.startswith(reserved+'.'):
            assert package+'.'+Path(path).stem in allowed and re.search(r'@Mixin\s*\(', text), 'Ordinary/unconfigured class in reserved Mixin namespace: '+str(path)


def check_reserved_classes(infos, config):
    reserved = config['package'].replace('.', '/')
    allowed = {reserved+'/'+name.replace('.', '/') for name in config['mixins']}
    seen = set()
    for info in infos:
        owner = info['owner']
        if owner.startswith(reserved+'/'):
            assert owner in allowed and 'Lorg/spongepowered/asm/mixin/Mixin;' in info['annotations'], 'Ordinary/unconfigured compiled class in reserved Mixin namespace: '+owner
            seen.add(owner)
    assert seen == allowed, 'Missing configured native Mixin class'


def check_reserved_namespace(classes=None):
    config = json.loads((ROOT/'src/nativeProbe/resources/better_wild_fruit_trees.native-light.mixins.json').read_text())
    check_reserved_sources({path: path.read_text() for path in (ROOT/'src').rglob('*.java')}, config)
    if classes is not None:
        check_reserved_classes([class_info(path.read_bytes()) for path in Path(classes).rglob('*.class')], config)


def check_codebook_subscriber(classes):
    info = class_info((Path(classes)/'com/bettercontent/betterwildfruittrees/gametest/NativeStateCodes.class').read_bytes())
    subscriber = info['annotations'].get('Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber;', {})
    assert subscriber.get('modid') == 'better_wild_fruit_trees' and subscriber.get('bus', (None, None))[1] == 'FORGE', 'Missing native-only codebook Forge subscriber'
    assert any(name == 'starting' and descriptor == '(Lnet/minecraftforge/event/server/ServerStartingEvent;)V'
               and access & 9 == 9 and 'Lnet/minecraftforge/eventbus/api/SubscribeEvent;' in annotations
               for name, descriptor, access, annotations in info['methods']), 'Missing startup-only codebook event handler'


def expected_refmap():
    owner = 'L'+LEVEL
    return {
        PACKAGE+'LeavesLightTraceMixin': {
            owner+'LevelAccessor;canSeeSkyFromBelowWater'+BLOCKPOS: owner+'LevelAccessor;m_46861_'+BLOCKPOS,
            owner+'LevelAccessor;getBrightness'+BRIGHT: owner+'LevelAccessor;m_45517_'+BRIGHT},
        PACKAGE+'SkyStorageTraceMixin': {'getLightValue(J)I': owner+'lighting/SkyLightSectionStorage;m_6181_(J)I'},
        PACKAGE+'LightPublicationTraceMixin': {
            'swapSectionMap()V': owner+'lighting/LayerLightSectionStorage;m_75790_()V',
            owner+'lighting/LayerLightSectionStorage;visibleSectionData:Lnet/minecraft/world/level/lighting/DataLayerStorageMap;':
                owner+'lighting/LayerLightSectionStorage;f_75731_:Lnet/minecraft/world/level/lighting/DataLayerStorageMap;'},
        PACKAGE+'ChunkWriteTraceMixin': {'setBlockState'+WRITE: owner+'chunk/LevelChunk;m_6978_'+WRITE}}


def check_refmap(path):
    refmap = json.loads(Path(path).read_text())
    expected = expected_refmap()
    assert refmap['mappings'] == expected, 'Wrong/missing native probe SRG mapping'
    assert refmap['data'] == {'searge': expected}, 'Native probe mapping namespace disagreement'


def controls(processor=None):
    out = ROOT/'build/source-native-light-controls'/str(uuid.uuid4())
    out.mkdir(parents=True)
    sources = []
    def write(name, text):
        p = out/'src'/(name.replace('.', '/')+'.java')
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text('package '+name.rsplit('.', 1)[0]+'; '+text)
        sources.append(p)
    write('net.minecraft.core.BlockPos', 'public class BlockPos { public int encodes; public long asLong() { encodes++; return 123L; } }')
    write('net.minecraft.world.level.block.state.properties.Property', 'public class Property<T extends Comparable<T>> { public String getName(){return "age";} public String getName(T value){return value.toString();} }')
    write('net.minecraft.world.level.block.state.BlockState', 'public class BlockState { public static int propertyReads; public java.util.Map<net.minecraft.world.level.block.state.properties.Property<?>,Comparable<?>> getValues(){propertyReads++; return java.util.Map.of();} }')
    write('net.minecraft.world.level.block.state.StateDefinition', 'public class StateDefinition { private final java.util.List<BlockState> states; public StateDefinition(BlockState state){states=java.util.List.of(state);} public java.util.List<BlockState> getPossibleStates(){return states;} }')
    write('net.minecraft.world.level.block.Block', 'public class Block { public final String name; public boolean failOnDefinition; private final net.minecraft.world.level.block.state.StateDefinition states; public Block(String n, net.minecraft.world.level.block.state.BlockState s){name=n;states=new net.minecraft.world.level.block.state.StateDefinition(s);} public net.minecraft.world.level.block.state.StateDefinition getStateDefinition(){if(failOnDefinition) throw new AssertionError("excluded state palette enumerated"); return states;} }')
    write('net.minecraft.core.registries.BuiltInRegistries', 'public class BuiltInRegistries { public static final Registry BLOCK = new Registry(); public static class Registry implements Iterable<net.minecraft.world.level.block.Block> { public int lookups; private final java.util.List<net.minecraft.world.level.block.Block> blocks=new java.util.ArrayList<>(); public void add(net.minecraft.world.level.block.Block b){blocks.add(b);} public String getKey(net.minecraft.world.level.block.Block b){lookups++; return b.name;} public java.util.Iterator<net.minecraft.world.level.block.Block> iterator(){lookups++; return blocks.iterator();} } }')
    write('net.minecraftforge.event.server.ServerStartingEvent', 'public class ServerStartingEvent {}')
    write('net.minecraftforge.eventbus.api.SubscribeEvent', 'public @interface SubscribeEvent {}')
    write('net.minecraftforge.fml.common.Mod', 'public @interface Mod { public @interface EventBusSubscriber {String modid(); Bus bus(); public enum Bus {FORGE, MOD} } }')
    write('net.minecraft.world.level.LightLayer', 'public enum LightLayer { SKY, BLOCK }')
    write('net.minecraft.world.level.LevelReader', 'public interface LevelReader { boolean canSeeSkyFromBelowWater(net.minecraft.core.BlockPos p); }')
    write('net.minecraft.world.level.BlockAndTintGetter', 'public interface BlockAndTintGetter { int getBrightness(LightLayer l, net.minecraft.core.BlockPos p); }')
    write('net.minecraft.world.level.LevelAccessor', 'public interface LevelAccessor extends LevelReader, BlockAndTintGetter {}')
    write('com.ferreusveritas.dynamictrees.block.leaves.LeavesProperties', 'public class LeavesProperties {}')
    write('com.ferreusveritas.dynamictrees.block.leaves.DynamicLeavesBlock', 'public class DynamicLeavesBlock { public boolean hasAdequateLight(net.minecraft.world.level.block.state.BlockState s, net.minecraft.world.level.LevelAccessor l, LeavesProperties f, net.minecraft.core.BlockPos p) { return l.canSeeSkyFromBelowWater(p) || l.getBrightness(net.minecraft.world.level.LightLayer.SKY,p)>0; } }')
    write('net.minecraft.world.level.lighting.DataLayerStorageMap', 'public class DataLayerStorageMap {}')
    write('net.minecraft.world.level.lighting.LayerLightSectionStorage', 'public class LayerLightSectionStorage { protected volatile DataLayerStorageMap visibleSectionData; protected void swapSectionMap() {visibleSectionData = new DataLayerStorageMap();} }')
    write('net.minecraft.world.level.lighting.SkyLightSectionStorage', 'public class SkyLightSectionStorage extends LayerLightSectionStorage { protected int getLightValue(long p) {return 15;} }')
    write('net.minecraft.world.level.chunk.LevelChunk', 'public class LevelChunk { public net.minecraft.world.level.block.state.BlockState setBlockState(net.minecraft.core.BlockPos p, net.minecraft.world.level.block.state.BlockState s, boolean b) {return s;} }')
    write('org.objectweb.asm.Opcodes', 'public interface Opcodes { int PUTFIELD=181; }')
    if processor is None:
        write('org.spongepowered.asm.mixin.Mixin', 'public @interface Mixin { Class<?>[] value(); }')
        write('org.spongepowered.asm.mixin.injection.At', 'public @interface At {String value(); String target() default ""; boolean remap() default true; int opcode() default -1; Shift shift() default Shift.NONE; enum Shift {NONE,BEFORE,AFTER} }')
        for name in ('Inject', 'Redirect'):
            write('org.spongepowered.asm.mixin.injection.'+name, 'public @interface '+name+' { String[] method(); At[] at(); int require(); int expect() default 1; int allow() default -1; boolean remap() default true; }')
        write('org.spongepowered.asm.mixin.injection.callback.CallbackInfo', 'public class CallbackInfo {}')
        write('org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable', 'public class CallbackInfoReturnable<T> extends CallbackInfo {private final T value; public CallbackInfoReturnable(String name, boolean cancellable, T v){value=v;} public T getReturnValue(){ return value; } }')
    sources += sorted((ROOT/'src/nativeProbe/java').rglob('*.java'))
    sources += [ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/OriginalTraceProfile.java', ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/OriginalLightTrace.java', ROOT/'src/main/java/com/bettercontent/betterwildfruittrees/gametest/OrderedEvents.java', ROOT/'scripts/NativeLightTraceSelfTest.java', ROOT/'scripts/StateCodebookSelfTest.java', ROOT/'scripts/KiwiStateSelectionSelfTest.java']
    classes = out/'classes'
    classes.mkdir()
    command = ['javac', '--release', '17', '-d', str(classes)]
    if processor:
        mapping = out/'input.srg'
        mapping.write_text('\n'.join(f'MD: {name} {desc} {srg} {desc}' for name, desc, srg in MAP)+'\n'+f'FD: {FIELD} {FIELD_SRG}\n')
        command += ['-cp', str(processor), '-processorpath', str(processor), '-processor', 'org.spongepowered.tools.obfuscation.MixinObfuscationProcessorInjection',
                    '-AreobfSrgFile='+str(mapping), '-AoutRefMapFile='+str(out/'refmap.json')]
    else:
        command += ['-proc:none']
    result = subprocess.run(command+list(map(str, sources)), capture_output=True, text=True, timeout=30)
    (out/'javac.log').write_text(result.stdout+result.stderr)
    assert result.returncode == 0, result.stderr
    check_required_annotations(classes)
    check_reserved_namespace(classes)
    check_codebook_subscriber(classes)
    runtime = str(classes) + (':'+str(processor) if processor else '')
    for name in ('NativeLightTraceSelfTest', 'com.bettercontent.betterwildfruittrees.gametest.StateCodebookSelfTest', 'com.bettercontent.betterwildfruittrees.gametest.KiwiStateSelectionSelfTest'):
        result = subprocess.run(['java', '-cp', runtime, name, str(out)], capture_output=True, text=True, timeout=15)
        (out/(name.rsplit('.', 1)[-1]+'.log')).write_text(result.stdout+result.stderr)
        assert result.returncode == 0, result.stderr
    if processor:
        check_refmap(out/'refmap.json')
    print('Retained pure native light controls:', out)
    return out


if __name__ == '__main__':
    assert len(sys.argv) in (5, 7), 'Expected FORGE_MAPPED DT_MAPPED SRG_TO_MCP MIXIN_PROCESSOR [COMPILED_REFMAP COMPILED_CLASSES]'
    native_abi(*sys.argv[1:4])
    controls(Path(sys.argv[4]))
    if len(sys.argv) == 7:
        check_refmap(sys.argv[5])
        check_required_annotations(sys.argv[6])
        check_reserved_namespace(sys.argv[6])
        check_codebook_subscriber(sys.argv[6])
        print('Actual compiled nonshipping native probe annotations/refmap verified (not native application acceptance)')
