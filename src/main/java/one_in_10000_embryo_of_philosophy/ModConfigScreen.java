package one_in_10000_embryo_of_philosophy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ModConfigScreen extends Screen {
    private final Screen parent;

    public ModConfigScreen(Screen parent) {
        super(Component.literal("1 in 10000 Embryo of Philosophy Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 5;

        Checkbox embryoCheckbox = new Checkbox(
            centerX - 100, startY, 200, 20,
            Component.literal("embryo_of_philosophy"),
            ModConfig.EMBRYO_OF_PHILOSOPHY.get()
        ) {
            @Override
            public void onPress() {
                super.onPress();
                ModConfig.EMBRYO_OF_PHILOSOPHY.set(this.selected());
                ModConfig.SPEC.save();
            }
        };
        this.addRenderableWidget(embryoCheckbox);

        Checkbox irontombCheckbox = new Checkbox(
            centerX - 100, startY + 30, 200, 20,
            Component.literal("Irontomb (WIP)"),
            false
        );
        irontombCheckbox.active = false;
        this.addRenderableWidget(irontombCheckbox);

        Checkbox debugCheckbox = new Checkbox(
            centerX - 100, startY + 60, 200, 20,
            Component.literal("Debug Mode (warning)"),
            ModConfig.DEBUG.get()
        ) {
            @Override
            public void onPress() {
                super.onPress();
                ModConfig.DEBUG.set(this.selected());
                ModConfig.SPEC.save();
            }
        };
        this.addRenderableWidget(debugCheckbox);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(centerX - 100, startY + 125, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        int centerX = this.width / 2;
        int startY = this.height / 5;

        String warningText = "anyone can summon any amounts and/or kill sras:embryo_of_philosophy if debug is enabled";
        guiGraphics.drawCenteredString(this.font, Component.literal(warningText), centerX, startY + 88, 0xFF5555);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
