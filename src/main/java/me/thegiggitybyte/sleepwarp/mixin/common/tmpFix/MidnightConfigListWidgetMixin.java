package me.thegiggitybyte.sleepwarp.mixin.common.tmpFix;

import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MidnightConfig.MidnightConfigListWidget.class)
public abstract class MidnightConfigListWidgetMixin extends ElementListWidget<MidnightConfig.ButtonEntry> {
    public MidnightConfigListWidgetMixin(MinecraftClient minecraftClient, int i, int j, int k, int l) {
        super(minecraftClient, i, j, k, l);
    }

    @SuppressWarnings("MissingUnique")
    public void method_25307(double value) {
        this.setScrollY(value);
    }

    @SuppressWarnings("MissingUnique")
    public double method_25341() {
        return this.getScrollY();
    }
}
