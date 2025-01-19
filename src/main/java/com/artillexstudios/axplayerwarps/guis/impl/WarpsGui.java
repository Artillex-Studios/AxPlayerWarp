package com.artillexstudios.axplayerwarps.guis.impl;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.nms.NMSHandlers;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.AsyncUtils;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axapi.utils.placeholder.Placeholder;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.category.Category;
import com.artillexstudios.axplayerwarps.guis.GuiFrame;
import com.artillexstudios.axplayerwarps.guis.actions.Actions;
import com.artillexstudios.axplayerwarps.input.InputManager;
import com.artillexstudios.axplayerwarps.placeholders.Placeholders;
import com.artillexstudios.axplayerwarps.sorting.WarpComparator;
import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.LANG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class WarpsGui extends GuiFrame {
    private static final Config GUI = new Config(new File(AxPlayerWarps.getInstance().getDataFolder(), "guis/warps.yml"),
            AxPlayerWarps.getInstance().getResource("guis/warps.yml"),
            GeneralSettings.builder().setUseDefaults(false).build(),
            LoaderSettings.builder().build(),
            DumperSettings.DEFAULT,
            UpdaterSettings.builder().build()
    );

    private final PaginatedGui gui = Gui
            .paginated()
            .disableAllInteractions()
            .title(Component.empty())
            .rows(GUI.getInt("rows", 5))
            .pageSize(GUI.getInt("page-size", 27))
            .create();

    private Category category = null;
    private String search = null;

    public WarpsGui(Player player, Category category, String search) {
        this(player, category);
        this.search = search;
    }

    public WarpsGui(Player player, Category category) {
        this(player);
        this.category = category;
    }

    public WarpsGui(Player player) {
        super(GUI, player);
        this.user = Users.get(player);
        setPlaceholder(new Placeholder((player1, s) -> {
            s = s.replace("%search%", search == null ? LANG.getString("placeholders.no-search") : search);
            s = s.replace("%sorting_selected%", user.getSorting().name());
            s = s.replace("%category_selected%", category == null ? LANG.getString("placeholders.no-category") : category.formatted());
            s = s.replace("%my_warps%", "" + WarpManager.getWarps().stream().filter(warp -> warp.getOwner().equals(player.getUniqueId())).count());
            return s;
        }));

        setGui(gui);
    }

    public static boolean reload() {
        return GUI.reload();
    }

    public void open() {
        open(1);
    }

    public void open(int page) {
        createItem("search", event -> {
            Actions.run(player, this, file.getStringList("search.actions"));
            if (event.isShiftClick()) {
                search = null;
                MESSAGEUTILS.sendLang(player, "search.reset");
                open();
                return;
            }
            InputManager.getInput(player, "search", result -> {
                if (result.isBlank()) search = null;
                else search = result;
                if (search == null)
                    MESSAGEUTILS.sendLang(player, "search.reset");
                else
                    MESSAGEUTILS.sendLang(player, "search.show", Map.of("%search%", search));
                open();
            });
        }, Map.of());

        createItem("sorting", event -> {
            Actions.run(player, this, file.getStringList("sorting.actions"));
            if (event.isShiftClick()) {
                user.resetSorting();
            } else {
                if (event.isLeftClick()) user.changeSorting(1);
                if (event.isRightClick()) user.changeSorting(-1);
            }
            open(gui.getCurrentPageNum());
        }, Map.of());

        createItem("category", event -> {
            Actions.run(player, this, file.getStringList("category.actions"));
            if (event.isShiftClick()) {
                user.resetCategory();
                category = null;
                open(gui.getCurrentPageNum());
                return;
            }
            if (event.isLeftClick()) user.changeCategory(1);
            if (event.isRightClick()) user.changeCategory(-1);
            category = user.getCategory();
            open(gui.getCurrentPageNum());
        }, Map.of());

        loadWarps().thenRun(() -> {
            gui.open(player, page);
        });
    }

    @Override
    public void updateTitle() {
        gui.updateTitle(StringUtils.formatToString(GUI.getString("title", ""), new HashMap<>(Map.of("%page%", "" + gui.getCurrentPageNum(), "%pages%", "" + Math.max(1, gui.getPagesNum())))));
    }

    public CompletableFuture<Void> loadWarps() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        AsyncUtils.submit(() -> {
            var filtered = WarpManager.getWarps()
                    .stream()
                    .sorted(new WarpComparator(user.getSorting(), player))
                    .toList();

            GuiItem[] axGuiItems = new GuiItem[filtered.size()];
            List<CompletableFuture<?>> futures = new ArrayList<>();
            int i = 0;
            for (Warp warp : filtered) {

                // category
                if (category != null && !Objects.equals(warp.getCategory(), category)) continue;

                // search
                if (search != null && (!warp.getName().toLowerCase().contains(search) && !warp.getOwnerName().toLowerCase().contains(search))) continue;

                CompletableFuture<Void> completableFuture = new CompletableFuture<>();
                futures.add(completableFuture);

                int i2 = i;
                AsyncUtils.submit(() -> {
                    Material icon = warp.getIcon();
                    ItemBuilder builder = new ItemBuilder(new ItemStack(icon));
                    builder.setName(Placeholders.parse(warp, player, GUI.getString("warp.name")));

                    String[] description = warp.getDescription().split("\n", CONFIG.getInt("warp-description.max-lines", 3));

                    List<String> lore = new ArrayList<>();
                    List<String> lore2 = new ArrayList<>(GUI.getStringList("warp.lore"));
                    for (int j = 0; j < lore2.size(); j++) {
                        String line = lore2.get(j);
                        if (!line.contains("%description%")) {
                            lore.add(line);
                            continue;
                        }
                        for (int k = description.length - 1; k >= 0; k--) {
                            lore.add(j, line.replace("%description%", description[k]));
                        }
                    }
                    builder.setLore(Placeholders.parseList(warp, player, lore));
                    if (icon == Material.PLAYER_HEAD) {
                        final Player pl = Bukkit.getPlayer(warp.getOwner());
                        if (pl != null) {
                            var textures = NMSHandlers.getNmsHandler().textures(pl);
                            if (textures != null) builder.setTextureValue(textures.getKey());
                        }
                    }

                    GuiItem axGuiItem = new GuiItem(builder.get(), event -> {
                        if (event.isLeftClick()) {
                            warp.teleportPlayer(player);
                        } else {
                            new RateWarpGui(player, warp).open();
                        }
                    });

                    axGuiItems[i2] = axGuiItem;
                    completableFuture.complete(null);
                });
                i++;
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                gui.clearPageItems();
                for (GuiItem axGuiItem : axGuiItems) {
                    if (axGuiItem == null) continue;
                    gui.addItem(axGuiItem);
                }

                Scheduler.get().run(scheduledTask -> {
                    updateTitle();
                    future.complete(null);
                });
            });
        });

        return future;
    }
}
