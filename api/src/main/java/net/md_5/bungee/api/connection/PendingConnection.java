package net.md_5.bungee.api.connection;

import net.md_5.bungee.api.config.ListenerInfo;

/**
 * Represents a user attempting to log into the proxy.
 */
public interface PendingConnection extends Connection
{

    /**
     * Get the numerical client version of the player attempting to log in.
     *
     * @return the protocol version of the remote client
     */
    public byte getVersion();

    /**
     * Get the listener that accepted this connection.
     *
     * @return the accepting listener
     */
    public ListenerInfo getListener();
}
