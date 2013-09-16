package net.md_5.bungee.connection;

import io.netty.util.concurrent.ScheduledFuture;

import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketDecoder;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.netty.PacketWrapper;
import net.md_5.bungee.protocol.MinecraftInput;
import net.md_5.bungee.protocol.packet.DefinedPacket;
import net.md_5.bungee.protocol.packet.Packet01PingAdditional;
import net.md_5.bungee.protocol.packet.Packet2Handshake;
import net.md_5.bungee.protocol.packet.PacketFAPluginMessage;
import net.md_5.bungee.protocol.packet.PacketFEPing;
import net.md_5.bungee.protocol.packet.PacketFFKick;

import com.google.common.base.Preconditions;

@RequiredArgsConstructor
public class InitialHandler extends PacketHandler implements PendingConnection
{

    private final ProxyServer bungee;
    private ChannelWrapper ch;
    @Getter
    private final ListenerInfo listener;
    @Getter
    private Packet2Handshake handshake;
    @Getter
    private BlockingQueue<Object> packets = new ArrayBlockingQueue<Object>( 1000 );
    private State thisState = State.HANDSHAKE;
    private final Unsafe unsafe = new Unsafe()
    {
        @Override
        public void sendPacket(DefinedPacket packet)
        {
            ch.write( packet );
        }
    };
    private ScheduledFuture<?> pingFuture1;
    private ScheduledFuture<?> pingFuture2;
    private byte version = -1;

    private enum State
    {

        HANDSHAKE, ENCRYPT, LOGIN, FINISHED;
    }

    @Override
    public void connected(ChannelWrapper channel) throws Exception
    {
        this.ch = channel;
    }

    @Override
    public void exception(Throwable t) throws Exception
    {
        disconnect( ChatColor.RED + Util.exception( t ) );
    }

    @Override
    public void handle(PacketFAPluginMessage pluginMessage) throws Exception
    {
        packets.add( pluginMessage );
        
        if ( pluginMessage.getTag().equals( "MC|PingHost" ) )
        {
            if ( pingFuture2 != null && pingFuture2.cancel( false ) )
            {
                MinecraftInput in = pluginMessage.getMCStream();
                version = in.readByte();
                in.readString(); // Host
                in.readInt();    // Port

                respondToPing();
            }

            return;
        }
    }

    private void respondToPing()
    {
        try
        {
            BungeeCord.getInstance().getConnectionThrottle().unthrottle( getAddress().getAddress() );
            finish();
        } catch ( Throwable t )
        {
            t.printStackTrace();
        }
    }

    @Override
    public void handle(PacketFEPing ping) throws Exception
    {
    	packets.add( ping );
        pingFuture1 = ch.getHandle().eventLoop().schedule( new Runnable()
        {
            @Override
            public void run()
            {
                respondToPing();
            }
        }, 100, TimeUnit.MILLISECONDS );
    }
    
    @Override
    public void handle(PacketWrapper packet) throws Exception
    {
    	if(packet.packet != null)
    		return;
    	packets.add( packet );
    	packet.buf.retain();
    }
    
    @Override
    public void handle(Packet01PingAdditional pingAdditional) throws Exception
    {
    	packets.add( pingAdditional );
        if ( pingFuture1 != null && pingFuture1.cancel( false ) )
        {
            version = -2;
            pingFuture2 = ch.getHandle().eventLoop().schedule( new Runnable()
            {
                @Override
                public void run()
                {
                    respondToPing();
                }
            }, 100, TimeUnit.MILLISECONDS );
        }
    }

    @Override
    public void handle(Packet2Handshake handshake) throws Exception
    {
        Preconditions.checkState( thisState == State.HANDSHAKE, "Not expecting HANDSHAKE" );
        packets.add( handshake );
        this.handshake = handshake;
        bungee.getLogger().log( Level.INFO, "{0} has connected", this );

        if ( ! bungee.getConfigurationAdapter().getServers().containsKey( (int) handshake.getProcolVersion() ) )
        {
            disconnect( "Unsupported client version: " + handshake.getProcolVersion() );
        }

        finish();
    }

    private void finish()
    {
        if ( ch.isClosed() )
        {
            return;
        }
        ch.getHandle().pipeline().get( PacketDecoder.class ).setParsePackets(false);
        thisState = InitialHandler.State.LOGIN;

        ch.getHandle().eventLoop().execute( new Runnable()
        {
            @Override
            public void run()
            {
                if ( ch.getHandle().isActive() )
                {
                    UserConnection userCon = new UserConnection( bungee, ch, InitialHandler.this );
                    userCon.init();
                    ch.getHandle().pipeline().get( HandlerBoss.class ).setHandler( new UpstreamBridge( userCon ) );
                    ServerInfo server = bungee.getConfigurationAdapter().getServers().get( (int) getVersion() );
                    if (server == null)
                    	server = bungee.getConfigurationAdapter().getServers().get( listener.getDefaultServer() );
                    userCon.connect( server, true );
                    thisState = State.FINISHED;
                }
            }
        } );
    }

    @Override
    public synchronized void disconnect(String reason)
    {
        if ( !ch.isClosed() )
        {
            unsafe().sendPacket( new PacketFFKick( reason ) );
            ch.close();
        }
    }

    @Override
    public byte getVersion()
    {
        return ( handshake == null ) ? version : handshake.getProcolVersion();
    }

    @Override
    public InetSocketAddress getAddress()
    {
        return (InetSocketAddress) ch.getHandle().remoteAddress();
    }

    @Override
    public Unsafe unsafe()
    {
        return unsafe;
    }

    @Override
    public String toString()
    {
        return "[" + getAddress() + "] <-> InitialHandler";
    }
}
