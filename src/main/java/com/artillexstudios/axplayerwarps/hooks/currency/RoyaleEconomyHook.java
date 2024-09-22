package com.artillexstudios.axplayerwarps.hooks.currency;

import me.qKing12.RoyaleEconomy.RoyaleEconomy;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CURRENCIES;

public class RoyaleEconomyHook implements CurrencyHook {

    @Override
    public void setup() {
    }

    @Override
    public String getName() {
        return "RoyaleEconomy";
    }

    @Override
    public String getDisplayName() {
        return CURRENCIES.getString("currencies.RoyaleEconomy.name");
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
        return RoyaleEconomy.apiHandler.balance.getBalance(player.toString());
    }

    @Override
    public void giveBalance(@NotNull UUID player, double amount) {
        RoyaleEconomy.apiHandler.balance.addBalance(player.toString(), amount);
    }

    @Override
    public void takeBalance(@NotNull UUID player, double amount) {
        RoyaleEconomy.apiHandler.balance.removeBalance(player.toString(), amount);
    }
}