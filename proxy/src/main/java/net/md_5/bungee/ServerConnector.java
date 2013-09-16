package net.md_5.bungee;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.connection.DownstreamBridge;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketDecoder;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.packet.PacketFAPluginMessage;
import net.md_5.bungee.protocol.packet.PacketFFKick;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

@RequiredArgsConstructor
public class ServerConnector extends PacketHandler
{

    private final ProxyServer bungee;
    private ChannelWrapper ch;
    private final UserConnection user;
    private final BungeeServerInfo target;

    @Override
    public void exception(Throwable t) throws Exception
    {
        String message = "Exception Connecting:" + Util.exception( t );
        user.disconnect( message );
    }

    @Override
    public void connected(ChannelWrapper channel) throws Exception
    {
        this.ch = channel;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF( "Login" );
        out.writeUTF( user.getAddress().getHostString() );
        out.writeInt( user.getAddress().getPort() );
        channel.write( new PacketFAPluginMessage( "BungeeCord", out.toByteArray() ) );
        
        ServerConnection server = new ServerConnection( ch, target );
        for ( Object packet : user.getPendingConnection().getPackets() )
        {
            ch.write( packet );
        }
        
        user.getPendingConnection().getPackets().clear();
        
        synchronized ( user.getSwitchMutex() )
        {
            // TODO: Fix this?
            if ( !user.isActive() )
            {
                server.disconnect( "Quitting" );
                // Silly server admins see stack trace and die
                bungee.getLogger().warning( "No client connected for pending server!" );
                return;
            }
            user.setServer( server );
            ch.getHandle().pipeline().get( PacketDecoder.class ).setParsePackets(false);
            ch.getHandle().pipeline().get( HandlerBoss.class ).setHandler( new DownstreamBridge( user, server ) );
        }
    }

    @Override
    public void disconnected(ChannelWrapper channel) throws Exception {}

    @Override
    public void handle(PacketFFKick kick) throws Exception
    {
        user.disconnect(kick.getMessage());
    }

    @Override
    public String toString()
    {
        return "[" + user + "] <-> ServerConnector [" + target.getName() + "]";
    }
}
