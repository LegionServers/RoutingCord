package net.md_5.bungee.api;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.logging.Logger;

import lombok.Getter;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ServerInfo;

import com.google.common.base.Preconditions;

public abstract class ProxyServer
{

    @Getter
    private static ProxyServer instance;

    /**
     * Sets the proxy instance. This method may only be called once per an
     * application.
     *
     * @param instance the new instance to set
     */
    public static void setInstance(ProxyServer instance)
    {
        Preconditions.checkNotNull( instance, "instance" );
        Preconditions.checkArgument( ProxyServer.instance == null, "Instance already set" );
        ProxyServer.instance = instance;
    }

    /**
     * Gets the name of the currently running proxy software.
     *
     * @return the name of this instance
     */
    public abstract String getName();

    /**
     * Gets the version of the currently running proxy software.
     *
     * @return the version of this instance
     */
    public abstract String getVersion();

    /**
     * Gets the main logger which can be used as a suitable replacement for
     * {@link System#out} and {@link System#err}.
     *
     * @return the {@link Logger} instance
     */
    public abstract Logger getLogger();

    /**
     * Return all servers registered to this proxy, keyed by name. Unlike the
     * methods in {@link ConfigurationAdapter#getServers()}, this will not
     * return a fresh map each time.
     *
     * @return all registered remote server destinations
     */
    public abstract Map<Integer, ServerInfo> getServers();

    /**
     * Gets the server info of a server.
     *
     * @param name the name of the configured server
     * @return the server info belonging to the specified server
     */
    public abstract ServerInfo getServerInfo(String name);

    /**
     * Returns the currently in use configuration adapter.
     *
     * @return the used configuration adapter
     */
    public abstract ConfigurationAdapter getConfigurationAdapter();

    /**
     * Set the configuration adapter to be used. Must be called from
     * {@link Plugin#onLoad()}.
     *
     * @param adapter the adapter to use
     */
    public abstract void setConfigurationAdapter(ConfigurationAdapter adapter);

    /**
     * Gracefully mark this instance for shutdown.
     */
    public abstract void stop();

    /**
     * Start this instance so that it may accept connections.
     *
     * @throws Exception any exception thrown during startup causing the
     * instance to fail to boot
     */
    public abstract void start() throws Exception;

    /**
     * Get the Minecraft version supported by this proxy.
     *
     * @return the supported Minecraft version
     */
    public abstract String getGameVersion();

    /**
     * Get the Minecraft protocol version supported by this proxy.
     *
     * @return the Minecraft protocol version
     */
    public abstract byte getProtocolVersion();

    /**
     * Factory method to construct an implementation specific server info
     * instance.
     *
     * @param name name of the server
     * @param address connectable Minecraft address + port of the server
     * @return the constructed instance
     */
    public abstract ServerInfo constructServerInfo(Integer name, InetSocketAddress address);

    /**
     * Returns the console overlord for this proxy. Being the console, this
     * command server cannot have permissions or groups, and will be able to
     * execute anything.
     *
     * @return the console command sender of this proxy
     */
    public abstract CommandSender getConsole();
}
