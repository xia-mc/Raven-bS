package keystrokesmod.module.impl.movement;

import keystrokesmod.Raven;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoSlow extends Module {
    public static ModeSetting mode;
    public static SliderSetting slowed;
    public static ButtonSetting disableBow;
    public static ButtonSetting disableSword;
    public static ButtonSetting disablePotions;
    public static ButtonSetting swordOnly;
    public static ButtonSetting vanillaSword;
    private final String[] modes = new String[]{"Vanilla", "Pre", "Post", "Alpha", "Test"};
    private boolean postPlace;

    public NoSlow() {
        super("NoSlow", Module.category.movement, 0);
        this.registerSetting(new DescriptionSetting("Default is 80% motion reduction."));
        this.registerSetting(mode = new ModeSetting("Mode", modes, 0));
        this.registerSetting(slowed = new SliderSetting("Slow %", 80.0D, 0.0D, 80.0D, 1.0D));
        this.registerSetting(disableSword = new ButtonSetting("Disable sword", false));
        this.registerSetting(disableBow = new ButtonSetting("Disable bow", false));
        this.registerSetting(disablePotions = new ButtonSetting("Disable potions", false));
        this.registerSetting(swordOnly = new ButtonSetting("Sword only", false));
        this.registerSetting(vanillaSword = new ButtonSetting("Vanilla sword", false));
    }

    public void onUpdate() {
        if (ModuleManager.bedAura.stopAutoblock) {
            return;
        }
        postPlace = false;
        if (vanillaSword.isToggled() && Utils.holdingSword()) {
            return;
        }
        boolean apply = getSlowed() != 0.2f;
        if (!apply || !mc.thePlayer.isUsingItem()) {
            return;
        }
        switch ((int) mode.getInput()) {
            case 1:
                if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.badPacketsHandler.C07) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                }
                break;
            case 2:
                postPlace = true;
                break;
            case 3:
                if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.badPacketsHandler.C07) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 1, null, 0, 0, 0));
                }
                break;
            case 4:
                if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.badPacketsHandler.C07) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement());
                }
                break;
        }
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        if (postPlace && mode.getInput() == 3) {
            if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.badPacketsHandler.C07) {
                mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            }
            postPlace = false;
        }
    }

    public static float getSlowed() {
        if (!mc.thePlayer.isUsingItem()) return (100.0F - 0.0F) / 100.0F;
        if (mc.thePlayer.getHeldItem() == null || ModuleManager.noSlow == null || !ModuleManager.noSlow.isEnabled()) {
            return 0.2f;
        }
        if (swordOnly.isToggled() && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
            return 0.2f;
        }
        if (mc.thePlayer.getHeldItem().getItem() instanceof ItemSword && disableSword.isToggled()) {
            return 0.2f;
        } if (mc.thePlayer.getHeldItem().getItem() instanceof ItemBow && disableBow.isToggled()) {
            return 0.2f;
        } else if (mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage()) && disablePotions.isToggled()) {
            return 0.2f;
        }
        return (100.0F - (float) slowed.getInput()) / 100.0F;
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }
}
