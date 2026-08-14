# Shader-backed block rendering experiment

Status: planning spike on `experiment/26.2-shader-blocks`

Baseline: `33133ce` (`main/fabric/26.2`), Minecraft 26.2, Polymer
0.17.3. The normal build remains display-backed wherever required.

## Objective

Determine how many Dye Depot blocks can render as ordinary client chunk blocks
instead of block-bound item-display entities, while retaining the unmodified
vanilla-client contract and exact behavior for both custom and vanilla blocks.

A converted family must have no `BlockWithElementHolder` attachment or item
display for either its custom blocks or any vanilla donor it reserves. It must
remain visible for the full block render distance and preserve collision,
selection, waterlogging, piston prediction, particles, lighting, animation, and
all state transitions.

## Feasibility conclusion

The experiment is worthwhile, but a terrain shader alone cannot make every
display-backed family block-only with full parity.

Minecraft's 26.2 terrain shader receives vertex position, baked color, atlas
UV, lightmap UV, and chunk/global uniforms. It does not receive the server's
custom block ID, its original block-state ID, or block-entity NBT. A shader can
recognize a uniquely allocated carrier model or marked texture, but two
positions sent as the same vanilla carrier state have the same render identity.
It cannot render one as custom maroon glazed terracotta and the other as real
orange glazed terracotta.

The sheep shader does have an additional channel: a packet-only entity scale
residue. Blocks have no comparable harmless per-placement scalar. Encoding
color in world position, biome, or light would fail for arbitrary placement or
corrupt unrelated rendering and gameplay.

This means shaders do not increase the vanilla protocol's block-state budget.
When a unique behavior-compatible carrier state exists, a resource-pack block
model can already reference the exact texture and is normally simpler than a
global shader. Shaders remain useful as a palette/marker renderer after a safe
identity channel has been established.

Glazed terracotta is the decisive example. The 16 custom and 16 vanilla colors
need 128 distinct color/facing identities. Vanilla supplies only 64 glazed
states (16 colors times four facings), and these are the only vanilla blocks
with the required `PUSH_ONLY` reaction. A generic full-block or note-block
carrier can supply custom pixels, but reintroduces incorrect sticky-piston and
slime/honey prediction. A shader cannot change that reaction.

## Current display inventory

| Family | Custom block types | Custom states | Displays per placement | Reason for the current carrier |
|---|---:|---:|---:|---|
| carpets | 16 | 16 | 1 | native one-pixel collision and selection |
| glazed terracotta | 16 | 64 | 1 | native facing and `PUSH_ONLY` piston behavior |
| candles | 16 | 256 | 1 | count, lit, waterlogged, collision, and native particles |
| candle cakes | 16 | 32 | 1 | shared slab carrier; custom lit particles and sound |
| stained-glass panes | 16 | 512 | 1 | connection masks, waterlogging, collision, and translucency |
| beds | 16 | 256 | 1 | facing, part, occupied state, and bed geometry |
| shulker boxes | 16 | 96 | 2 | native block entity plus independently animated base/lid shell |

In total, 112 custom block types and 1,232 custom block states currently use
display holders. Five real donor families also use displays because their
native model or texture is hidden globally and then restored: orange carpet,
orange candle, orange glazed terracotta, brown stained-glass pane, and brown
shulker box.

Already block-only families are wool, ordinary terracotta, concrete, concrete
powder, stained glass, dye baskets, and both banner forms. These should remain
controls; ordinary terracotta was never converted to a display. The recent
change affected glazed terracotta.

## Candidate assessment

| Family | Initial result | Direction |
|---|---|---|
| candle cakes | promising | Allocate exact `SLAB_BOTTOM` block models if the combined state pool has 32 safe states. Keep server particles/sound without a holder. |
| beds | promising but capacity-sensitive | Test direct models in the eight bed carrier pools. Sixteen colors may consume every usable state per facing/part. |
| panes | conditional | Continue only if every dry/waterlogged connection mask has 16 safe BARS/native carrier identities. Validate translucent sorting and face culling. |
| carpets | poor | No known second native carpet bank preserves both custom and ordinary vanilla carpets with exact client collision. |
| candles | poor | The 256 custom state combinations require another behavior-compatible candle bank while preserving all vanilla candles. |
| glazed terracotta | hard blocker / decision spike | Requires a second 64-state `PUSH_ONLY` bank or a new harmless identity bit. |
| shulker boxes | hard blocker | A chunk mesh cannot reproduce per-block lid progress; the native renderer has only vanilla color identities and also culls at block-entity range. |

The likely production result is a hybrid: direct chunk-block models for the
families with adequate carrier pools, and displays/native renderers retained
where the protocol cannot represent another exact identity.

## Experiment phases

### 0. Baseline and information-budget gate

1. Build a deterministic test scene containing every custom family/state and
   its exact real-vanilla donor controls.
2. Record block-bound holder count, synthetic entity count, spawn/metadata
   packet bytes, holder tick work, heap use, server MSPT, client frame time, and
   the current approximately 64-block display-cull boundary.
3. At startup after the complete mod set registers, report
   `PolymerBlockResourceUtils.getBlocksLeft` for every relevant carrier type.
4. Enumerate custom plus vanilla logical states and require an injective client
   carrier mapping with matching collision, selection, fluid, piston,
   block-entity, event, and neighbor behavior.

Do not start a family conversion when two distinct visuals alias the same
client state. A shader cannot repair that alias.

