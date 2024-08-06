package keystrokesmod.module.impl.render.targetvisual.targethud;

import keystrokesmod.module.impl.render.TargetHUD;
import keystrokesmod.module.impl.render.targetvisual.ITargetVisual;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.IFont;
import keystrokesmod.utility.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static keystrokesmod.module.impl.render.TargetHUD.*;

public class RavenTargetHUD extends SubMode<TargetHUD> implements ITargetVisual {
    private final ModeSetting theme;
    private final ModeSetting font;
    private final ButtonSetting showStatus;
    private final ButtonSetting healthColor;

    public RavenTargetHUD(String name, @NotNull TargetHUD parent) {
        super(name, parent);
        this.registerSetting(theme = new ModeSetting("Theme", Theme.themes, 0));
        this.registerSetting(font = new ModeSetting("Font", new String[]{"Minecraft", "ProductSans", "Regular"}, 0));
        this.registerSetting(showStatus = new ButtonSetting("Show win or loss", true));
        this.registerSetting(healthColor = new ButtonSetting("Traditional health color", false));
    }

    private IFont getFont() {
        switch ((int) font.getInput()) {
            default:
            case 0:
                return FontManager.getMinecraft();
            case 1:
                return FontManager.productSansMedium;
            case 2:
                return FontManager.regular22;
        }
    }

    @Override
    public void render(@NotNull EntityLivingBase target) {
        String string = target.getDisplayName().getFormattedText();
        float health = Utils.limit(target.getHealth() / target.getMaxHealth(), 0, 1);
        if (Float.isInfinite(health) || Float.isNaN(health)) {
            health = 0;
        }

        if (showStatus.isToggled()) {
            string = string + " " + ((health <= Utils.getCompleteHealth(mc.thePlayer) / mc.thePlayer.getMaxHealth()) ? "§aW" : "§cL");
        }
        final int n10 = 255;
        final int n11 = Math.min(n10, 110);
        final int n12 = Math.min(n10, 210);
        final int[] array = Theme.getGradients((int) theme.getInput());
        RenderUtils.drawRoundedGradientOutlinedRectangle((float) current$minX, (float) current$minY, (float) current$maxX, (float) (current$maxY + 13), 10.0f, Utils.merge(Color.black.getRGB(), n11), Utils.merge(array[0], n10), Utils.merge(array[1], n10)); // outline
        final int n13 = current$minX + 6;
        final int n14 = current$maxX - 6;
        RenderUtils.drawRoundedRectangle((float) n13, (float) current$maxY, (float) n14, (float) (current$maxY + 5), 4.0f, Utils.merge(Color.black.getRGB(), n11)); // background
        int k = Utils.merge(array[0], n12);
        int n16 = Utils.merge(array[1], n12);
        float healthBar = (float) (int) (n14 + (n13 - n14) * (1.0 - ((health < 0.05) ? 0.05 : health)));
        if (healthBar - n13 < 3) { // if goes below, the rounded health bar glitches out
            healthBar = n13 + 3;
        }
        float lastHealthBar = healthBar;
        if (healthColor.isToggled()) {
            k = n16 = Utils.merge(Utils.getColorForHealth(health), n12);
        }
        RenderUtils.drawRoundedGradientRect((float) n13, (float) current$maxY, lastHealthBar, (float) (current$maxY + 5), 4.0f, k, k, k, n16); // health bar
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        getFont().drawString(string, (float) current$minX + 8, (float) current$minY + 8, (new Color(220, 220, 220, 255).getRGB() & 0xFFFFFF) | Utils.clamp(n10 + 15) << 24, true);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
