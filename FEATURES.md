# Dye Depot feature inventory

This is the parity contract for the Fabric 26.2 port. It was derived from
`origin/main/fabric/1.21` at commit `ef9ca68`, before porting changes were made.

## Color system

- Dye Depot extends vanilla `DyeColor` with 16 real enum values. They are visible
  to vanilla and third-party code through `DyeColor.values()`.
- IDs and ordinals are contiguous from 16 through 31. Several render paths rely
  on `id - 16`, so their order is part of the compatibility contract.
- Each color has lowercase serialization, an RGB diffuse color, firework color,
  text color, and vanilla map color.

| ID | Serialized name | RGB, firework, and text color | Map color |
|---:|---|---:|---|
| 16 | `maroon` | `#7B2713` | `CRIMSON_HYPHAE` |
| 17 | `rose` | `#FF5E64` | `TERRACOTTA_MAGENTA` |
| 18 | `coral` | `#DF7758` | `RAW_IRON` |
| 19 | `indigo` | `#331E57` | `TERRACOTTA_BLUE` |
| 20 | `navy` | `#153D64` | `COLOR_CYAN` |
| 21 | `slate` | `#4C5E86` | `WARPED_NYLIUM` |
| 22 | `olive` | `#8C8F2A` | `TERRACOTTA_LIGHT_GREEN` |
| 23 | `amber` | `#D7AF00` | `WOOD` |
| 24 | `beige` | `#E1D5A3` | `SAND` |
| 25 | `teal` | `#2F7B67` | `TERRACOTTA_CYAN` |
| 26 | `mint` | `#38CE7D` | `WARPED_WART_BLOCK` |
| 27 | `aqua` | `#5EF0CC` | `DIAMOND` |
| 28 | `verdant` | `#255714` | `TERRACOTTA_GREEN` |
| 29 | `forest` | `#32A326` | `EMERALD` |
| 30 | `ginger` | `#CF6121` | `TERRACOTTA_ORANGE` |
| 31 | `tan` | `#F49C5D` | `DIRT` |

Concrete has a separate visual map-color table:

- maroon `CRIMSON_HYPHAE`; rose `COLOR_RED`; coral `PODZOL`;
  indigo `WARPED_HYPHAE`; navy and verdant `TERRACOTTA_BLACK`;
  slate and teal `COLOR_GRAY`; olive `COLOR_BROWN`; amber `WOOD`;
  beige `TERRACOTTA_WHITE`; mint `DEEPSLATE`; aqua `WARPED_WART_BLOCK`;
  forest `COLOR_GREEN`; ginger `NETHER`; tan `DIRT`.

## Registry matrix

For every custom color the mod registers:

- One dye item.
- Wool, carpet, terracotta, concrete, concrete powder, glazed terracotta,
  stained glass, stained glass pane, shulker box, candle, candle cake,
  standing banner, wall banner, bed, and dye-basket blocks.
- Items for all those block families except candle cakes and wall banners.

It also registers a dye basket for each of the 16 vanilla colors.

| Registry content | Expected count |
|---|---:|
| Dye Depot blocks | 256 |
| Dye Depot items | 240 |
| Custom map-decoration types | 16 |
| Custom bed POI types | 1 |
| Complex block-particle types | 1 |
| Variable-range sound events | 1 |

Custom beds, banners, and shulker boxes use vanilla block-entity behavior rather
than adding new block-entity types. The 1.21 branch aliases the old
`dye_depot:bed`, `dye_depot:banner`, and `dye_depot:shulker_box` block-entity
IDs to their vanilla types for world migration. Fabric 26.2 retains only the
banner and shulker aliases; it does not register a `dye_depot:bed` block-entity
alias. This is a forced registration difference, not a loss of custom-bed
behavior.

## Vanilla dye behavior

- Each custom dye is a real dye item and works anywhere vanilla accepts a
  `DyeColor`: sheep, collars, sign text, banners, fireworks, and dyeable-item
  recipes.
- Every custom dye is included in the vanilla `dyes`, `loom_dyes`,
  `cat_collar_dyes`, and `wolf_collar_dyes` item tags. The Loom and both tame
  animal collar interactions therefore accept the original server item, not
  merely its Polymer client carrier.
- The Loom produces a real patterned-banner result for every custom dye,
  records the exact custom pattern color, and consumes one banner and one dye
  when the result is taken. Five authored layers can receive the vanilla sixth
  layer, while the server rejects any button packet that attempts a seventh.
