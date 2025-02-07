package com.artillexstudios.axplayerwarps.warps;

import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.Cooldown;
import com.artillexstudios.axapi.utils.PaperUtils;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.category.Category;
import com.artillexstudios.axplayerwarps.database.impl.Base;
import com.artillexstudios.axplayerwarps.enums.Access;
import com.artillexstudios.axplayerwarps.enums.AccessList;
import com.artillexstudios.axplayerwarps.hooks.currency.CurrencyHook;
import com.artillexstudios.axplayerwarps.placeholders.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class Warp {
    private final int id;
    private UUID owner;
    private String ownerName;
    private Location location;
    private String name;
    private @Nullable String description;
    private @Nullable Category category;
    private final long created;
    private Access access;
    private @Nullable CurrencyHook currency;
    private double teleportPrice;
    private double earnedMoney;
    private Material icon;
    private int favorites;
    private HashMap<UUID, Integer> rating = new HashMap<>();
    private int visits;
    private HashSet<UUID> visitors = new HashSet<>();
    private List<Base.AccessPlayer> whitelisted = Collections.synchronizedList(new ArrayList<>());
    private List<Base.AccessPlayer> blacklisted = Collections.synchronizedList(new ArrayList<>());
    private UUID worldUUID;

    public Warp(int id, long created, @Nullable String description, String name,
                Location location, @Nullable Category category,
                UUID owner, String ownerName, Access access, @Nullable CurrencyHook currency,
                double teleportPrice, double earnedMoney, @Nullable Material icon
    ) {
        location.setX(location.getBlockX());
        location.setY(location.getBlockY());
        location.setZ(location.getBlockZ());
        location.add(0.5, 0, 0.5);
        this.id = id;
        this.created = created;
        this.description = description;
        this.name = name;
        this.location = location;
        this.worldUUID = location.getWorld().getUID();
        this.category = category;
        this.owner = owner;
        this.ownerName = ownerName;
        this.access = access;
        this.currency = currency;
        this.teleportPrice = teleportPrice;
        this.earnedMoney = earnedMoney;
        this.icon = icon;

        AxPlayerWarps.getThreadedQueue().submit(() -> {
            favorites = AxPlayerWarps.getDatabase().getFavorites(this);
            rating = AxPlayerWarps.getDatabase().getAllRatings(this);
            visits = AxPlayerWarps.getDatabase().getVisits(this);
            visitors = AxPlayerWarps.getDatabase().getVisitors(this);
            whitelisted = AxPlayerWarps.getDatabase().getAccessList(this, AccessList.WHITELIST);
            blacklisted = AxPlayerWarps.getDatabase().getAccessList(this, AccessList.BLACKLIST);
        });
    }

    public void reload() {
        // reload category & other stuff
    }

    public int getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public boolean setName(String name) {
        if (AxPlayerWarps.getDatabase().warpExists(name)) return false;
        this.name = name;
        return true;
    }

    public String getDescription() {
        if (description == null) {
            return CONFIG.getString("warp-description.default", "");
        }
        return description;
    }

    @Nullable
    public String getRealDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public void setDescription(List<String> description) {
        String newDesc = String.join("\n", description);
        this.description = newDesc.isBlank() ? null : newDesc;
    }

    @Nullable
    public Category getCategory() {
        return category;
    }

    public void setCategory(@Nullable Category category) {
        this.category = category;
    }

    public long getCreated() {
        return created;
    }

    public Access getAccess() {
        return access;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    @Nullable
    public CurrencyHook getCurrency() {
        return currency;
    }

    public void setCurrency(@Nullable CurrencyHook currency) {
        this.currency = currency;
    }

    public double getTeleportPrice() {
        return teleportPrice;
    }

    public void setTeleportPrice(double teleportPrice) {
        this.teleportPrice = teleportPrice;
    }

    public double getEarnedMoney() {
        return earnedMoney;
    }

    public void setEarnedMoney(double earnedMoney) {
        this.earnedMoney = earnedMoney;
    }

    public Material getIcon() {
        if (icon == null) {
            return Material.matchMaterial(CONFIG.getString("default-material", "PLAYER_HEAD"));
        }
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public int getFavorites() {
        return favorites;
    }

    public void setFavorites(int favorites) {
        this.favorites = favorites;
    }

    public HashMap<UUID, Integer> getAllRatings() {
        return rating;
    }

    public float getRating() {
        return (float) rating.values().stream().mapToDouble(Integer::doubleValue).average().orElse(0);
    }

    public int getRatingAmount() {
        return rating.size();
    }

    public void setRating(HashMap<UUID, Integer> rating) {
        this.rating = rating;
    }

    public int getVisits() {
        return visits;
    }

    public void setVisits(int visits) {
        this.visits = visits;
    }

    public HashSet<UUID> getVisitors() {
        return visitors;
    }

    public int getUniqueVisits() {
        return visitors.size();
    }

    public List<Base.AccessPlayer> getBlacklisted() {
        return blacklisted;
    }

    public List<Base.AccessPlayer> getWhitelisted() {
        return whitelisted;
    }

    public List<Base.AccessPlayer> getAccessList(AccessList al) {
        return al == AccessList.WHITELIST ? whitelisted : blacklisted;
    }

    public boolean isPaid() {
        return currency != null && teleportPrice > 0;
    }

    public CompletableFuture<Boolean> isDangerous() {
        if (!CONFIG.getBoolean("check-unsafe-warps", true)) {
            return CompletableFuture.completedFuture(false);
        }

        return PaperUtils.getChunkAtAsync(location).thenApply((chunk) -> {
            int x = location.getBlockX() & 15;
            int y = location.getBlockY();
            int z = location.getBlockZ() & 15;
            Block at = chunk.getBlock(x, y, z);
            Block under = at.getRelative(BlockFace.DOWN);
            Block above = at.getRelative(BlockFace.UP);

            if (!at.getType().isAir()) {
                return true;
            }
            if (!above.getType().isAir()) {
                return true;
            }
            if (!under.getType().isSolid()) {
                return true;
            }
            return false;
        });
    }

    private final Cooldown<Player> confirmUnsafe = new Cooldown<>();
    private final Cooldown<Player> confirmPaid = new Cooldown<>();
    public void teleportPlayer(Player player) {
        validateTeleport(player, false, bool -> {
            if (!bool) return;
            if (player.hasPermission("axplayerwarps.delay-bypass")) {
                completeTeleportPlayer(player);
                return;
            }
            Scheduler.get().run(player::closeInventory);
            WarpQueue.addToQueue(player, this);
        });
    }

    public void validateTeleport(Player player, boolean noConfirm, Consumer<Boolean> response) {
        if (location.getWorld() == null) {
            World world = Bukkit.getWorld(worldUUID);
            if (world == null) {
                MESSAGEUTILS.sendLang(player, "errors.invalid-world");
                response.accept(false);
                return;
            }
            location.setWorld(world);
        }

        boolean isOwner = player.getUniqueId().equals(owner);
        if (!isOwner && isPaid() && !confirmPaid.hasCooldown(player)) {
            confirmPaid.addCooldown(player, CONFIG.getLong("confirmation-milliseconds"));
            MESSAGEUTILS.sendLang(player, "confirm.paid",
                    Map.of("%warp%", getName(), "%price%",
                            currency.getDisplayName()
                                    .replace("%price%", Placeholders.df.format(teleportPrice))
                    ));
            response.accept(false);
            return;
        }

        isDangerous().thenAccept(dangerous -> {
            if (!noConfirm) {
                if (dangerous && !confirmUnsafe.hasCooldown(player)) {
                    confirmUnsafe.addCooldown(player, CONFIG.getLong("confirmation-milliseconds"));
                    MESSAGEUTILS.sendLang(player, "confirm.unsafe", Map.of("%warp%", getName()));
                    response.accept(false);
                    return;
                }

                // check whitelist/blacklist, check access state
                if (access == Access.PRIVATE && !isOwner) {
                    MESSAGEUTILS.sendLang(player, "errors.private", Map.of("%warp%", getName()));
                    response.accept(false);
                    return;
                }
            }

            if (blacklisted.stream().anyMatch(accessPlayer -> accessPlayer.player().equals(player))) {
                MESSAGEUTILS.sendLang(player, "errors.blacklisted", Map.of("%warp%", getName()));
                response.accept(false);
                return;
            }

            if (access == Access.WHITELISTED && !isOwner && whitelisted.stream().noneMatch(accessPlayer -> accessPlayer.player().equals(player))) {
                MESSAGEUTILS.sendLang(player, "errors.whitelisted", Map.of("%warp%", getName()));
                response.accept(false);
                return;
            }

            // check balance
            if (!isOwner && isPaid() && currency.getBalance(player.getUniqueId()) < teleportPrice) {
                MESSAGEUTILS.sendLang(player, "errors.not-enough-balance");
                response.accept(false);
                return;
            }

            response.accept(true);
        });
    }

    public void completeTeleportPlayer(Player player) {
        validateTeleport(player, true, bool -> {
            if (!bool) return;
            Scheduler.get().run(player::closeInventory);
            boolean isOwner = player.getUniqueId().equals(owner);
            if (!isOwner && isPaid()) {
                currency.takeBalance(player.getUniqueId(), teleportPrice);
                earnedMoney += teleportPrice;
                AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().updateWarp(this));
                MESSAGEUTILS.sendLang(player, "money.take", Map.of("%price%",
                        currency.getDisplayName().replace("%price%", Placeholders.df.format(teleportPrice))));
            }

            // send message
            MESSAGEUTILS.sendLang(player, "teleport.success", Map.of("%warp%", getName()));
            confirmUnsafe.remove(player);
            confirmPaid.remove(player);

            Scheduler.get().run(() -> {
                PaperUtils.teleportAsync(player, location);
                for (String m : CONFIG.getStringList("teleport-commands")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Placeholders.parse(this, player, m).replace("%player%", player.getName()));
                }
            });
            AxPlayerWarps.getThreadedQueue().submit(() -> {
                AxPlayerWarps.getDatabase().addVisit(player, this);
            });
        });
    }

    public void delete() {
        Player player = Bukkit.getPlayer(owner);
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            MESSAGEUTILS.sendLang(player, "delete.deleted", Map.of("%warp%", getName()));
            AxPlayerWarps.getDatabase().deleteWarp(this);
        });
    }

    public void withdrawMoney() {
        Player player = Bukkit.getPlayer(owner);
        if (earnedMoney <= 0 || currency == null) {
            MESSAGEUTILS.sendLang(player, "errors.nothing-withdrawable");
            return;
        }
        currency.giveBalance(owner, earnedMoney);
        MESSAGEUTILS.sendLang(player, "money.got", Map.of("%price%",
                currency.getDisplayName()
                        .replace("%price%", Placeholders.df.format(earnedMoney))));
        earnedMoney = 0;
        AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().updateWarp(this));
    }
}
