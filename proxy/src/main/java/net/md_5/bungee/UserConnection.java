package net.md_5.bungee;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.PlatformDependent;

import java.net.InetSocketAddress;
import java.util.logging.Level;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketWrapper;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.packet.DefinedPacket;
import net.md_5.bungee.protocol.packet.PacketFFKick;

@RequiredArgsConstructor
public final class UserConnection implements ProxiedPlayer
{

    /*========================================================================*/
    @NonNull
    private final ProxyServer bungee;
    @NonNull
    private final ChannelWrapper ch;
    @Getter
    private final InitialHandler pendingConnection;
    /*========================================================================*/
    @Getter
    @Setter
    private ServerConnection server;
    @Getter
    private final Object switchMutex = new Object();
    /*========================================================================*/
    private final Unsafe unsafe = new Unsafe()
    {
        @Override
        public void sendPacket(DefinedPacket packet)
        {
            ch.write( packet );
        }
    };

    public void init()
    {
    }

    public void sendPacket(PacketWrapper packet)
    {
        ch.write( packet );
    }

    @Deprecated
    public boolean isActive()
    {
        return !ch.isClosed();
    }

    @Override
    public void connect(ServerInfo target)
    {
        connect( target, false );
    }

    public void connectNow(ServerInfo target)
    {
        connect( target );
    }

    public void connect(ServerInfo info, final boolean retry)
    {
        final BungeeServerInfo target = (BungeeServerInfo) info;

        ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>()
        {
            @Override
            protected void initChannel(Channel ch) throws Exception
            {
                PipelineUtils.BASE.initChannel( ch );
                ch.pipeline().get( HandlerBoss.class ).setHandler( new ServerConnector( bungee, UserConnection.this, target ) );
            }
        };
        ChannelFutureListener listener = new ChannelFutureListener()
        {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception
            {
                if ( !future.isSuccess() )
                {
                    future.channel().close();
                    disconnect( "Error connecting to server." );
                }
            }
        };
        Bootstrap b = new Bootstrap()
                .channel( NioSocketChannel.class )
                .group( BungeeCord.getInstance().eventLoops )
                .handler( initializer )
                .option( ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000 ) // TODO: Configurable
                .remoteAddress( target.getAddress() );
        // Windows is bugged, multi homed users will just have to live with random connecting IPs
        if ( getPendingConnection().getListener().isSetLocalAddress() && !PlatformDependent.isWindows() )
        {
            b.localAddress( getPendingConnection().getListener().getHost().getHostString(), 0 );
        }
        b.connect().addListener( listener );
    }

    @Override
    public synchronized void disconnect(String reason)
    {
        if ( ch.getHandle().isActive() )
        {
            bungee.getLogger().log( Level.INFO, "[" + toString() + "] disconnected with: " + reason );
            unsafe().sendPacket( new PacketFFKick( reason ) );
            ch.close();
            if ( server != null )
            {
                server.disconnect( "Quitting" );
            }
        }
    }

    @Override
    public InetSocketAddress getAddress()
    {
        return (InetSocketAddress) ch.getHandle().remoteAddress();
    }

    @Override
    public String toString()
    {
        return getAddress().toString();
    }

    @Override
    public Unsafe unsafe()
    {
        return unsafe;
    }
}