- Owned cats and wolves accept every custom dye and retain the exact custom
  collar color in authoritative server state.
- `DyeItem` color lookup resolves each custom color to the matching item.
- Codecs and serialized color lookup round-trip every custom value.

## Sheep

- Sheep color storage expands from four to five bits; the sheared flag moves to
  bit five so all 32 colors and the sheared state coexist.
- Custom dyes can dye sheep.
- Shearing and entity loot select the matching custom wool.
- Sheep named `jeb_` cycle through all 32 registered colors on vanilla's
  25-tick cadence.
- Natural color selection adds beige and rare aqua sheep:
  - aqua: `1/500`;
  - beige: `23/100 * 499/500`;
  - otherwise the original vanilla choice.

## Dye baskets and poof particles

- All 32 baskets place horizontally, rotate, mirror, burn, and use their dye map
  color.
- Collision height is 15/16 of a block; support and visual shapes are full
  cubes. Baskets are not pathfindable and have shade brightness `0.2`.
- Falling entities and projectiles hitting the top face emit 60 colored poof
  particles and play the basket sound. Side and bottom projectile hits do not.
- While an entity is inside, the server has a `1/15` chance per callback to emit
  one silent particle.
- Main poofs originate at `y + 1.2`, spread `0.75/0.2/0.75`, speed `3`, sound
  volume `1.5`, and pitch `1`.
- Particles use dye text colors except for the vanilla blue, green, magenta,
  cyan, red, and orange overrides in `PoofParticleProvider`.
- The opaque, eight-sprite particle rotates in flight, stops rotating on the
  ground, and accelerates downward by `0.003` per tick to a `-0.14` cap.

## Colored block behavior

- Concrete powder hardens into the matching concrete.
- Glass and panes retain their custom color and render translucently.
- Carpets retain their custom color and work as llama decor.
- Carpet flammability is `(60, 20)` and wool flammability is `(60, 100)`.
- Candles retain vanilla count, lighting, waterlogging, and candle-cake behavior.
- Other standard variants inherit the corresponding white vanilla block's
  behavior and shapes.

## Banners and maps

- Vanilla banner-by-color lookup resolves custom standing banners.
- Standing and wall banners use vanilla banner data and pattern behavior.
- Banner items stack to 16 and begin with empty banner-pattern data.
- Vanilla shield decoration accepts custom banners and preserves the exact
  custom base, translated color name, and every authored pattern type, order,
  and color in authoritative server state.
- Placing a custom banner marker on a map uses its matching custom
  map-decoration type and 8x8 marker texture.
- Map decorations show on item frames, are exploration-map elements, do not
  track the decoration count, and use map color `-1`.
- Water cauldrons clean custom banner patterns.

## Shulker boxes

- Vanilla shulker-by-color lookup resolves each custom shulker box.
- Custom shulkers use vanilla storage, stack size one, content-preserving drops,
  dispenser placement, and water-cauldron undyeing.
- Placed-block and inventory render paths use the corresponding custom texture.

## Beds and villager AI

- Custom beds use normal two-part bed placement, sleeping, and visuals.
- `dye_depot:home` contains every head-half state of all custom beds: 128 states
  (`16 colors * 4 facings * 2 occupied states`), with one ticket and range one.
- `dye_depot:beds` combines the custom and vanilla home POIs, and
  `minecraft:village` includes that combined tag.
- Villager goal and nearest-bed filters recognize the combined tag, so villagers
  can claim and sleep in custom beds.
- Polymer marks `dye_depot:home` as a server-only registry entry, keeping its
  server-side villager behavior without sending an unknown POI type to vanilla
  clients during Fabric registry synchronization.

## Llamas

- Custom carpets are wool carpets and can be equipped as llama decor.
- Each custom color renders its matching llama-decoration texture.
- In 26.2, this same behavior is represented by 16
  `assets/minecraft/equipment/<color>_carpet.json` equipment assets pointing to
  the unchanged pixels at
  `dye_depot:textures/entity/equipment/llama_body/<color>.png`. The legacy
  `textures/entity/llama/decor` copies remain; this path/schema migration does
  not add or remove a llama feature.

## Creative inventory

- The Ingredients tab interleaves all 16 custom dyes with vanilla dyes.
- The Colored Blocks tab adds all 32 baskets and interleaves 12 custom block
  families with their vanilla color families (224 entries).
- The Functional Blocks tab adds custom shulkers, beds, candles, and banners
  (64 entries).
