package ru.notsaww.notgrindstone.mixin;

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
import ru.notsaww.notgrindstone.common.UiText;
import ru.notsaww.notgrindstone.screen.CustomGrindstoneScreenHandler;

@Mixin(GrindstoneBlock.class)
public class GrindstoneBlockMixin {

    @Inject(method = "createScreenHandlerFactory", at = @At("HEAD"), cancellable = true)
    private void onCreateScreenHandlerFactory(BlockState state, World world, BlockPos pos,
                                              CallbackInfoReturnable<NamedScreenHandlerFactory> cir) {
        NamedScreenHandlerFactory factory = new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.translatable(UiText.SCREEN_TITLE);
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                return new CustomGrindstoneScreenHandler(syncId, playerInventory);
            }
        };

        cir.setReturnValue(factory);
    }
}