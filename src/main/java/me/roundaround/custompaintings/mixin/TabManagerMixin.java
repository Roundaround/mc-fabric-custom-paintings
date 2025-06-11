package me.roundaround.custompaintings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.roundaround.custompaintings.client.gui.screen.editor.PackEditorTab;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;

@Mixin(TabManager.class)
public abstract class TabManagerMixin {
  @Shadow
  private Tab currentTab;

  @Inject(method = "setCurrentTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/tab/Tab;forEachChild(Ljava/util/function/Consumer;)V", ordinal = 0))
  private void beforeUnloadTab(Tab newTab, boolean clickSound, CallbackInfo ci) {
    if (this.currentTab instanceof PackEditorTab tab) {
      tab.beforeUnload();
    }
  }

  @Inject(method = "setCurrentTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/tab/Tab;forEachChild(Ljava/util/function/Consumer;)V", ordinal = 1))
  private void beforeLoadTab(Tab newTab, boolean clickSound, CallbackInfo ci) {
    if (newTab instanceof PackEditorTab tab) {
      tab.beforeLoad();
    }
  }

  @Unique
  private TabManager self() {
    return (TabManager) (Object) this;
  }
}
