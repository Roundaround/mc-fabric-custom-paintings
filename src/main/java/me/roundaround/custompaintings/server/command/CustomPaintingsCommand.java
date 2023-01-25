package me.roundaround.custompaintings.server.command;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData.MismatchedCategory;
import me.roundaround.custompaintings.network.ServerNetworking;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

public class CustomPaintingsCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    LiteralArgumentBuilder<ServerCommandSource> baseCommand = CommandManager.literal(CustomPaintingsMod.MOD_ID)
        .requires(source -> source.hasPermissionLevel(2))
        .requires(source -> source.isExecutedByPlayer());

    LiteralArgumentBuilder<ServerCommandSource> identifySub = CommandManager
        .literal("identify")
        .executes(context -> {
          return executeIdentify(context.getSource());
        });

    LiteralArgumentBuilder<ServerCommandSource> removeSub = CommandManager
        .literal("remove")
        .then(CommandManager.literal("unknown")
            .executes(context -> {
              return executeRemove(context.getSource(), Optional.empty());
            }))
        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
            .executes(context -> {
              return executeRemove(context.getSource(),
                  Optional.of(IdentifierArgumentType.getIdentifier(context, "id")));
            }));

    LiteralArgumentBuilder<ServerCommandSource> fixSub = CommandManager
        .literal("fix")
        .then(CommandManager.literal("id")
            .then(CommandManager.argument("from", IdentifierArgumentType.identifier())
                .suggests(new ExistingPaintingIdentifierSuggestionProvider(true))
                .then(CommandManager.argument("to", IdentifierArgumentType.identifier())
                    .suggests(new KnownPaintingIdentifierSuggestionProvider())
                    .executes(context -> {
                      return executeFixIds(
                          context.getSource(),
                          IdentifierArgumentType.getIdentifier(context, "from"),
                          IdentifierArgumentType.getIdentifier(context, "to"));
                    }))))
        .then(CommandManager.literal("size")
            .executes(context -> {
              return executeFixSizes(context.getSource(), null);
            })
            .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                .suggests(new KnownPaintingIdentifierSuggestionProvider(true))
                .executes(context -> {
                  return executeFixSizes(
                      context.getSource(),
                      IdentifierArgumentType.getIdentifier(context, "id"));
                })))
        .then(CommandManager.literal("info")
            .executes(context -> {
              return executeFixInfo(context.getSource(), null);
            })
            .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                .suggests(new KnownPaintingIdentifierSuggestionProvider(true))
                .executes(context -> {
                  return executeFixInfo(
                      context.getSource(),
                      IdentifierArgumentType.getIdentifier(context, "id"));
                })))
        .then(CommandManager.literal("everything")
            .executes(context -> {
              return executeFixEverything(context.getSource(), null);
            })
            .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                .suggests(new KnownPaintingIdentifierSuggestionProvider(true))
                .executes(context -> {
                  return executeFixEverything(
                      context.getSource(),
                      IdentifierArgumentType.getIdentifier(context, "id"));
                })));

    LiteralArgumentBuilder<ServerCommandSource> manageSub = CommandManager
        .literal("manage")
        .executes(context -> {
          return executeManage(context.getSource());
        });

    LiteralArgumentBuilder<ServerCommandSource> autoFixSub = CommandManager
        .literal("autofix")
        .executes(context -> {
          return executeAutoFix(context.getSource());
        });

    LiteralArgumentBuilder<ServerCommandSource> finalCommand = baseCommand
        .then(identifySub)
        .then(removeSub)
        .then(fixSub)
        .then(manageSub)
        .then(autoFixSub);

    dispatcher.register(finalCommand);
  }

  private static int executeIdentify(ServerCommandSource source) {
    ServerPlayerEntity player = source.getPlayer();

    Entity camera = player.getCameraEntity();
    double distance = 64;
    Vec3d posVec = camera.getCameraPosVec(0f);
    Vec3d rotationVec = camera.getRotationVec(1f);
    Vec3d targetVec = posVec.add(
        rotationVec.x * distance,
        rotationVec.y * distance,
        rotationVec.z * distance);

    HitResult crosshairTarget = ProjectileUtil.raycast(
        player.getCameraEntity(),
        posVec,
        targetVec,
        camera.getBoundingBox().stretch(rotationVec.multiply(distance)).expand(1.0, 1.0, 1.0),
        entity -> entity instanceof PaintingEntity,
        distance * distance);
    if (!(crosshairTarget instanceof EntityHitResult)) {
      source.sendFeedback(Text.translatable("custompaintings.command.identify.none"), true);
      return 0;
    }

    EntityHitResult entityHitResult = (EntityHitResult) crosshairTarget;
    if (!(entityHitResult.getEntity() instanceof PaintingEntity)) {
      source.sendFeedback(Text.translatable("custompaintings.command.identify.none"), true);
      return 0;
    }

    PaintingEntity vanillaPainting = (PaintingEntity) entityHitResult.getEntity();
    if (!(vanillaPainting instanceof ExpandedPaintingEntity)) {
      identifyVanillaPainting(source, vanillaPainting);
      return 1;
    }

    ExpandedPaintingEntity painting = (ExpandedPaintingEntity) vanillaPainting;
    PaintingData paintingData = painting.getCustomData();
    if (paintingData.isEmpty() || paintingData.isVanilla()) {
      identifyVanillaPainting(source, vanillaPainting);
      return 1;
    }

    ArrayList<Text> lines = new ArrayList<>();
    lines.add(Text.literal(paintingData.id().toString()));

    if (paintingData.hasLabel()) {
      lines.add(paintingData.getLabel());
    }

    lines.add(Text.translatable(
        "custompaintings.painting.dimensions",
        paintingData.width(),
        paintingData.height()));

    Map<Identifier, PaintingData> known = CustomPaintingsMod.knownPaintings.get(player.getUuid())
        .stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity()));
    if (!known.containsKey(paintingData.id())) {
      lines.add(Text.translatable("custompaintings.command.identify.missing"));
    } else {
      if (paintingData.isMismatched(known.get(paintingData.id()), MismatchedCategory.INFO)) {
        lines.add(Text.translatable("custompaintings.command.identify.mismatched.info"));
      } else if (paintingData.isMismatched(known.get(paintingData.id()), MismatchedCategory.SIZE)) {
        lines.add(Text.translatable("custompaintings.command.identify.mismatched.size"));
      }
    }

    for (Text line : lines) {
      source.sendFeedback(line, true);
    }
    return 1;
  }

  private static void identifyVanillaPainting(ServerCommandSource source, PaintingEntity painting) {
    ArrayList<Text> lines = new ArrayList<>();

    PaintingVariant variant = painting.getVariant().value();
    String id = Registry.PAINTING_VARIANT.getId(variant).toString();

    lines.add(Text.literal(id));
    lines.add(Text.translatable(
        "custompaintings.painting.dimensions",
        variant.getWidth() / 16,
        variant.getHeight() / 16));

    for (Text line : lines) {
      source.sendFeedback(line, true);
    }
  }

  private static int executeRemove(ServerCommandSource source, Optional<Identifier> idToRemove) {
    ServerPlayerEntity player = source.getPlayer();
    ArrayList<PaintingEntity> toRemove = new ArrayList<>();

    if (idToRemove.isPresent()) {
      source.getServer().getWorlds().forEach((world) -> {
        world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
            .stream()
            .filter((entity) -> ((ExpandedPaintingEntity) entity).getCustomData().id().equals(idToRemove.get()))
            .forEach((entity) -> {
              toRemove.add((PaintingEntity) entity);
            });
      });
    } else {
      Set<Identifier> known = ServerPaintingManager.getKnownPaintings(player).keySet();

      source.getServer().getWorlds().forEach((world) -> {
        world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
            .stream()
            .filter((entity) -> {
              Identifier id = ((ExpandedPaintingEntity) entity).getCustomData().id();
              return !known.contains(id);
            })
            .forEach((entity) -> {
              toRemove.add((PaintingEntity) entity);
            });
      });
    }

    toRemove.forEach((painting) -> {
      painting.damage(DamageSource.player(player), 0f);
    });

    if (toRemove.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.remove.none"), true);
    } else {
      source.sendFeedback(Text.translatable("custompaintings.command.remove.success", toRemove.size()), true);
    }

    return toRemove.size();
  }

  private static int executeFixIds(ServerCommandSource source, Identifier from, Identifier to) {
    ArrayList<ExpandedPaintingEntity> toUpdate = new ArrayList<>();

    source.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> ((ExpandedPaintingEntity) entity).getCustomData().id().equals(from))
          .forEach((entity) -> {
            toUpdate.add((ExpandedPaintingEntity) entity);
          });
    });

    toUpdate.forEach((painting) -> {
      PaintingData data = painting.getCustomData();
      painting.setCustomData(
          to,
          data.index(),
          data.width(),
          data.height(),
          data.name(),
          data.artist(),
          data.isVanilla());
    });

    if (toUpdate.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.ids.none"), true);
    } else {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.ids.success", toUpdate.size()), true);
    }

    return toUpdate.size();
  }

  private static int executeFixSizes(ServerCommandSource source, Identifier id) {
    ArrayList<ExpandedPaintingEntity> toUpdate = new ArrayList<>();
    Map<Identifier, PaintingData> known = ServerPaintingManager.getKnownPaintings(source.getPlayer());

    source.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> {
            Identifier entityId = ((ExpandedPaintingEntity) entity).getCustomData().id();
            return (id == null || entityId.equals(id)) && known.containsKey(entityId);
          })
          .forEach((entity) -> {
            toUpdate.add((ExpandedPaintingEntity) entity);
          });
    });

    toUpdate.forEach((painting) -> {
      PaintingData currentData = painting.getCustomData();
      PaintingData knownData = known.get(currentData.id());
      painting.setCustomData(
          currentData.id(),
          currentData.index(),
          knownData.width(),
          knownData.height(),
          currentData.name(),
          currentData.artist(),
          currentData.isVanilla());
    });

    if (toUpdate.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.sizes.none"), true);
    } else {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.sizes.success", toUpdate.size()), true);
    }

    return toUpdate.size();
  }

  private static int executeFixInfo(ServerCommandSource source, Identifier id) {
    ArrayList<ExpandedPaintingEntity> toUpdate = new ArrayList<>();
    Map<Identifier, PaintingData> known = ServerPaintingManager.getKnownPaintings(source.getPlayer());

    source.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> {
            Identifier entityId = ((ExpandedPaintingEntity) entity).getCustomData().id();
            return (id == null || entityId.equals(id)) && known.containsKey(entityId);
          })
          .forEach((entity) -> {
            toUpdate.add((ExpandedPaintingEntity) entity);
          });
    });

    toUpdate.forEach((painting) -> {
      PaintingData currentData = painting.getCustomData();
      PaintingData knownData = known.get(currentData.id());
      painting.setCustomData(
          currentData.id(),
          knownData.index(),
          currentData.width(),
          currentData.height(),
          knownData.name(),
          knownData.artist(),
          knownData.isVanilla());
    });

    if (toUpdate.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.info.none"), true);
    } else {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.info.success", toUpdate.size()), true);
    }

    return toUpdate.size();
  }

  private static int executeFixEverything(ServerCommandSource source, Identifier id) {
    ArrayList<ExpandedPaintingEntity> toUpdate = new ArrayList<>();
    Map<Identifier, PaintingData> known = ServerPaintingManager.getKnownPaintings(source.getPlayer());

    source.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> {
            Identifier entityId = ((ExpandedPaintingEntity) entity).getCustomData().id();
            return (id == null || entityId.equals(id)) && known.containsKey(entityId);
          })
          .forEach((entity) -> {
            toUpdate.add((ExpandedPaintingEntity) entity);
          });
    });

    toUpdate.forEach((painting) -> {
      PaintingData currentData = painting.getCustomData();
      PaintingData knownData = known.get(currentData.id());
      painting.setCustomData(
          currentData.id(),
          knownData.index(),
          knownData.width(),
          knownData.height(),
          knownData.name(),
          knownData.artist(),
          knownData.isVanilla());
    });

    if (toUpdate.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.everything.none"), true);
    } else {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.everything.success", toUpdate.size()), true);
    }

    return toUpdate.size();
  }

  private static int executeManage(ServerCommandSource source) {
    ServerNetworking.sendOpenManageScreenPacket(source.getPlayer());
    return 1;
  }

  private static int executeAutoFix(ServerCommandSource source) {
    int fixed = ServerPaintingManager.autoFixPaintings(source.getServer(), source.getPlayer());
    if (fixed == 0) {
      source.sendFeedback(Text.translatable("custompaintings.command.autofix.none"), true);
    } else {
      source.sendFeedback(Text.translatable("custompaintings.command.autofix.success", fixed), true);
    }
    return fixed;
  }
}
