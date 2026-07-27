package io.github.ximu.autofish.client.screen;

import io.github.ximu.autofish.client.config.AutoFishConfig;
import io.github.ximu.autofish.client.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public final class AutoFishConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;

    private final Screen parent;
    private final ConfigManager configManager;
    private AutoFishConfig working;
    private int rightColumnX;
    private int regexLabelY;

    public AutoFishConfigScreen(Screen parent, ConfigManager configManager) {
        super(Component.translatable("screen.autofish.title"));
        this.parent = parent;
        this.configManager = configManager;
        this.working = configManager.get().copy();
    }

    @Override
    protected void init() {
        int gap = 8;
        int columnWidth = Math.min(200, (width - 24 - gap) / 2);
        int left = width / 2 - columnWidth - gap / 2;
        int right = width / 2 + gap / 2;
        int top = 34;
        rightColumnX = right;

        addToggle(left, top, columnWidth, "option.autofish.enabled", () -> working.enabled, value -> working.enabled = value);
        addToggle(left, top + ROW_HEIGHT, columnWidth, "option.autofish.multi_rod", () -> working.multiRod, value -> working.multiRod = value);
        addToggle(left, top + ROW_HEIGHT * 2, columnWidth, "option.autofish.no_break", () -> working.noBreak, value -> working.noBreak = value);
        addToggle(left, top + ROW_HEIGHT * 3, columnWidth, "option.autofish.persistent", () -> working.persistentMode,
                value -> working.persistentMode = value);
        addStepper(left, top + ROW_HEIGHT * 4, columnWidth, "option.autofish.recast_delay", () -> working.recastDelayMs,
                value -> working.recastDelayMs = value, 100, 500, 5000, " ms");

        addToggle(right, top, columnWidth, "option.autofish.sound_detection", () -> working.useSoundDetection,
                value -> working.useSoundDetection = value);
        addToggle(right, top + ROW_HEIGHT, columnWidth, "option.autofish.force_multiplayer",
                () -> working.forceMultiplayerDetection, value -> working.forceMultiplayerDetection = value);
        addToggle(right, top + ROW_HEIGHT * 2, columnWidth, "option.autofish.show_hud", () -> working.showHud,
                value -> working.showHud = value);
        addToggle(right, top + ROW_HEIGHT * 3, columnWidth, "option.autofish.require_sneaking",
                () -> working.requireSneaking, value -> working.requireSneaking = value);
        addToggle(right, top + ROW_HEIGHT * 4, columnWidth, "option.autofish.run_while_paused",
                () -> working.runWhilePaused, value -> working.runWhilePaused = value);

        regexLabelY = top + ROW_HEIGHT * 5;
        EditBox regex = new EditBox(font, right, regexLabelY + 10, columnWidth, 20,
                Component.translatable("option.autofish.clear_lag_regex"));
        regex.setMaxLength(512);
        regex.setValue(working.clearLagRegex);
        regex.setHint(Component.translatable("option.autofish.clear_lag_regex"));
        regex.setResponder(value -> working.clearLagRegex = value);
        addRenderableWidget(regex);

        int bottom = height - 28;
        int commandWidth = Math.min(100, (width - 32) / 3);
        int commandGap = 4;
        int totalWidth = commandWidth * 3 + commandGap * 2;
        int commandLeft = (width - totalWidth) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
                .pos(commandLeft, bottom).size(commandWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("controls.reset"), button -> reset())
                .pos(commandLeft + commandWidth + commandGap, bottom).size(commandWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .pos(commandLeft + (commandWidth + commandGap) * 2, bottom).size(commandWidth, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 14, CommonColors.WHITE);
        graphics.text(font, Component.translatable("option.autofish.clear_lag_regex"), rightColumnX, regexLabelY,
                CommonColors.WHITE);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void saveAndClose() {
        configManager.replace(working);
        minecraft.setScreen(parent);
    }

    private void reset() {
        working = new AutoFishConfig();
        rebuildWidgets();
    }

    private void addToggle(int x, int y, int width, String labelKey, BooleanGetter getter, BooleanSetter setter) {
        Button button = Button.builder(toggleLabel(labelKey, getter.get()), pressed -> {
            boolean value = !getter.get();
            setter.set(value);
            pressed.setMessage(toggleLabel(labelKey, value));
        }).pos(x, y).size(width, 20).build();
        addRenderableWidget(button);
    }

    private void addStepper(int x, int y, int width, String labelKey, IntGetter getter, IntSetter setter,
                            int step, int minimum, int maximum, String suffix) {
        int sideWidth = 24;
        int gap = 4;
        int valueWidth = width - sideWidth * 2 - gap * 2;
        Button[] valueButton = new Button[1];
        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            setter.set(Math.max(minimum, getter.get() - step));
            valueButton[0].setMessage(stepperLabel(labelKey, getter.get(), suffix));
        }).pos(x, y).size(sideWidth, 20).build());

        valueButton[0] = addRenderableWidget(Button.builder(stepperLabel(labelKey, getter.get(), suffix), button -> {
        }).pos(x + sideWidth + gap, y).size(valueWidth, 20).build());
        valueButton[0].active = false;

        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            setter.set(Math.min(maximum, getter.get() + step));
            valueButton[0].setMessage(stepperLabel(labelKey, getter.get(), suffix));
        }).pos(x + width - sideWidth, y).size(sideWidth, 20).build());
    }

    private static Component toggleLabel(String labelKey, boolean value) {
        return Component.translatable(labelKey).append(": ")
                .append(Component.translatable(value ? "options.on" : "options.off"));
    }

    private static Component stepperLabel(String labelKey, int value, String suffix) {
        return Component.translatable(labelKey).append(": " + value + suffix);
    }

    @FunctionalInterface
    private interface BooleanGetter {
        boolean get();
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }

    @FunctionalInterface
    private interface IntGetter {
        int get();
    }

    @FunctionalInterface
    private interface IntSetter {
        void set(int value);
    }
}
