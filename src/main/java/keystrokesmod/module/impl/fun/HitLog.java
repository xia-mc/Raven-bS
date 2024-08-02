package keystrokesmod.module.impl.fun;

import keystrokesmod.Raven;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.classes.Vec3;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class HitLog extends Module {
    private static final ModeSetting language = new ModeSetting("Language", new String[]{"English", "Chinese"}, 0);
    private static final SliderSetting coolDown = new SliderSetting("CoolDown", 100, 0, 1000, 50);
    private static long lastS08 = 0;
    private static long lastAttack = 0;

    public HitLog() {
        super("HitLog", category.fun);
        this.registerSetting(language, coolDown);
    }

    public static void onAttack(int predTicks, EntityPlayer target, Vec3 predHitPos, Vec3 selfPos, float yaw, float pitch) {
        if (!ModuleManager.hitLog.isEnabled()) return;
        if (target == null) return;
        if (System.currentTimeMillis() - lastAttack < coolDown.getInput()) return;
        lastAttack = System.currentTimeMillis();

        Raven.getExecutor().schedule(() -> {
            if (target.hurtTime == 0) {
                Reason reason = Reason.UNKNOWN;
                if (!target.getEntityBoundingBox().isVecInside(predHitPos.toVec3())) {
                    reason = Reason.PRED_FAIL;
                } else if (Math.round(System.currentTimeMillis() - lastS08) <= predTicks) {
                    reason = Reason.WATCHDOG;
                } else if (target.isBlocking()) {
                    reason = Reason.BLOCK;
                }

                switch ((int) language.getInput()) {
                    case 0:
                        Utils.sendMessage("Miss "+ target.getName() + "'s head Cause " + reason.getEnglish() + " | Trying to predict " + predTicks + " ticks");
                        break;
                    case 1:
                        Utils.sendMessage("空了 " + target.getName() + "的头部 原因 " + reason.getChinese() + " | 尝试预测 " + predTicks + " ticks");
                        break;
                }
            } else {
                switch ((int) language.getInput()) {
                    case 0:
                        Utils.sendMessage("Hit "+ target.getName() + "'s head | Trying to predict " + predTicks + " ticks | Health " + target.getHealth());
                        break;
                    case 1:
                        Utils.sendMessage("命中 " + target.getName() + "的头部 | 尝试预测 " + predTicks + " ticks | 血量 " + target.getHealth());
                        break;
                }
            }
        }, predTicks * 50L, TimeUnit.MILLISECONDS);
    }

    @SubscribeEvent
    public void onReceivePacket(@NotNull ReceivePacketEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            lastS08 = System.currentTimeMillis();
        }
    }

    @AllArgsConstructor
    @Getter
    enum Reason {
        UNKNOWN("Unknown", "未知"),
        WATCHDOG("Watchdog", "Watchdog"),
        PRED_FAIL("Predicted failed", "预测失败"),
        BLOCK("Blocking", "格挡");
        
        final String english;
        final String chinese;
    }
}
