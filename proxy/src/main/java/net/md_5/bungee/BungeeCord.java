package net.md_5.bungee;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;
import jline.internal.Log;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.command.CommandEnd;
import net.md_5.bungee.command.CommandGraceful;
import net.md_5.bungee.command.CommandReload;
import net.md_5.bungee.command.ConsoleCommandSender;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfig;
import net.md_5.bungee.log.BungeeLogger;
import net.md_5.bungee.log.LoggingOutputStream;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.Vanilla;

import org.fusesource.jansi.AnsiConsole;

import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;

/**
 * Main BungeeCord proxy class.
 */
public class BungeeCord extends ProxyServer
{

    /**
     * Current operation state.
     */
    public volatile boolean isRunning;
    /**
     * Configuration.
     */
    public final Configuration config = new Configuration();
    public final MultithreadEventLoopGroup eventLoops = new NioEventLoopGroup( 0, new ThreadFactoryBuilder().setNameFormat( "Netty IO Thread #%1$d" ).build() );
    /**
     * Server socket listener.
     */
    private Collection<Channel> listeners = new HashSet<>();
    @Getter
    @Setter
    private ConfigurationAdapter configurationAdapter = new YamlConfig();
    @Getter
    private ConsoleReader consoleReader;
    @Getter
    private final Logger logger;
    public final Gson gson = new Gson();
    @Getter
    private ConnectionThrottle connectionThrottle;
    @Getter
    @Setter
    private static boolean exitWhenEmpty = false;

    public static BungeeCord getInstance()
    {
        return (BungeeCord) ProxyServer.getInstance();
    }

    public BungeeCord() throws IOException
    {
        Log.setOutput( new PrintStream( ByteStreams.nullOutputStream() ) ); // TODO: Bug JLine
        AnsiConsole.systemInstall();
        consoleReader = new ConsoleReader();

        logger = new BungeeLogger( this );
        System.setErr( new PrintStream( new LoggingOutputStream( logger, Level.SEVERE ), true ) );
        System.setOut( new PrintStream( new LoggingOutputStream( logger, Level.INFO ), true ) );

        if ( consoleReader.getTerminal() instanceof UnsupportedTerminal )
        {
            logger.info( "Unable to initialize fancy terminal. To fix this on Windows, install the correct Microsoft Visual C++ 2008 Runtime" );
            logger.info( "NOTE: This error is non crucial, and BungeeCord will still function correctly! Do not bug the author about it unless you are still unable to get it working" );
        }
    }

    /**
     * Starts a new instance of BungeeCord.
     *
     * @param args command line arguments, currently none are used
     * @throws Exception when the server cannot be started
     */
    public static void main(String[] args) throws Exception
    {
        BungeeCord bungee = new BungeeCord();
        ProxyServer.setInstance( bungee );
        bungee.getLogger().info( "Enabled BungeeCord version " + bungee.getVersion() );
        bungee.start();

        File commands = new File( ".command" );
        while ( bungee.isRunning )
        {
        	Thread.sleep(1000);
        	if( commands.exists() )
        	{
        		BufferedReader br = new BufferedReader( new FileReader( commands ) );
	            String line;
	            while ( ( line = br.readLine() ) != null )
	            {
	            	if( line.trim().equalsIgnoreCase( "end" ) )
	            		new CommandEnd().execute(ConsoleCommandSender.getInstance(), new String[] {});
	            	else if( line.trim().equalsIgnoreCase( "reload" ) )
	            		new CommandReload().execute(ConsoleCommandSender.getInstance(), new String[] {});
	            	else if( line.trim().equalsIgnoreCase( "graceful" ) )
	            		new CommandGraceful().execute(ConsoleCommandSender.getInstance(), new String[] {});
	            }
	            br.close();
	            commands.delete();
        	}
        }
    }

    /**
     * Start this proxy instance by loading the configuration, plugins and
     * starting the connect thread.
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception
    {
        ResourceLeakDetector.setEnabled( false ); // Eats performance

        config.load();
        isRunning = true;

        connectionThrottle = new ConnectionThrottle( config.getThrottle() );
        startListeners();
    }

    public void startListeners()
    {
        for ( final ListenerInfo info : config.getListeners() )
        {
            ChannelFutureListener listener = new ChannelFutureListener()
            {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception
                {
                    if ( future.isSuccess() )
                    {
                        listeners.add( future.channel() );
                        getLogger().info( "Listening on " + info.getHost() );
                    } else
                    {
                        getLogger().log( Level.WARNING, "Could not bind to host " + info.getHost(), future.cause() );
                    }
                }
            };
            new ServerBootstrap()
                    .channel( NioServerSocketChannel.class )
                    .childAttr( PipelineUtils.LISTENER, info )
                    .childHandler( PipelineUtils.SERVER_CHILD )
                    .group( eventLoops )
                    .localAddress( info.getHost() )
                    .bind().addListener( listener );
        }
    }

    public void stopListeners()
    {
        for ( Channel listener : listeners )
        {
            getLogger().log( Level.INFO, "Closing listener {0}", listener );
            try
            {
                listener.close().syncUninterruptibly();
            } catch ( ChannelException ex )
            {
                getLogger().severe( "Could not close listen thread" );
            }
        }
        listeners.clear();
    }

    @Override
    public void stop()
    {
        new Thread( "Shutdown Thread" )
        {
            @Override
            public void run()
            {
                BungeeCord.this.isRunning = false;

                stopListeners();
                getLogger().info( "Closing pending connections" );

                getLogger().info( "Closing IO threads" );
                eventLoops.shutdownGracefully();
                try
                {
                    eventLoops.awaitTermination( Long.MAX_VALUE, TimeUnit.NANOSECONDS );
                } catch ( InterruptedException ex )
                {
                }

                getLogger().info( "Thankyou and goodbye" );
                System.exit( 0 );
            }
        }.start();
    }

    @Override
    public String getName()
    {
        return "BungeeCord";
    }

    @Override
    public String getVersion()
    {
        return ( BungeeCord.class.getPackage().getImplementationVersion() == null ) ? "unknown" : BungeeCord.class.getPackage().getImplementationVersion();
    }

    @Override
    public Map<Integer, ServerInfo> getServers()
    {
        return config.getServers();
    }

    @Override
    public ServerInfo getServerInfo(String name)
    {
        return getServers().get( name );
    }

    @Override
    public byte getProtocolVersion()
    {
        return Vanilla.PROTOCOL_VERSION;
    }

    @Override
    public String getGameVersion()
    {
        return Vanilla.GAME_VERSION;
    }

    @Override
    public ServerInfo constructServerInfo(Integer name, InetSocketAddress address)
    {
        return new BungeeServerInfo( name, address );
    }

    @Override
    public CommandSender getConsole()
    {
        return ConsoleCommandSender.getInstance();
    }
}