- Custom colors retain this relative order around vanilla anchors:
  - before red: maroon, rose; after red: coral;
  - before orange: ginger; after orange: tan;
  - before yellow: beige; after yellow: amber, olive;
  - before green: forest; after green: verdant;
  - before cyan: teal; after cyan: mint, aqua;
  - after blue: slate, navy;
  - before purple: indigo.

## Trading and loot integration

- The mod adds 128 villager trade factories:
  - cartographer level 4: 16 banners;
  - mason level 4: 16 terracotta and 16 glazed terracotta;
  - shepherd level 2: five dye purchases, 16 wool sales, and 16 carpet sales;
  - shepherd level 3: five dye purchases and 16 bed sales;
  - shepherd level 4: six dye purchases and 16 banner sales.
- It adds 16 wandering-trader dye offers.
- Hero-of-the-Village shepherd gifts gain all 16 custom wool blocks.
- Desert-pyramid archaeology gains beige dye at weight two.
- Cold-ocean-ruin archaeology gains verdant dye at weight three.

Exact offer costs, counts, uses, XP, and multipliers are regression-tested.

## Recipes, loot tables, and tags

The 1.21 source-generated semantic baseline and its equivalent 26.2 output are:

| Resource | 1.21 baseline | Fabric 26.2 |
|---|---:|---:|
| `dye_depot` recipes | 381 | 381 |
| `minecraft` recipes | 58 | 74 |
| Supplementaries recipes | 32 | 32 |
| Supplementaries Squared recipes | 16 | 16 |
| Total recipes | 487 | 503 |
| Matching recipe advancements | 487 | 487 |
| Core block loot tables | 240 | 240 |
| Custom sheep loot tables | 16 | 16 |
| Compatibility loot tables | 48 | 48 |

The 16 additional 26.2 `minecraft` recipes are
`<color>_banner_duplicate.json`, one for each custom banner. In 1.21 banner
duplication is handled by a parameter-free special recipe; 26.2 requires the
accepted banner and result item in per-color data. These recipes have no
advancements and preserve the existing patterned-banner copying behavior, so
the 503-versus-487 count is a forced data representation change rather than a
feature difference.

Recipe families include all standard colored blocks, 32 basket compact/unpack
pairs, all bed/carpet/wool recolors, 15 direct dye sources, four smelting
sources, and 94 mass-conserving dye mixes. Ten selected vanilla dye recipes are
intentionally disabled by false conditions.

Tags integrate custom variants with vanilla and common dye, colored-block,
mining-tool, wool, carpet, banner, bed, candle, terracotta, concrete, glass,
shulker, impermeable, and piglin-interest behavior. Dye baskets are
hoe-mineable and excluded from Supplementaries soap cleaning.

## Optional compatibility

Resources and conditionally loaded data support:

- Supplementaries candle holders, flags, presents, and trapped presents for all
  16 custom colors.
- Supplementaries Squared gold candle holders for all 16 custom colors.
- Gold holders are piglin-loved and piglin-guarded.
- Compatibility recipes, advancements, and loot require both the target mod
  and its relevant `supplementaries:flag`. Fabric 26.2 eagerly decodes every
  condition before evaluating `all_mods_loaded`, so Dye Depot registers an
  always-false decoder for that foreign condition only while Supplementaries
  is absent. When Supplementaries is installed, its real config-aware
  condition remains authoritative. Models, translations, textures, and
  optional tag entries remain harmless when the mods are absent.

## Client assets, localization, and built-in pack

- Authored assets include 384 Dye Depot textures, 114 optional-compat textures,
  five basket sounds, and the particle sprite manifest.
- Generated client resources cover every registered block and item plus
  optional-compat models.
- English contains 1,122 generated translation keys. Existing handwritten
  Spanish, Japanese, Portuguese, Toki Pona, and Chinese translations are kept.
- The built-in `dye_override` pack is default-enabled and user-disableable. It
  renames light blue to Sky and light gray to Ash across vanilla, Dye Depot, and
  compatible content, and overrides nine vanilla dye icons.

## Fabric 26.2 server-only Polymer parity mapping

The 26.2 branch is a server-only mod. It has no client entrypoint or client
mixin and requires Polymer Core, Polymer Blocks, Polymer Resource Pack,
Resource Pack Extras, and Polymer Virtual Entity. The generated Polymer pack is
required and includes the normal mod assets, generated assets, and the built-in
Sky/Ash override pack.

