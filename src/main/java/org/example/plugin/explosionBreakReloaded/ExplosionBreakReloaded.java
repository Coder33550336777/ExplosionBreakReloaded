package org.example.plugin.explosionBreakReloaded;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class ExplosionBreakReloaded extends JavaPlugin implements Listener {

    private Connection connection;
    public static boolean EBRenabled = true;

    @Override
    public void onEnable() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:blocks.db");
            String createTableQuery = "CREATE TABLE IF NOT EXISTS placed_blocks (world TEXT, x INTEGER, y INTEGER, z INTEGER)";
            connection.createStatement().execute(createTableQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        getCommand("EnableBR").setExecutor(this::onCommand);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch(command.getName())
        {
            case "EnableBR" -> {
                if(sender.isOp()) {
                    if (args.length == 0) {
                        EBRenabled = !EBRenabled;
                        if(EBRenabled) {
                            sender.sendMessage(ChatColor.GREEN + "Enabled EBR");
                        }else{
                            sender.sendMessage(ChatColor.RED + "Disabled EBR");
                        }
                    }else{
                        switch(args[0]){
                            case "delete" -> {
                                String sql = "DELETE FROM placed_blocks";
                                sender.sendMessage(ChatColor.RED + "TABLE DELETED! / blocks.db: placed_blocks");
                                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:blocks.db");
                                     Statement stmt = conn.createStatement()) {
                                    stmt.executeUpdate(sql);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }

                        }
                    }
                }else{
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        String query = "INSERT INTO placed_blocks (world, x, y, z) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, block.getWorld().getName());
            statement.setInt(2, block.getX());
            statement.setInt(3, block.getY());
            statement.setInt(4, block.getZ());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if(EBRenabled){
            Set<Block> protectedBlocks = new HashSet<>();
            String query = "SELECT * FROM placed_blocks";
            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String world = resultSet.getString("world");
                    int x = resultSet.getInt("x");
                    int y = resultSet.getInt("y");
                    int z = resultSet.getInt("z");
                    Block block = Bukkit.getWorld(world).getBlockAt(x, y, z);
                    protectedBlocks.add(block);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            event.blockList().removeIf(block -> !protectedBlocks.contains(block));
        }
    }
}
