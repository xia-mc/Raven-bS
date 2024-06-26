package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PrePlayerInput;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static keystrokesmod.module.ModuleManager.speed;

public class TargetStrafe extends Module {
    private final ButtonSetting onlySpeed;
    private final SliderSetting strafeMove;

    public static float getMovementYaw() {
        return movementYaw != null ? movementYaw : mc.thePlayer.rotationYaw;
    }

    private static Float movementYaw = null;

    public TargetStrafe() {
        super("TargetStrafe", category.movement);
        this.registerSetting(new DescriptionSetting("Strafes around the target."));
        this.registerSetting(onlySpeed = new ButtonSetting("Only Speed", true));
        this.registerSetting(strafeMove = new SliderSetting("Strafe move", 0, -1, 1, 0.5));
    }

    private float normalize(float yaw) {
        while (yaw > 180) {
            yaw -= 360;
        }
        while (yaw < -180) {
            yaw += 360;
        }
        return yaw;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreInput(PrePlayerInput input) {
        if (KillAura.target == null || (onlySpeed.isToggled() && !speed.isEnabled())) {
            movementYaw = null;
            return;
        }

        float current = normalize(mc.thePlayer.rotationYaw);
        float toTarget = RotationUtils.getRotations(KillAura.target)[0];
        float diff = toTarget - current;

        if (diff == 0) return;

        if (Utils.isMoving()) {
            input.setForward(1);
            input.setStrafe((float) strafeMove.getInput());
            input.setYaw(toTarget);
        }
        movementYaw = toTarget;
    }

    @Override
    public void onDisable() {
        movementYaw = null;
    }
}