### Items, creative inventory, and outbound data

- All 240 registered items have Polymer overlays. Custom dyes and banners use
  their nearest-color vanilla dye and banner carriers so Loom slot rules and
  the Loom client's `BannerItem` type requirement remain valid. Custom shulker
  boxes use nearest-color vanilla shulker carriers so vanilla container and
  bundle rules still reject nesting them; other items use a neutral vanilla
  carrier. Banner items use the vanilla banner special renderer for both plain
  and patterned forms. A custom banner's exact base color is reconstructed as
  a generated, full-cloth first pattern layer. Each authored custom-color layer
  is represented by an inline client-only copy of its original pattern with a
  generated exact-color atlas mask and a codec-safe white tint. Pattern order
  and translation keys stay intact, and tooltip lines are rebuilt from the
  untouched authoritative layers. The visual base and exact-color variants
  never enter the server stack. Vanilla banners keep their native base and do
  not receive a synthetic layer.
- The Polymer creative tab contains the 240 items exactly once in baseline
  family/color order. Existing server-side vanilla-tab insertion behavior is
  retained for compatible clients. The vanilla protocol cannot inject a custom
  tab into an unmodified client's native creative screen; vanilla users can
  open the same server-authoritative contents through Polymer's built-in
  `/polymer creative dye_depot:items` fallback.
- Every direct item component whose wire value is a `DyeColor` is made
  codec-safe in the client copy: dye, ordinary base color, wolf collar, cat
  collar, sheep color, shulker color, and both tropical-fish colors are mapped
  to safe vanilla colors. A custom decorated shield's unsafe `BASE_COLOR` is
  instead removed and reconstructed by its synthetic shield-atlas layer.
- Banner pattern layers and recursively nested container contents receive the
  same client-copy sanitization. All 43 built-in masks, plus pack-visible
  third-party banner/shield masks, receive generated palettes for every custom
  dye. Custom client banner copies prepend the exact visual base and preserve
  authored tooltip lines separately. Only the clientbound copy from the Loom's
  actual five-pattern banner slot omits that synthetic base, so the vanilla UI
  still exposes the sixth real pattern slot; identical banners elsewhere,
  inventory icons, the result
  slot, and placed banners retain the exact custom base. Polymer recovery strips
  all client-only representation data by restoring the original stack. Typed
  block-entity data dispatches by its
  known vanilla type: banners sanitize only pattern colors, signs and hanging
  signs sanitize only front/back text colors, and every other type is retained
  byte-for-byte without replacing its component. `BUCKET_ENTITY_DATA`,
  `ENTITY_DATA`, and opaque `CUSTOM_DATA`, including Polymer's reserved original-
  stack recovery payload, is not rewritten, so client-to-server item round
  trips retain exact custom colors and user data. The original server stack and
  its nested contents are not mutated.
- Vanilla shields decorated from custom banners are detected before their
  extended base-color ID can bypass Polymer. Their client copy removes the
  unsafe enum value, prepends the same exact-color synthetic base in the shield
  atlas, renders custom-colored authored layers through matching generated
  shield masks, retains every pattern in order, and uses the custom translated
  shield name. Polymer recovery restores the exact untouched server shield,
  including shields nested in containers; ordinary vanilla shields are not
  wrapped.
- Custom-colored regular and hanging signs retain exact server state while the
  outbound front/back message components receive the intended RGB. Non-glowing
  text uses vanilla's 40-percent darkening and glowing text uses the full dye
  RGB; authored component colors and filtered messages are preserved. Only the
  sign-level enum carrier and the client-computed glow outline use a safe
  vanilla color.
- Custom banner update tags prepend the same exact visual base layer for initial
  chunk data and live changes; vanilla banner tags remain native. Packet
  sanitization replaces only client-visible authored extended-color layers with
  their inline exact-mask variants while running in a real outbound
  `PacketContext`; internal server patterns and every other server read retain
  the exact custom values.

### Blocks

Every state of all 256 registered blocks has a vanilla-protocol block state,
and the exact authored color/model is supplied by Polymer models, a virtual
item display, or a native banner pattern as follows. Every custom overlay is registered as a
`PolymerTexturedBlock`, so Polymer preserves its requested textured/empty
carrier instead of remapping it a second time to a visible vanilla note block:

