package keystrokesmod.module.impl.render;

import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PrePlayerInput;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RotationUtils;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;

public class FreeLook extends Module {
    private final ButtonSetting onlyIfPressed = new ButtonSetting("Only if pressed", true);

    private @Nullable ViewData viewData = null;

    public FreeLook() {
        super("FreeLook", category.render);
        this.registerSetting(onlyIfPressed);
    }

    @Override
    public void onEnable() {
        viewData = new ViewData(
                mc.gameSettings.thirdPersonView,
                mc.thePlayer.rotationYaw,
                mc.thePlayer.rotationPitch
        );
    }

    @Override
    public void onDisable() {
        if (viewData != null) {
            mc.gameSettings.thirdPersonView = viewData.thirdPersonView;
            mc.thePlayer.rotationYaw = viewData.rotationYaw;
            mc.thePlayer.rotationPitch = viewData.rotationPitch;
        }
        viewData = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreUpdate(PreUpdateEvent event) {
        mc.gameSettings.thirdPersonView = 1;
        if (onlyIfPressed.isToggled() && !Keyboard.isKeyDown(this.getKeycode())) {
            disable();
            return;
        }

        if (viewData != null) {
            mc.objectMouseOver = RotationUtils.rayCast(mc.playerController.getBlockReachDistance(), viewData.rotationYaw, viewData.rotationPitch);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent event) {
        if (viewData != null){
            if (event.getYaw() == mc.thePlayer.rotationYaw && event.getPitch() == mc.thePlayer.rotationPitch) {
                event.setYaw(viewData.rotationYaw);
                event.setPitch(viewData.rotationPitch);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreInput(@NotNull PrePlayerInput event) {
        if (viewData != null) {
            event.setYaw(viewData.rotationYaw);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onJump(JumpEvent event) {
        if (viewData != null) {
            event.setYaw(viewData.rotationYaw);
        }
    }

    private static class ViewData {
        private final int thirdPersonView;
        private final float rotationYaw;
        private final float rotationPitch;

        public ViewData(int thirdPersonView, float rotationYaw, float rotationPitch) {
            this.thirdPersonView = thirdPersonView;
            this.rotationYaw = rotationYaw;
            this.rotationPitch = rotationPitch;
        }
    }
}
