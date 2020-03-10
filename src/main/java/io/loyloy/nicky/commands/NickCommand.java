package io.loyloy.nicky.commands;

import io.loyloy.nicky.Nick;
import io.loyloy.nicky.Nicky;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NickCommand implements CommandExecutor
{
    private Nicky plugin;

    public NickCommand( Nicky plugin )
    {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        if( ! (sender instanceof Player) )
        {
            runAsConsole( args );
        }
        else if( args.length >= 2 )
        {
            runAsAdmin( sender, args );
        }
        else
        {
            runAsPlayer( sender, args );
        }

        return true;
    }

    private void runAsConsole( String[] args )
    {
        if( args.length >= 2  )
        {
            OfflinePlayer receiver = plugin.getServer().getOfflinePlayer( args[0] );

            if( !receiver.hasPlayedBefore() )
            {
                plugin.log( "Could not find '" + args[0] + "', did you get the name right?");
                return;
            }

            String nickname = args[1].trim();

            if( nickname.equals( receiver.getName() ) )
            {
                new DelNickCommand( plugin ).runAsConsole( args );
                return;
            }

            String strippedNickname = ChatColor.stripColor( ChatColor.translateAlternateColorCodes( '&', nickname ) );
            if( strippedNickname.length() < Nicky.getMinLength() )
            {
                plugin.log( "Nicks must be at least " + Nicky.getMinLength() + " characters." );
                return;
            }

            Nick nick = new Nick( receiver );

            if( Nick.isUsed( nickname ) )
            {
                plugin.log( "Sorry the nick " + nickname + "  is already in use :(" );
                return;
            }

            nick.set( nickname );
            nickname = nick.get();

            if( receiver.isOnline() )
            {
                receiver.getPlayer().sendMessage( Nicky.getPrefix() + "Your nickname has been set to " + ChatColor.YELLOW + nickname + ChatColor.GREEN + " by console!" );
            }
            plugin.log( receiver.getName() + "'s nick has been set to " + nickname );
        }
        else
        {
            plugin.log( "Usage: /nick <player> <nickname>" );
        }
    }

    private void runAsAdmin( CommandSender sender, String[] args )
    {
        OfflinePlayer receiver = plugin.getServer().getOfflinePlayer( args[0] );

        if( !receiver.hasPlayedBefore() )
        {
            sender.sendMessage( Nicky.getPrefix() + "Could not find " + ChatColor.YELLOW + args[0] + ChatColor.GREEN + ", did you get the name right?");
            return;
        }

        String nickname = args[1];

        if( sender.hasPermission( "nicky.set.other" ) )
        {
            if( nickname.equals( receiver.getName() ) )
            {
                new DelNickCommand( plugin ).runAsAdmin( sender, args );
                return;
            }

            String strippedNickname = ChatColor.stripColor( ChatColor.translateAlternateColorCodes( '&', nickname ) );
            if( strippedNickname.length() < Nicky.getMinLength() )
            {
                sender.sendMessage( Nicky.getPrefix() + ChatColor.RED + "Nicks must be at least " + ChatColor.YELLOW + Nicky.getMinLength() + ChatColor.RED + " characters!" );
                return;
            }

            Nick nick = new Nick( receiver );

            if( Nick.isBlacklisted( nickname ) && !sender.hasPermission( "nicky.noblacklist" ) )
            {
                sender.sendMessage( Nicky.getPrefix() + "Sorry but " + ChatColor.YELLOW + nick.format( nickname ) + ChatColor.GREEN + " contains a blacklisted word :(" );
                return;
            }

            if( Nick.isUsed( nickname ) )
            {
                sender.sendMessage( Nicky.getPrefix() + "Sorry the nick " + ChatColor.YELLOW + nick.format( nickname ) + ChatColor.GREEN + " is already in use :(" );
                return;
            }

            nick.set( nickname );
            nickname = nick.get();

            if( receiver.isOnline() )
            {
                receiver.getPlayer().sendMessage( Nicky.getPrefix() + "Your nickname has been set to " + ChatColor.YELLOW + nickname + ChatColor.GREEN + " by " + ChatColor.YELLOW + sender.getName() + ChatColor.GREEN + "!" );
            }

            sender.sendMessage( Nicky.getPrefix() + "You have set " + ChatColor.YELLOW + receiver.getName() + ChatColor.GREEN + "'s nickname to " + ChatColor.YELLOW + nickname + ChatColor.GREEN + "." );
        }
        else
        {
            sender.sendMessage( Nicky.getPrefix() + ChatColor.RED + "Sorry, you don't have permission to set other players nicks." );
        }
    }

    private boolean canUse(UUID uuid) {
        if(Nicky.cooldown.containsKey(uuid)) {
            Long now = System.currentTimeMillis();
            Long exp = Nicky.cooldown.get(uuid);
            if(now >= exp) {
                Nicky.cooldown.remove(uuid);
                Nicky.cooldown.put(uuid, (System.currentTimeMillis()+(1000*plugin.getConfig().getInt("nickcooldown"))));
                return true;
            } else {
                return false;
            }
        }
        Nicky.cooldown.put(uuid, (System.currentTimeMillis()+(1000*plugin.getConfig().getInt("nickcooldown"))));
        return true;
    }

    private void runAsPlayer( CommandSender sender, String[] args )
    {
        Player player = (Player) sender;

        if( sender.hasPermission( "nicky.set" ) )
        {
            if(!canUse(player.getUniqueId()) && !player.hasPermission("nicky.cooldownbypass")) {
                player.sendMessage(Nicky.getPrefix() + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cooldown")).replace("{nickcooldown}", plugin.getConfig().getString("nickcooldown")));
                return;
            }
            if( args.length >= 1 )
            {
                String nickname = args[0];

                if( nickname.equals( sender.getName() ) )
                {
                    new DelNickCommand( plugin ).runAsPlayer( sender );
                    return;
                }

                String strippedNickname = ChatColor.stripColor( ChatColor.translateAlternateColorCodes( '&', nickname ) );
                if( strippedNickname.length() < Nicky.getMinLength() )
                {
                    player.sendMessage( Nicky.getPrefix() + ChatColor.RED + "Your nick must be at least " + ChatColor.YELLOW + Nicky.getMinLength() + ChatColor.RED + " characters!" );
                    return;
                }

                if(strippedNickname.length() >= 3 && !strippedNickname.substring(0, 3).equalsIgnoreCase(player.getName().substring(0, 3)) && !player.hasPermission("nicky.fthreebypass")) {
                    player.sendMessage(Nicky.getPrefix() + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("three_letters_not_met")));
                    return;
                }

                Nick nick = new Nick( player );

                if( Nick.isBlacklisted( nickname ) && !player.hasPermission( "nicky.noblacklist" ) )
                {
                    player.sendMessage( Nicky.getPrefix() + "Sorry but " + ChatColor.YELLOW + nick.format( nickname ) + ChatColor.GREEN + " contains a blacklisted word :(" );
                    return;
                }

                if( Nick.isUsed( nickname ) )
                {
                    player.sendMessage( Nicky.getPrefix() + "Sorry the nick " + ChatColor.YELLOW + nick.format( nickname ) + ChatColor.GREEN + " is already in use :(" );
                    return;
                }

                nick.set( nickname );
                nickname = nick.get();

                player.sendMessage( Nicky.getPrefix() + "Your nickname has been set to " + ChatColor.YELLOW + nickname + ChatColor.GREEN + " !" );
            }
            else
            {
                player.sendMessage( Nicky.getPrefix() + "To set a nick do " + ChatColor.YELLOW + "/nick <nickname>" );
            }
        }
        else
        {
            player.sendMessage( Nicky.getPrefix() + ChatColor.RED + "Sorry, you don't have permission to set a nick." );
        }
    }
}