| Family | Vanilla-client representation |
|---|---|
| wool, terracotta, concrete, concrete powder | exact resource-pack model on a full-block carrier |
| stained glass | exact translucent model on a non-occluding full-cube carrier, preventing the carrier from culling terrain faces below the glass |
| glazed terracotta | exact model with state-driven horizontal rotation |
| dye baskets | exact model with state-driven horizontal rotation |
| carpets | reserved orange-carpet donor carrier with native one-pixel movement/selection collision; the generated pack hides the placed donor model and exact-color displays render both custom carpets and real orange carpets without carrier bleed |
| candles | reserved orange-candle donor carrier with native count/lit/waterlogged states and collision; the generated pack hides the placed donor models and state-aware displays render both custom candles and real orange candles while native client candle particles avoid duplicates |
| candle cakes | one shared invisible bottom-slab carrier plus exact lit/unlit display models |
| stained-glass panes | reserved hidden brown-pane donor carrier with native dry/waterlogged connection states, collision, and selection; exact precombined displays reproduce all 16 connection masks with vanilla pane geometry/UV placement, a 180-degree item-display basis correction, and connection end caps omitted because display entities cannot perform vanilla neighbor-face culling; a server-only overlay restores real brown panes without exposing the donor inside custom panes or allocating Polymer model-pool states |
| beds | eight shared invisible bed carriers for facing and head/foot state plus exact display models; entity yaw compensates for the vanilla item-display renderer transform |
| shulker boxes | reserved hidden brown-shulker donor carrier preserves native facing, animated collision/pushing, and block-entity behavior; separate exact-texture base and lid displays reproduce the vanilla 26.2 hollow shell atlas, with inward-facing floor, ceiling, and lower-wall surfaces replacing entity-renderer backfaces unavailable to item displays, and let the client interpolate every server-tick lid transform continuously; the same split renderer restores real brown shulkers without allocating Polymer model-pool states |
| standing banners | nearest-color native standing-banner carriers preserve all 16 rotations, target shape, pole, block entity, and time-varying cloth wave; vanilla bases remain untouched, while a generated opaque full-cloth native pattern covers the donor and reconstructs each exact custom base before exact-color authored mask variants render |
| wall banners | nearest-color native wall-banner carriers preserve all four facings, target shape, bar, block entity, and time-varying cloth wave; the same generated first pattern reconstructs every custom base and palette-generated authored masks retain custom colors without a display entity or hidden VINES carrier |

The normal standalone allocation uses nine shared invisible virtual carriers,
two native brown donor families, and the proven orange carpet/candle donors. It
has zero color fallbacks. Exact Polymer model allocation is deliberately
non-fatal under a combined-mod state-pool shortage: an exhausted model falls
back to the nearest vanilla color with the same geometry/properties, retains
all server behavior, and is counted and reported at startup. Thus cosmetic
carrier contention cannot prevent the combined server from starting.

### Entities, particles, sound, and maps

- Sheep remain native sheep to the vanilla client. Their extended five-bit
  metadata is rewritten to a codec-safe sheared value, while a six-part,
  native-shaped virtual coat reconstructs the adult/baby head, body, and
  four leg shapes with vanilla's white special case and 75-percent wool tint.
  Adults retain the 26.2 undercoat after shearing while babies correctly have
  no undercoat. Its parts follow body/head pose, eating offset, walking, color,
  visibility, shearing/regrowth, age state, and visible glowing outlines. `jeb_`
  sheep step across
  all 32 registered colors on the native 25-tick cadence. Only the client copy
  of the exact server name receives a zero-width suffix, suppressing the
  duplicate native 16-color rainbow without changing authoritative name data.
  Removing a sheep destroys the associated virtual attachment.
- Tamed cats and wolves retain the real tame bit and native animated model.
  Their packet overlay selects one of 640 synchronized, spawn-disabled client
  variants covering all 32 vanilla/custom collar colors across all 11 vanilla
  cat and nine vanilla wolf bodies. The Polymer pack bakes the exact native
  collar mask into 1,280 adult/baby body textures and makes the four native
  collar-layer masks transparent, so there is no display entity or RGB color
  approximation in the ordinary dry, unhurt, unarmored render state.
  Authoritative collar color and body variant remain untouched
  on the server. Unknown third-party variants pass through safely instead of
  being remapped to a missing synthetic entry. Because the required pack makes
  the four shared vanilla collar masks globally transparent, however, those
  unsupported variants have no visible collar until their body textures are
  added to the generated composite integration. Sheep, cat, and wolf remain
  client-visible vanilla entity registry entries so later vanilla entity IDs
  cannot shift during registry sync.
