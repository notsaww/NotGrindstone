package ru.notsaww.notgrindstone.mixin;

import ru.notsaww.notgrindstone.screen.CustomGrindstoneScreenHandler;
import ru.notsaww.notgrindstone.util.LanguageHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.GrindstoneBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrindstoneBlock.class)
public class GrindstoneBlockMixin {

    @Inject(method = "createScreenHandlerFactory", at = @At("HEAD"), cancellable = true)
    private void onCreateScreenHandlerFactory(BlockState state, World world, BlockPos pos,
                                              CallbackInfoReturnable<NamedScreenHandlerFactory> cir) {
        NamedScreenHandlerFactory factory = new NamedScreenHandlerFactory() {
            private String playerLanguage = "en_us";

            @Override
            public Text getDisplayName() {
                boolean isCis = LanguageHelper.isCisLanguage(playerLanguage);
                return Text.literal(isCis ? "Починка и снятие чар" : "Grindstone");
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                this.playerLanguage = LanguageHelper.getLanguage(player);
                return new CustomGrindstoneScreenHandler(syncId, playerInventory);
            }
        };

        cir.setReturnValue(factory);
    }
}