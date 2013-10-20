package net.md_5.bungee.protocol;

import io.netty.buffer.ByteBuf;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import lombok.Getter;
import net.md_5.bungee.protocol.packet.DefinedPacket;
import net.md_5.bungee.protocol.packet.Packet01PingAdditional;
import net.md_5.bungee.protocol.packet.Packet0NewHandshake;
import net.md_5.bungee.protocol.packet.Packet2Handshake;
import net.md_5.bungee.protocol.packet.PacketFAPluginMessage;
import net.md_5.bungee.protocol.packet.PacketFEPing;
import net.md_5.bungee.protocol.packet.PacketFFKick;

public class Vanilla implements Protocol
{

    public static final byte PROTOCOL_VERSION = 74;
    public static final String GAME_VERSION = "1.6.2";
    @Getter
    private static final Vanilla instance = new Vanilla();
    /*========================================================================*/
    @SuppressWarnings("unchecked")
    @Getter
    protected Class<? extends DefinedPacket>[] classes = new Class[ 256 ];
    @SuppressWarnings("unchecked")
    @Getter
    private Constructor<? extends DefinedPacket>[] constructors = new Constructor[ 256 ];
    /*========================================================================*/

    public Vanilla()
    {
    	classes[0x01] = Packet01PingAdditional.class;
        classes[0x02] = Packet2Handshake.class;
        classes[0xFA] = PacketFAPluginMessage.class;
        classes[0xFE] = PacketFEPing.class;
        classes[0xFF] = PacketFFKick.class;
    }

    @Override
    public DefinedPacket read(short packetId, ByteBuf buf, boolean parsePackets, boolean firstPacket)
    {
        int start = buf.readerIndex();
        DefinedPacket packet = read( packetId, buf, this, parsePackets, firstPacket );
        if ( buf.readerIndex() == start )
        {
            //throw new RuntimeException( "Unknown packet id " + packetId );
        }
        return packet;
    }

    public static DefinedPacket read(short id, ByteBuf buf, Protocol protocol, boolean parsePackets, boolean firstPacket)
    {
    	if (parsePackets) {
	        DefinedPacket packet = packet( id, protocol );
	        if ( packet != null )
	        {
	            packet.read( buf );
	            return packet;
	        } else if ( firstPacket )
	        {
	        	buf.readerIndex(buf.readerIndex() - 1);
	        	packet = new Packet0NewHandshake();
	        	packet.read( buf );
	        	return packet;
	        } else
	        {
	        	throw new RuntimeException( "Unknown packet id " + id );
	        }
    	}
    	if (buf.readableBytes() > 0)
    		buf.readerIndex(buf.writerIndex());
        return null;
    }

    public static DefinedPacket packet(short id, Protocol protocol)
    {
        DefinedPacket ret = null;
        Class<? extends DefinedPacket> clazz = protocol.getClasses()[id];

        if ( clazz != null )
        {
            try
            {
                Constructor<? extends DefinedPacket> constructor = protocol.getConstructors()[id];
                if ( constructor == null )
                {
                    constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible( true );
                    protocol.getConstructors()[id] = constructor;
                }

                if ( constructor != null )
                {
                    ret = constructor.newInstance();
                }
            } catch ( NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex )
            {
            }
        }

        return ret;
    }
}
