package net.md_5.bungee.connection;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.netty.PacketWrapper;

public class UpstreamBridge extends PacketHandler
{

    private final UserConnection con;

    public UpstreamBridge(UserConnection con)
    {
        this.con = con;
    }

    @Override
    public void exception(Throwable t) throws Exception
    {
        con.disconnect( Util.exception( t ) );
    }

    @Override
    public void disconnected(ChannelWrapper channel) throws Exception
    {
        con.getServer().disconnect( "Quitting" );
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception
    {
        con.getServer().getCh().write( packet );
    }

    @Override
    public String toString()
    {
        return "[" + con + "] -> UpstreamBridge";
    }
}
