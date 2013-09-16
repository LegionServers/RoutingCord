package net.md_5.bungee;

import java.net.InetSocketAddress;
import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.config.ServerInfo;

@RequiredArgsConstructor
public class BungeeServerInfo implements ServerInfo
{

    @Getter
    private final Integer name;
    @Getter
    private final InetSocketAddress address;

    @Override
    public boolean equals(Object obj)
    {
        return ( obj instanceof ServerInfo ) && Objects.equals( getAddress(), ( (ServerInfo) obj ).getAddress() );
    }

    @Override
    public int hashCode()
    {
        return address.hashCode();
    }
}
