package net.md_5.bungee.api.config;

import java.net.InetSocketAddress;

import lombok.Data;

/**
 * Class representing the configuration of a server listener. Used for allowing
 * multiple listeners on different ports.
 */
@Data
public class ListenerInfo
{

    /**
     * Host to bind to.
     */
    private final InetSocketAddress host;
    /**
     * Name of the server which users will be taken to by default.
     */
    private final Integer defaultServer;
    /**
     * Whether to set the local address when connecting to servers.
     */
    private final boolean setLocalAddress;
}