- Dye-basket poofs retain their exact RGB through a vanilla dust-particle
  overlay. The basket sound has a vanilla-safe Polymer sound overlay.
- Custom banner map decorations are exposed as the nearest vanilla banner
  marker so an unknown registry value never reaches a vanilla client.
- The resource pack contains all 16 llama equipment definitions and textures,
  preserving custom-carpet llama decor without a client mod.

### Automated Polymer regression coverage

- Loader-aware JUnit asserts server-only metadata, all 240 item overlays,
  all four vanilla dye-behavior tags, per-color Loom-compatible dye/banner
  carriers, non-nestable shulker carriers,
  all 256 textured block overlays and every state mapping, preservation of
  every requested carrier through Polymer's real default mapper,
  dry/waterlogged carrier geometry and sharing, bed renderer-compensated yaw,
  deterministic fallback accounting, schema-scoped outbound component safety,
  Sky/Ash merge ordering, all-banner special-model JSON, all 16 generated custom
  banner-base registry entries/textures, non-occluding glass,
  exact carpet/candle collision parity, all 16 pane masks and their face UVs,
  native banner carrier/state parity, split shulker shell models, exposed-interior UVs,
  hidden donors, all 640 synchronized collar-variant definitions, all 1,280
  exact adult/baby collar composites, and four transparent native collar masks,
  distinct banner/shield base textures, both exact-pattern atlases, every
  256-entry compensated palette, all 43 built-in mask references, exact sign RGB
  for all 16 colors across glow states, regular/hanging types, both faces,
  normal and filtered messages, nested sign item data, and live/initial packet paths;
  real-`Connection` block-entity packet sanitization, entity metadata, Polymer
  creative ordering, particles/sound/maps, and every generated virtual model.
- Server GameTests cover the baseline gameplay/data contract; real Loom
  result creation and take-time consumption for every custom dye; every
  registered authored banner pattern across all 16 custom shield bases and all
  43 × 16 exact inline-pattern combinations through the real stream codec,
  including zero/multiple layers and nested bundle/charged-projectile recovery;
  exact outbound-to-real Polymer item round trips; exact client-only
  banner-base injection and tooltip hiding; real five-to-six Loom creation,
  client-predicted remote-slot hash correction, exact consumption, and
  server-side seventh-layer rejection; native
  standing/wall banner carrier and exact-color update-tag behavior; real
  standing/hanging sign dye and glow-ink interactions on both faces; exact sign
  RGB, consumption, and persistence; actual cat/wolf dye interactions for all
  16 custom dyes with ownership, negative, consumption, persistence, synthetic
  exact-variant selection, tame-bit preservation, and server-state immutability; shulker lid
  animation, server collision, and display lighting; candle auto-tick and
  vanilla flame offsets; and lazy adult/baby articulated sheep allocation,
  posing, shearing, regrowth, visibility, cleanup, visible glowing outlines,
  real survival dye interaction/consumption for all 16 custom dyes, and the exact
  32-color `jeb_` cycle.
- Combined `runServer` startup, Polymer pack generation, and a connected
  vanilla-client visual/interaction pass remain the final acceptance gate; the
  standalone tests do not replace that combined check.

### Combined `runServer` and vanilla-client acceptance checklist

Use the full compatible server mod/dependency set and the
`26.2_Fabric_Testing` Prism instance with only Polymer, Component Viewer, and
Fabric API. Copy `_My_Assets/options.txt` into the instance before testing.

- [ ] Start through Gradle `runServer`; confirm `Done`, successful Polymer pack
  generation, no duplicate top-level JARs, registry/mixin/model errors, or
  relevant warnings. Connect to `localhost`, accept the pack, and confirm no
  missing textures, raw keys, or disconnects. Sky/Ash names and overridden dye
  icons must win in the received pack.
- [ ] Open `/polymer creative dye_depot:items`; verify 240 unique entries and
  representative dye, basket, wool, carpet, glass, pane, candle, bed, shulker,
  and banner names/models. An invalid tab identifier must fail cleanly without
  changing inventory or disconnecting the player.
- [ ] Place, rotate, break, and recover representative custom full blocks,
  glazed terracotta, baskets, carpets, panes, candles, beds, shulkers, and both
  banner forms. Check drops and nearby vanilla controls; inspect all four bed
  facings and both halves. Carpet selection and movement collision must match a
  vanilla carpet rather than string or a pressure plate.
