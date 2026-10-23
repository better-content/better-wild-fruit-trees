"""Pinned CoconutBlock ABI and production annotation/refmap contracts (no Minecraft launch)."""
import hashlib
import struct
import sys
import zipfile

TARGET = 'net/hibiscus/naturespirit/blocks/CoconutBlock'
MIXIN = 'com/bettercontent/betterwildfruittrees/mixin/CoconutFoodBlockMixin'
INTERFACE = 'net/minecraft/world/level/block/BonemealableBlock'
METHODS = {
    'isValidBonemealTarget': ('m_7370_', '(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z'),
    'performBonemeal': ('m_214148_', '(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V'),
}


class Reader:
    def __init__(self, data):
        self.data, self.pos = data, 0
    def take(self, size):
        value = self.data[self.pos:self.pos+size]
        assert len(value) == size, 'Truncated class file'
        self.pos += size
        return value
    def u1(self): return self.take(1)[0]
    def u2(self): return struct.unpack('>H', self.take(2))[0]
    def u4(self): return struct.unpack('>I', self.take(4))[0]


def class_info(data):
    """Read class/member annotations and declarations, never infer safety from UTF8 presence."""
    r = Reader(data)
    assert r.u4() == 0xcafebabe, 'Invalid class magic'
    r.take(4)
    cp = [None] * r.u2()
    i = 1
    while i < len(cp):
        tag = r.u1()
        if tag == 1: cp[i] = r.take(r.u2()).decode('utf-8')
        elif tag == 3: cp[i] = struct.unpack('>i', r.take(4))[0]
        elif tag in (4, 9, 10, 11, 12, 17, 18): r.take(4)
        elif tag in (5, 6): r.take(8); i += 1
        elif tag in (7, 8, 16, 19, 20): cp[i] = r.u2()
        elif tag == 15: r.take(3)
        else: raise AssertionError('Unknown constant pool tag ' + str(tag))
        i += 1

    def annotation(a):
        name = cp[a.u2()]
        values = {}
        for _ in range(a.u2()):
            key = cp[a.u2()]
            values[key] = element(a)
        return name, values

    def element(a):
        tag = chr(a.u1())
        if tag in 'BCDFIJSZsc': return cp[a.u2()]
        if tag == 'e': return (cp[a.u2()], cp[a.u2()])
        if tag == '@': return annotation(a)
        if tag == '[': return [element(a) for _ in range(a.u2())]
        raise AssertionError('Unknown annotation value ' + tag)

    def attributes():
        result = {}
        for _ in range(r.u2()):
            name, size = cp[r.u2()], r.u4()
            content = r.take(size)
            if name in ('RuntimeVisibleAnnotations', 'RuntimeInvisibleAnnotations'):
                a = Reader(content)
                for _ in range(a.u2()):
                    key, value = annotation(a)
                    assert key not in result, 'Duplicate annotation'
                    result[key] = value
                assert a.pos == len(content), 'Trailing annotation bytes'
        return result

    r.u2()
    owner, parent = cp[cp[r.u2()]], r.u2()
    interfaces = [cp[cp[r.u2()]] for _ in range(r.u2())]
    members = []
    for kind in ('field', 'method'):
        for _ in range(r.u2()):
            access, name, descriptor = r.u2(), cp[r.u2()], cp[r.u2()]
            annotations = attributes()
            if kind == 'method': members.append((name, descriptor, access, annotations))
    annotations = attributes()
    assert r.pos == len(data), 'Trailing class bytes'
    return {'owner': owner, 'interfaces': interfaces, 'methods': members, 'annotations': annotations}


def check_native(raw, mapped):
    # Curse Maven natures-spirit-1044992:6962783 (2.2.5), independently bound raw archive.
    with open(raw, 'rb') as stream:
        assert hashlib.sha256(stream.read()).hexdigest() == '07818dd985b100ae71b98c755961c135b07c3a08faf5d95afc87fab71a3b9e86', 'Wrong pinned raw Nature Spirit archive'
    with zipfile.ZipFile(raw) as archive:
        raw_info = class_info(archive.read(TARGET + '.class'))
    with zipfile.ZipFile(mapped) as archive:
        mapped_info = class_info(archive.read(TARGET + '.class'))
    for info, raw_names in ((raw_info, True), (mapped_info, False)):
        assert info['owner'] == TARGET and INTERFACE in info['interfaces'], 'Coconut native interface drift'
        declarations = {(name, desc) for name, desc, access, _ in info['methods'] if access & 1}
        for name, (srg, desc) in METHODS.items():
            assert (srg if raw_names else name, desc) in declarations, 'Coconut native bonemeal ABI drift: ' + name
    print('Pinned raw/FG CoconutBlock bonemeal ABI matched')


def check_mapping(refmap, info):
    assert info['owner'] == MIXIN, 'Wrong coconut mixin class'
    annotations = info['annotations']
    assert 'Lorg/spongepowered/asm/mixin/Pseudo;' not in annotations, 'Pseudo suppresses required mapping'
    target = annotations.get('Lorg/spongepowered/asm/mixin/Mixin;', {})
    assert target.get('value') == ['L' + TARGET + ';'] and not target.get('targets'), 'Coconut must use typed AP target'
    assert target.get('remap', 1) == 1, 'Coconut target remapping disabled'
    injections = {}
    for _, _, _, anns in info['methods']:
        inject = anns.get('Lorg/spongepowered/asm/mixin/injection/Inject;')
        if inject is None: continue
        selectors = inject.get('method')
        assert isinstance(selectors, list) and len(selectors) == 1, 'Unexpected coconut selectors'
        name = selectors[0]
        assert name in METHODS and name not in injections, 'Unknown/duplicate coconut injection'
        assert inject.get('remap', 1) == 1 and inject.get('require', -1) != 0, 'Fail-open coconut injector'
        assert inject.get('cancellable') == 1, 'Coconut injection not cancellable'
        ats = inject.get('at', [])
        assert len(ats) == 1 and ats[0] == ('Lorg/spongepowered/asm/mixin/injection/At;', {'value': 'HEAD'}), 'Wrong coconut injection point'
        injections[name] = inject
    assert set(injections) == set(METHODS), 'Missing coconut injection'
    mappings = refmap.get('mappings', {}).get(MIXIN, {})
    # Actual Mixin AP resolves inherited interface mappings onto the concrete target owner.
    expected = {name: 'L' + TARGET + ';' + srg + desc for name, (srg, desc) in METHODS.items()}
    assert mappings == expected, 'Wrong/incomplete CoconutFoodBlockMixin SRG mapping'
    for namespace, table in refmap.get('data', {}).items():
        assert namespace == 'searge', 'Unsupported coconut mapping namespace'
        assert table.get(MIXIN) == expected, 'Coconut default/searge refmap disagreement'


if __name__ == '__main__':
    assert len(sys.argv) == 4 and sys.argv[1] == 'native', 'Expected native RAW MAPPED'
    check_native(sys.argv[2], sys.argv[3])
