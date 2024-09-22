package com.artillexstudios.axplayerwarps.hooks.currency;

import me.mraxetv.beasttokens.api.BeastTokensAPI;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CURRENCIES;

public class BeastTokensHook implements CurrencyHook {

    @Override
    public void setup() {
    }

    @Override
    public String getName() {
        return "BeastTokens";
    }

    @Override
    public String getDisplayName() {
        return CURRENCIES.getString("currencies.BeastTokens.name");
    }

    @Override
    public boolean worksOffline() {
        return true;
    }

    @Override
    public boolean usesDouble() {
        return true;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public double getBalance(@NotNull UUID player) {
        return BeastTokensAPI.getTokensManager().getTokens(Bukkit.getOfflinePlayer(player));
    }

    @Override
    public void giveBalance(@NotNull UUID player, double amount) {
        BeastTokensAPI.getTokensManager().addTokens(Bukkit.getOfflinePlayer(player), amount);
    }

    @Override
    public void takeBalance(@NotNull UUID player, double amount) {
        BeastTokensAPI.getTokensManager().removeTokens(Bukkit.getOfflinePlayer(player), amount);
    }
}