- [ ] Connect panes in several masks and waterlog panes/candles. Pane posts and
  arms must remain centered on the carrier outline. Water must be visible and
  behave normally; dry controls must stay dry. Place glass over opaque terrain
  and confirm the ground face remains rendered instead of becoming an x-ray.
  Candle selection and movement collision must match vanilla for all four
  candle counts. Light/extinguish
  one-to-four candles and candle cakes and observe the exact lit model,
  flame/smoke, ambient sound, and the unlit negative case. Compare one versus
  four custom candles and vanilla candles: light levels must remain 3/6/9/12
  (candle cake 3), with no fullbright display.
- [ ] In a Loom, apply each of the 16 custom dyes to a banner, explicitly add
  a sixth authored pattern to a five-pattern custom banner, reinsert it and
  confirm a seventh cannot be selected or produced, duplicate a
  patterned banner, wash it in a cauldron, and place it
  standing and on a wall. Layers must appear and update/remove without a raw
  color, synthetic tooltip line, or crash. Compare all 16 vanilla and 16 custom
  banner item icons and placed bases; each must retain its intended exact base
  color. The native pole/bar, all standing rotations and wall facings, base
  cloth, and authored patterns must wave together. Every custom-colored
  authored layer must use its intended custom color on both banners and shields,
  with the correct translated tooltip description rather than a nearest-color
  name.
- [ ] Craft custom banners with zero, one, and several authored patterns into
  shields. Confirm the exact custom base and custom shield name appear in the
  inventory, first/third person, dropped-item, and blocking views; all authored
  patterns must remain in order. Move a patterned shield through a container
  and reconnect to confirm exact server round-trip persistence. Compare an
  ordinary vanilla banner/shield control.
- [ ] Dye both sides of normal and hanging signs with representative light and
  dark custom dyes, then apply glow ink. Check both the live update and the
  initial render after leaving/reloading the chunk. Confirm non-glowing text has
  vanilla's darkened shade, glowing glyphs use the full exact RGB,
  editing/reloading keeps ordinary and filtered text and color, and the
  nearest-color glow outline has no black-specific pale halo.
- [ ] Fill, name, place, open, close, break, dispense, and cauldron-wash a
  custom shulker. Its lid must visibly animate without turning black or exposing
  a filled/water-textured base model through the opened lid, and its
  authoritative expanding collision must push/block entities like vanilla.
  Contents must survive and the item must be rejected from shulker-box slots
  and bundles; compare with a vanilla shulker.
- [ ] Dye adult and baby sheep, then watch them idle, walk, turn their heads,
  and eat grass. Shear/regrow both: an adult must retain only its correctly
  tinted undercoat while a sheared baby has no coat. Name a sheep `jeb_` and
  confirm the 32-color, 25-tick cycle with no second native rainbow layer. Give
  a visible custom sheep the glowing flag and confirm its complete coat receives
  the outline. Remove a custom sheep and confirm no orphaned parts remain. Dye an owned wolf and cat
  collar with each of the 16 custom dyes; repeat representative vanilla/custom
  colors on adult and baby cats/wolves of every vanilla body variant. Verify
  the exact ordinary-state collar texture color, native movement/sitting/tame
  behavior, and no duplicate collar layer. Also compare a cat's collar edge
  and a wolf while wet, hurt-flashing, and armored against a vanilla control;
  these cases exercise the documented baked-layer differences below. Equip a
  llama with custom carpet; place/use beds and let a
  villager claim one. Verify the exact collar/sheep/llama/bed models.
- [ ] Trigger basket poofs/sound from the top with a falling entity/projectile
  and while inside. Side/bottom projectile controls must not trigger the main
  burst. Confirm exact dust RGB and the documented non-legacy sprite shape.
- [ ] Craft representative direct, recolor, mix, smelting, basket pack/unpack,
  patterned-banner duplicate, and concrete-powder hardening cases. Check
  representative villager/wandering trades and archaeology/loot additions,
  plus an invalid recipe/control case.
- [ ] Restart the complete combined set, reconnect, accept the regenerated
  pack, and recheck saved banners, shulkers, beds, waterlogged states, recipes,
  and logs for cross-mod registry, mixin, translation, model, or Polymer state-
  pool collisions.

### Vanilla-protocol visual limits

These are presentation limits of fields whose vanilla wire codecs contain only
the 16 built-in colors; server state, interactions, recipes, drops, storage,
and data remain exact:

