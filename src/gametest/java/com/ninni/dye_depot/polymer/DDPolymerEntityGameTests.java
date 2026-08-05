package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.mixin.CatDataAccessor;
import com.ninni.dye_depot.mixin.EntityDataAccessors;
import com.ninni.dye_depot.mixin.SheepDataAccessor;
import com.ninni.dye_depot.mixin.WolfDataAccessor;
import com.ninni.dye_depot.registry.DDBlocks;
import com.ninni.dye_depot.registry.DDDyes;
import com.ninni.dye_depot.registry.DDItems;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.api.other.PlayerBoundConsumer;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import eu.pb4.polymer.core.impl.interfaces.GenericPlayerContext;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.impl.networking.context.PacketContextImpl;
import net.fabricmc.fabric.mixin.networking.accessor.ServerCommonPacketListenerImplAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.ARGB;
import net.minecraft.util.HashOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShieldDecorationRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;

public final class DDPolymerEntityGameTests {
    @GameTest
    @SuppressWarnings("removal")
    public void polymerOutboundRoundTripPreservesExactCustomItemData(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var registries = helper.getLevel().registryAccess();
        var color = DDDyes.MAROON.get();
        var pattern = registries.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(BannerPatterns.CROSS);
        var originalPatterns = new BannerPatternLayers.Builder().add(pattern, color).build();
        var nestedDye = new ItemStack(DDItems.DYES.getOrThrow(color));
        var userTag = new CompoundTag();
        userTag.putString("color", color.getName());
        userTag.putString("marker", "preserve_me");

        var serverStack = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        serverStack.set(DataComponents.BANNER_PATTERNS, originalPatterns);
        serverStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(nestedDye)));
        serverStack.set(DataComponents.CUSTOM_DATA, CustomData.of(userTag));

        var playerConnection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        var context = playerConnection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, registries);
        context.set(PacketContextImpl.SERVER_INSTANCE, helper.getLevel().getServer());
        context.set(PacketContextImpl.GAME_PROFILE, player.getGameProfile());
        ItemStack clientStack = PolymerItemUtils.getPolymerItemStack(serverStack, context, registries);
        helper.assertValueEqual(
                clientStack.get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/maroon_banner_patterned"),
                "patterned banner items select the ground banner special model"
        );
        helper.assertTrue(
                clientStack.get(DataComponents.BANNER_PATTERNS).layers().stream()
                        .allMatch(layer -> layer.color().getId() < 16),
                "client-visible banner pattern colors are vanilla-codec safe"
        );
        helper.assertValueEqual(
                clientStack.get(DataComponents.BANNER_PATTERNS).layers().getFirst()
                        .pattern().unwrapKey().orElseThrow().identifier(),
                DyeDepot.modLoc("polymer_base_maroon"),
                "client banner prepends its exact full-cloth visual base"
        );
        var clientAuthored = clientStack.get(DataComponents.BANNER_PATTERNS).layers().get(1);
        helper.assertTrue(clientAuthored.pattern().unwrapKey().isEmpty(), "custom authored mask is an inline client pattern");
        helper.assertValueEqual(
                clientAuthored.pattern().value().assetId(),
                DDPolymerBannerPatterns.visualAssetId(pattern.value().assetId(), color),
                "custom authored mask selects its exact generated palette variant"
        );
        helper.assertValueEqual(clientAuthored.pattern().value().translationKey(), pattern.value().translationKey(),
                "custom authored mask retains its tooltip translation family");
        helper.assertValueEqual(clientAuthored.color(), DyeColor.WHITE, "custom authored mask uses a safe white multiplier");
        helper.assertTrue(
                !clientStack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
                        .shows(DataComponents.BANNER_PATTERNS),
                "the synthetic base is hidden from the normal banner-pattern tooltip"
        );
        helper.assertValueEqual(
                clientStack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().size(),
                1,
                "the authored pattern keeps one visible tooltip line"
        );
        CompoundTag carrierData = clientStack.get(DataComponents.CUSTOM_DATA).copyTag();
        helper.assertTrue(
                carrierData.contains(PolymerItemUtils.POLYMER_STACK),
                "Polymer carrier retains its reserved original-stack payload"
        );
        helper.assertTrue(
                carrierData.get(PolymerItemUtils.POLYMER_STACK).toString().contains(color.getName()),
                "reserved original-stack payload retains the exact custom color"
        );
        ItemStack clientNested = clientStack.get(DataComponents.CONTAINER)
                .nonEmptyItemCopyStream().findFirst().orElseThrow();
        helper.assertTrue(clientNested.get(DataComponents.DYE).getId() < 16, "nested client dye color is codec safe");

        ItemStack restored = PolymerItemUtils.getRealItemStack(clientStack, context, registries);
        helper.assertValueEqual(restored.getItem(), serverStack.getItem(), "round trip restores the custom banner item");
        helper.assertValueEqual(
                restored.get(DataComponents.BANNER_PATTERNS),
                originalPatterns,
                "round trip restores exact custom banner patterns"
        );
        helper.assertValueEqual(
                restored.get(DataComponents.CUSTOM_DATA).copyTag(),
                userTag,
                "round trip restores opaque user custom data"
        );
        ItemStack restoredNested = restored.get(DataComponents.CONTAINER)
                .nonEmptyItemCopyStream().findFirst().orElseThrow();
        helper.assertValueEqual(
                restoredNested.getItem(),
                nestedDye.getItem(),
                "round trip restores the exact nested custom dye"
        );
        helper.assertValueEqual(
                restoredNested.get(DataComponents.DYE),
                color,
                "round trip restores the nested custom dye color"
        );

        ItemStack plainBanner = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        ItemStack plainClientStack = PolymerItemUtils.getPolymerItemStack(plainBanner, context, registries);
        helper.assertValueEqual(
                plainClientStack.get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("maroon_banner"),
                "unpatterned banner items use their native animated banner model"
        );
        helper.assertValueEqual(
                plainClientStack.get(DataComponents.BANNER_PATTERNS).layers().getFirst()
                        .pattern().unwrapKey().orElseThrow().identifier(),
                DyeDepot.modLoc("polymer_base_maroon"),
                "plain banner items receive the same exact animated base layer"
        );
        helper.assertTrue(
                plainClientStack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().isEmpty(),
                "plain banners do not expose a synthetic tooltip line"
        );
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void customBannerCraftsAnExactCodecSafePolymerShield(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var registries = helper.getLevel().registryAccess();
        var color = DDDyes.MAROON.get();
        var patterns = registries.lookupOrThrow(Registries.BANNER_PATTERN);
        var cross = patterns.getOrThrow(BannerPatterns.CROSS);
        var stripeBottom = patterns.getOrThrow(BannerPatterns.STRIPE_BOTTOM);
        var authoredPatterns = new BannerPatternLayers.Builder()
                .add(cross, DDDyes.ROSE.get())
                .add(stripeBottom, DyeColor.BLUE)
                .build();
        var banner = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        banner.set(DataComponents.BANNER_PATTERNS, authoredPatterns);
        var input = CraftingInput.of(2, 1, List.of(banner, new ItemStack(Items.SHIELD)));
        var recipe = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow()
                .value();
        helper.assertTrue(recipe instanceof ShieldDecorationRecipe, "the vanilla shield-decoration recipe accepts a custom banner");

        ItemStack serverShield = recipe.assemble(input);
        helper.assertValueEqual(serverShield.getItem(), Items.SHIELD, "shield decoration keeps the vanilla shield item");
        helper.assertValueEqual(serverShield.get(DataComponents.BASE_COLOR), color, "server shield retains the exact custom base");
        helper.assertValueEqual(
                serverShield.get(DataComponents.BANNER_PATTERNS),
                authoredPatterns,
                "server shield retains exact authored pattern colors"
        );

        var connection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        var context = connection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, registries);
        context.set(PacketContextImpl.SERVER_INSTANCE, helper.getLevel().getServer());
        context.set(PacketContextImpl.GAME_PROFILE, player.getGameProfile());
        ItemStack clientShield = PolymerItemUtils.getPolymerItemStack(serverShield, context, registries);
        var clientPatterns = clientShield.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);

        ItemStack resanitizedShield = clientShield.copy();
        DDPolymerItemSanitizer.sanitize(clientShield, resanitizedShield, registries, context);
        helper.assertTrue(
                ItemStack.matches(clientShield, resanitizedShield),
                "sanitizing an already-client-safe custom shield is idempotent"
        );

        ItemStack forgedVisualShield = clientShield.copy();
        var forgedLayers = new ArrayList<>(clientPatterns.layers());
        forgedLayers.set(0, new BannerPatternLayers.Layer(
                forgedLayers.getFirst().pattern(),
                DDDyes.MAROON.get()
        ));
        forgedVisualShield.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(forgedLayers));
        ItemStack sanitizedForgedShield = forgedVisualShield.copy();
        DDPolymerItemSanitizer.sanitize(forgedVisualShield, sanitizedForgedShield, registries, context);
        helper.assertTrue(
                sanitizedForgedShield.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers().stream().noneMatch(layer -> DDDyes.isModDye(layer.color())),
                "an internal synthetic-base pattern with a forged custom tint cannot reach the client"
        );

        helper.assertTrue(clientShield.get(DataComponents.BASE_COLOR) == null, "custom base ID never reaches a vanilla client");
        helper.assertValueEqual(clientPatterns.layers().size(), 3, "shield client copy has exact base plus authored patterns");
        helper.assertValueEqual(
                clientPatterns.layers().getFirst().pattern().unwrapKey().orElseThrow().identifier(),
                DyeDepot.modLoc("polymer_base_maroon"),
                "shield starts with the exact full-face custom base pattern"
        );
        helper.assertValueEqual(clientPatterns.layers().getFirst().color(), net.minecraft.world.item.DyeColor.WHITE, "synthetic base uses a codec-safe tint");
        helper.assertTrue(clientPatterns.layers().get(1).pattern().unwrapKey().isEmpty(),
                "custom authored shield pattern is an inline exact-color visual");
        helper.assertValueEqual(
                clientPatterns.layers().get(1).pattern().value().assetId(),
                DDPolymerBannerPatterns.visualAssetId(cross.value().assetId(), DDDyes.ROSE.get()),
                "first authored shield pattern keeps its mask and exact custom color"
        );
        helper.assertValueEqual(
                clientPatterns.layers().get(1).pattern().value().translationKey(),
                cross.value().translationKey(),
                "first authored shield pattern keeps its translation key"
        );
        helper.assertValueEqual(
                clientPatterns.layers().get(1).color(),
                DyeColor.WHITE,
                "first authored custom pattern uses a white codec-safe multiplier"
        );
        helper.assertValueEqual(
                clientPatterns.layers().get(2).pattern(),
                stripeBottom,
                "second authored shield pattern keeps its order"
        );
        helper.assertValueEqual(
                clientPatterns.layers().get(2).color(),
                DyeColor.BLUE,
                "authored vanilla pattern color remains exact"
        );
        helper.assertTrue(
                clientPatterns.layers().stream().allMatch(layer -> layer.color().getId() < 16),
                "every shield pattern color sent to the client is codec safe"
        );
        helper.assertValueEqual(
                clientShield.get(DataComponents.ITEM_NAME),
                Component.translatable("item.minecraft.shield.maroon"),
                "client shield keeps its exact custom-color name"
        );
        helper.assertTrue(
                !clientShield.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
                        .shows(DataComponents.BANNER_PATTERNS),
                "synthetic shield base is hidden from the pattern tooltip"
        );
        helper.assertValueEqual(
                clientShield.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().size(),
                2,
                "both authored shield patterns keep their tooltip lines"
        );
        helper.assertTrue(
                clientShield.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().getFirst().toString()
                        .contains(cross.value().translationKey() + "." + DDDyes.ROSE.getName()),
                "custom authored shield tooltip names the exact Dye Depot color"
        );
        CompoundTag clientData = clientShield.get(DataComponents.CUSTOM_DATA).copyTag();
        helper.assertTrue(clientData.contains(PolymerItemUtils.POLYMER_STACK), "vanilla shield carries exact Polymer recovery data");
        helper.assertValueEqual(serverShield.get(DataComponents.BASE_COLOR), color, "outbound conversion never mutates the server shield");
        helper.assertValueEqual(serverShield.get(DataComponents.BANNER_PATTERNS), authoredPatterns, "outbound conversion never mutates server patterns");

        ItemStack restoredShield = PolymerItemUtils.getRealItemStack(clientShield, context, registries);
        helper.assertValueEqual(restoredShield.get(DataComponents.BASE_COLOR), color, "shield round trip restores exact custom base");
        helper.assertValueEqual(restoredShield.get(DataComponents.BANNER_PATTERNS), authoredPatterns, "shield round trip restores exact authored patterns");

        var plainBanner = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        var plainInput = CraftingInput.of(2, 1, List.of(plainBanner, new ItemStack(Items.SHIELD)));
        ItemStack plainServerShield = recipe.assemble(plainInput);
        helper.assertValueEqual(
                plainServerShield.get(DataComponents.BASE_COLOR),
                color,
                "an unpatterned custom banner still gives the shield its exact server base"
        );
        helper.assertValueEqual(
                plainServerShield.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY),
                BannerPatternLayers.EMPTY,
                "zero-pattern shield remains unpatterned server-side"
        );
        ItemStack plainClientShield = PolymerItemUtils.getPolymerItemStack(plainServerShield, context, registries);
        helper.assertValueEqual(
                plainClientShield.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers().size(),
                1,
                "zero-pattern custom shield receives only its exact synthetic visual base"
        );
        helper.assertTrue(
                plainClientShield.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().isEmpty(),
                "synthetic-only shield base adds no tooltip line"
        );
        ItemStack restoredPlainShield = PolymerItemUtils.getRealItemStack(plainClientShield, context, registries);
        helper.assertValueEqual(
                restoredPlainShield.get(DataComponents.BASE_COLOR),
                color,
                "zero-pattern shield round trip restores its exact custom base"
        );
        helper.assertValueEqual(
                restoredPlainShield.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY),
                BannerPatternLayers.EMPTY,
                "zero-pattern shield round trip does not retain the synthetic layer"
        );

        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(
                DataComponents.BUNDLE_CONTENTS,
                new BundleContents(List.of(ItemStackTemplate.fromNonEmptyStack(serverShield)))
        );
        ItemStack clientBundle = encodeClientboundStack(bundle, connection, registries);
        ItemStack nestedClientShield = clientBundle.get(DataComponents.BUNDLE_CONTENTS)
                .itemCopyStream().findFirst().orElseThrow();
        helper.assertTrue(
                nestedClientShield.get(DataComponents.CUSTOM_DATA).copyTag().contains(PolymerItemUtils.POLYMER_STACK),
                "the real bundle packet codec converts its nested shield through Polymer"
        );
        helper.assertTrue(nestedClientShield.get(DataComponents.BASE_COLOR) == null, "bundled shield base is codec safe");
        helper.assertValueEqual(
                nestedClientShield.get(DataComponents.BANNER_PATTERNS).layers().getFirst()
                        .pattern().unwrapKey().orElseThrow().identifier(),
                DyeDepot.modLoc("polymer_base_maroon"),
                "bundled shield receives the exact visual base"
        );
        helper.assertValueEqual(
                bundle.get(DataComponents.BUNDLE_CONTENTS).itemCopyStream().findFirst().orElseThrow()
                        .get(DataComponents.BASE_COLOR),
                color,
                "clientbound bundle serialization does not mutate its server-side contents"
        );
        ItemStack restoredBundledShield = decodeServerboundStack(clientBundle, connection, registries)
                .get(DataComponents.BUNDLE_CONTENTS).itemCopyStream().findFirst().orElseThrow();
        helper.assertValueEqual(restoredBundledShield.get(DataComponents.BASE_COLOR), color, "bundle packet round trip restores exact base");
        helper.assertValueEqual(restoredBundledShield.get(DataComponents.BANNER_PATTERNS), authoredPatterns, "bundle packet round trip restores authored patterns");

        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(List.of(serverShield)));
        ItemStack clientCrossbow = encodeClientboundStack(crossbow, connection, registries);
        ItemStack clientProjectile = clientCrossbow.get(DataComponents.CHARGED_PROJECTILES).itemCopies().getFirst();
        helper.assertTrue(clientProjectile.get(DataComponents.BASE_COLOR) == null, "charged-projectile shield base is codec safe");
        helper.assertTrue(
                clientProjectile.get(DataComponents.CUSTOM_DATA).copyTag().contains(PolymerItemUtils.POLYMER_STACK),
                "the generic nested ItemStackTemplate codec also converts charged projectiles"
        );
        ItemStack restoredProjectile = decodeServerboundStack(clientCrossbow, connection, registries)
                .get(DataComponents.CHARGED_PROJECTILES).itemCopies().getFirst();
        helper.assertValueEqual(restoredProjectile.get(DataComponents.BASE_COLOR), color, "projectile packet round trip restores exact base");
        helper.assertValueEqual(restoredProjectile.get(DataComponents.BANNER_PATTERNS), authoredPatterns, "projectile round trip restores authored patterns");

        ItemStack vanillaShield = new ItemStack(Items.SHIELD);
        vanillaShield.set(DataComponents.BASE_COLOR, net.minecraft.world.item.DyeColor.BROWN);
        ItemStack vanillaClientShield = PolymerItemUtils.getPolymerItemStack(vanillaShield, context, registries);
        helper.assertTrue(vanillaClientShield == vanillaShield, "ordinary vanilla shield bypasses unnecessary Polymer wrapping");
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void everyBannerPatternSurvivesEveryCustomShieldBase(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var registries = helper.getLevel().registryAccess();
        var connection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        var context = connection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, registries);
        context.set(PacketContextImpl.SERVER_INSTANCE, helper.getLevel().getServer());
        context.set(PacketContextImpl.GAME_PROFILE, player.getGameProfile());

        var authoredPatternTypes = registries.lookupOrThrow(Registries.BANNER_PATTERN)
                .listElements()
                .filter(pattern -> {
                    var id = pattern.unwrapKey().orElseThrow().identifier();
                    return !DyeDepot.MOD_ID.equals(id.getNamespace())
                            || !id.getPath().startsWith("polymer_base_");
                })
                .toList();
        helper.assertTrue(!authoredPatternTypes.isEmpty(), "the live registry exposes authored banner patterns");

        for (DDDyes baseEntry : DDDyes.values()) {
            DyeColor base = baseEntry.get();
            for (int index = 0; index < authoredPatternTypes.size(); index++) {
                var pattern = authoredPatternTypes.get(index);
                DyeColor authoredColor = DDDyes.values()[index % DDDyes.values().length].get();
                var authored = new BannerPatternLayers.Builder().add(pattern, authoredColor).build();
                ItemStack serverShield = new ItemStack(Items.SHIELD);
                serverShield.set(DataComponents.BASE_COLOR, base);
                serverShield.set(DataComponents.BANNER_PATTERNS, authored);

                ItemStack clientShield = PolymerItemUtils.getPolymerItemStack(serverShield, context, registries);
                var clientLayers = clientShield.getOrDefault(
                        DataComponents.BANNER_PATTERNS,
                        BannerPatternLayers.EMPTY
                ).layers();
                String caseName = base.getName() + '/' + pattern.unwrapKey().orElseThrow().identifier();
                helper.assertValueEqual(clientLayers.size(), 2, caseName + " retains its base and authored layer");
                helper.assertValueEqual(
                        clientLayers.getFirst().pattern().unwrapKey().orElseThrow().identifier(),
                        DDPolymerBannerBases.patternId(base),
                        caseName + " receives the exact custom shield base"
                );
                helper.assertTrue(
                        clientLayers.get(1).pattern().unwrapKey().isEmpty(),
                        caseName + " uses a client-only inline authored pattern"
                );
                helper.assertValueEqual(
                        clientLayers.get(1).pattern().value().assetId(),
                        DDPolymerBannerPatterns.visualAssetId(pattern.value().assetId(), authoredColor),
                        caseName + " retains the authored mask with its exact custom palette"
                );
                helper.assertValueEqual(
                        clientLayers.get(1).pattern().value().translationKey(),
                        pattern.value().translationKey(),
                        caseName + " retains the authored pattern translation"
                );
                helper.assertValueEqual(
                        clientLayers.get(1).color(),
                        DyeColor.WHITE,
                        caseName + " applies the generated exact-color mask with a safe white multiplier"
                );
                helper.assertValueEqual(
                        clientShield.get(DataComponents.ITEM_NAME),
                        Component.translatable("item.minecraft.shield." + base.getName()),
                        caseName + " retains the translated custom shield name"
                );

                ItemStack restored = PolymerItemUtils.getRealItemStack(clientShield, context, registries);
                helper.assertValueEqual(restored.get(DataComponents.BASE_COLOR), base, caseName + " restores its exact base");
                helper.assertValueEqual(
                        restored.get(DataComponents.BANNER_PATTERNS),
                        authored,
                        caseName + " restores its exact authored pattern and color"
                );
                helper.assertValueEqual(
                        serverShield.get(DataComponents.BANNER_PATTERNS),
                        authored,
                        caseName + " never mutates the authoritative server shield"
                );
            }
        }
        helper.succeed();
    }

    @GameTest
    public void everyLiveBannerPatternStreamsWithEveryExactCustomColor(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var authoredPatternTypes = registries.lookupOrThrow(Registries.BANNER_PATTERN)
                .listElements()
                .filter(pattern -> {
                    var id = pattern.unwrapKey().orElseThrow().identifier();
                    return !DyeDepot.MOD_ID.equals(id.getNamespace())
                            || !id.getPath().startsWith("polymer_base_");
                })
                .toList();
        helper.assertValueEqual(authoredPatternTypes.size(), 43, "26.2 exposes all 43 authored vanilla banner masks");

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            for (var pattern : authoredPatternTypes) {
                for (DDDyes dye : DDDyes.values()) {
                    DyeColor exactColor = dye.get();
                    var source = new BannerPatternLayers.Layer(pattern, exactColor);
                    var visual = DDPolymerBannerPatterns.visualize(source);
                    String caseName = pattern.unwrapKey().orElseThrow().identifier() + "/" + dye.getName();

                    helper.assertTrue(visual.pattern().unwrapKey().isEmpty(), caseName + " is encoded inline");
                    helper.assertValueEqual(
                            visual.pattern().value().assetId(),
                            DDPolymerBannerPatterns.visualAssetId(pattern.value().assetId(), exactColor),
                            caseName + " selects its generated exact-color mask"
                    );
                    helper.assertValueEqual(
                            visual.pattern().value().translationKey(),
                            pattern.value().translationKey(),
                            caseName + " retains its original description family"
                    );
                    helper.assertValueEqual(visual.color(), DyeColor.WHITE, caseName + " uses a client-safe multiplier");

                    buffer.clear();
                    BannerPatternLayers.STREAM_CODEC.encode(buffer, new BannerPatternLayers(List.of(visual)));
                    BannerPatternLayers decoded = BannerPatternLayers.STREAM_CODEC.decode(buffer);
                    var decodedLayer = decoded.layers().getFirst();
                    helper.assertTrue(decodedLayer.pattern().unwrapKey().isEmpty(), caseName + " remains inline after streaming");
                    helper.assertValueEqual(decodedLayer.pattern().value(), visual.pattern().value(),
                            caseName + " preserves the inline asset and translation over the real stream codec");
                    helper.assertValueEqual(decodedLayer.color(), DyeColor.WHITE,
                            caseName + " never streams an extended DyeColor ID");
                    helper.assertValueEqual(source.color(), exactColor,
                            caseName + " does not mutate the authoritative pattern layer");
                }
            }
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest
    public void loomAcceptsServerDyesAndVanillaTypedPolymerCarriersForEveryCustomColor(GameTestHelper helper) {
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        var context = new Connection(PacketFlow.CLIENTBOUND).getPacketContext();
        var loom = new LoomMenu(0, player.getInventory());

        for (DDDyes entry : DDDyes.values()) {
            var color = entry.get();

            Item customDye = DDItems.DYES.getOrThrow(color);
            helper.assertTrue(
                    loom.getDyeSlot().mayPlace(new ItemStack(customDye)),
                    color.getName() + " custom dye is accepted by the server Loom dye slot"
            );
            Object dyeSynced = PolymerSyncedObject.getSyncedObject(BuiltInRegistries.ITEM, customDye);
            helper.assertTrue(dyeSynced instanceof PolymerItem, color.getName() + " dye has a Polymer overlay");
            Item dyeCarrier = ((PolymerItem) dyeSynced).getPolymerItem(new ItemStack(customDye), context);
            helper.assertTrue(
                    loom.getDyeSlot().mayPlace(new ItemStack(dyeCarrier)),
                    color.getName() + " dye carrier is accepted by the Loom dye slot"
            );

            Item customBanner = DDItems.BANNERS.getOrThrow(color);
            Object bannerSynced = PolymerSyncedObject.getSyncedObject(BuiltInRegistries.ITEM, customBanner);
            helper.assertTrue(bannerSynced instanceof PolymerItem, color.getName() + " banner has a Polymer overlay");
            Item bannerCarrier = ((PolymerItem) bannerSynced).getPolymerItem(new ItemStack(customBanner), context);
            helper.assertTrue(
                    bannerCarrier instanceof BannerItem,
                    color.getName() + " banner carrier is a BannerItem for Loom UI type safety"
            );
            helper.assertTrue(
                    loom.getBannerSlot().mayPlace(new ItemStack(bannerCarrier)),
                    color.getName() + " banner carrier is accepted by the Loom banner slot"
            );
        }

        loom.removed(player);
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void loomAppliesEveryCustomDyeToARealBannerResult(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var base = DDDyes.TAN.get();

        for (DDDyes entry : DDDyes.values()) {
            DyeColor color = entry.get();
            var loom = new LoomMenu(0, player.getInventory());
            ItemStack banner = new ItemStack(DDItems.BANNERS.getOrThrow(base));
            ItemStack dye = new ItemStack(DDItems.DYES.getOrThrow(color));
            loom.getBannerSlot().set(banner);
            loom.getDyeSlot().set(dye);

            helper.assertTrue(
                    !loom.getSelectablePatterns().isEmpty(),
                    color.getName() + " custom dye exposes the normal no-item Loom patterns"
            );
            var selectedPattern = loom.getSelectablePatterns().getFirst();
            helper.assertTrue(
                    loom.clickMenuButton(player, 0),
                    color.getName() + " custom dye selects a real Loom pattern"
            );
            ItemStack result = loom.getResultSlot().getItem();
            helper.assertTrue(!result.isEmpty(), color.getName() + " custom dye creates a Loom result");
            helper.assertValueEqual(
                    result.getItem(),
                    banner.getItem(),
                    color.getName() + " Loom result retains the custom banner base item"
            );
            var layers = result.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers();
            helper.assertValueEqual(layers.size(), 1, color.getName() + " Loom result adds exactly one pattern");
            helper.assertValueEqual(
                    layers.getFirst().pattern(),
                    selectedPattern,
                    color.getName() + " Loom result retains the selected pattern type"
            );
            helper.assertValueEqual(
                    layers.getFirst().color(),
                    color,
                    color.getName() + " Loom result stores the exact custom dye color"
            );

            loom.getResultSlot().onTake(player, result.copy());
            helper.assertTrue(
                    loom.getBannerSlot().getItem().isEmpty() && loom.getDyeSlot().getItem().isEmpty(),
                    color.getName() + " Loom take consumes one banner and one custom dye"
            );
            loom.removed(player);
        }
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void loomRetainsAllSixAuthoredPatternSlotsForCustomBanners(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var registries = helper.getLevel().registryAccess();
        var pattern = registries.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(BannerPatterns.CROSS);
        var color = DDDyes.MAROON.get();
        var builder = new BannerPatternLayers.Builder();
        for (int index = 0; index < 5; index++) {
            builder.add(pattern, color);
        }
        var authoredPatterns = builder.build();
        var serverStack = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        serverStack.set(DataComponents.BANNER_PATTERNS, authoredPatterns);
        player.getInventory().setItem(0, serverStack.copy());
        var loom = new LoomMenu(17, player.getInventory());
        loom.getBannerSlot().set(serverStack);
        ItemStack loomInput = loom.getBannerSlot().getItem();

        int inventoryMenuSlot = java.util.stream.IntStream.range(0, loom.slots.size())
                .filter(index -> loom.getSlot(index).container == player.getInventory()
                        && loom.getSlot(index).getContainerSlot() == 0)
                .findFirst()
                .orElseThrow();
        var initialPacket = new AtomicReference<ClientboundContainerSetContentPacket>();
        var bannerSlotPacket = new AtomicReference<ClientboundContainerSetSlotPacket>();
        var inventorySlotPacket = new AtomicReference<ClientboundContainerSetSlotPacket>();
        var playerConnection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        var context = playerConnection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, registries);
        context.set(PacketContextImpl.SERVER_INSTANCE, helper.getLevel().getServer());
        context.set(PacketContextImpl.GAME_PROFILE, player.getGameProfile());
        loom.setSynchronizer(new ContainerSynchronizer() {
            @Override
            public void sendInitialData(
                    AbstractContainerMenu container,
                    List<ItemStack> slotItems,
                    ItemStack carried,
                    int[] dataSlots
            ) {
                initialPacket.set(new ClientboundContainerSetContentPacket(
                        container.containerId,
                        container.incrementStateId(),
                        slotItems,
                        carried
                ));
            }

            @Override
            public void sendSlotChange(AbstractContainerMenu container, int slotIndex, ItemStack itemStack) {
                var packet = new ClientboundContainerSetSlotPacket(
                        container.containerId,
                        container.incrementStateId(),
                        slotIndex,
                        itemStack
                );
                if (container.getSlot(slotIndex) == loom.getBannerSlot()) {
                    bannerSlotPacket.set(packet);
                } else if (slotIndex == inventoryMenuSlot) {
                    inventorySlotPacket.set(packet);
                }
            }

            @Override
            public void sendCarriedChange(AbstractContainerMenu container, ItemStack itemStack) {
            }

            @Override
            public void sendDataChange(AbstractContainerMenu container, int id, int value) {
            }

            @Override
            public RemoteSlot createSlot() {
                return new RemoteSlot() {
                    @Override
                    public void force(ItemStack outgoing) {
                    }

                    @Override
                    public void receive(HashedStack incoming) {
                    }

                    @Override
                    public boolean matches(ItemStack local) {
                        return false;
                    }
                };
            }
        });
        // ServerPlayer.openMenu sends initial contents before assigning the
        // newly opened menu. A provider-prepopulated Loom must still omit the
        // synthetic sixth layer from its actual banner-slot packet copy.
        var decodedInitial = encodeClientboundContainerContent(initialPacket.get(), playerConnection, registries);
        player.containerMenu = loom;
        loom.broadcastChanges();

        var decodedBannerSlot = encodeClientboundContainerSlot(bannerSlotPacket.get(), playerConnection, registries);
        var decodedInventorySlot = encodeClientboundContainerSlot(inventorySlotPacket.get(), playerConnection, registries);
        ItemStack clientStack = decodedInitial.items().getFirst();

        var clientPatterns = clientStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        helper.assertValueEqual(
                clientPatterns.layers().size(),
                5,
                "five authored layers remain five client layers so the Loom offers a sixth"
        );
        helper.assertTrue(
                clientPatterns.layers().stream().noneMatch(layer -> layer.pattern().unwrapKey()
                        .map(key -> key.identifier().getNamespace().equals(DyeDepot.MOD_ID))
                        .orElse(false)),
                "the Loom-capacity fallback omits the synthetic visual base"
        );
        helper.assertValueEqual(
                loomInput.get(DataComponents.BANNER_PATTERNS),
                authoredPatterns,
                "Loom-safe client conversion does not mutate authored server patterns"
        );

        helper.assertValueEqual(
                decodedBannerSlot.getItem().getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers().size(),
                5,
                "the copied ClientboundContainerSetSlot Loom input still exposes the sixth authored slot"
        );
        ItemStack identicalInventoryCopy = decodedInitial.items().get(inventoryMenuSlot);
        helper.assertValueEqual(
                identicalInventoryCopy.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers().size(),
                6,
                "an identical inventory copy in the same content packet retains its exact visual base"
        );
        helper.assertValueEqual(
                decodedInventorySlot.getItem().getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers().size(),
                6,
                "an identical inventory copy in a real slot packet retains its exact visual base"
        );

        player.containerMenu = player.inventoryMenu;
        ItemStack normalClientStack = PolymerItemUtils.getPolymerItemStack(loomInput.copy(), context, registries);
        helper.assertValueEqual(
                normalClientStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers().size(),
                6,
                "the same five-pattern banner keeps its exact synthetic base outside the Loom input slot"
        );
        helper.assertValueEqual(
                normalClientStack.get(DataComponents.BANNER_PATTERNS).layers().getFirst()
                        .pattern().unwrapKey().orElseThrow().identifier(),
                DyeDepot.modLoc("polymer_base_maroon"),
                "normal five-pattern item icons retain the exact custom base"
        );
        loom.removed(player);
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void loomCorrectsClientPredictedSyntheticBaseHashAtFivePatterns(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var registries = helper.getLevel().registryAccess();
        var pattern = registries.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(BannerPatterns.CROSS);
        var color = DDDyes.MAROON.get();
        var builder = new BannerPatternLayers.Builder();
        for (int index = 0; index < 5; index++) {
            builder.add(pattern, color);
        }
        var authoredPatterns = builder.build();
        var serverStack = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        serverStack.set(DataComponents.BANNER_PATTERNS, authoredPatterns);

        var loom = new LoomMenu(18, player.getInventory());
        loom.getBannerSlot().set(serverStack);
        int bannerSlotIndex = java.util.stream.IntStream.range(0, loom.slots.size())
                .filter(index -> loom.getSlot(index) == loom.getBannerSlot())
                .findFirst()
                .orElseThrow();

        var playerConnection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        var context = playerConnection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, registries);
        context.set(PacketContextImpl.SERVER_INSTANCE, helper.getLevel().getServer());
        context.set(PacketContextImpl.GAME_PROFILE, player.getGameProfile());
        var hashOps = registries.createSerializationContext(HashOps.CRC32C_INSTANCE);
        HashedPatchMap.HashGenerator hasher = component -> component.encodeValue(hashOps)
                .getOrThrow(message -> new IllegalArgumentException("Failed to hash " + component + ": " + message))
                .asInt();

        ItemStack clientPrediction = PolymerItemUtils.getPolymerItemStack(serverStack.copy(), context, registries);
        var predictedPatterns = clientPrediction.getOrDefault(
                DataComponents.BANNER_PATTERNS,
                BannerPatternLayers.EMPTY
        );
        helper.assertValueEqual(
                predictedPatterns.layers().size(),
                6,
                "a normal client prediction contains the exact synthetic base plus five authored layers"
        );
        helper.assertValueEqual(
                predictedPatterns.layers().getFirst().pattern().unwrapKey().orElseThrow().identifier(),
                DyeDepot.modLoc("polymer_base_maroon"),
                "the predicted sixth visible layer is the synthetic custom-color base"
        );

        var bannerSlotPacket = new AtomicReference<ClientboundContainerSetSlotPacket>();
        loom.setSynchronizer(new ContainerSynchronizer() {
            @Override
            public void sendInitialData(
                    AbstractContainerMenu container,
                    List<ItemStack> slotItems,
                    ItemStack carried,
                    int[] dataSlots
            ) {
            }

            @Override
            public void sendSlotChange(AbstractContainerMenu container, int slotIndex, ItemStack itemStack) {
                if (container.getSlot(slotIndex) == loom.getBannerSlot()) {
                    bannerSlotPacket.set(new ClientboundContainerSetSlotPacket(
                            container.containerId,
                            container.incrementStateId(),
                            slotIndex,
                            itemStack
                    ));
                }
            }

            @Override
            public void sendCarriedChange(AbstractContainerMenu container, ItemStack itemStack) {
            }

            @Override
            public void sendDataChange(AbstractContainerMenu container, int id, int value) {
            }

            @Override
            public RemoteSlot createSlot() {
                var remote = new RemoteSlot.Synchronized(hasher);
                ((GenericPlayerContext) (Object) remote).polymer$setPlayer(player);
                return remote;
            }
        });
        player.containerMenu = loom;
        bannerSlotPacket.set(null);

        // A real client predicts the ordinary six-layer Polymer item before
        // the Loom-specific five-layer correction reaches it. Polymer hashes
        // the authoritative stack through that same ordinary representation,
        // so the Loom slot must force a corrective packet despite this hash.
        loom.setRemoteSlotUnsafe(
                bannerSlotIndex,
                HashedStack.create(clientPrediction, hasher)
        );
        loom.broadcastChanges();

        ClientboundContainerSetSlotPacket correction = bannerSlotPacket.get();
        helper.assertTrue(
                correction != null,
                "the matching six-visible-layer client hash cannot suppress the Loom slot correction"
        );
        var decodedCorrection = encodeClientboundContainerSlot(correction, playerConnection, registries);
        var correctedPatterns = decodedCorrection.getItem().getOrDefault(
                DataComponents.BANNER_PATTERNS,
                BannerPatternLayers.EMPTY
        );
        helper.assertValueEqual(
                correctedPatterns.layers().size(),
                5,
                "the corrective Loom packet exposes exactly five authored layers"
        );
        helper.assertTrue(
                correctedPatterns.layers().stream().noneMatch(layer -> layer.pattern().unwrapKey()
                        .map(key -> key.identifier().getNamespace().equals(DyeDepot.MOD_ID))
                        .orElse(false)),
                "the corrective Loom packet omits the synthetic custom-color base"
        );
        helper.assertValueEqual(
                loom.getBannerSlot().getItem().get(DataComponents.BANNER_PATTERNS),
                authoredPatterns,
                "remote hash correction does not mutate authoritative banner patterns"
        );

        player.containerMenu = player.inventoryMenu;
        loom.removed(player);
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void placedBannersUseNativeCarriersAndExactOutboundBaseLayers(GameTestHelper helper) {
        var level = helper.getLevel();
        var registries = level.registryAccess();
        var color = DDDyes.MAROON.get();
        var pattern = registries.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(BannerPatterns.CROSS);
        var patterns = new BannerPatternLayers.Builder().add(pattern, color).build();

        BlockPos standingRelative = new BlockPos(1, 2, 1);
        BlockPos standingPos = helper.absolutePos(standingRelative);
        var standingState = DDBlocks.BANNERS.getOrThrow(color).defaultBlockState();
        helper.setBlock(standingRelative, standingState);
        var standingEntity = (BannerBlockEntity) level.getBlockEntity(standingPos);
        helper.assertValueEqual(
                DDPolymerBlocks.polymerState(standingState).getBlock(),
                Blocks.BANNER.pick(DDPolymerColors.vanillaColor(color)),
                "standing banners use a native targetable banner carrier"
        );
        helper.assertTrue(
                BlockBoundAttachment.get(level, standingPos) == null,
                "native banner rendering does not allocate a duplicate display entity"
        );

        var patternedStanding = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        patternedStanding.set(DataComponents.BANNER_PATTERNS, patterns);
        standingEntity.applyComponentsFromItemStack(patternedStanding);
        standingEntity.setChanged();
        helper.assertValueEqual(
                standingEntity.getPatterns(),
                patterns,
                "standing banner block entity accepts the added pattern layers"
        );
        CompoundTag standingTag = standingEntity.getUpdateTag(registries);
        var standingLayers = (net.minecraft.nbt.ListTag) standingTag.get("patterns");
        helper.assertValueEqual(standingLayers.size(), 2, "outbound standing banner keeps base plus authored layer");
        helper.assertValueEqual(
                ((CompoundTag) standingLayers.getFirst()).getStringOr("pattern", ""),
                "dye_depot:polymer_base_maroon",
                "standing banner update tag starts with its exact visual base"
        );
        CompoundTag standingOutbound = DDPolymerBlockEntityNbt.sanitize(
                BlockEntityTypes.BANNER,
                standingTag,
                registries
        );
        var standingOutboundLayers = (net.minecraft.nbt.ListTag) standingOutbound.get("patterns");
        var exactAuthored = (CompoundTag) standingOutboundLayers.get(1);
        helper.assertValueEqual(
                exactAuthored.getStringOr("color", ""),
                DyeColor.WHITE.getName(),
                "placed custom pattern streams with a vanilla-safe white multiplier"
        );
        var inlinePattern = (CompoundTag) exactAuthored.get("pattern");
        helper.assertValueEqual(
                inlinePattern.getStringOr("asset_id", ""),
                "minecraft:cross_dye_depot_maroon",
                "placed custom pattern selects its generated exact-color mask"
        );
        helper.assertValueEqual(
                inlinePattern.getStringOr("translation_key", ""),
                "block.minecraft.banner.cross",
                "placed custom pattern keeps its exact tooltip identity"
        );
        helper.assertValueEqual(
                standingEntity.getPatterns(),
                patterns,
                "client-only visual base never mutates server pattern data"
        );

        BlockPos wallRelative = new BlockPos(3, 2, 1);
        BlockPos wallPos = helper.absolutePos(wallRelative);
        var wallState = DDBlocks.WALL_BANNERS.getOrThrow(color).defaultBlockState();
        helper.setBlock(wallRelative, wallState);
        var wallEntity = (BannerBlockEntity) level.getBlockEntity(wallPos);
        wallEntity.applyComponentsFromItemStack(patternedStanding);
        wallEntity.setChanged();
        helper.assertValueEqual(
                DDPolymerBlocks.polymerState(wallState).getBlock(),
                Blocks.WALL_BANNER.pick(DDPolymerColors.vanillaColor(color)),
                "wall banners use a native wall-banner carrier"
        );
        helper.assertValueEqual(
                ((CompoundTag) ((net.minecraft.nbt.ListTag) wallEntity.getUpdateTag(registries).get("patterns"))
                        .getFirst()).getStringOr("pattern", ""),
                "dye_depot:polymer_base_maroon",
                "wall banner update tag starts with the same exact visual base"
        );
        helper.assertTrue(
                BlockBoundAttachment.get(level, wallPos) == null,
                "wall banners also avoid duplicate display entities"
        );
        helper.succeed();
    }

    @GameTest
    public void bannerPatternLookupNeverLoadsAnAbsentChunk(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos absentPos = helper.absolutePos(new BlockPos(4096, 2, 4096));
        int chunkX = absentPos.getX() >> 4;
        int chunkZ = absentPos.getZ() >> 4;

        helper.assertTrue(
                level.getChunkSource().getChunkNow(chunkX, chunkZ) == null,
                "regression target starts outside every loaded chunk"
        );
        helper.assertValueEqual(
                DDPolymerBlocks.StateDisplayHolder.loadedBannerPatterns(level, absentPos),
                BannerPatternLayers.EMPTY,
                "an unloaded banner position has no visible patterns"
        );
        helper.assertTrue(
                level.getChunkSource().getChunkNow(chunkX, chunkZ) == null,
                "banner pattern inspection must not synchronously load its chunk"
        );
        helper.succeed();
    }

    @GameTest
    public void initialChunkPacketSanitizesCustomSignText(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos relative = new BlockPos(1, 2, 1);
        BlockPos pos = helper.absolutePos(relative);
        helper.setBlock(relative, Blocks.OAK_SIGN.defaultBlockState());
        var sign = (SignBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(sign != null, "test sign has a block entity");

        DyeColor exact = DDDyes.MINT.get();
        SignText text = new SignText()
                .setMessage(0, Component.literal("exact chunk text"))
                .setColor(exact)
                .setHasGlowingText(true);
        sign.setText(text, true);
        sign.setText(text, false);
        CompoundTag serverSnapshot = sign.getUpdateTag(level.registryAccess()).copy();

        var player = helper.makeMockServerPlayerInLevel();
        var connection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        var context = connection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, level.registryAccess());
        context.set(PacketContextImpl.SERVER_INSTANCE, level.getServer());
        context.set(PacketContextImpl.GAME_PROFILE, player.getGameProfile());
        var chunk = level.getChunkAt(pos);
        var packetData = PacketContext.supplyWithContext(
                connection,
                () -> new ClientboundLevelChunkPacketData(chunk)
        );

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        ClientboundLevelChunkPacketData decoded;
        try {
            PacketContext.supplyWithContext(connection, () -> {
                packetData.write(buffer);
                return null;
            });
            var chunkPos = chunk.getPos();
            decoded = new ClientboundLevelChunkPacketData(buffer, chunkPos.x(), chunkPos.z());
        } finally {
            buffer.release();
        }

        AtomicReference<CompoundTag> found = new AtomicReference<>();
        var chunkPos = chunk.getPos();
        decoded.getBlockEntitiesTagsConsumer(chunkPos.x(), chunkPos.z()).accept((blockPos, type, tag) -> {
            if (blockPos.equals(pos) && type == BlockEntityTypes.SIGN) {
                found.set(tag);
            }
        });
        CompoundTag outbound = found.get();
        helper.assertTrue(outbound != null, "decoded chunk retains the test sign NBT");
        var front = (CompoundTag) outbound.get("front_text");
        DyeColor carrier = DyeColor.byName(front.getStringOr("color", ""), null);
        helper.assertTrue(
                carrier != null && carrier.getId() < 16,
                "chunk sign color is vanilla-codec safe"
        );

        var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        SignText decodedText = SignText.DIRECT_CODEC.parse(ops, front).getOrThrow();
        helper.assertValueEqual(
                decodedText.getMessage(0, false).getStyle().getColor().getValue(),
                exact.getTextColor() & 0xffffff,
                "chunk sign message carries exact custom glowing RGB"
        );
        helper.assertTrue(decodedText.hasGlowingText(), "glow state survives chunk serialization");
        helper.assertValueEqual(
                sign.getUpdateTag(level.registryAccess()),
                serverSnapshot,
                "chunk packet conversion never mutates server sign NBT"
        );
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void shulkerDisplayTracksNativeLidAnimationCollisionAndLighting(GameTestHelper helper) {
        var level = helper.getLevel();
        var color = DDDyes.ROSE.get();
        BlockPos relative = new BlockPos(1, 2, 1);
        BlockPos pos = helper.absolutePos(relative);
        var state = DDBlocks.SHULKER_BOXES.getOrThrow(color).defaultBlockState();
        helper.setBlock(relative, state);

        var entity = (ShulkerBoxBlockEntity) level.getBlockEntity(pos);
        var attachment = BlockBoundAttachment.get(level, pos);
        helper.assertTrue(attachment != null, "custom shulker has a live Polymer block attachment");
        var holder = attachment.holder();
        var display = (ItemDisplayElement) holder.getElements().getFirst();
        var lid = (ItemDisplayElement) holder.getElements().get(1);
        helper.assertValueEqual(
                display.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/rose_shulker_base"),
                "shulker base uses the exact split shell model"
        );
        helper.assertValueEqual(
                lid.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/rose_shulker_lid"),
                "shulker lid uses the independently animated shell model"
        );

        var player = helper.makeMockServerPlayerInLevel();
        var connection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        var context = connection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, level.registryAccess());
        context.set(PacketContextImpl.SERVER_INSTANCE, level.getServer());
        context.set(PacketContextImpl.GAME_PROFILE, player.getGameProfile());
        holder.startWatching(player);

        entity.triggerEvent(1, 1);
        for (int tick = 0; tick < 5; tick++) {
            ShulkerBoxBlockEntity.tick(level, pos, state, entity);
            holder.tick();
        }
        helper.assertTrue(Math.abs(lid.getTranslation().y() - 0.25f) < 0.001f, "half-open lid reaches half its travel");
        helper.assertValueEqual(lid.getInterpolationDuration(), 1, "lid interpolates every server-tick transform on the client");
        helper.assertTrue(entity.getBoundingBox(state).maxY > 1.0, "opening lid expands the authoritative collision box");
        helper.assertTrue(display.getBrightness() != null, "shulker display uses surrounding light instead of sampling inside its opaque carrier");

        for (int tick = 5; tick < 10; tick++) {
            ShulkerBoxBlockEntity.tick(level, pos, state, entity);
            holder.tick();
        }
        helper.assertTrue(Math.abs(lid.getTranslation().y() - 0.5f) < 0.001f, "fully open lid reaches vanilla eight-pixel travel");

        entity.triggerEvent(1, 0);
        for (int tick = 0; tick < 10; tick++) {
            ShulkerBoxBlockEntity.tick(level, pos, state, entity);
            holder.tick();
        }
        helper.assertTrue(Math.abs(lid.getTranslation().y()) < 0.001f, "closing shulker returns its lid to the closed position");
        holder.destroy();
        helper.succeed();
    }

    @GameTest
    public void adjacentPaneDisplaysCompensateItemModelOrientation(GameTestHelper helper) {
        var level = helper.getLevel();
        var pane = DDBlocks.STAINED_GLASS_PANES.getOrThrow(DDDyes.MAROON.get());
        BlockPos leftRelative = new BlockPos(1, 2, 1);
        BlockPos rightRelative = leftRelative.east();
        helper.setBlock(leftRelative, pane.defaultBlockState());
        helper.setBlock(rightRelative, pane.defaultBlockState());

        var leftHolder = BlockBoundAttachment.get(level, helper.absolutePos(leftRelative)).holder();
        var rightHolder = BlockBoundAttachment.get(level, helper.absolutePos(rightRelative)).holder();
        var left = (ItemDisplayElement) leftHolder.getElements().getFirst();
        var right = (ItemDisplayElement) rightHolder.getElements().getFirst();
        helper.assertValueEqual(
                left.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/maroon_pane_2"),
                "left pane selects its east-connected model"
        );
        helper.assertValueEqual(
                right.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/maroon_pane_8"),
                "right pane selects its west-connected model"
        );
        helper.assertTrue(Math.abs(left.getYaw() - 180.0f) < 0.001f, "left pane compensates item-display orientation");
        helper.assertTrue(Math.abs(right.getYaw() - 180.0f) < 0.001f, "right pane compensates item-display orientation");

        leftHolder.destroy();
        rightHolder.destroy();
        helper.succeed();
    }

    @GameTest
    public void brownShulkerDonorHasVisibleAnimatedOverlay(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos relative = new BlockPos(1, 2, 1);
        helper.setBlock(relative, Blocks.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.BROWN));
        var holder = BlockBoundAttachment.get(level, helper.absolutePos(relative)).holder();
        helper.assertValueEqual(holder.getElements().size(), 2, "brown donor has restored base and lid displays");
        var base = (ItemDisplayElement) holder.getElements().getFirst();
        var lid = (ItemDisplayElement) holder.getElements().get(1);
        helper.assertValueEqual(
                base.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/donor_brown_shulker_base"),
                "brown donor base uses its restored texture"
        );
        helper.assertValueEqual(
                lid.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/donor_brown_shulker_lid"),
                "brown donor lid uses its restored texture"
        );
        holder.destroy();
        helper.succeed();
    }

    @GameTest
    public void candleDonorUsesNativeParticlesWhileCandleCakeUsesServerParticles(GameTestHelper helper) {
        Map<Integer, List<Vec3>> expected = Map.of(
                1, List.of(new Vec3(8 / 16.0, 8 / 16.0, 8 / 16.0)),
                2, List.of(new Vec3(6 / 16.0, 7 / 16.0, 8 / 16.0), new Vec3(10 / 16.0, 8 / 16.0, 7 / 16.0)),
                3, List.of(
                        new Vec3(8 / 16.0, 5 / 16.0, 10 / 16.0),
                        new Vec3(6 / 16.0, 7 / 16.0, 8 / 16.0),
                        new Vec3(9 / 16.0, 8 / 16.0, 7 / 16.0)
                ),
                4, List.of(
                        new Vec3(7 / 16.0, 5 / 16.0, 9 / 16.0),
                        new Vec3(10 / 16.0, 7 / 16.0, 9 / 16.0),
                        new Vec3(6 / 16.0, 7 / 16.0, 6 / 16.0),
                        new Vec3(9 / 16.0, 8 / 16.0, 6 / 16.0)
                )
        );
        var candle = DDBlocks.CANDLES.getOrThrow(DDDyes.MAROON.get());
        for (int count = 1; count <= 4; count++) {
            var unlit = candle.defaultBlockState()
                    .setValue(CandleBlock.CANDLES, count)
                    .setValue(CandleBlock.LIT, false);
            var lit = unlit.setValue(CandleBlock.LIT, true);
            var overlay = BlockWithElementHolder.get(lit);
            helper.assertTrue(
                    !overlay.tickElementHolder(helper.getLevel(), BlockPos.ZERO, unlit),
                    count + " unlit candle relies on its native donor state and does not server-tick particles"
            );
            helper.assertTrue(
                    !overlay.tickElementHolder(helper.getLevel(), BlockPos.ZERO, lit),
                    count + " lit candle relies on native donor particles without duplicates"
            );
            helper.assertValueEqual(
                    DDPolymerBlocks.candleParticleOffsets(lit),
                    expected.get(count),
                    count + " candle flame offsets"
            );
            var holder = overlay.createElementHolder(helper.getLevel(), BlockPos.ZERO, lit);
            var display = (ItemDisplayElement) holder.getElements().getFirst();
            helper.assertTrue(
                    display.getBrightness() == null,
                    count + " lit candle display samples world light instead of forcing 15/15"
            );
            holder.destroy();
        }

        var cake = DDBlocks.CANDLE_CAKES.getOrThrow(DDDyes.MAROON.get()).defaultBlockState();
        var litCake = cake.setValue(CandleCakeBlock.LIT, true);
        var cakeOverlay = BlockWithElementHolder.get(litCake);
        helper.assertTrue(
                cakeOverlay.tickElementHolder(helper.getLevel(), BlockPos.ZERO, cake),
                "initially unlit candle cake remains tick-enabled for a later lit transition"
        );
        helper.assertTrue(
                cakeOverlay.tickElementHolder(helper.getLevel(), BlockPos.ZERO, litCake),
                "lit candle-cake display ticks"
        );
        helper.assertValueEqual(
                DDPolymerBlocks.candleParticleOffsets(litCake),
                List.of(new Vec3(8 / 16.0, 16 / 16.0, 8 / 16.0)),
                "candle-cake flame offset"
        );
        var cakeHolder = cakeOverlay.createElementHolder(helper.getLevel(), BlockPos.ZERO, litCake);
        var cakeDisplay = (ItemDisplayElement) cakeHolder.getElements().getFirst();
        helper.assertTrue(
                cakeDisplay.getBrightness() == null,
                "lit candle-cake display samples world light instead of forcing 15/15"
        );
        cakeHolder.destroy();
        helper.succeed();
    }

    @GameTest
    public void customDyesColorCatAndWolfWhileOutboundCollarsRemainCodecSafe(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        Cat cat = helper.spawn(EntityTypes.CAT, new BlockPos(1, 2, 1));
        cat.tame(player);
        Wolf wolf = helper.spawn(EntityTypes.WOLF, new BlockPos(3, 2, 1));
        wolf.tame(player);

        for (DDDyes entry : DDDyes.values()) {
            DyeColor color = entry.get();
            player.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    new ItemStack(DDItems.DYES.getOrThrow(color))
            );
            helper.assertTrue(
                    cat.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                    color.getName() + " dye interaction colors an owned cat collar"
            );
            helper.assertValueEqual(
                    cat.getCollarColor(),
                    color,
                    color.getName() + " remains exact on the server cat"
            );
            var originalCatVariant = cat.getEntityData().get(CatDataAccessor.dyeDepot$getVariantData());
            var catData = new ArrayList<>(Optional.ofNullable(cat.getEntityData().getNonDefaultValues()).orElseThrow());
            var catTameData = catData.stream()
                    .filter(value -> value.value() instanceof Byte flags && (flags & 4) != 0)
                    .findFirst()
                    .orElseThrow();
            PolymerEntity.get(cat).modifyRawTrackedData(catData, player, true);
            helper.assertValueEqual(
                    catData.stream()
                            .filter(value -> value.id() == CatDataAccessor.dyeDepot$getCollarData().id())
                            .findFirst()
                            .orElseThrow()
                            .value(),
                    DDPolymerEntities.vanillaCollarData(color.getId()),
                    color.getName() + " cat collar packet uses the transparent protocol-safe donor"
            );
            var clientCatVariantData = catData.stream()
                    .filter(value -> value.id() == CatDataAccessor.dyeDepot$getVariantData().id())
                    .findFirst()
                    .orElseThrow();
            helper.assertTrue(clientCatVariantData.value() instanceof Holder<?>, "cat packet has a holder variant");
            Holder<?> clientCatVariant = (Holder<?>) clientCatVariantData.value();
            helper.assertValueEqual(
                    clientCatVariant.unwrapKey().orElseThrow().identifier(),
                    DDPolymerEntities.collarVariantId(
                            "cat",
                            originalCatVariant.unwrapKey().orElseThrow(),
                            color
                    ),
                    color.getName() + " cat packet selects the exact synthetic collar variant"
            );
            helper.assertValueEqual(
                    catData.stream()
                            .filter(value -> value.id() == catTameData.id())
                            .findFirst()
                            .orElseThrow()
                            .value(),
                    catTameData.value(),
                    color.getName() + " cat packet preserves the native tame bit"
            );
            helper.assertValueEqual(
                    cat.getCollarColor(),
                    color,
                    color.getName() + " cat packet conversion does not mutate server state"
            );
            helper.assertValueEqual(
                    cat.getEntityData().get(CatDataAccessor.dyeDepot$getVariantData()),
                    originalCatVariant,
                    color.getName() + " cat packet conversion does not mutate the server variant"
            );

            player.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    new ItemStack(DDItems.DYES.getOrThrow(color))
            );
            helper.assertTrue(
                    wolf.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                    color.getName() + " dye interaction colors an owned wolf collar"
            );
            helper.assertValueEqual(
                    wolf.getCollarColor(),
                    color,
                    color.getName() + " remains exact on the server wolf"
            );
            var originalWolfVariant = wolf.getEntityData().get(WolfDataAccessor.dyeDepot$getVariantData());
            var wolfData = new ArrayList<>(Optional.ofNullable(wolf.getEntityData().getNonDefaultValues()).orElseThrow());
            var wolfTameData = wolfData.stream()
                    .filter(value -> value.value() instanceof Byte flags && (flags & 4) != 0)
                    .findFirst()
                    .orElseThrow();
            PolymerEntity.get(wolf).modifyRawTrackedData(wolfData, player, true);
            helper.assertValueEqual(
                    wolfData.stream()
                            .filter(value -> value.id() == WolfDataAccessor.dyeDepot$getCollarData().id())
                            .findFirst()
                            .orElseThrow()
                            .value(),
                    DDPolymerEntities.vanillaCollarData(color.getId()),
                    color.getName() + " wolf collar packet uses the transparent protocol-safe donor"
            );
            var clientWolfVariantData = wolfData.stream()
                    .filter(value -> value.id() == WolfDataAccessor.dyeDepot$getVariantData().id())
                    .findFirst()
                    .orElseThrow();
            helper.assertTrue(clientWolfVariantData.value() instanceof Holder<?>, "wolf packet has a holder variant");
            Holder<?> clientWolfVariant = (Holder<?>) clientWolfVariantData.value();
            helper.assertValueEqual(
                    clientWolfVariant.unwrapKey().orElseThrow().identifier(),
                    DDPolymerEntities.collarVariantId(
                            "wolf",
                            originalWolfVariant.unwrapKey().orElseThrow(),
                            color
                    ),
                    color.getName() + " wolf packet selects the exact synthetic collar variant"
            );
            helper.assertValueEqual(
                    wolfData.stream()
                            .filter(value -> value.id() == wolfTameData.id())
                            .findFirst()
                            .orElseThrow()
                            .value(),
                    wolfTameData.value(),
                    color.getName() + " wolf packet preserves the native tame bit"
            );
            helper.assertValueEqual(
                    wolf.getCollarColor(),
                    color,
                    color.getName() + " wolf packet conversion does not mutate server state"
            );
            helper.assertValueEqual(
                    wolf.getEntityData().get(WolfDataAccessor.dyeDepot$getVariantData()),
                    originalWolfVariant,
                    color.getName() + " wolf packet conversion does not mutate the server variant"
            );
        }

        cat.getEntityData().packDirty();
        DyeColor incrementalCatColor = DDDyes.MAROON.get();
        cat.getEntityData().set(CatDataAccessor.dyeDepot$getCollarData(), incrementalCatColor.getId());
        var catCollarOnly = new ArrayList<>(Optional.ofNullable(cat.getEntityData().packDirty()).orElseThrow());
        helper.assertTrue(
                catCollarOnly.stream().anyMatch(value -> value.id() == CatDataAccessor.dyeDepot$getCollarData().id())
                        && catCollarOnly.stream().noneMatch(value -> value.id() == CatDataAccessor.dyeDepot$getVariantData().id()),
                "the authoritative cat collar transition does not dirty its body variant"
        );
        PolymerEntity.get(cat).modifyRawTrackedData(catCollarOnly, player, false);
        Holder<?> incrementalCatVariant = (Holder<?>) catCollarOnly.stream()
                .filter(value -> value.id() == CatDataAccessor.dyeDepot$getVariantData().id())
                .findFirst()
                .orElseThrow()
                .value();
        helper.assertValueEqual(
                incrementalCatVariant.unwrapKey().orElseThrow().identifier(),
                DDPolymerEntities.collarVariantId(
                        "cat",
                        cat.getEntityData().get(CatDataAccessor.dyeDepot$getVariantData()).unwrapKey().orElseThrow(),
                        incrementalCatColor
                ),
                "a collar-only cat update appends the exact synthetic body variant"
        );

        wolf.getEntityData().packDirty();
        DyeColor incrementalWolfColor = DDDyes.ROSE.get();
        wolf.getEntityData().set(WolfDataAccessor.dyeDepot$getCollarData(), incrementalWolfColor.getId());
        var wolfCollarOnly = new ArrayList<>(Optional.ofNullable(wolf.getEntityData().packDirty()).orElseThrow());
        helper.assertTrue(
                wolfCollarOnly.stream().anyMatch(value -> value.id() == WolfDataAccessor.dyeDepot$getCollarData().id())
                        && wolfCollarOnly.stream().noneMatch(value -> value.id() == WolfDataAccessor.dyeDepot$getVariantData().id()),
                "the authoritative wolf collar transition does not dirty its body variant"
        );
        PolymerEntity.get(wolf).modifyRawTrackedData(wolfCollarOnly, player, false);
        Holder<?> incrementalWolfVariant = (Holder<?>) wolfCollarOnly.stream()
                .filter(value -> value.id() == WolfDataAccessor.dyeDepot$getVariantData().id())
                .findFirst()
                .orElseThrow()
                .value();
        helper.assertValueEqual(
                incrementalWolfVariant.unwrapKey().orElseThrow().identifier(),
                DDPolymerEntities.collarVariantId(
                        "wolf",
                        wolf.getEntityData().get(WolfDataAccessor.dyeDepot$getVariantData()).unwrapKey().orElseThrow(),
                        incrementalWolfColor
                ),
                "a collar-only wolf update appends the exact synthetic body variant"
        );

        cat.setTame(false, false);
        var catUntameData = new ArrayList<>(Optional.ofNullable(cat.getEntityData().packDirty()).orElseThrow());
        var catUntameFlags = catUntameData.stream()
                .filter(value -> value.value() instanceof Byte)
                .findFirst()
                .orElseThrow();
        helper.assertTrue(((Byte) catUntameFlags.value() & 4) == 0, "cat untame transition clears the native tame bit");
        PolymerEntity.get(cat).modifyRawTrackedData(catUntameData, player, false);
        helper.assertValueEqual(
                catUntameData.stream().filter(value -> value.id() == catUntameFlags.id()).findFirst().orElseThrow().value(),
                catUntameFlags.value(),
                "cat untame packet retains the native flag byte"
        );
        helper.assertValueEqual(
                catUntameData.stream()
                        .filter(value -> value.id() == CatDataAccessor.dyeDepot$getVariantData().id())
                        .findFirst()
                        .orElseThrow()
                        .value(),
                cat.getEntityData().get(CatDataAccessor.dyeDepot$getVariantData()),
                "an untamed cat switches its client copy back to the original body variant"
        );
        cat.setTame(true, false);
        var catRetameData = new ArrayList<>(Optional.ofNullable(cat.getEntityData().packDirty()).orElseThrow());
        var catRetameFlags = catRetameData.stream()
                .filter(value -> value.value() instanceof Byte)
                .findFirst()
                .orElseThrow();
        PolymerEntity.get(cat).modifyRawTrackedData(catRetameData, player, false);
        helper.assertValueEqual(
                catRetameData.stream().filter(value -> value.id() == catRetameFlags.id()).findFirst().orElseThrow().value(),
                catRetameFlags.value(),
                "cat retame packet retains the native flag byte"
        );
        helper.assertValueEqual(
                ((Holder<?>) catRetameData.stream()
                                .filter(value -> value.id() == CatDataAccessor.dyeDepot$getVariantData().id())
                                .findFirst()
                                .orElseThrow()
                                .value())
                        .unwrapKey().orElseThrow().identifier(),
                DDPolymerEntities.collarVariantId(
                        "cat",
                        cat.getEntityData().get(CatDataAccessor.dyeDepot$getVariantData()).unwrapKey().orElseThrow(),
                        incrementalCatColor
                ),
                "a retamed cat switches its client copy back to the exact synthetic variant"
        );

        wolf.setTame(false, false);
        var wolfUntameData = new ArrayList<>(Optional.ofNullable(wolf.getEntityData().packDirty()).orElseThrow());
        var wolfUntameFlags = wolfUntameData.stream()
                .filter(value -> value.value() instanceof Byte)
                .findFirst()
                .orElseThrow();
        helper.assertTrue(((Byte) wolfUntameFlags.value() & 4) == 0, "wolf untame transition clears the native tame bit");
        PolymerEntity.get(wolf).modifyRawTrackedData(wolfUntameData, player, false);
        helper.assertValueEqual(
                wolfUntameData.stream().filter(value -> value.id() == wolfUntameFlags.id()).findFirst().orElseThrow().value(),
                wolfUntameFlags.value(),
                "wolf untame packet retains the native flag byte"
        );
        helper.assertValueEqual(
                wolfUntameData.stream()
                        .filter(value -> value.id() == WolfDataAccessor.dyeDepot$getVariantData().id())
                        .findFirst()
                        .orElseThrow()
                        .value(),
                wolf.getEntityData().get(WolfDataAccessor.dyeDepot$getVariantData()),
                "an untamed wolf switches its client copy back to the original body variant"
        );
        wolf.setTame(true, false);
        var wolfRetameData = new ArrayList<>(Optional.ofNullable(wolf.getEntityData().packDirty()).orElseThrow());
        var wolfRetameFlags = wolfRetameData.stream()
                .filter(value -> value.value() instanceof Byte)
                .findFirst()
                .orElseThrow();
        PolymerEntity.get(wolf).modifyRawTrackedData(wolfRetameData, player, false);
        helper.assertValueEqual(
                wolfRetameData.stream().filter(value -> value.id() == wolfRetameFlags.id()).findFirst().orElseThrow().value(),
                wolfRetameFlags.value(),
                "wolf retame packet retains the native flag byte"
        );
        helper.assertValueEqual(
                ((Holder<?>) wolfRetameData.stream()
                                .filter(value -> value.id() == WolfDataAccessor.dyeDepot$getVariantData().id())
                                .findFirst()
                                .orElseThrow()
                                .value())
                        .unwrapKey().orElseThrow().identifier(),
                DDPolymerEntities.collarVariantId(
                        "wolf",
                        wolf.getEntityData().get(WolfDataAccessor.dyeDepot$getVariantData()).unwrapKey().orElseThrow(),
                        incrementalWolfColor
                ),
                "a retamed wolf switches its client copy back to the exact synthetic variant"
        );

        var keyedCatVariant = cat.getEntityData().get(CatDataAccessor.dyeDepot$getVariantData());
        var directCatVariant = Holder.direct(keyedCatVariant.value());
        cat.getEntityData().set(CatDataAccessor.dyeDepot$getVariantData(), directCatVariant);
        var directCatData = new ArrayList<SynchedEntityData.DataValue<?>>();
        directCatData.add(SynchedEntityData.DataValue.create(
                CatDataAccessor.dyeDepot$getVariantData(),
                directCatVariant
        ));
        PolymerEntity.get(cat).modifyRawTrackedData(directCatData, player, false);
        helper.assertValueEqual(
                directCatData.getFirst().value(),
                directCatVariant,
                "an unkeyed third-party cat body passes through instead of crashing"
        );
        cat.getEntityData().set(CatDataAccessor.dyeDepot$getVariantData(), keyedCatVariant);

        var keyedWolfVariant = wolf.getEntityData().get(WolfDataAccessor.dyeDepot$getVariantData());
        var directWolfVariant = Holder.direct(keyedWolfVariant.value());
        wolf.getEntityData().set(WolfDataAccessor.dyeDepot$getVariantData(), directWolfVariant);
        var directWolfData = new ArrayList<SynchedEntityData.DataValue<?>>();
        directWolfData.add(SynchedEntityData.DataValue.create(
                WolfDataAccessor.dyeDepot$getVariantData(),
                directWolfVariant
        ));
        PolymerEntity.get(wolf).modifyRawTrackedData(directWolfData, player, false);
        helper.assertValueEqual(
                directWolfData.getFirst().value(),
                directWolfVariant,
                "an unkeyed third-party wolf body passes through instead of crashing"
        );
        wolf.getEntityData().set(WolfDataAccessor.dyeDepot$getVariantData(), keyedWolfVariant);

        helper.assertTrue(
                !DDPolymerCollarPack.supportsVariant("cat", DyeDepot.modLoc("third_party"))
                        && !DDPolymerCollarPack.supportsVariant("wolf", DyeDepot.modLoc("third_party")),
                "namespaced third-party variants are never remapped to nonexistent synthetic entries"
        );
        helper.succeed();
    }

    @GameTest
    public void sheepUndercoatAllocationRemainsLazy(GameTestHelper helper) {
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, new BlockPos(1, 2, 1));
        PolymerEntity overlay = PolymerEntity.get(sheep);

        if (DDPolymerSheepShaderPack.isEnabled()) {
            var transports = new java.util.HashSet<String>();
            for (DyeColor color : DyeColor.values()) {
                for (boolean sheared : List.of(false, true)) {
                    int shaderClass = DDPolymerSheepShaderPack.shaderClass(color, sheared);
                    DyeColor donor = DDPolymerSheepShaderPack.donorColor(color, sheared);
                    transports.add(donor.getId() + ":" + shaderClass);
                    helper.assertTrue(donor.getId() < 16, color.getName() + " has a codec-safe donor");
                    if (color.getId() >= 16 && sheared) {
                        helper.assertTrue(
                                donor != DyeColor.WHITE,
                                color.getName() + " sheared transport keeps the native undercoat render call"
                        );
                    }
                    for (double scale : List.of(0.0625, 0.5, 1.0, 1.25, 4.0, 16.0)) {
                        double encoded = DDPolymerSheepShaderPack.encodeScale(scale, shaderClass);
                        helper.assertValueEqual(
                                DDPolymerSheepShaderPack.decodeScaleClass(encoded),
                                shaderClass,
                                color.getName() + " scale residue round trip at " + scale
                        );
                        helper.assertTrue(
                                Math.abs(encoded / scale - 1.0) <= 0.022,
                                color.getName() + " client-only scale residue remains below 2.2%"
                        );
                    }
                }
            }
            helper.assertValueEqual(
                    transports.size(),
                    48,
                    "donor plus residue class uniquely transports 16 vanilla and 32 custom coat states"
            );
            sheep.setColor(DDDyes.MAROON.get());
            helper.assertTrue(
                    DDPolymerEntities.sheepWoolHolder(overlay) == null,
                    "enabled native shader mode creates no sheep display entities"
            );
            helper.succeed();
            return;
        }

        helper.assertTrue(
                DDPolymerEntities.sheepWoolHolder(overlay) == null,
                "ordinary vanilla sheep allocate no virtual display entities"
        );

        sheep.setColor(DDDyes.MAROON.get());
        sheep.setBaby(true);
        sheep.setSheared(true);
        helper.assertTrue(
                DDPolymerEntities.sheepWoolHolder(overlay) == null,
                "a sheared custom baby allocates no virtual parts because 26.2 has no baby undercoat"
        );

        sheep.setBaby(false);
        ElementHolder visibleHolder = DDPolymerEntities.sheepWoolHolder(overlay);
        helper.assertTrue(visibleHolder != null, "a sheared custom adult lazily creates the exact undercoat");
        helper.assertValueEqual(visibleHolder.getElements().size(), 6, "visible custom undercoat part count");
        var undercoatParts = visibleHolder.getElements().stream().map(ItemDisplayElement.class::cast).toList();
        helper.assertValueEqual(
                undercoatParts.get(0).getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/sheep/adult_undercoat_head"),
                "sheared adult selects the head undercoat model"
        );
        helper.assertTrue(
                undercoatParts.subList(2, 6).stream()
                        .map(part -> part.getItem().get(DataComponents.ITEM_MODEL))
                        .toList()
                        .equals(List.of(
                                DyeDepot.modLoc("polymer/sheep/adult_undercoat_right_leg"),
                                DyeDepot.modLoc("polymer/sheep/adult_undercoat_leg"),
                                DyeDepot.modLoc("polymer/sheep/adult_undercoat_right_leg"),
                                DyeDepot.modLoc("polymer/sheep/adult_undercoat_leg")
                        )),
                "all four sheared adult legs retain their exact mirrored or unmirrored undercoat"
        );
        HolderAttachment visibleAttachment = visibleHolder.getAttachment();

        sheep.setInvisible(true);
        visibleHolder.tick();
        helper.assertTrue(
                visibleAttachment != null && visibleAttachment.isRemoved(),
                "invisibility destroys the virtual attachment instead of continuously posing hidden parts"
        );
        helper.assertTrue(
                DDPolymerEntities.sheepWoolHolder(overlay) == null,
                "an invisible custom sheep retains no display holder"
        );

        sheep.setInvisible(false);
        ElementHolder restoredHolder = DDPolymerEntities.sheepWoolHolder(overlay);
        helper.assertTrue(restoredHolder != null, "visibility restoration lazily recreates the custom undercoat");
        HolderAttachment restoredAttachment = restoredHolder.getAttachment();

        sheep.setColor(DyeColor.WHITE);
        restoredHolder.tick();
        helper.assertTrue(
                restoredAttachment != null && restoredAttachment.isRemoved(),
                "recoloring to vanilla removes the custom display attachment"
        );
        helper.assertTrue(
                DDPolymerEntities.sheepWoolHolder(overlay) == null,
                "a sheep recolored to vanilla returns to zero virtual parts"
        );
        helper.succeed();
    }

    @GameTest
    public void jebSheepUsesExactThirtyTwoColorVirtualCycle(GameTestHelper helper) {
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, new BlockPos(1, 2, 1));
        sheep.setColor(DyeColor.WHITE);
        sheep.setCustomName(Component.literal("jeb_"));
        PolymerEntity overlay = PolymerEntity.get(sheep);

        if (DDPolymerSheepShaderPack.isEnabled()) {
            var nameAccessor = EntityDataAccessors.dyeDepot$getCustomNameData();
            var nameUpdate = new ArrayList<SynchedEntityData.DataValue<?>>();
            nameUpdate.add(SynchedEntityData.DataValue.create(
                    nameAccessor,
                    Optional.of(Component.literal("jeb_"))
            ));
            overlay.modifyRawTrackedData(nameUpdate, helper.makeMockServerPlayerInLevel(), false);
            @SuppressWarnings("unchecked")
            Optional<Component> clientName = (Optional<Component>) nameUpdate.getFirst().value();
            helper.assertValueEqual(clientName.orElseThrow().getString(), "jeb_", "native shader keeps the magic name exact");
            helper.assertValueEqual(sheep.getCustomName().getString(), "jeb_", "server magic name remains exact");
            helper.assertValueEqual(
                    DDPolymerSheepShaderPack.shaderClass(sheep),
                    DDPolymerSheepShaderPack.VANILLA_CLASS,
                    "experimental jeb sheep retains vanilla's native renderer and animation"
            );
            helper.assertTrue(
                    DDPolymerEntities.sheepWoolHolder(overlay) == null,
                    "native jeb sheep creates no virtual wool parts"
            );
            helper.succeed();
            return;
        }

        var nameAccessor = EntityDataAccessors.dyeDepot$getCustomNameData();
        var woolAccessor = SheepDataAccessor.dyeDepot$getWoolData();
        var nameUpdate = new ArrayList<SynchedEntityData.DataValue<?>>();
        nameUpdate.add(SynchedEntityData.DataValue.create(
                nameAccessor,
                Optional.of(Component.literal("jeb_"))
        ));
        overlay.modifyRawTrackedData(nameUpdate, helper.makeMockServerPlayerInLevel(), false);

        @SuppressWarnings("unchecked")
        Optional<Component> clientName = (Optional<Component>) nameUpdate.getFirst().value();
        helper.assertValueEqual(
                clientName.orElseThrow().getString(),
                "jeb_\u200C",
                "client copy appends vanilla's zero-width non-joiner to suppress its 16-color magic renderer"
        );
        helper.assertValueEqual(sheep.getCustomName().getString(), "jeb_", "server magic name remains exact");
        SynchedEntityData.DataValue<?> proxyWool = nameUpdate.stream()
                .filter(value -> value.id() == woolAccessor.id())
                .findFirst()
                .orElseThrow();
        helper.assertValueEqual(
                proxyWool.value(),
                (byte) (DyeColor.WHITE.getId() | 16),
                "jeb name update also hides native outer wool and unconditional adult undercoat"
        );

        ElementHolder holder = DDPolymerEntities.sheepWoolHolder(overlay);
        helper.assertTrue(holder != null, "a vanilla-base jeb sheep lazily receives the virtual 32-color coat");
        ItemDisplayElement head = (ItemDisplayElement) holder.getElements().getFirst();

        sheep.tickCount = 400;
        holder.tick();
        helper.assertValueEqual(
                head.getItem().get(DataComponents.DYED_COLOR),
                new DyedItemColor(nativeSheepTint(DDDyes.MAROON.get()) & 0xFFFFFF),
                "tick 400 enters the first extended custom color"
        );

        sheep.tickCount = 412;
        holder.tick();
        int customMidpoint = ARGB.srgbLerp(
                12.0f / 25.0f,
                nativeSheepTint(DDDyes.MAROON.get()),
                nativeSheepTint(DDDyes.ROSE.get())
        ) & 0xFFFFFF;
        helper.assertValueEqual(
                head.getItem().get(DataComponents.DYED_COLOR),
                new DyedItemColor(customMidpoint),
                "custom palette transition uses the native 25-tick sRGB interpolation"
        );

        sheep.tickCount = 425;
        holder.tick();
        helper.assertValueEqual(
                head.getItem().get(DataComponents.DYED_COLOR),
                new DyedItemColor(nativeSheepTint(DDDyes.ROSE.get()) & 0xFFFFFF),
                "the cycle reaches the next extended custom color after 25 ticks"
        );

        sheep.setCustomName(Component.literal("ordinary"));
        var ordinaryNameUpdate = new ArrayList<SynchedEntityData.DataValue<?>>();
        ordinaryNameUpdate.add(SynchedEntityData.DataValue.create(
                nameAccessor,
                Optional.of(Component.literal("ordinary"))
        ));
        HolderAttachment jebAttachment = holder.getAttachment();
        overlay.modifyRawTrackedData(ordinaryNameUpdate, helper.makeMockServerPlayerInLevel(), false);
        helper.assertTrue(
                jebAttachment != null && jebAttachment.isRemoved(),
                "renaming a vanilla-base jeb sheep releases its virtual attachment"
        );
        SynchedEntityData.DataValue<?> restoredWool = ordinaryNameUpdate.stream()
                .filter(value -> value.id() == woolAccessor.id())
                .findFirst()
                .orElseThrow();
        helper.assertValueEqual(
                restoredWool.value(),
                (byte) DyeColor.WHITE.getId(),
                "renaming away from jeb restores the native unsheared proxy"
        );
        helper.succeed();
    }

    private static int nativeSheepTint(DyeColor color) {
        if (color == DyeColor.WHITE) {
            return 0xFFE6E6E6;
        }
        int rgb = color.getTextureDiffuseColor();
        return ARGB.color(
                255,
                (int) (((rgb >> 16) & 0xFF) * 0.75f),
                (int) (((rgb >> 8) & 0xFF) * 0.75f),
                (int) ((rgb & 0xFF) * 0.75f)
        );
    }

    private static ItemStack encodeClientboundStack(ItemStack stack, Connection connection, RegistryAccess registries) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            PacketContext.supplyWithContext(connection, () -> {
                ItemStack.STREAM_CODEC.encode(buffer, stack);
                return null;
            });
            return ItemStack.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private static ClientboundContainerSetContentPacket encodeClientboundContainerContent(
            ClientboundContainerSetContentPacket packet,
            Connection connection,
            RegistryAccess registries
    ) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            PacketContext.supplyWithContext(connection, () -> {
                ClientboundContainerSetContentPacket.STREAM_CODEC.encode(buffer, packet);
                return null;
            });
            return ClientboundContainerSetContentPacket.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private static ClientboundContainerSetSlotPacket encodeClientboundContainerSlot(
            ClientboundContainerSetSlotPacket packet,
            Connection connection,
            RegistryAccess registries
    ) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            PacketContext.supplyWithContext(connection, () -> {
                ClientboundContainerSetSlotPacket.STREAM_CODEC.encode(buffer, packet);
                return null;
            });
            return ClientboundContainerSetSlotPacket.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private static ClientboundUpdateAttributesPacket encodeClientboundAttributes(
            ClientboundUpdateAttributesPacket packet,
            Connection connection,
            RegistryAccess registries
    ) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            PacketContext.supplyWithContext(connection, () -> {
                ClientboundUpdateAttributesPacket.STREAM_CODEC.encode(buffer, packet);
                return null;
            });
            return ClientboundUpdateAttributesPacket.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private static ItemStack decodeServerboundStack(ItemStack stack, Connection connection, RegistryAccess registries) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            PacketContext.supplyWithContext(new Connection(PacketFlow.CLIENTBOUND), () -> {
                ItemStack.STREAM_CODEC.encode(buffer, stack);
                return null;
            });
            return PacketContext.supplyWithContext(connection, () -> ItemStack.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @GameTest
    public void sheepMetadataUpdateImmediatelyCarriesAnEntityBoundShaderScale(GameTestHelper helper) {
        if (!DDPolymerSheepShaderPack.isEnabled()) {
            helper.succeed();
            return;
        }

        Sheep sheep = helper.spawn(EntityTypes.SHEEP, new BlockPos(1, 2, 1));
        PolymerEntity overlay = PolymerEntity.get(sheep);
        var player = helper.makeMockServerPlayerInLevel();
        var registries = helper.getLevel().registryAccess();
        var connection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        var context = connection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, registries);
        context.set(PacketContextImpl.SERVER_INSTANCE, helper.getLevel().getServer());
        context.set(PacketContextImpl.GAME_PROFILE, player.getGameProfile());
        var woolAccessor = SheepDataAccessor.dyeDepot$getWoolData();
        var emitted = new ArrayList<Packet<?>>();
        PlayerBoundConsumer<Packet<?>> sender = PlayerBoundConsumer.createPacketFor(
                Set.of(player.connection),
                sheep,
                emitted::add
        );

        for (DyeColor color : List.of(DDDyes.MAROON.get(), DDDyes.TAN.get())) {
            for (boolean sheared : List.of(false, true)) {
                sheep.setColor(color);
                sheep.setSheared(sheared);
                emitted.clear();
                sender.accept(new ClientboundSetEntityDataPacket(
                        sheep.getId(),
                        List.of(SynchedEntityData.DataValue.create(
                                woolAccessor,
                                sheep.getEntityData().get(woolAccessor)
                        ))
                ));

                helper.assertValueEqual(
                        emitted.size(),
                        2,
                        color.getName() + " metadata is immediately followed by one scale update"
                );
                helper.assertTrue(
                        emitted.getFirst() instanceof ClientboundSetEntityDataPacket,
                        "the original metadata packet remains first"
                );
                helper.assertTrue(
                        emitted.get(1) instanceof ClientboundUpdateAttributesPacket,
                        "the corrective scale packet follows in the same tracker pass"
                );
                helper.assertTrue(
                        PolymerEntityUtils.getEntityContext(emitted.get(1)) == sheep,
                        "Polymer attaches the sheep context to the corrective scale packet"
                );

                var outbound = encodeClientboundAttributes(
                        (ClientboundUpdateAttributesPacket) emitted.get(1),
                        connection,
                        registries
                );
                var scale = outbound.getValues().stream()
                        .filter(snapshot -> snapshot.attribute().equals(Attributes.SCALE))
                        .findFirst()
                        .orElseThrow();
                var residue = scale.modifiers().stream()
                        .filter(modifier -> modifier.id().equals(DyeDepot.modLoc("polymer/sheep_scale_residue")))
                        .findFirst()
                        .orElseThrow();
                double clientScale = scale.base() * (1.0 + residue.amount());
                helper.assertValueEqual(
                        DDPolymerSheepShaderPack.decodeScaleClass(clientScale),
                        DDPolymerSheepShaderPack.shaderClass(color, sheared),
                        color.getName() + " transition reaches its exact unsheared/sheared shader class"
                );
                helper.assertTrue(
                        Float.floatToIntBits(sheep.getScale()) == Float.floatToIntBits(1.0f),
                        "corrective packet leaves authoritative scale and collision unchanged"
                );
            }
        }
        helper.succeed();
    }

    @GameTest
    public void customSheepCoatTracksStateAndCleansUpWithItsEntity(GameTestHelper helper) {
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, new BlockPos(1, 2, 1));
        sheep.setColor(DDDyes.MAROON.get());

        PolymerEntity overlay = PolymerEntity.get(sheep);
        if (DDPolymerSheepShaderPack.isEnabled()) {
            var player = helper.makeMockServerPlayerInLevel();
            var woolAccessor = SheepDataAccessor.dyeDepot$getWoolData();
            for (DyeColor color : List.of(DDDyes.MAROON.get(), DDDyes.TAN.get(), DyeColor.BROWN)) {
                sheep.setColor(color);
                for (boolean sheared : List.of(false, true)) {
                    sheep.setSheared(sheared);
                    byte raw = sheep.getEntityData().get(woolAccessor);
                    var tracked = new ArrayList<SynchedEntityData.DataValue<?>>();
                    tracked.add(SynchedEntityData.DataValue.create(woolAccessor, raw));
                    overlay.modifyRawTrackedData(tracked, player, false);
                    byte clientData = (Byte) tracked.getFirst().value();
                    helper.assertValueEqual(
                            clientData & 15,
                            DDPolymerSheepShaderPack.donorColor(color, sheared).getId(),
                            color.getName() + " client metadata uses its native donor"
                    );
                    helper.assertValueEqual(
                            (clientData & 16) != 0,
                            sheared,
                            color.getName() + " preserves the native sheared state"
                    );
                    helper.assertValueEqual(sheep.getColor(), color, "server keeps the exact authoritative color");

                    double authoritativeScale = sheep.getScale();
                    var attributes = new ArrayList<ClientboundUpdateAttributesPacket.AttributeSnapshot>();
                    var scale = sheep.getAttribute(Attributes.SCALE);
                    attributes.add(new ClientboundUpdateAttributesPacket.AttributeSnapshot(
                            scale.getAttribute(),
                            scale.getBaseValue(),
                            scale.getModifiers()
                    ));
                    overlay.modifyRawEntityAttributeData(attributes, player, true);
                    var snapshot = attributes.getFirst();
                    var residue = snapshot.modifiers().stream()
                            .filter(modifier -> modifier.id().equals(DyeDepot.modLoc("polymer/sheep_scale_residue")))
                            .toList();
                    helper.assertValueEqual(
                            residue.size(),
                            1,
                            color.getName() + " has exactly one packet-only residue"
                    );
                    helper.assertValueEqual(
                            residue.getFirst().operation(),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                            color.getName() + " residue is applied after real scale modifiers"
                    );
                    double clientScale = snapshot.base();
                    for (AttributeModifier modifier : snapshot.modifiers()) {
                        if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                            clientScale *= 1.0 + modifier.amount();
                        }
                    }
                    helper.assertValueEqual(
                            DDPolymerSheepShaderPack.decodeScaleClass(clientScale),
                            DDPolymerSheepShaderPack.shaderClass(sheep),
                            color.getName() + " packet carries the expected shader class"
                    );
                    helper.assertTrue(
                            Float.floatToIntBits(sheep.getScale()) == Float.floatToIntBits((float) authoritativeScale),
                            "packet encoding never changes server collision scale"
                    );
                }
            }
            helper.assertTrue(
                    DDPolymerEntities.sheepWoolHolder(overlay) == null,
                    "native shader path uses the real sheep model without display overlays"
            );
            helper.succeed();
            return;
        }
        ElementHolder holder = DDPolymerEntities.sheepWoolHolder(overlay);
        helper.assertTrue(holder != null, "custom sheep overlay creates a wool element holder");
        helper.assertValueEqual(holder.getElements().size(), 6, "custom sheep uses six independently posed wool parts");

        var parts = holder.getElements().stream().map(ItemDisplayElement.class::cast).toList();
        ItemDisplayElement head = parts.get(0);
        ItemDisplayElement body = parts.get(1);
        ItemDisplayElement rightHindLeg = parts.get(2);
        ItemDisplayElement leftHindLeg = parts.get(3);
        ItemDisplayElement rightFrontLeg = parts.get(4);
        ItemDisplayElement leftFrontLeg = parts.get(5);
        holder.tick();
        helper.assertTrue(
                parts.stream().noneMatch(part -> part.getItem().isEmpty()),
                "unsheared custom sheep displays every articulated coat part"
        );
        helper.assertValueEqual(
                head.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/sheep/adult_head"),
                "adult sheep head uses the authored fur geometry"
        );
        helper.assertValueEqual(
                body.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/sheep/adult_body"),
                "adult sheep body uses the authored fur geometry"
        );
        helper.assertTrue(
                DyeDepot.modLoc("polymer/sheep/adult_right_leg")
                                .equals(rightHindLeg.getItem().get(DataComponents.ITEM_MODEL))
                        && DyeDepot.modLoc("polymer/sheep/adult_right_leg")
                                .equals(rightFrontLeg.getItem().get(DataComponents.ITEM_MODEL)),
                "both right adult legs use the model with the mirrored native undercoat UVs"
        );
        helper.assertTrue(
                DyeDepot.modLoc("polymer/sheep/adult_leg")
                                .equals(leftHindLeg.getItem().get(DataComponents.ITEM_MODEL))
                        && DyeDepot.modLoc("polymer/sheep/adult_leg")
                                .equals(leftFrontLeg.getItem().get(DataComponents.ITEM_MODEL)),
                "both left adult legs use the unmirrored native undercoat UVs"
        );
        helper.assertValueEqual(
                head.getItem().get(DataComponents.DYED_COLOR),
                new DyedItemColor(0x5C1D0E),
                "sheep coat carries the native renderer's exact 75%-scaled custom RGB tint"
        );
        helper.assertTrue(
                Math.abs(head.getTranslation().y() - 1.126f) < 0.001f
                        && Math.abs(head.getTranslation().z() - 0.5f) < 0.001f,
                "adult head uses the native SheepFurModel pivot"
        );
        helper.assertTrue(
                Math.abs(body.getTranslation().y() - 1.1885f) < 0.001f
                        && Math.abs(body.getTranslation().z() + 0.125f) < 0.001f,
                "adult body uses the native SheepFurModel pivot"
        );
        helper.assertTrue(
                Math.abs(rightHindLeg.getTranslation().x() + 0.1875f) < 0.001f
                        && Math.abs(rightHindLeg.getTranslation().z() + 0.4375f) < 0.001f,
                "adult hind leg uses its native articulated pivot"
        );

        sheep.yBodyRot = 37.0f;
        sheep.yHeadRot = 57.0f;
        sheep.walkAnimation.update(1.0f, 1.0f, 1.0f);
        sheep.handleEntityEvent((byte) 10);
        float idleHeadY = head.getTranslation().y();
        holder.tick();
        helper.assertTrue(
                Math.abs(head.getYaw() - 37.0f) < 0.001f && Math.abs(body.getYaw() - 37.0f) < 0.001f,
                "all wool parts follow native body yaw"
        );
        helper.assertTrue(
                Math.abs(head.getLeftRotation().y()) > 0.01f,
                "wool head follows native head yaw"
        );
        helper.assertTrue(
                head.getTranslation().y() < idleHeadY,
                "wool head follows the native eating offset"
        );
        helper.assertTrue(
                Math.abs(rightHindLeg.getLeftRotation().x()) > 0.01f,
                "wool legs follow the native walk animation"
        );

        sheep.setSheared(true);
        HolderAttachment shearedAttachment = holder.getAttachment();
        holder.tick();
        helper.assertTrue(
                shearedAttachment != null && !shearedAttachment.isRemoved(),
                "shearing retains the six visible adult undercoat parts"
        );
        helper.assertTrue(
                DDPolymerEntities.sheepWoolHolder(overlay) == holder,
                "a sheared custom adult keeps its existing lazy attachment"
        );
        helper.assertValueEqual(
                head.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/sheep/adult_undercoat_head"),
                "shearing switches the head to its exact undercoat-only model"
        );
        helper.assertValueEqual(
                body.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/sheep/adult_undercoat_body"),
                "shearing switches the body to its exact undercoat-only model"
        );
        helper.assertTrue(
                DyeDepot.modLoc("polymer/sheep/adult_undercoat_right_leg")
                                .equals(rightHindLeg.getItem().get(DataComponents.ITEM_MODEL))
                        && DyeDepot.modLoc("polymer/sheep/adult_undercoat_right_leg")
                                .equals(rightFrontLeg.getItem().get(DataComponents.ITEM_MODEL)),
                "sheared right adult legs retain the native mirrored undercoat UVs"
        );
        helper.assertTrue(
                DyeDepot.modLoc("polymer/sheep/adult_undercoat_leg")
                                .equals(leftHindLeg.getItem().get(DataComponents.ITEM_MODEL))
                        && DyeDepot.modLoc("polymer/sheep/adult_undercoat_leg")
                                .equals(leftFrontLeg.getItem().get(DataComponents.ITEM_MODEL)),
                "sheared left adult legs retain the native unmirrored undercoat UVs"
        );

        sheep.setSheared(false);
        holder.tick();
        helper.assertTrue(
                parts.stream().noneMatch(part -> part.getItem().isEmpty()),
                "wool regrowth restores every virtual wool part"
        );
        helper.assertValueEqual(
                head.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/sheep/adult_head"),
                "wool regrowth restores the composite undercoat and outer-wool model"
        );

        sheep.setInvisible(true);
        HolderAttachment invisibleAttachment = holder.getAttachment();
        holder.tick();
        helper.assertTrue(
                invisibleAttachment != null && invisibleAttachment.isRemoved(),
                "entity invisibility destroys the virtual coat"
        );
        helper.assertTrue(
                DDPolymerEntities.sheepWoolHolder(overlay) == null,
                "an invisible custom sheep has no virtual holder"
        );
        sheep.setInvisible(false);
        sheep.setBaby(true);
        holder = DDPolymerEntities.sheepWoolHolder(overlay);
        helper.assertTrue(holder != null, "visible baby transition recreates the virtual coat");
        parts = holder.getElements().stream().map(ItemDisplayElement.class::cast).toList();
        head = parts.get(0);
        body = parts.get(1);
        holder.tick();
        helper.assertValueEqual(
                head.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/sheep/baby_head"),
                "baby transition selects the separately authored head geometry"
        );
        helper.assertValueEqual(
                body.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/sheep/baby_body"),
                "baby transition selects the separately authored body geometry"
        );
        helper.assertValueEqual(
                parts.subList(2, 6).stream()
                        .map(part -> part.getItem().get(DataComponents.ITEM_MODEL))
                        .toList(),
                List.of(
                        DyeDepot.modLoc("polymer/sheep/baby_right_hind_leg"),
                        DyeDepot.modLoc("polymer/sheep/baby_left_hind_leg"),
                        DyeDepot.modLoc("polymer/sheep/baby_right_front_leg"),
                        DyeDepot.modLoc("polymer/sheep/baby_left_front_leg")
                ),
                "baby transition preserves all four authored leg UV layouts"
        );
        float babyEatOffset = sheep.getHeadEatPositionScale(1.0f) * 9.0f * sheep.getAgeScale();
        float expectedBabyHeadY = 1.501f - (15.5f + babyEatOffset) / 16.0f;
        helper.assertTrue(
                Math.abs(head.getTranslation().y() - expectedBabyHeadY) < 0.001f
                        && Math.abs(body.getTranslation().y() - 0.4385f) < 0.001f,
                "baby coat uses native BabySheepModel pivots plus the current eating offset instead of scaling the adult coat"
        );

        sheep.setSheared(true);
        HolderAttachment shearedBabyAttachment = holder.getAttachment();
        holder.tick();
        helper.assertTrue(
                shearedBabyAttachment != null && shearedBabyAttachment.isRemoved(),
                "a sheared baby releases all virtual parts because 26.2 renders no baby undercoat"
        );
        helper.assertTrue(
                DDPolymerEntities.sheepWoolHolder(overlay) == null,
                "a sheared custom baby retains no empty attachment"
        );
        sheep.setSheared(false);
        holder = DDPolymerEntities.sheepWoolHolder(overlay);
        helper.assertTrue(holder != null, "baby wool regrowth lazily recreates the coat");

        sheep.setHealth(0.0f);
        HolderAttachment deathAttachment = holder.getAttachment();
        holder.tick();
        helper.assertTrue(
                deathAttachment != null && deathAttachment.isRemoved(),
                "death removes the upright virtual parts while the native sheep performs its death roll"
        );
        helper.assertTrue(
                DDPolymerEntities.sheepWoolHolder(overlay) == null,
                "a dead custom sheep retains no virtual holder"
        );

        Sheep removedSheep = helper.spawn(EntityTypes.SHEEP, new BlockPos(3, 2, 1));
        removedSheep.setColor(DDDyes.MAROON.get());
        ElementHolder removedHolder = DDPolymerEntities.sheepWoolHolder(PolymerEntity.get(removedSheep));
        helper.assertTrue(removedHolder != null, "second custom sheep creates a coat for removal coverage");
        HolderAttachment attachment = removedHolder.getAttachment();
        helper.assertTrue(attachment != null && !attachment.isRemoved(), "wool holder starts attached");
        removedSheep.discard();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(attachment.isRemoved(), "removing the sheep destroys its virtual coat attachment");
            helper.assertTrue(removedHolder.getAttachment() == null, "destroyed coat holder releases its attachment");
            helper.succeed();
        });
    }
}
