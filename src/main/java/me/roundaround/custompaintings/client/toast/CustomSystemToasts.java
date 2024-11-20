package me.roundaround.custompaintings.client.toast;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

public class CustomSystemToasts {
  public static final SystemToast.Type PACK_DROP_FAILURE = new SystemToast.Type();
  public static final SystemToast.Type PACK_LOAD_SKIPPED = new SystemToast.Type();
  public static final SystemToast.Type PACK_LOAD_FAILURE = new SystemToast.Type();
  public static final SystemToast.Type LEGACY_PACKS_FOUND = new SystemToast.Type();

  public static void addPackCopyFailure(MinecraftClient client, String directory) {
    addPackCopyFailure(client.getToastManager(), directory);
  }

  public static void addPackCopyFailure(ToastManager manager, String directory) {
    SystemToast.add(
        manager, PACK_DROP_FAILURE, Text.translatable("custompaintings.toasts.copy.failure.title"), Text.of(directory));
  }

  public static void addPackLoadSkipped(MinecraftClient client) {
    addPackLoadSkipped(client.getToastManager());
  }

  public static void addPackLoadSkipped(ToastManager manager) {
    SystemToast.add(manager, PACK_LOAD_SKIPPED, Text.translatable("custompaintings.toasts.load.skipped.title"),
        Text.translatable("custompaintings.toasts.load.skipped.body")
    );
  }

  public static void addPackLoadFailure(MinecraftClient client) {
    addPackLoadFailure(client.getToastManager());
  }

  public static void addPackLoadFailure(ToastManager manager) {
    SystemToast.add(manager, PACK_LOAD_FAILURE, Text.translatable("custompaintings.toasts.load.failure.title"),
        Text.translatable("custompaintings.toasts.load.failure.body")
    );
  }

  public static void addLegacyPacksFound(MinecraftClient client, int count) {
    addLegacyPacksFound(client.getToastManager(), count);
  }

  public static void addLegacyPacksFound(ToastManager manager, int count) {
    SystemToast.add(manager, LEGACY_PACKS_FOUND, Text.translatable("custompaintings.toasts.legacy.title"),
        Text.translatable("custompaintings.toasts.legacy.body", count)
    );
  }
}
