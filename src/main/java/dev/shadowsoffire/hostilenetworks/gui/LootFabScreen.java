package dev.shadowsoffire.hostilenetworks.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.hostilenetworks.util.FabSelection;
import dev.shadowsoffire.hostilenetworks.util.FabSelection.ProductionMode;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.screen.PlaceboContainerScreen;
import dev.shadowsoffire.placebo.util.DrawsOnLeft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class LootFabScreen extends PlaceboContainerScreen<LootFabMenu> implements DrawsOnLeft {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 178;
    public static final ResourceLocation BASE = HostileNetworks.loc("textures/gui/loot_fabricator.png");
    public static final ResourceLocation PLAYER = HostileNetworks.loc("textures/gui/default_gui.png");
    public static final WidgetSprites LEFT_BUTTON = DeepLearnerScreen.makeSprites("widget/fab_left", "widget/fab_left_hovered");
    public static final WidgetSprites RIGHT_BUTTON = DeepLearnerScreen.makeSprites("widget/fab_right", "widget/fab_right_hovered");

    /** The Fixed-mode large preview slot at the existing big-preview position. */
    private static final int PREVIEW_X = 79, PREVIEW_Y = 5;
    /** The production-mode toggle button. An 18x18 tab fully off the right edge of the panel. */
    private static final int MODE_X = WIDTH + 4, MODE_Y = 0;
    /** Clears the entire queue. An 18x18 tab to the right of the mode button; shown only in Queue mode. */
    private static final int CLEAR_X = MODE_X + 20, CLEAR_Y = MODE_Y;
    /** Vanilla barrier icon used as the clear-queue button's glyph. */
    private static final ResourceLocation CLEAR_QUEUE_ICON = ResourceLocation.withDefaultNamespace("textures/item/barrier.png");
    /**
     * The Queue-mode 3x3 grid (off-panel right, below the mode button and the "Current Queue" label). Cells show the
     * queue in insertion order with the cursor entry highlighted. When the queue holds more than {@link #QUEUE_VISIBLE}
     * entries the last cell becomes a "+N" overflow indicator that exposes the hidden entries via its hover tooltip.
     */
    private static final int QUEUE_X = WIDTH + 4, QUEUE_Y = 30, GRID_SLOT_SIZE = 18;
    private static final int QUEUE_COLS = 3, QUEUE_ROWS = 3, QUEUE_VISIBLE = QUEUE_COLS * QUEUE_ROWS;
    /** Y position of the "Current Queue" label rendered above the queue grid. */
    private static final int QUEUE_LABEL_Y = 20;
    /** The redstone-control button. An 18x18 tab off the left edge of the panel; always visible (independent of model). */
    private static final int REDSTONE_X = -22, REDSTONE_Y = 0;

    private DynamicHolder<DataModel> model = DataModelRegistry.INSTANCE.emptyHolder();
    private int currentPage = 0;
    private ImageButton btnLeft, btnRight;

    public LootFabScreen(LootFabMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageHeight = HEIGHT;
        this.imageWidth = WIDTH;
    }

    /** The production configuration for the currently-loaded model, or {@link FabSelection#EMPTY} if none is loaded. */
    private FabSelection selection() {
        return this.model.isBound() ? this.menu.getSelection(this.model.get()) : FabSelection.EMPTY;
    }

    @Override
    public void render(GuiGraphics gfx, int pMouseX, int pMouseY, float pPartialTicks) {
        this.model = DataModelItem.getStoredModel(this.menu.getSlot(0).getItem());

        if (this.model.isBound()) {
            this.btnLeft.visible = this.currentPage > 0;
            this.btnRight.visible = this.currentPage < this.model.get().fabDrops().size() / 9;
        }
        else {
            this.btnLeft.visible = false;
            this.btnRight.visible = false;
        }

        super.render(gfx, pMouseX, pMouseY, pPartialTicks);
    }

    @Override
    public void init() {
        super.init();
        this.btnLeft = this.addRenderableWidget(new ImageButton(this.getGuiLeft() + 13, this.getGuiTop() + 68, 29, 12, LEFT_BUTTON, btn -> {
            if (this.model.isBound() && this.currentPage > 0) this.currentPage--;
        }));

        this.btnRight = this.addRenderableWidget(new ImageButton(this.getGuiLeft() + 46, this.getGuiTop() + 68, 29, 12, RIGHT_BUTTON, btn -> {
            if (this.model.isBound() && this.currentPage < this.model.get().fabDrops().size() / 9) this.currentPage++;
        }));
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int pX, int pY) {
        if (this.model.isBound() && this.selection().mode() == ProductionMode.QUEUE) {
            gfx.drawString(this.font, Component.translatable("hostilenetworks.gui.queue_current"), QUEUE_X, QUEUE_LABEL_Y, Color.AQUA);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics gfx, int pX, int pY) {
        if (this.isHovering(6, 10, 7, 53, pX, pY)) {
            List<Component> txt = new ArrayList<>(2);
            txt.add(Component.translatable("hostilenetworks.gui.energy", this.menu.getEnergyStored(), HostileConfig.fabPowerCap));
            txt.add(Component.translatable("hostilenetworks.gui.fab_cost", HostileConfig.fabPowerCost));
            gfx.renderComponentTooltip(this.font, txt, pX, pY);
        }
        // Redstone-control tooltip is always available (independent of whether a model is loaded).
        if (this.isHovering(REDSTONE_X, REDSTONE_Y, 18, 18, pX, pY)) {
            gfx.renderTooltip(this.font, Component.translatable(this.menu.getRedstoneState().getKey()), pX, pY);
        }
        if (this.model.isBound()) {
            FabSelection sel = this.selection();
            boolean queue = sel.mode() == ProductionMode.QUEUE;
            List<ItemStack> drops = this.model.get().fabDrops();

            if (this.isHovering(MODE_X, MODE_Y, 18, 18, pX, pY)) {
                gfx.renderTooltip(this.font, Component.translatable(sel.mode().getKey()), pX, pY);
            }

            // Top-center preview tooltip: clear-current-item hint in Queue mode, clear-selection hint in Fixed mode.
            int selection = this.menu.getSelectedDrop(this.model.get());
            if (selection != -1 && this.isHovering(PREVIEW_X, PREVIEW_Y, 16, 16, pX, pY)) {
                if (queue) {
                    List<Component> txt = new ArrayList<>(getTooltipFromItem(this.minecraft, drops.get(selection)));
                    txt.add(Component.translatable("hostilenetworks.gui.queue_clear_current"));
                    gfx.renderComponentTooltip(this.font, txt, pX, pY);
                }
                else {
                    gfx.renderComponentTooltip(this.font, Arrays.asList(Component.translatable("hostilenetworks.gui.clear")), pX, pY);
                }
            }

            if (queue) {
                if (this.isHovering(CLEAR_X, CLEAR_Y, 18, 18, pX, pY)) {
                    gfx.renderTooltip(this.font, Component.translatable("hostilenetworks.gui.clear_queue"), pX, pY);
                }

                List<Integer> entries = sel.entries();
                int size = entries.size();
                boolean hasOverflow = size > QUEUE_VISIBLE;
                int visibleEntries = hasOverflow ? QUEUE_VISIBLE - 1 : size;
                for (int i = 0; i < visibleEntries; i++) {
                    int col = i % QUEUE_COLS;
                    int row = i / QUEUE_COLS;
                    if (this.isHovering(QUEUE_X + col * GRID_SLOT_SIZE, QUEUE_Y + row * GRID_SLOT_SIZE, 16, 16, pX, pY)) {
                        int dropIdx = entries.get(i);
                        if (dropIdx >= 0 && dropIdx < drops.size()) {
                            List<Component> txt = new ArrayList<>(getTooltipFromItem(this.minecraft, drops.get(dropIdx)));
                            txt.add(Component.translatable("hostilenetworks.gui.queue_remove"));
                            gfx.renderComponentTooltip(this.font, txt, pX, pY);
                        }
                    }
                }

                // "+N" overflow slot tooltip: list the hidden upcoming entries in order.
                if (hasOverflow) {
                    int col = (QUEUE_VISIBLE - 1) % QUEUE_COLS;
                    int row = (QUEUE_VISIBLE - 1) / QUEUE_COLS;
                    if (this.isHovering(QUEUE_X + col * GRID_SLOT_SIZE, QUEUE_Y + row * GRID_SLOT_SIZE, 16, 16, pX, pY)) {
                        List<Component> txt = new ArrayList<>();
                        for (int j = visibleEntries; j < size; j++) {
                            int dropIdx = entries.get(j);
                            if (dropIdx >= 0 && dropIdx < drops.size()) {
                                txt.add(drops.get(dropIdx).getHoverName());
                            }
                        }
                        if (!txt.isEmpty()) {
                            gfx.renderComponentTooltip(this.font, txt, pX, pY);
                        }
                    }
                }
            }

            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    if (y * 3 + x < Math.min(drops.size() - this.currentPage * 9, 9) && this.isHovering(18 + 18 * x, 10 + 18 * y, 16, 16, pX, pY)) {
                        List<Component> tip = new ArrayList<>(getTooltipFromItem(this.minecraft, drops.get(this.currentPage * 9 + y * 3 + x)));
                        if (queue) tip.add(Component.translatable("hostilenetworks.gui.queue_add"));
                        gfx.pose().pushPose();
                        gfx.pose().translate(-4, 0, 0);
                        this.drawOnLeft(gfx, tip, this.getGuiTop() + 15, Math.min(this.getGuiLeft(), 240));
                        gfx.pose().popPose();
                    }
                }
            }
        }

        super.renderTooltip(gfx, pX, pY);
    }

    @Override
    public boolean mouseClicked(double pX, double pY, int pButton) {
        // Redstone control is always clickable (independent of whether a model is loaded).
        if (this.isHovering(REDSTONE_X, REDSTONE_Y, 18, 18, pX, pY)) {
            this.click(2000 + this.menu.getRedstoneState().next().ordinal());
        }
        if (this.model.isBound()) {
            List<ItemStack> drops = this.model.get().fabDrops();
            FabSelection sel = this.selection();
            boolean queue = sel.mode() == ProductionMode.QUEUE;
            int selection = this.menu.getSelectedDrop(this.model.get());

            // Production mode toggle.
            if (this.isHovering(MODE_X, MODE_Y, 18, 18, pX, pY)) {
                this.click(-2);
            }

            // Drop palette: Fixed selects, Queue appends.
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    int idx = this.currentPage * 9 + y * 3 + x;
                    if (idx < drops.size() && this.isHovering(18 + 18 * x, 10 + 18 * y, 16, 16, pX, pY)) {
                        if (queue || selection != idx) {
                            this.click(idx);
                        }
                    }
                }
            }

            // Preview click: Fixed mode clears the selection; Queue mode removes the cursor entry from the queue.
            if (selection != -1 && this.isHovering(PREVIEW_X, PREVIEW_Y, 16, 16, pX, pY)) {
                if (queue && !sel.entries().isEmpty()) {
                    this.click(1000 + sel.cursor() % sel.entries().size());
                }
                else if (!queue) {
                    this.click(-1);
                }
            }

            if (queue) {
                // Clear-queue button: empties the queue while staying in Queue mode.
                if (this.isHovering(CLEAR_X, CLEAR_Y, 18, 18, pX, pY)) {
                    this.click(-3);
                }
                // Queue grid: click a real entry to remove it. The "+N" overflow slot (when present) is unclickable.
                List<Integer> entries = sel.entries();
                int size = entries.size();
                boolean hasOverflow = size > QUEUE_VISIBLE;
                int visibleEntries = hasOverflow ? QUEUE_VISIBLE - 1 : size;
                for (int i = 0; i < visibleEntries; i++) {
                    int col = i % QUEUE_COLS;
                    int row = i / QUEUE_COLS;
                    if (this.isHovering(QUEUE_X + col * GRID_SLOT_SIZE, QUEUE_Y + row * GRID_SLOT_SIZE, 18, 18, pX, pY)) {
                        this.click(1000 + i);
                    }
                }
            }
        }
        return super.mouseClicked(pX, pY, pButton);
    }

    private void click(int id) {
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float pPartialTicks, int pX, int pY) {
        int left = this.getGuiLeft();
        int top = this.getGuiTop();

        // Loot Fab Window
        gfx.blit(BASE, left, top, 0, 0, 176, 83, 256, 256);

        // Energy Bar
        int energyHeight = Mth.floor(53F * this.menu.getEnergyStored() / HostileConfig.fabPowerCap);
        gfx.blit(BASE, left + 6, top + 10 + 53 - energyHeight, 0, 83, 7, energyHeight, 256, 256);

        // Progress Bar
        int progHeight = Mth.floor(35F * this.menu.getRuntime() / 60F);
        gfx.blit(BASE, left + 84, top + 23 + 35 - progHeight, 7, 83, 6, progHeight, 256, 256);

        // Player Inventory
        gfx.blit(PLAYER, left, top + 88, 0, 0, 176, 90, 256, 256);

        // Redstone control button: an off-panel tab on the left, always visible. Independent of any loaded model.
        gfx.blit(BASE, left + REDSTONE_X, top + REDSTONE_Y, 31, 83, 18, 18, 256, 256);
        gfx.blit(this.menu.getRedstoneState().getResourceLocation(), left + REDSTONE_X + 1, top + REDSTONE_Y + 1, 0, 0, 16, 16, 16, 16);

        if (this.model.isBound()) {
            List<ItemStack> drops = this.model.get().fabDrops();
            FabSelection sel = this.selection();
            boolean queue = sel.mode() == ProductionMode.QUEUE;

            // Drop palette hover highlight.
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    if (y * 3 + x < Math.min(drops.size() - this.currentPage * 9, 9) && this.isHovering(18 + 18 * x, 10 + 18 * y, 16, 16, pX, pY)) {
                        gfx.blit(BASE, left + 16 + 19 * x, top + 8 + 19 * y, 13, 83, 18, 18, 256, 256);
                    }
                }
            }

            int selection = this.menu.getSelectedDrop(this.model.get());
            // Fixed-mode palette highlight on the currently-selected drop.
            if (!queue && selection != -1 && selection / 9 == this.currentPage) {
                int selIdx = selection - this.currentPage * 9;
                gfx.blit(BASE, left + 16 + 19 * (selIdx % 3), top + 8 + 19 * (selIdx / 3), 31, 83, 18, 18, 256, 256);
            }
            // Top-center preview shows the active drop in both modes:
            // FIXED -> the chosen drop; QUEUE -> the cursor entry (what's being fabricated next).
            if (selection != -1) {
                gfx.renderItem(drops.get(selection), left + PREVIEW_X, top + PREVIEW_Y);
                gfx.renderItemDecorations(this.font, drops.get(selection), left + PREVIEW_X - 1, top + PREVIEW_Y - 1);
            }
            if (queue) {
                this.renderQueueGrid(gfx, left, top, sel, drops);
            }

            // Drop palette items.
            int gridLeft = left + 17;
            int gridTop = top + 9;
            for (int i = 0; i < Math.min(drops.size() - this.currentPage * 9, 9); i++) {
                int x = i % 3;
                int y = i / 3;
                gfx.renderItem(drops.get(i + this.currentPage * 9), gridLeft + x * 19, gridTop + y * 19);
                gfx.renderItemDecorations(this.font, drops.get(i + this.currentPage * 9), gridLeft + x * 19 - 1, gridTop + y * 19 - 1);
            }

            // Production mode button: frame + the mode's icon.
            gfx.blit(BASE, left + MODE_X, top + MODE_Y, 31, 83, 18, 18, 256, 256);
            gfx.blit(sel.mode().getResourceLocation(), left + MODE_X + 1, top + MODE_Y + 1, 0, 0, 16, 16, 16, 16);

            // Clear-queue button: only shown in Queue mode.
            if (queue) {
                gfx.blit(BASE, left + CLEAR_X, top + CLEAR_Y, 31, 83, 18, 18, 256, 256);
                gfx.blit(CLEAR_QUEUE_ICON, left + CLEAR_X + 1, top + CLEAR_Y + 1, 0, 0, 16, 16, 16, 16);
            }
        }
    }

    /**
     * Renders the Queue-mode 3x3 grid in insertion order. The cursor entry uses the brighter "selected" sprite to
     * stand out; other cells use the milder "hover" sprite as a neutral frame. When the queue exceeds
     * {@link #QUEUE_VISIBLE} entries, the last cell becomes a "+N" overflow indicator (whose remaining items are
     * exposed via its hover tooltip).
     */
    private void renderQueueGrid(GuiGraphics gfx, int left, int top, FabSelection sel, List<ItemStack> drops) {
        List<Integer> entries = sel.entries();
        int size = entries.size();
        boolean hasOverflow = size > QUEUE_VISIBLE;
        int visibleEntries = hasOverflow ? QUEUE_VISIBLE - 1 : size;
        int cursor = entries.isEmpty() ? -1 : sel.cursor() % size;

        for (int i = 0; i < QUEUE_VISIBLE; i++) {
            int col = i % QUEUE_COLS;
            int row = i / QUEUE_COLS;
            int cx = left + QUEUE_X + col * GRID_SLOT_SIZE;
            int cy = top + QUEUE_Y + row * GRID_SLOT_SIZE;
            boolean isCursor = i < visibleEntries && i == cursor;
            gfx.blit(BASE, cx, cy, isCursor ? 31 : 13, 83, 18, 18, 256, 256);

            if (i < visibleEntries) {
                int dropIdx = entries.get(i);
                if (dropIdx >= 0 && dropIdx < drops.size()) {
                    gfx.renderItem(drops.get(dropIdx), cx + 1, cy + 1);
                    gfx.renderItemDecorations(this.font, drops.get(dropIdx), cx, cy);
                }
            }
            else if (i == QUEUE_VISIBLE - 1 && hasOverflow) {
                // Overflow cell: render "+N" centered in the 16x16 slot area.
                String txt = "+" + (size - visibleEntries);
                int textX = cx + 1 + (16 - this.font.width(txt)) / 2;
                int textY = cy + 2 + (16 - this.font.lineHeight) / 2;
                gfx.drawString(this.font, txt, textX, textY, Color.WHITE, true);
            }
        }
    }

}
