package com.rolcraftlandia.delimitarciudades;

import com.sk8922.worldedit.WorldEdit;
import com.sk8922.worldedit.bukkit.BukkitAdapter;
import com.sk8922.worldedit.regions.Region;
import com.sk8922.worldedit.session.SessionManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Main extends JavaPlugin implements CommandExecutor {

    private final Map<String, CiudadRegion> ciudades = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cargarCiudades();

        if (getCommand("dc") != null) {
            getCommand("dc").setExecutor(this);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CiudadExpansion(this).register();
        }
    }

    private void cargarCiudades() {
        ciudades.clear();
        FileConfiguration config = getConfig();
        if (config.getConfigurationSection("ciudades") == null) return;

        for (String key : config.getConfigurationSection("ciudades").getKeys(false)) {
            String world = config.getString("ciudades." + key + ".world");
            int minX = config.getInt("ciudades." + key + ".minX");
            int minY = config.getInt("ciudades." + key + ".minY");
            int minZ = config.getInt("ciudades." + key + ".minZ");
            int maxX = config.getInt("ciudades." + key + ".maxX");
            int maxY = config.getInt("ciudades." + key + ".maxY");
            int maxZ = config.getInt("ciudades." + key + ".maxZ");

            ciudades.put(key.toLowerCase(), new CiudadRegion(key, world, minX, minY, minZ, maxX, maxY, maxZ));
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo lo puede usar un jugador.");
            return true;
        }

        if (!player.hasPermission("delimitarciudades.admin")) {
            player.sendMessage(ChatColor.RED + "No tienes permisos para usar este comando.");
            return true;
        }

        if (args.length >= 2) {
            String accion = args[0];
            String nombreCiudad = args[1].replace("\"", ""); // Remueve comillas si las escriben

            // COMANDO CREAR
            if (accion.equalsIgnoreCase("create")) {
                try {
                    com.sk8922.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
                    SessionManager manager = WorldEdit.getInstance().getSessionManager();
                    Region region = manager.get(actor).getSelection(actor.getWorld());

                    if (region == null) {
                        player.sendMessage(ChatColor.RED + "Debes hacer una selección previa con el hacha de WorldEdit.");
                        return true;
                    }

                    String world = player.getWorld().getName();
                    int minX = region.getMinimumPoint().getBlockX();
                    int minY = region.getMinimumPoint().getBlockY();
                    int minZ = region.getMinimumPoint().getBlockZ();
                    int maxX = region.getMaximumPoint().getBlockX();
                    int maxY = region.getMaximumPoint().getBlockY();
                    int maxZ = region.getMaximumPoint().getBlockZ();

                    FileConfiguration config = getConfig();
                    config.set("ciudades." + nombreCiudad + ".world", world);
                    config.set("ciudades." + nombreCiudad + ".minX", minX);
                    config.set("ciudades." + nombreCiudad + ".minY", minY);
                    config.set("ciudades." + nombreCiudad + ".minZ", minZ);
                    config.set("ciudades." + nombreCiudad + ".maxX", maxX);
                    config.set("ciudades." + nombreCiudad + ".maxY", maxY);
                    config.set("ciudades." + nombreCiudad + ".maxZ", maxZ);
                    saveConfig();

                    ciudades.put(nombreCiudad.toLowerCase(), new CiudadRegion(nombreCiudad, world, minX, minY, minZ, maxX, maxY, maxZ));

                    player.sendMessage(ChatColor.GREEN + "¡Ciudad '" + nombreCiudad + "' delimitada y creada con éxito!");
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Selecciona un área válida con el hacha de WorldEdit primero.");
                }
                return true;
            }

            // COMANDO ELIMINAR
            if (accion.equalsIgnoreCase("delete")) {
                if (!ciudades.containsKey(nombreCiudad.toLowerCase())) {
                    player.sendMessage(ChatColor.RED + "La ciudad '" + nombreCiudad + "' no existe.");
                    return true;
                }

                FileConfiguration config = getConfig();
                config.set("ciudades." + nombreCiudad, null);
                saveConfig();

                ciudades.remove(nombreCiudad.toLowerCase());

                player.sendMessage(ChatColor.YELLOW + "La ciudad '" + nombreCiudad + "' ha sido eliminada.");
                return true;
            }
        }

        player.sendMessage(ChatColor.YELLOW + "Uso: /dc create \"nombre\"  o  /dc delete \"nombre\"");
        return true;
    }

    public String getCiudadEnUbicacion(Location loc) {
        for (CiudadRegion ciudad : ciudades.values()) {
            if (ciudad.contains(loc)) {
                return ciudad.getNombre();
            }
        }
        return "Ninguna";
    }

    private static class CiudadRegion {
        private final String nombre;
        private final String world;
        private final int minX, minY, minZ, maxX, maxY, maxZ;

        public CiudadRegion(String nombre, String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.nombre = nombre;
            this.world = world;
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
        }

        public String getNombre() { return nombre; }

        public boolean contains(Location loc) {
            if (!loc.getWorld().getName().equals(world)) return false;
            return loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                   loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                   loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
        }
    }

    public static class CiudadExpansion extends PlaceholderExpansion {
        private final Main plugin;

        public CiudadExpansion(Main plugin) {
            this.plugin = plugin;
        }

        @Override public @NotNull String getIdentifier() { return "dc"; }
        @Override public @NotNull String getAuthor() { return "Rolcraftlandia"; }
        @Override public @NotNull String getVersion() { return "1.0"; }
        @Override public boolean persist() { return true; }

        @Override
        public String onPlaceholderRequest(Player player, @NotNull String params) {
            if (player == null) return "Ninguna";
            if (params.equalsIgnoreCase("ciudad")) {
                return plugin.getCiudadEnUbicacion(player.getLocation());
            }
            return null;
        }
    }
}