- A vanilla client cannot decode the 16 extended colors in banner-pattern,
  sheep-wool, collar-color, sign-outline, or map-decoration enum fields. Banner
  and decorated-shield bases remain exact when placed because custom bases are
  baked into one client-only native pattern per color and rendered with a
  codec-safe white tint. Authored custom-color patterns use inline copies of
  their original pattern descriptors plus palette-generated banner and shield
  masks, avoiding the enum without changing server data. The white carrier's
  `0xF9FFFE` multiplier imposes a six-level ceiling only on otherwise-255 red;
  all representable channels are compensated exactly. Sign glyphs bypass the
  enum limitation through exact RGB component styles. Cat/wolf collars bypass
  it with synchronized body variants whose ordinary-state textures use the
  exact native mask and dye RGB, while the native glow outline and map markers
  retain the safe nearest tint. Baking the collar into that body texture means
  a cat loses the native collar layer's tiny geometry inflation, and a wolf's
  collar shares body wet/hurt shading and armor ordering instead of the native
  collar layer's independent overlay. Embedded 26.2 vanilla body/mask sources
  also do not inherit another server pack's replacement cat/wolf artwork.
  Sheep have no native
  RGB or variant field, so their exact coat is articulated with server-driven
  item displays over a native sheared proxy. The banner itself remains a native block-entity
  renderer, so its pole, cloth, base, and patterns retain the vanilla wave.
  The vanilla Loom counts visible layers and caps them at six, so its input
  slot temporarily shows the nearest vanilla carrier base when a custom banner
  already has exactly five authored layers. That narrowly scoped view omits the
  synthetic base so the sixth pattern remains selectable. The Loom result,
  normal inventory icon, and placed banner all retain the exact custom base.
- Sheep item-display tint changes are server-tick updates rather than the
  vanilla renderer's per-frame `jeb_` interpolation. Invisible glowing sheep can
  expose only the native proxy outline, and the virtual coat is removed during
  the death roll because those client-only root transforms cannot be reproduced
  safely without a dedicated server-side pose implementation.
- Custom shulkers use authored-texture base/lid models. The server sends each
  real progress change and the vanilla client interpolates the lid translation
  and rotation between ticks, avoiding the former 11-frame snapping. The server
  retains exact expanding collision and entity pushing. The invisible vanilla-client
  carrier itself stays a closed full cube, so client-side collision prediction
  for the protruding lid is corrected by the authoritative server rather than
  encoded as an extra visible carrier block.
- Brown stained-glass panes and brown shulker boxes are reserved as hidden
  physical donors, then reconstructed only for actual brown server blocks so
  both their placed forms and inventory items remain visible.
- The removed custom eight-sprite poof provider cannot run on a vanilla client;
  the replacement is an exact-RGB vanilla dust particle rather than the legacy
  rotating sprite animation.
- The baseline `dye_override` pack is default-enabled but user-disableable on a
  modded client. Polymer must merge it into the single required server pack so
  Sky/Ash remains the default presentation for vanilla clients; the vanilla
  server-pack flow provides no per-client opt-out for that merged sub-pack.

## Known base quirks preserved unless 26.2 requires otherwise

These are pre-existing 1.21 data/configuration quirks, not missing port features:

- Per-color custom dye tags are emitted under `c:dyed/<color>` rather than
  `c:dyes/<color>`; aggregate `c:dyes` still directly includes the custom dyes.
- Datagen emits 16 unused wall-banner item models and 16 unused wrong-namespace
  flag item models.
- Handwritten locales have existing coverage drift relative to generated
  English.
- The 1.21 mixin configuration lists `ShulkerBoxBlockMixin` twice.

## Parity gates

The 26.2 port is complete only when automated checks cover and pass:

1. Enum bootstrap, metadata, codecs, and 32-value ordering.
2. The complete 256-block/240-item registry manifest and associated registries.
3. Sheep color/shearing/loot behavior plus adult, baby, and 32-color `jeb_`
   Polymer visuals.
4. Standard colored-block, basket, banner/map, shulker, bed/POI, and llama
   behavior, including real Loom results and custom-banner shield decoration
   with complete pattern parity.
5. Creative-tab ordering, trades, loot additions, recipes, and tags.
6. Generated-resource completeness and reference validation.
7. Exact RGB transport for regular/hanging signs and exact server interactions
   for custom-dyed cat/wolf collars.
8. Dedicated-server bootstrap plus connected-client resource, render, and
   interaction acceptance tests.
9. Discovery and default activation of the Sky/Ash built-in resource pack.
