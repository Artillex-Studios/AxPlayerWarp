package com.artillexstudios.axplayerwarps.guis.impl;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.NumberUtils;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axapi.utils.placeholder.Placeholder;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.guis.GuiFrame;
import com.artillexstudios.axplayerwarps.guis.actions.Actions;
import com.artillexstudios.axplayerwarps.input.InputManager;
import com.artillexstudios.axplayerwarps.placeholders.Placeholders;
import com.artillexstudios.axplayerwarps.utils.StarUtils;
import com.artillexstudios.axplayerwarps.warps.Warp;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.LANG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class RateWarpGui extends GuiFrame {
    private static final Config GUI = new Config(new File(AxPlayerWarps.getInstance().getDataFolder(), "guis/rate-warp.yml"),
            AxPlayerWarps.getInstance().getResource("guis/rate-warp.yml"),
            GeneralSettings.builder().setUseDefaults(false).build(),
            LoaderSettings.builder().build(),
            DumperSettings.DEFAULT,
            UpdaterSettings.builder().build()
    );

    private final Gui gui;
    private final Warp warp;

    public RateWarpGui(Player player, Warp warp) {
        super(GUI, player);
        setPlaceholder(new Placeholder((pl, s) -> {
            Integer rating = warp.getAllRatings().get(player.getUniqueId());
            s = s.replace("%given_rating_decimal%", rating == null ? "" : Placeholders.df.format(rating));
            s = s.replace("%given_rating_stars%", rating == null ? LANG.getString("placeholders.no-rating") : StarUtils.getFormatted(rating, 5));
            return s;
        }));
        this.warp = warp;
        this.gui = Gui.gui()
            .disableAllInteractions()
            .title(StringUtils.format(GUI.getString("title", ""), Map.of("%warp%", warp.getName())))
            .rows(GUI.getInt("rows", 5))
            .create();

        setWarp(warp);
        setGui(gui);
    }

    public static boolean reload() {
        return GUI.reload();
    }

    public void open() {
        boolean isFavorite = user.getFavorites().contains(warp);
        createItem("favorite." + (isFavorite ? "favorite" : "not-favorite"), event -> {
            Actions.run(player, this, file.getStringList("favorite.actions"));
            AxPlayerWarps.getThreadedQueue().submit(() -> {
                if (isFavorite) {
                    AxPlayerWarps.getDatabase().removeFromFavorites(player, warp);
                    MESSAGEUTILS.sendLang(player, "favorite.remove", Map.of("%warp%", warp.getName()));
                } else {
                    AxPlayerWarps.getDatabase().addToFavorites(player, warp);
                    MESSAGEUTILS.sendLang(player, "favorite.add", Map.of("%warp%", warp.getName()));
                }
                Scheduler.get().run(this::open);
            });
        }, Map.of(), getSlots("favorite"));

        createItem("teleport", event -> {
            Actions.run(player, this, file.getStringList("teleport.actions"));
            warp.teleportPlayer(player);
        }, Map.of());

        createItem("rate", event -> {
            Actions.run(player, this, file.getStringList("rate.actions"));
            if (event.isRightClick()) {
                AxPlayerWarps.getThreadedQueue().submit(() -> {
                    AxPlayerWarps.getDatabase().removeRating(player, warp);
                    MESSAGEUTILS.sendLang(player, "rate.remove");
                    Scheduler.get().run(this::open);
                });
            }
            if (event.isLeftClick()) {
                InputManager.getInput(player, "rate", result -> {
                    if (!NumberUtils.isInt(result)) {
                        MESSAGEUTILS.sendLang(player, "errors.not-a-number");
                    } else {
                        int i = Math.max(1, Math.min(5, Integer.parseInt(result)));
                        AxPlayerWarps.getThreadedQueue().submit(() -> {
                            AxPlayerWarps.getDatabase().setRating(player, warp, i);
                            MESSAGEUTILS.sendLang(player, "rate.add", Map.of("%rating%", "" + i));
                        });
                    }
                    Scheduler.get().run(this::open);
                });
            }
        }, Map.of());

        gui.update();
        gui.open(player);
    }
}
