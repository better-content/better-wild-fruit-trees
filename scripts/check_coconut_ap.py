"""Pure JDK + pinned Mixin AP controls using authored ABI stubs, NOT native acceptance."""
from pathlib import Path
import json
import subprocess
import sys
import uuid
from coconut_mapping import INTERFACE, METHODS, MIXIN, TARGET, check_mapping, class_info

ROOT = Path(__file__).resolve().parents[1]


def run(processor):
    out = ROOT / 'build/source-coconut-ap-controls' / str(uuid.uuid4())
    out.mkdir(parents=True)
    source = (ROOT / ('src/main/java/' + MIXIN + '.java')).read_text()
    # Stub only the pinned signatures/hierarchy. No Minecraft/Forge runtime or optional implementation.
    types = ['net.minecraft.core.BlockPos', 'net.minecraft.server.level.ServerLevel',
             'net.minecraft.util.RandomSource', 'net.minecraft.world.level.LevelReader',
             'net.minecraft.world.level.block.state.BlockState']
    files = []
    def write(name, text):
        p = out / ('src/' + name.replace('.', '/') + '.java')
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(text)
        files.append(p)
    for name in types:
        package, cls = name.rsplit('.', 1)
        write(name, f'package {package}; public class {cls} {{}}')
    valid = 'boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader l, net.minecraft.core.BlockPos p, net.minecraft.world.level.block.state.BlockState s, boolean c)'
    perform = 'void performBonemeal(net.minecraft.server.level.ServerLevel l, net.minecraft.util.RandomSource r, net.minecraft.core.BlockPos p, net.minecraft.world.level.block.state.BlockState s)'
    write(INTERFACE.replace('/', '.'), 'package net.minecraft.world.level.block; public interface BonemealableBlock {' + valid + ';' + perform + ';}')
    write(TARGET.replace('/', '.'), 'package net.hibiscus.naturespirit.blocks; public class CoconutBlock implements net.minecraft.world.level.block.BonemealableBlock { public ' + valid + ' {return true;} public ' + perform + ' {} }')
    mappings = out / 'input.srg'
    mappings.write_text('\n'.join(f'MD: {INTERFACE}/{name} {desc} {INTERFACE}/{srg} {desc}' for name, (srg, desc) in METHODS.items()) + '\n')
    mixin_source = out / ('src/' + MIXIN + '.java')
    for mode in ('typed', 'pseudo'):
        dest = out / mode
        dest.mkdir()
        text = source if mode == 'typed' else source.replace('@Mixin(CoconutBlock.class)', '@Pseudo\n@Mixin(targets = "' + TARGET.replace('/', '.') + '")')
        mixin_source.parent.mkdir(parents=True, exist_ok=True)
        mixin_source.write_text(text)
        refmap = dest / 'refmap.json'
        command = ['javac', '-cp', str(processor), '-processorpath', str(processor),
                   '-processor', 'org.spongepowered.tools.obfuscation.MixinObfuscationProcessorInjection',
                   '-AdisableTargetValidator=true', '-AreobfSrgFile=' + str(mappings),
                   '-AoutRefMapFile=' + str(refmap), '-d', str(dest), *map(str, files), str(mixin_source)]
        result = subprocess.run(command, capture_output=True, text=True, timeout=30)
        (dest / 'javac.log').write_text(result.stdout + result.stderr)
        assert result.returncode == 0, result.stderr
        info = class_info((dest / (MIXIN + '.class')).read_bytes())
        mapping = json.loads(refmap.read_text())
        if mode == 'typed':
            check_mapping(mapping, info)
            for name in METHODS:
                bad = json.loads(json.dumps(mapping))
                del bad['mappings'][MIXIN][name]
                try: check_mapping(bad, info)
                except AssertionError: pass
                else: raise AssertionError('Missing mapping accepted')
        else:
            assert 'will not be obfuscated' in result.stderr, 'Pseudo failure not reproduced'
            try: check_mapping(mapping, info)
            except AssertionError: pass
            else: raise AssertionError('Pseudo regression accepted')
    print('Retained pure Mixin AP typed/pseudo controls:', out)


if __name__ == '__main__':
    run(Path(sys.argv[1]))
