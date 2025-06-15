package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteIdentifier.class)
public abstract class SpriteIdentifierMixin {
  @Shadow
  @Final
  private Identifier texture;

  @WrapMethod(method = "getSprite")
  private Sprite getSprite(Operation<Sprite> original) {
    if (!this.texture.getNamespace().equals(Constants.MOD_ID) || !this.texture.getPath().startsWith("item")) {
      return original.call();
    }
    return ClientPaintingRegistry.getInstance().getSprite(CustomId.from(this.texture));
  }
}
