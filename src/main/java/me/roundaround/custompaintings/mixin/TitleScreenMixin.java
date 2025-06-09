package me.roundaround.custompaintings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.roundaround.custompaintings.client.gui.screen.editor.ZeroScreen;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.roundalib.client.gui.icon.BuiltinIcon;
import me.roundaround.custompaintings.roundalib.client.gui.screen.ScreenParent;
import me.roundaround.custompaintings.roundalib.client.gui.widget.IconButtonWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
  @Inject(method = "addNormalWidgets", at = @At("RETURN"))
  private void afterAddNormalWidgets(int startY, int spacingY, CallbackInfoReturnable<Integer> cir) {
    if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
      return;
    }

    this.addDrawableChild(IconButtonWidget.builder(BuiltinIcon.FIX_18, Constants.MOD_ID)
        .position((this.width / 2 - 100) - 4 - 20, cir.getReturnValueI())
        .vanillaSize()
        .messageAndTooltip(Text.of("Pack Editor"))
        .onPress((button) -> {
          this.client.setScreen(new ZeroScreen(new ScreenParent(this), this.client));
        })
        .build());
  }

  private TitleScreenMixin() {
    super(Text.empty());
  }
}
