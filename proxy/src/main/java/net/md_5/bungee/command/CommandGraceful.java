package net.md_5.bungee.command;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

public class CommandGraceful
{

    public CommandGraceful()
    {
    }

    public void execute(CommandSender sender, String[] args)
    {
        BungeeCord.getInstance().stopListeners();
        File currentJar;
        try {
            currentJar = new File( CommandGraceful.class.getProtectionDomain().getCodeSource().getLocation().toURI() );
        } catch( URISyntaxException e1 ) {
            sender.sendMessage( ChatColor.RED + "URISyntaxException trying to get bungeecord.jar cononical path." );
            return;
        }
        if( ! currentJar.getName().endsWith( ".jar" ) ) {
            sender.sendMessage( ChatColor.RED + "Could not find jar file." );
            return;
        }
        final File startSh = new File( currentJar.getParent(), "start.sh" );
        if( ! startSh.exists() || ! startSh.canRead() || ! startSh.canExecute() ) {
            sender.sendMessage( ChatColor.RED + "Could not find start.sh file." );
            return;
        }
        String command;
        try {
            command = startSh.getCanonicalPath();
        } catch( IOException e ) {
            sender.sendMessage( ChatColor.RED + "IOException trying to get start.sh cononical path." );
            return;
        }
        final ProcessBuilder builder = new ProcessBuilder( command );
        try {
            builder.start();
        } catch( IOException e ) {
            sender.sendMessage( ChatColor.RED + "IOException trying to execute start.sh" );
            return;
        }
        sender.sendMessage( ChatColor.GREEN + "New bungeecord up." );
        BungeeCord.setExitWhenEmpty( true );
    }
}
