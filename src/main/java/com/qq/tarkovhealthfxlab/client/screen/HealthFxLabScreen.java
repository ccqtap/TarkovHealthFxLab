package com.qq.tarkovhealthfxlab.client.screen;

import com.qq.tarkovhealthfxlab.client.BodyEffect;
import com.qq.tarkovhealthfxlab.client.BodyRegion;
import com.qq.tarkovhealthfxlab.client.HeadRedirectClientStatus;
import com.qq.tarkovhealthfxlab.client.HealthFxClientConfig;
import com.qq.tarkovhealthfxlab.client.HealthFxController;
import com.qq.tarkovhealthfxlab.client.HealthFxPreset;
import com.qq.tarkovhealthfxlab.client.HealthFxSource;
import com.qq.tarkovhealthfxlab.client.HealthFxState;
import com.qq.tarkovhealthfxlab.client.compat.ClientFxProviderStatus;
import com.qq.tarkovhealthfxlab.client.overlay.HealthOverlayRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public final class HealthFxLabScreen extends Screen {
    private static final int CONTROL_WIDTH = 154;

    private Page page = Page.INJURY;
    private Button sourceButton;
    private Button pageButton;
    private Button regionButton;
    private Button bleedButton;
    private Button fractureButton;
    private Button blackenedButton;
    private Button analgesiaButton;
    private Button repairButton;
    private Button regenerationButton;
    private Button clearButton;
    private Button headRedirectButton;
    private Button screenModeButton;
    private Button audioButton;
    private Button motionButton;
    private Button chromaticButton;
    private Button heartbeatButton;
    private Button contrastButton;
    private ValueSlider painSlider;
    private ValueSlider healthSlider;
    private final List<Button> presetButtons = new ArrayList<>();

    public HealthFxLabScreen() {
        super(Component.translatable("screen.tarkov_health_fx_lab.title"));
    }

    @Override
    protected void init() {
        clearPageReferences();
        this.presetButtons.clear();
        int left = this.width / 2 - CONTROL_WIDTH - 4;
        int right = this.width / 2 + 4;
        int y = 62;

        this.sourceButton = addRenderableWidget(button(left, y, this::sourceText, ignored -> {
            HealthFxController.cycleSource();
            refreshControls();
        }));
        this.pageButton = addRenderableWidget(button(right, y, this::pageText, ignored -> {
            this.page = this.page.next();
            clearWidgets();
            init();
        }));

        switch (this.page) {
            case INJURY -> initInjuryPage(left, right, y + 24);
            case SERVER -> initServerPage(left, right, y + 24);
            case PRESENTATION -> initPresentationPage(left, right, y + 24);
        }
        refreshControls();
    }

    private void clearPageReferences() {
        this.regionButton = null;
        this.bleedButton = null;
        this.fractureButton = null;
        this.blackenedButton = null;
        this.analgesiaButton = null;
        this.repairButton = null;
        this.regenerationButton = null;
        this.clearButton = null;
        this.headRedirectButton = null;
        this.screenModeButton = null;
        this.audioButton = null;
        this.motionButton = null;
        this.chromaticButton = null;
        this.heartbeatButton = null;
        this.contrastButton = null;
        this.painSlider = null;
        this.healthSlider = null;
    }

    private void initInjuryPage(int left, int right, int y) {
        this.regionButton = addRenderableWidget(button(left, y, this::regionText, ignored -> {
            HealthFxController.cycleSelectedRegion();
            refreshControls();
        }));
        this.healthSlider = addRenderableWidget(new ValueSlider(
                right, y, CONTROL_WIDTH, "screen.tarkov_health_fx_lab.part_health",
                () -> HealthFxController.selectedEffect().healthRatio(),
                HealthFxController::setSelectedPartHealthRatio));

        y += 24;
        this.bleedButton = addRenderableWidget(button(left, y, this::bleedText, ignored -> {
            HealthFxController.cycleBleeding();
            refreshControls();
        }));
        this.fractureButton = addRenderableWidget(button(right, y, this::fractureText, ignored -> {
            HealthFxController.toggleFracture();
            refreshControls();
        }));

        y += 24;
        this.painSlider = addRenderableWidget(new ValueSlider(
                left, y, CONTROL_WIDTH, "screen.tarkov_health_fx_lab.pain",
                () -> HealthFxController.selectedEffect().pain() / 100.0D,
                value -> HealthFxController.setPain(value * 100.0D)));
        this.blackenedButton = addRenderableWidget(button(right, y, this::blackenedText, ignored -> {
            HealthFxController.toggleBlackened();
            refreshControls();
        }));

        y += 28;
        addPresetButtons(y);
        addDoneButton(Math.min(this.height - 24, y + 48));
    }

    private void initServerPage(int left, int right, int y) {
        this.analgesiaButton = addRenderableWidget(button(left, y, this::analgesiaText, ignored -> {
            HealthFxController.requestAnalgesia(!HealthFxController.displayedState().painkillerActive());
            refreshControls();
        }));
        this.repairButton = addRenderableWidget(button(right, y, this::repairText, ignored -> {
            HealthFxController.requestRepair();
            refreshControls();
        }));

        y += 24;
        this.regenerationButton = addRenderableWidget(button(left, y, this::regenerationText, ignored -> {
            HealthFxController.requestRegeneration();
            refreshControls();
        }));
        this.clearButton = addRenderableWidget(button(right, y,
                () -> Component.translatable("screen.tarkov_health_fx_lab.clear_truth"), ignored -> {
                    HealthFxController.requestClear();
                    refreshControls();
                }));

        y += 24;
        this.headRedirectButton = addRenderableWidget(button(left, y, this::headRedirectText, ignored -> {
            HealthFxController.requestHeadRedirectToggle();
            refreshControls();
        }));
        addRenderableWidget(button(right, y,
                () -> Component.translatable("screen.tarkov_health_fx_lab.head_test"), ignored -> {
                    HealthFxController.requestHeadRedirectTest();
                    refreshControls();
                }));
        addDoneButton(Math.min(this.height - 24, y + 76));
    }

    private void initPresentationPage(int left, int right, int y) {
        this.screenModeButton = addRenderableWidget(button(left, y, this::screenModeText, ignored -> {
            HealthFxClientConfig.SCREEN_EFFECTS.set(HealthFxClientConfig.SCREEN_EFFECTS.get().next());
            refreshControls();
        }));
        this.audioButton = addRenderableWidget(button(right, y, this::audioText, ignored -> {
            HealthFxClientConfig.AUDIO_ENABLED.set(!HealthFxClientConfig.AUDIO_ENABLED.get());
            refreshControls();
        }));

        y += 24;
        this.motionButton = addRenderableWidget(button(left, y, this::motionText, ignored -> {
            HealthFxClientConfig.REDUCE_MOTION.set(!HealthFxClientConfig.REDUCE_MOTION.get());
            refreshControls();
        }));
        this.chromaticButton = addRenderableWidget(button(right, y, this::chromaticText, ignored -> {
            HealthFxClientConfig.CHROMATIC_OFFSET.set(!HealthFxClientConfig.CHROMATIC_OFFSET.get());
            refreshControls();
        }));

        y += 24;
        this.heartbeatButton = addRenderableWidget(button(left, y, this::heartbeatText, ignored -> {
            HealthFxClientConfig.DISABLE_HEARTBEAT.set(!HealthFxClientConfig.DISABLE_HEARTBEAT.get());
            refreshControls();
        }));
        this.contrastButton = addRenderableWidget(button(right, y, this::contrastText, ignored -> {
            HealthFxClientConfig.HIGH_CONTRAST.set(!HealthFxClientConfig.HIGH_CONTRAST.get());
            refreshControls();
        }));

        y += 24;
        addRenderableWidget(new ValueSlider(
                left, y, CONTROL_WIDTH, "screen.tarkov_health_fx_lab.intensity",
                HealthFxClientConfig.MASTER_INTENSITY::get,
                HealthFxClientConfig.MASTER_INTENSITY::set));
        addRenderableWidget(new ValueSlider(
                right, y, CONTROL_WIDTH, "screen.tarkov_health_fx_lab.camera",
                HealthFxClientConfig.CAMERA_INTENSITY::get,
                HealthFxClientConfig.CAMERA_INTENSITY::set));
        y += 24;
        addRenderableWidget(new ValueSlider(
                left, y, CONTROL_WIDTH * 2 + 8, "screen.tarkov_health_fx_lab.ringing_volume",
                HealthFxClientConfig.RINGING_VOLUME::get,
                HealthFxClientConfig.RINGING_VOLUME::set));
        addDoneButton(Math.min(this.height - 24, y + 32));
    }

    private void addPresetButtons(int y) {
        HealthFxPreset[] presets = HealthFxPreset.values();
        int columns = 5;
        int gap = 3;
        int usable = Math.min(this.width - 12, 394);
        int presetWidth = Math.max(48, (usable - gap * (columns - 1)) / columns);
        int totalWidth = presetWidth * columns + gap * (columns - 1);
        int presetLeft = this.width / 2 - totalWidth / 2;
        for (int index = 0; index < presets.length; index++) {
            HealthFxPreset preset = presets[index];
            int px = presetLeft + (index % columns) * (presetWidth + gap);
            int py = y + (index / columns) * 22;
            Button presetButton = addRenderableWidget(Button.builder(presetText(preset), ignored -> {
                        HealthFxController.applyScene(preset);
                        refreshControls();
                    })
                    .bounds(px, py, presetWidth, 20)
                    .build());
            this.presetButtons.add(presetButton);
        }
    }

    private void addDoneButton(int y) {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> onClose())
                .bounds(this.width / 2 - 75, y, 150, 20)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        refreshControls();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        HealthOverlayRenderer.render(graphics, partialTick, this.width, this.height, false);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFF2F2F2);
        graphics.drawCenteredString(this.font, Component.translatable(HealthFxController.liveStatusKey()),
                this.width / 2, 21, 0xFF9BC8C3);

        ClientFxProviderStatus providerStatus = HealthFxController.providerStatus();
        drawTrimmedCentered(graphics, providerStatus.visualProviderDisplay(), 34, 0xFF8FA6B2);
        drawTrimmedCentered(graphics, providerStatus.externalProviderDisplay(), 45, 0xFF8FA6B2);

        if (this.page == Page.INJURY) {
            BodyEffect effect = HealthFxController.selectedEffect();
            String truth = Component.translatable(
                    "screen.tarkov_health_fx_lab.selected_truth",
                    Component.translatable("healthfx.part." + HealthFxController.selectedRegion().id()),
                    oneDecimal(effect.currentHealth()),
                    oneDecimal(effect.maximumHealth())
            ).getString();
            drawTrimmedCentered(graphics, truth, 151, 0xFFE4DDCF);
        } else if (this.page == Page.SERVER) {
            HealthFxState state = HealthFxController.displayedState();
            String conditions = Component.translatable(
                    "screen.tarkov_health_fx_lab.truth_summary",
                    state.bleedingPartCount(), state.fractureCount(), state.blackenedCount(),
                    Math.round(state.rawPain())
            ).getString();
            drawTrimmedCentered(graphics, conditions, 159, 0xFFE4DDCF);
            String last = Component.translatable("screen.tarkov_health_fx_lab.head_last",
                    HeadRedirectClientStatus.lastRecord()).getString();
            drawTrimmedCentered(graphics, last, 172, 0xFFC5BDAF);
            drawTrimmedCentered(graphics,
                    Component.translatable(HealthFxController.serverControlAvailable()
                            ? "healthfx.lab.permission_ok" : "healthfx.lab.permission_required").getString(),
                    185, HealthFxController.serverControlAvailable() ? 0xFF8FD19C : 0xFFE19A77);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTrimmedCentered(GuiGraphics graphics, String text, int y, int color) {
        String trimmed = this.font.plainSubstrByWidth(text, Math.max(80, this.width - 16));
        graphics.drawCenteredString(this.font, Component.literal(trimmed), this.width / 2, y, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Button button(int x, int y, TextSupplier text, Button.OnPress action) {
        return Button.builder(text.get(), action).bounds(x, y, CONTROL_WIDTH, 20).build();
    }

    private void refreshControls() {
        if (this.sourceButton == null) return;
        this.sourceButton.setMessage(sourceText());
        this.pageButton.setMessage(pageText());
        if (this.regionButton != null) this.regionButton.setMessage(regionText());
        if (this.bleedButton != null) this.bleedButton.setMessage(bleedText());
        if (this.fractureButton != null) this.fractureButton.setMessage(fractureText());
        if (this.blackenedButton != null) this.blackenedButton.setMessage(blackenedText());
        if (this.analgesiaButton != null) this.analgesiaButton.setMessage(analgesiaText());
        if (this.repairButton != null) this.repairButton.setMessage(repairText());
        if (this.regenerationButton != null) this.regenerationButton.setMessage(regenerationText());
        if (this.headRedirectButton != null) this.headRedirectButton.setMessage(headRedirectText());
        if (this.screenModeButton != null) this.screenModeButton.setMessage(screenModeText());
        if (this.audioButton != null) this.audioButton.setMessage(audioText());
        if (this.motionButton != null) this.motionButton.setMessage(motionText());
        if (this.chromaticButton != null) this.chromaticButton.setMessage(chromaticText());
        if (this.heartbeatButton != null) this.heartbeatButton.setMessage(heartbeatText());
        if (this.contrastButton != null) this.contrastButton.setMessage(contrastText());

        boolean editable = HealthFxController.isEditable();
        boolean limb = HealthFxController.selectedRegion().isArm()
                || HealthFxController.selectedRegion().isLeg();
        if (this.bleedButton != null) this.bleedButton.active = editable;
        if (this.fractureButton != null) this.fractureButton.active = editable && limb;
        if (this.blackenedButton != null) this.blackenedButton.active = editable && limb;
        if (this.painSlider != null) {
            this.painSlider.active = editable;
            this.painSlider.sync();
        }
        if (this.healthSlider != null) {
            this.healthSlider.active = editable;
            this.healthSlider.sync();
        }
        boolean scenesAvailable = HealthFxController.source() == HealthFxSource.MOCK
                || HealthFxController.source() == HealthFxSource.LAB_SERVER
                && HealthFxController.serverControlAvailable();
        for (Button presetButton : this.presetButtons) {
            presetButton.active = scenesAvailable;
        }

        boolean server = HealthFxController.source() == HealthFxSource.LAB_SERVER
                && HealthFxController.serverControlAvailable();
        if (this.analgesiaButton != null) {
            this.analgesiaButton.active = HealthFxController.source() == HealthFxSource.MOCK || server;
        }
        if (this.repairButton != null) this.repairButton.active = server;
        if (this.regenerationButton != null) this.regenerationButton.active = server;
        if (this.clearButton != null) this.clearButton.active = server;
        if (this.headRedirectButton != null) this.headRedirectButton.active = server;
    }

    private Component sourceText() {
        String key = switch (HealthFxController.source()) {
            case MOCK -> "healthfx.source.mock";
            case LAB_SERVER -> "healthfx.source.lab";
            case TARKOV_LIVE -> "healthfx.source.tarkov";
        };
        return Component.translatable("screen.tarkov_health_fx_lab.source", Component.translatable(key));
    }

    private Component pageText() {
        return Component.translatable("screen.tarkov_health_fx_lab.page",
                Component.translatable(this.page.translationKey));
    }

    private Component regionText() {
        return Component.translatable("screen.tarkov_health_fx_lab.part",
                Component.translatable("healthfx.part." + HealthFxController.selectedRegion().id()));
    }

    private Component bleedText() {
        return Component.translatable("screen.tarkov_health_fx_lab.bleed",
                Component.translatable("healthfx.bleed."
                        + HealthFxController.selectedEffect().bleeding().name().toLowerCase(Locale.ROOT)));
    }

    private Component fractureText() {
        return Component.translatable("screen.tarkov_health_fx_lab.fracture",
                onOff(HealthFxController.selectedEffect().fractured()));
    }

    private Component blackenedText() {
        return Component.translatable("screen.tarkov_health_fx_lab.blackened",
                onOff(HealthFxController.selectedEffect().blackened()));
    }

    private Component analgesiaText() {
        return Component.translatable("screen.tarkov_health_fx_lab.analgesia",
                onOff(HealthFxController.displayedState().painkillerActive()));
    }

    private Component repairText() {
        return Component.translatable("screen.tarkov_health_fx_lab.repair",
                onOff(HealthFxController.labSnapshot().repair()));
    }

    private Component regenerationText() {
        return Component.translatable("screen.tarkov_health_fx_lab.regeneration",
                onOff(HealthFxController.labSnapshot().regeneration()));
    }

    private Component headRedirectText() {
        String key = switch (HeadRedirectClientStatus.state()) {
            case UNKNOWN -> "healthfx.state.unknown";
            case ENABLED -> "options.on";
            case DISABLED -> "options.off";
        };
        return Component.translatable("screen.tarkov_health_fx_lab.head_redirect",
                Component.translatable(key));
    }

    private Component screenModeText() {
        return Component.translatable("screen.tarkov_health_fx_lab.screen_fx",
                Component.translatable("healthfx.mode."
                        + HealthFxClientConfig.SCREEN_EFFECTS.get().name().toLowerCase(Locale.ROOT)));
    }

    private Component audioText() {
        return Component.translatable("screen.tarkov_health_fx_lab.audio",
                onOff(HealthFxClientConfig.AUDIO_ENABLED.get()));
    }

    private Component motionText() {
        return Component.translatable("screen.tarkov_health_fx_lab.reduce_motion",
                onOff(HealthFxClientConfig.REDUCE_MOTION.get()));
    }

    private Component chromaticText() {
        return Component.translatable("screen.tarkov_health_fx_lab.chromatic",
                onOff(HealthFxClientConfig.CHROMATIC_OFFSET.get()));
    }

    private Component heartbeatText() {
        return Component.translatable("screen.tarkov_health_fx_lab.heartbeat",
                onOff(!HealthFxClientConfig.DISABLE_HEARTBEAT.get()));
    }

    private Component contrastText() {
        return Component.translatable("screen.tarkov_health_fx_lab.contrast",
                onOff(HealthFxClientConfig.HIGH_CONTRAST.get()));
    }

    private static Component presetText(HealthFxPreset preset) {
        return Component.translatable("healthfx.preset." + preset.name().toLowerCase(Locale.ROOT));
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @FunctionalInterface
    private interface TextSupplier {
        Component get();
    }

    private enum Page {
        INJURY("healthfx.page.injury"),
        SERVER("healthfx.page.server"),
        PRESENTATION("healthfx.page.presentation");

        private final String translationKey;

        Page(String translationKey) {
            this.translationKey = translationKey;
        }

        private Page next() {
            Page[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final String translationKey;
        private final DoubleSupplier reader;
        private final DoubleConsumer writer;

        private ValueSlider(
                int x,
                int y,
                int width,
                String translationKey,
                DoubleSupplier reader,
                DoubleConsumer writer
        ) {
            super(x, y, width, 20, Component.empty(), clamp(reader.getAsDouble()));
            this.translationKey = translationKey;
            this.reader = reader;
            this.writer = writer;
            updateMessage();
        }

        private void sync() {
            this.value = clamp(this.reader.getAsDouble());
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(this.translationKey, Math.round(this.value * 100.0D)));
        }

        @Override
        protected void applyValue() {
            this.writer.accept(clamp(this.value));
        }

        private static double clamp(double value) {
            return Math.max(0.0D, Math.min(1.0D, value));
        }
    }
}
