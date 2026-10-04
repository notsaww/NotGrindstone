package ru.notsaww.notgrindstone.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.notsaww.notgrindstone.common.UiText;
import ru.notsaww.notgrindstone.screen.CustomGrindstoneScreenHandler;

@Mixin(GrindstoneBlock.class)
public class GrindstoneBlockMixin {

    @Inject(method = "getMenuProvider", at = @At("HEAD"), cancellable = true)
    private void notsaww$onGetMenuProvider(BlockState state, Level level, BlockPos pos,
                                           CallbackInfoReturnable<MenuProvider> cir) {
        cir.setReturnValue(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable(UiText.SCREEN_TITLE);
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new CustomGrindstoneScreenHandler(syncId, playerInventory);
            }
        });
    }
}