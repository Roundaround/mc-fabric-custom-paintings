package me.roundaround.custompaintings.client.toast;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

public class CustomSystemToasts {
  public static final SystemToast.Type PACK_DROP_FAILURE = new SystemToast.Type();
  public static final SystemToast.Type PACK_LOAD_SKIPPED = new SystemToast.Type();
  public static final SystemToast.Type PACK_LOAD_FAILURE = new SystemToast.Type();
  public static final SystemToast.Type LEGACY_PACKS_FOUND = new SystemToast.Type();

  public static void addPackCopyFailure(MinecraftClient client, String directory) {
    SystemToast toast = SystemToast.create(
        client, PACK_DROP_FAILURE, Text.translatable("custompaintings.toasts.copy.failure.title"), Text.of(directory));
    client.getToastManager().add(toast);
  }

  public static void addPackLoadSkipped(MinecraftClient client) {
    SystemToast toast = SystemToast.create(client, PACK_LOAD_SKIPPED,
        Text.translatable("custompaintings.toasts.load.skipped.title"),
        Text.translatable("custompaintings.toasts.load.skipped.body")
    );
    client.getToastManager().add(toast);
  }

  public static void addPackLoadFailure(MinecraftClient client) {
    SystemToast toast = SystemToast.create(client, PACK_LOAD_FAILURE,
        Text.translatable("custompaintings.toasts.load.failure.title"),
        Text.translatable("custompaintings.toasts.load.failure.body")
    );
    client.getToastManager().add(toast);
  }

  public static void addLegacyPacksFound(MinecraftClient client, int count) {
    SystemToast toast = SystemToast.create(client, LEGACY_PACKS_FOUND,
        Text.translatable("custompaintings.toasts.legacy.title"),
        Text.translatable("custompaintings.toasts.legacy.body", count)
    );
    client.getToastManager().add(toast);
  }
}
