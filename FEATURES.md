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