### 1. Minimal shader transport spike

1. Add an opt-in `DDPolymerBlockShaderPack` while retaining the current display
   path as an automatic fallback.
2. Fork the exact Minecraft 26.2 shaders and preserve all vanilla fog, RGSS,
   lightmap, chunk visibility, alpha-cutout, and render-layer behavior.
3. Cover `terrain.vsh/fsh` for chunk meshes, `block.vsh/fsh` for moving piston
   and falling-block rendering, and `particle.vsh/fsh` for break particles.
4. Refuse the shader backend atomically if another pack contributor already
   owns any required core shader. Shader-dependent carriers must never remain
   active after fallback.
5. Use markers that survive atlas stitching, mipmaps, filtering, and RGSS. The
   sheep shader's fixed texture-corner sentinel cannot be copied directly
   because terrain uses a dynamically stitched atlas.

Start with one color and all four glazed facings. Place the custom block beside
the exact real donor, move both with pistons, break both, reload resources, and
cross chunk/render-distance boundaries. This is a diagnostic gate, not a
production mapping.

### 2. Glazed identity gate

Attempt to scale the spike to all 16 custom and all 16 real vanilla glazed
colors simultaneously. Success requires:

- 128 distinct color/facing identities;
- native `PUSH_ONLY` behavior for every identity;
- no display holder for custom or donor blocks;
- exact direct push, sticky retraction, lateral slime/honey, moving-piston, and
  break-particle visuals;
- no donor recoloring or ghost correction dependency.

If no second safe identity bank or harmless vanilla-protocol bit exists, record
glazed terracotta as a principled no-go and retain the current implementation.
Do not return it to generic full-block/note-block carriers.

### 3. Direct block-model conversions

Prototype these independently, behind experimental flags and separate commits:

1. Candle cakes: allocate exact lit/unlit models on compatible slab states and
   reproduce particles/sound without a virtual holder.
2. Beds: allocate exact models for every facing/part/color while preserving
   occupied state and ordinary vanilla beds. Fail atomically when the combined
   pool cannot satisfy the full set.
3. Panes: proceed only after the capacity report proves 16 identities for every
   connection/water state. Do not accept missing caps, opaque sorting, or wrong
   chunk-boundary connections.

Compare direct models with shader recoloring. Prefer direct models whenever a
safe state already selects the exact model; a global fragment shader adds no
identity and imposes work on unrelated terrain.

### 4. Remaining hard families

Evaluate carpets and candles only if the capacity audit finds a second native-
behavior state bank. Treat shulkers separately: a conversion must retain native
open/close interpolation, all facings, collision/pushing, inventories, and
simultaneous exact vanilla shulkers. A static closed shell is not parity.

### 5. Integration and performance decision

For each claimed conversion:

- assert zero `BlockWithElementHolder` overlays and zero item-display spawn
  packets for both custom blocks and affected vanilla donors;
- cover all 16 colors and every state transition with vanilla controls;
- test reconnect, chunk unload/reload, resource reload, full render distance,
  mipmap levels 0-4, and chunk boundaries;
- run 100 dedicated-server piston cycles, including sticky, slime, and honey;
- test deliberate shader-owner collisions and carrier-pool exhaustion;
- run the full production-JAR combined `runServer` and vanilla-client checklist.

Use identical 4,096-block baseline and experimental scenes. Holder count,
packet bytes, heap, and server p95 MSPT must improve or remain neutral. On a
control scene without Dye Depot blocks, the global shader's median and p95 GPU
frame time must stay within 5 percent of vanilla; the display-heavy scene must
show a net client improvement.

## Checkpoint sequence

1. Capacity report, baseline scene, and metrics.
2. Opt-in shader/fallback harness.
3. One-color glazed terrain/piston/particle spike.
4. Glazed all-color identity decision.
5. Candle-cake direct block-model prototype.
6. Bed direct block-model prototype.
7. Conditional pane prototype.
8. Combined stress results and final hybrid/no-go decision.

Each checkpoint remains independently revertible on this experiment branch.
No experimental rendering backend should be merged into
`main/fabric/26.2` until its complete family passes the automated and connected
vanilla-client gates.

## Expected implementation surface

- `DDPolymerBlocks`: backend selection, carrier allocation, display removal,
  and attachment-count assertions.
- `DDPolymerPack`: conditional donor/model resources and atlas marker data.
- a new `DDPolymerBlockShaderPack`: atomic ownership, exact 26.2 shader
  installation, and display fallback selection.
- private `terrain`, `block`, and `particle` shader sources under
  `src/main/resources/dye_depot`.
- `DDPolymerSheepShaderPack` only if a later bed/shulker experiment must compose
  with the already-owned entity shader.
- `PolymerParityTest`, `DyeDepotGameTests`, and the existing Polymer entity
  GameTests for capacity, packet, state-machine, shader-resource, and behavior
  coverage.
- `FEATURES.md` after each family receives a final keep/convert decision.

No client mod, custom client packet handler, or broad rendering mixin is part
of the plan. Keep `PistonBaseBlockMixin` until combined dedicated-server tests
prove that a replacement renderer makes its correction unnecessary.

## Short-term control

The current holders set display size but not display view range, so vanilla's
default view range culls them at roughly 64 blocks. Raising item-display view
range can be tested as a low-risk visibility control while this experiment is
running. It does not reduce entity, tracking, or rendering overhead and is not
the intended final optimization.
