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
- `DyeItem` color lookup resolves each custom color to the matching item.
- Codecs and serialized color lookup round-trip every custom value.

## Sheep

- Sheep color storage expands from four to five bits; the sheared flag moves to
  bit five so all 32 colors and the sheared state coexist.
- Custom dyes can dye sheep.
- Shearing and entity loot select the matching custom wool.
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
  carrier. Unpatterned items retain the original Dye Depot item-model
  identifier. A patterned banner selects a generated vanilla banner special
  model so its layers remain visible; its base and extended layer colors are
  mapped to their nearest codec-safe vanilla colors for that client copy.
- The Polymer creative tab contains the 240 items exactly once in baseline
  family/color order. Existing server-side vanilla-tab insertion behavior is
  retained for compatible clients. The vanilla protocol cannot inject a custom
  tab into an unmodified client's native creative screen; vanilla users can
  open the same server-authoritative contents through Polymer's built-in
  `/polymer creative dye_depot:items` fallback.
- Every direct item component whose wire value is a `DyeColor` is mapped to a
  codec-safe vanilla color in the client copy: dye, base color, wolf collar,
  cat collar, sheep color, shulker color, and both tropical-fish colors.
- Banner pattern layers and recursively nested container contents receive the
  same client-copy sanitization. Typed block-entity data dispatches by its
  known vanilla type: banners sanitize only pattern colors, signs and hanging
  signs sanitize only front/back text colors, and every other type is retained
  byte-for-byte without replacing its component. `BUCKET_ENTITY_DATA`,
  `ENTITY_DATA`, and opaque `CUSTOM_DATA`, including Polymer's reserved original-
  stack recovery payload, is not rewritten, so client-to-server item round
  trips retain exact custom colors and user data. The original server stack and
  its nested contents are not mutated.
- Live block-entity update packets sanitize those known extended-color fields
  only while running in a real outbound `PacketContext`; internal server reads
  retain the exact custom values.

### Blocks

Every state of all 256 registered blocks has a vanilla-protocol block state,
and the exact authored color/model is supplied by Polymer models or a virtual
item display as follows. Every overlay is registered as a
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
| standing banners | one shared invisible targetable vines carrier; plain banners combine the ground-attached vanilla special renderer with an exact custom-color cloth overlay, while patterned banners use the codec-safe vanilla special renderer |
| wall banners | the shared invisible targetable vines carrier; plain banners combine the wall-attached vanilla special renderer with an exact custom-color cloth overlay, while patterned banners use the codec-safe vanilla special renderer |

Banner holders inspect block-entity patterns only from an already loaded
chunk. This preserves patterned visuals while preventing recursive chunk loads
when holders are reconstructed during server restart.

The normal standalone allocation uses 10 shared invisible virtual carriers,
two native brown donor families, and the proven orange carpet/candle donors. It
has zero color fallbacks. Exact Polymer model allocation is deliberately
non-fatal under a combined-mod state-pool shortage: an exhausted model falls
back to the nearest vanilla color with the same geometry/properties, retains
all server behavior, and is counted and reported at startup. Thus cosmetic
carrier contention cannot prevent the combined server from starting.

### Entities, particles, sound, and maps

- Sheep remain native sheep to the vanilla client. Their extended five-bit
  metadata is rewritten to a codec-safe value, while an exact-color virtual
  wool coat follows color, sheared/regrown, adult/baby, position, and yaw
  state. Removing a sheep destroys the associated virtual attachment.
- Cat and wolf collar metadata is mapped to the nearest vanilla client color;
  the exact custom value remains authoritative on the server. Their packet
  overlays leave sheep, cat, and wolf as client-visible vanilla registry
  entries so later vanilla entity IDs cannot shift during registry sync.
- Dye-basket poofs retain their exact RGB through a vanilla dust-particle
  overlay. The basket sound has a vanilla-safe Polymer sound overlay.
- Custom banner map decorations are exposed as the nearest vanilla banner
  marker so an unknown registry value never reaches a vanilla client.
- The resource pack contains all 16 llama equipment definitions and textures,
  preserving custom-carpet llama decor without a client mod.

### Automated Polymer regression coverage

- Loader-aware JUnit asserts server-only metadata, all 240 item overlays,
  per-color Loom-compatible dye/banner carriers, non-nestable shulker carriers,
  all 256 textured block overlays and every state mapping, preservation of
  every requested carrier through Polymer's real default mapper,
  dry/waterlogged carrier geometry and sharing, bed renderer-compensated yaw,
  deterministic fallback accounting, schema-scoped outbound component safety,
  Sky/Ash merge ordering, all-banner special-model JSON, non-occluding glass,
  exact carpet/candle collision parity, all 16 pane masks and their face UVs,
  exact plain-banner composites, split shulker shell models, exposed-interior UVs,
  and hidden donors,
  real-`Connection` block-entity packet sanitization, entity metadata, Polymer
  creative ordering, particles/sound/maps, and every generated virtual model.
- Server GameTests cover the baseline gameplay/data contract, Loom acceptance
  and banner-item type safety for every custom color, exact outbound-to-real
  Polymer item round trips, patterned item/standing/wall banner model selection
  and block-entity add/remove updates, non-loading banner lookup during chunk
  reconstruction, shulker lid animation/server collision/display lighting,
  candle auto-tick and vanilla flame offsets, and the custom
  sheep coat lifecycle, including shearing, regrowth, and entity-removal
  cleanup.
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
- [ ] In a Loom, combine representative custom dyes and banners, add multiple
  patterns, duplicate a patterned banner, wash it in a cauldron, and place it
  standing and on a wall. Layers must appear and update/remove without a raw
  color or crash. Plain banners must retain the exact custom base color over
  vanilla pole/cloth geometry; patterned banners use the documented safe nearest
  base/layer colors. Cloth uses a fixed wave phase on the item-display path.
- [ ] Fill, name, place, open, close, break, dispense, and cauldron-wash a
  custom shulker. Its lid must visibly animate without turning black or exposing
  a filled/water-textured base model through the opened lid, and its
  authoritative expanding collision must push/block entities like vanilla.
  Contents must survive and the item must be rejected from shulker-box slots
  and bundles; compare with a vanilla shulker.
- [ ] Dye, shear, and regrow a sheep; dye wolf and cat collars; equip a llama
  with custom carpet; place/use beds and let a villager claim one. Verify exact
  sheep/llama/bed models and the documented safe nearest collar tint.
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
  sign-text-color, collar-color, or map-decoration enum fields. Plain placed
  banners retain their exact custom base through a model overlay on the
  ground/wall vanilla special renderer. Once patterns are present, the base and
  extended layer colors use safe nearest vanilla colors so every layer remains
  visible and codec-safe; sign/collar/map tints have the same wire limitation.
  The special renderer is carried by an item display, so its cloth uses a fixed
  wave phase rather than the client block-entity renderer's time-varying wave.
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
3. Sheep color/shearing/loot behavior.
4. Standard colored-block, basket, banner/map, shulker, bed/POI, and llama
   behavior.
5. Creative-tab ordering, trades, loot additions, recipes, and tags.
6. Generated-resource completeness and reference validation.
7. Dedicated-server bootstrap plus client resource/render smoke tests.
8. Discovery and default activation of the Sky/Ash built-in resource pack.
