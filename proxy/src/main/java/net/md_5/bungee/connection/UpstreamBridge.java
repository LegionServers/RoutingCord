package net.md_5.bungee.connection;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.netty.PacketWrapper;

public class UpstreamBridge extends PacketHandler
{

    private final UserConnection con;
    
    private static volatile long connections = 0;

    public UpstreamBridge(UserConnection con)
    {
        this.con = con;
        connections++;
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
        connections--;
        if ( BungeeCord.isExitWhenEmpty() && connections <= 0 )
        	BungeeCord.getInstance().stop();
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception
    {
    	synchronized(con.getSwitchMutex()) {
	    	if(con.getServer() == null) {
	    		con.getPendingConnection().getPackets().add( packet );
	    		packet.buf.retain();
	    	} else
	    		con.getServer().getCh().write( packet );
    	}
    }

    @Override
    public String toString()
    {
        return "[" + con + "] -> UpstreamBridge";
    }
}
