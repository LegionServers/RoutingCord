package net.md_5.bungee.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;

import com.google.common.base.Preconditions;

/**
 * Core configuration for the proxy.
 */
@Getter
public class Configuration
{

    /**
     * Time before users are disconnected due to no network activity.
     */
    private int timeout = 30000;
    /**
     * Set of all listeners.
     */
    private Collection<ListenerInfo> listeners;
    /**
     * Set of all servers.
     */
    private Map<Integer, ServerInfo> servers;
    private int throttle = 4000;

    public void load()
    {
        ConfigurationAdapter adapter = ProxyServer.getInstance().getConfigurationAdapter();
        adapter.load();

        listeners = adapter.getListeners();
        timeout = adapter.getInt( "timeout", timeout );
        throttle = adapter.getInt( "connection_throttle", throttle );

        Preconditions.checkArgument( listeners != null && !listeners.isEmpty(), "No listeners defined." );

        Map<Integer, ServerInfo> newServers = adapter.getServers();
        Preconditions.checkArgument( newServers != null && !newServers.isEmpty(), "No servers defined" );

        servers = new HashMap<>( newServers );

        for ( ListenerInfo listener : listeners )
        {
            Preconditions.checkArgument( servers.containsKey( listener.getDefaultServer() ), "Default server %s is not defined", listener.getDefaultServer() );
        }
    }
}
