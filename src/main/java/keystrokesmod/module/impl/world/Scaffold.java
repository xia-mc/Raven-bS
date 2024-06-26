package keystrokesmod.module.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.player.InvManager;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import static keystrokesmod.script.ScriptDefaults.client;
import keystrokesmod.utility.*;
import keystrokesmod.utility.render.RenderUtils;
import net.minecraft.block.BlockAir;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Scaffold extends Module { // from b4 :)
    private final SliderSetting motion;
    private final ModeSetting rotation;
    private final ModeSetting fastScaffold;
    private final ButtonSetting autoSwap;
    private final ButtonSetting fastOnRMB;
    private final ButtonSetting highlightBlocks;
    private final ButtonSetting multiPlace;
    public final ButtonSetting safeWalk;
    private final ButtonSetting showBlockCount;
    private final ButtonSetting silentSwing;
    public final ButtonSetting tower;
    public final ButtonSetting fast;
    public final ButtonSetting onlyOffGround;

    protected MovingObjectPosition placeBlock;
    private int lastSlot;
    public float placeYaw;
    public float placePitch;
    public int at;
    public int index;
    public boolean rmbDown;
    private double startPos = -1;
    private final Map<BlockPos, Timer> highlight = new HashMap<>();
    private boolean forceStrict;
    private boolean down;
    private int add;
    public Scaffold() {
        super("Scaffold", category.world);
        this.registerSetting(motion = new SliderSetting("Motion", 0.95, 0.5, 1.2, 0.01));
        String[] rotationModes = new String[]{"None", "Backwards", "Strict", "Raytrace"};
        this.registerSetting(rotation = new ModeSetting("Rotation", rotationModes, 1));
        String[] fastScaffoldModes = new String[]{"Disabled", "Sprint", "Edge", "Jump", "Float"};
        this.registerSetting(fastScaffold = new ModeSetting("Fast scaffold", fastScaffoldModes, 0));
        //this.registerSetting(delayOnJump = new ButtonSetting("Delay on jump", true));
        this.registerSetting(autoSwap = new ButtonSetting("AutoSwap", true));
        this.registerSetting(fastOnRMB = new ButtonSetting("Fast on RMB", false));
        this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", true));
        this.registerSetting(multiPlace = new ButtonSetting("Multi-place", false));
        this.registerSetting(safeWalk = new ButtonSetting("SafeWalk", true));
        this.registerSetting(showBlockCount = new ButtonSetting("Show block count", true));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
        this.registerSetting(tower = new ButtonSetting("Tower", false));
        this.registerSetting(fast = new ButtonSetting("Fast", false));
        this.registerSetting(onlyOffGround = new ButtonSetting("Only offGround", false));
    }

    public void onDisable() {
        placeBlock = null;
        if (lastSlot != -1) {
            mc.thePlayer.inventory.currentItem = lastSlot;
            lastSlot = -1;
        }
        highlight.clear();
        at = index = 0;
        add = 0;
        startPos = -1;
        forceStrict = false;
        down = false;
    }

    public void onEnable() {
        lastSlot = -1;
        startPos = mc.thePlayer.posY;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (rotation.getInput() > 0) {
            if ((rotation.getInput() == 2 && forceStrict) || rotation.getInput() == 3) {
                event.setYaw(placeYaw);
                event.setPitch(placePitch);
            } else {
                event.setYaw(getYaw());
                event.setPitch(85);
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) { // place here
        if (onlyOffGround.isToggled() && mc.thePlayer.onGround) return;

        if (fast.isToggled() && mc.gameSettings.keyBindJump.isKeyDown()) {
            if (mc.gameSettings.keyBindForward.isKeyDown() && client.getPlayer().onGround()) {
                if (!isDiagonal() && !(mc.gameSettings.keyBindRight.isKeyDown() || mc.gameSettings.keyBindLeft.isKeyDown())) {
                    client.setSpeed(0.5);
                    client.setSprinting(false);
                    client.jump();
                }
            }
        }

        final ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (!autoSwap.isToggled() || getSlot() == -1) {
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
                return;
            }
        }
        if (keepYPosition() && !down) {
            startPos = Math.floor(mc.thePlayer.posY);
            down = true;
        }
        else if (!keepYPosition()) {
            down = false;
        }
        if (keepYPosition() && fastScaffold.getInput() == 3 && mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            add = 0;
        }
        double original = startPos;
        if (groundDistance() >= 2 && add == 0) {
            original++;
            add++;
        }
        final HashMap<BlockPos, EnumFacing> possiblePositions = new HashMap<>();
        int yOffSet = mc.thePlayer.onGround ? -1 : -2;
        final int n2 = 3;
        for (int i = yOffSet; i < 0; ++i) {
            for (int j = -n2; j <= n2; ++j) {
                for (int k = -n2; k <= n2; ++k) {
                    final BlockPos blockPos = new BlockPos(mc.thePlayer.posX + j, (keepYPosition() ? original : mc.thePlayer.posY) + i, mc.thePlayer.posZ + k);
                    if (!BlockUtils.replaceable(blockPos)) {
                        EnumFacing enumFacing = null;
                        double lastDistance = 0.0;
                        for (EnumFacing enumFacing2 : EnumFacing.VALUES) {
                            enumLoop: {
                                if (enumFacing2 != EnumFacing.DOWN) {
                                    if (enumFacing2 == EnumFacing.UP) {
                                        if (mc.thePlayer.onGround) {
                                            break enumLoop;
                                        }
                                    }
                                    final BlockPos offset = blockPos.offset(enumFacing2);
                                    if (BlockUtils.replaceable(offset)) {
                                        final double distanceSqToCenter = offset.distanceSqToCenter(mc.thePlayer.posX, (keepYPosition() ? original : mc.thePlayer.posY) - 1, mc.thePlayer.posZ);
                                        if (enumFacing == null || distanceSqToCenter < lastDistance) {
                                            enumFacing = enumFacing2;
                                            lastDistance = distanceSqToCenter;
                                        }
                                    }
                                }
                            }
                        }
                        if (enumFacing != null) {
                            possiblePositions.put(blockPos, enumFacing);
                        }
                    }
                }
            }
        }
        if (possiblePositions.isEmpty()) {
            return;
        }
        if (mc.thePlayer.onGround && Utils.isMoving() && motion.getInput() != 1.0) {
            Utils.setSpeed(Utils.getHorizontalSpeed() * motion.getInput());
        }
        int slot = getSlot();
        if (slot == -1) {
            return;
        }
        if (lastSlot == -1) {
            lastSlot = mc.thePlayer.inventory.currentItem;
        }
        mc.thePlayer.inventory.currentItem = slot;
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
            return;
        }
        MovingObjectPosition rayCasted = null;
        double distance = -1.0;
        float searchYaw = 25;
        float[] searchPitch = new float[]{70, 23};
        for (int i = 0; i < 2; i++) {
            if (i == 1 && Utils.overPlaceable(-1) && !keepYPosition()) {
                searchYaw = 180;
                searchPitch = new float[]{60, 28};
            }
            else if (i == 1) {
                break;
            }
            for (float checkYaw = -searchYaw; checkYaw < searchYaw; ++checkYaw) {
                if (!Utils.overPlaceable(-1)) {
                    continue;
                }
                float fixedYaw = (float) ((i == 0 ? getYaw() : 0) - checkYaw + getRandom());
                for (float checkPitch = 0; checkPitch < searchPitch[1]; ++checkPitch) {
                    float fixedPitch = RotationUtils.clamp((float) (searchPitch[0] + checkPitch + getRandom()));
                    MovingObjectPosition raycast = RotationUtils.rayCast(mc.playerController.getBlockReachDistance(), fixedYaw, fixedPitch);
                    if (raycast != null) {
                        if (raycast.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                            final EnumFacing enumFacing3 = possiblePositions.get(raycast.getBlockPos());
                            if (enumFacing3 != null) {
                                if (enumFacing3 == raycast.sideHit) {
                                    if (rayCasted == null || !BlockUtils.isSamePos(raycast.getBlockPos(), rayCasted.getBlockPos())) {
                                        if (((ItemBlock) heldItem.getItem()).canPlaceBlockOnSide(mc.theWorld, raycast.getBlockPos(), raycast.sideHit, mc.thePlayer, heldItem)) {
                                            double squareDistanceTo = mc.thePlayer.getPositionVector().squareDistanceTo(raycast.hitVec);
                                            if (rayCasted == null || squareDistanceTo < distance) {
                                                forceStrict = (forceStrict(checkYaw)) && i == 1;
                                                rayCasted = raycast;
                                                distance = squareDistanceTo;
                                                placeYaw = fixedYaw;
                                                placePitch = fixedPitch;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (rayCasted != null) {
                break;
            }
        }
        if (rayCasted != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            placeBlock = rayCasted;
            if (multiPlace.isToggled()) {
                place(placeBlock, true);
            }
            place(placeBlock, false);
        }
    }

    private boolean isDiagonal() {
        float yaw = client.getPlayer().getYaw();
        yaw = (yaw + 360) % 360;
        final float threshold = 9;
        boolean isNorth = Math.abs(yaw - 0) < threshold || Math.abs(yaw - 360) < threshold;
        boolean isSouth = Math.abs(yaw - 180) < threshold;
        boolean isEast = Math.abs(yaw - 90) < threshold;
        boolean isWest = Math.abs(yaw - 270) < threshold;
        return !(isNorth || isSouth || isEast || isWest);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck() || !showBlockCount.isToggled()) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null) {
                return;
            }
            final ScaledResolution scaledResolution = new ScaledResolution(mc);
            int blocks = totalBlocks();
            String color = "§";
            if (blocks <= 5) {
                color += "c";
            }
            else if (blocks <= 15) {
                color += "6";
            }
            else if (blocks <= 25) {
                color += "e";
            }
            else {
                color = "";
            }
            mc.fontRendererObj.drawStringWithShadow(color + blocks + " §rblock" + (blocks == 1 ? "" : "s"), scaledResolution.getScaledWidth()/2 + 8, scaledResolution.getScaledHeight()/2 + 4, -1);
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent mouseEvent) {
        if (mouseEvent.button == 1) {
            rmbDown = mouseEvent.buttonstate;
            if (placeBlock != null && rmbDown) {
                mouseEvent.setCanceled(true);
            }
        }
    }

    public boolean stopFastPlace() {
        return this.isEnabled() && placeBlock != null;
    }

    public double groundDistance() {
        for (int i = 1; i <= 20; i++) {
            if (!mc.thePlayer.onGround && !(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - (i / 10), mc.thePlayer.posZ)) instanceof BlockAir)) {
                return (i / 10);
            }
        }
        return -1;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || !highlightBlocks.isToggled() || highlight.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Timer>> iterator = highlight.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Timer> entry = iterator.next();
            if (entry.getValue() == null) {
                entry.setValue(new Timer(750));
                entry.getValue().start();
            }
            int alpha = entry.getValue() == null ? 210 : 210 - entry.getValue().getValueInt(0, 210, 1);
            if (alpha == 0) {
                iterator.remove();
                continue;
            }
            RenderUtils.renderBlock(entry.getKey(), Utils.merge(Theme.getGradient((int) HUD.theme.getInput(), 0), alpha), true, false);
        }
    }

    public boolean sprint() {
        if (this.isEnabled() && fastScaffold.getInput() > 0 && placeBlock != null && (!fastOnRMB.isToggled() || Mouse.isButtonDown(1))) {
            switch ((int) fastScaffold.getInput()) {
                case 1:
                    return true;
                case 2:
                    return Utils.onEdge();
                case 3:
                case 4:
                    return keepYPosition();
            }
        }
        return false;
    }

    private boolean forceStrict(float value) {
        return (inBetween(-170, -105, value) || inBetween(-80, 80, value) || inBetween(98, 170, value)) && !inBetween(-10, 10, value);
    }

    private boolean keepYPosition() {
        return this.isEnabled() && Utils.keysDown() && (fastScaffold.getInput() == 4 || fastScaffold.getInput() == 3) && (!Utils.jumpDown() || fastScaffold.getInput() == 4) && (!fastOnRMB.isToggled() || Mouse.isButtonDown(1));
    }

    public boolean safewalk() {
        return this.isEnabled() && safeWalk.isToggled();
    }

    public boolean stopRotation() {
        return this.isEnabled() && (rotation.getInput() <= 1 || (rotation.getInput() == 2 && placeBlock != null));
    }

    public boolean inBetween(float min, float max, float value) {
        return value >= min && value <= max;
    }

    public static double getRandom() {
        return Utils.randomizeInt(5, 25) / 100.0;
    }

    public static float getYaw() {
        float n = 0.0f;
        final double n2 = mc.thePlayer.movementInput.moveForward;
        final double n3 = mc.thePlayer.movementInput.moveStrafe;
        if (n2 == 0.0) {
            if (n3 == 0.0) {
                n = 180.0f;
            }
            else if (n3 > 0.0) {
                n = 90.0f;
            }
            else if (n3 < 0.0) {
                n = -90.0f;
            }
        }
        else if (n2 > 0.0) {
            if (n3 == 0.0) {
                n = 180.0f;
            }
            else if (n3 > 0.0) {
                n = 135.0f;
            }
            else if (n3 < 0.0) {
                n = -135.0f;
            }
        }
        else if (n2 < 0.0) {
            if (n3 == 0.0) {
                n = 0.0f;
            }
            else if (n3 > 0.0) {
                n = 45.0f;
            }
            else if (n3 < 0.0) {
                n = -45.0f;
            }
        }
        return mc.thePlayer.rotationYaw + n;
    }

    protected boolean place(MovingObjectPosition block, boolean extra) {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
            return false;
        }
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, block.getBlockPos(), block.sideHit, block.hitVec)) {
            if (silentSwing.isToggled()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            }
            else {
                mc.thePlayer.swingItem();
                mc.getItemRenderer().resetEquippedProgress();
            }
            if (!extra) {
                highlight.put(block.getBlockPos().offset(block.sideHit), null);
            }
            return true;
        }
        return false;
    }

    private int getSlot() {
        return ContainerUtils.getSlot(ItemBlock.class, InvManager::canBePlaced);
    }

    public int totalBlocks() {
        int totalBlocks = 0;
        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock && InvManager.canBePlaced((ItemBlock) stack.getItem()) && stack.stackSize > 0) {
                totalBlocks += stack.stackSize;
            }
        }
        return totalBlocks;
    }
}
