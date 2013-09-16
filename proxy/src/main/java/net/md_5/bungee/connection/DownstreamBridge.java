package net.md_5.bungee.connection;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.netty.PacketWrapper;

@RequiredArgsConstructor
public class DownstreamBridge extends PacketHandler
{

    private final UserConnection con;
    private final ServerConnection server;

    @Override
    public void exception(Throwable t) throws Exception
    {
        con.disconnect( Util.exception( t ) );
    }

    @Override
    public void disconnected(ChannelWrapper channel) throws Exception
    {
        con.disconnect( "Connection to server lost." );
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception
    {
        con.sendPacket( packet );
    }

    @Override
    public String toString()
    {
        return "[" + con.toString() + "] <-> DownstreamBridge <-> [" + server.getInfo().getName() + "]";
    }
}
