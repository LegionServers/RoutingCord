package net.md_5.bungee.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class Packet2Handshake extends DefinedPacket
{

    private byte procolVersion;

    private Packet2Handshake()
    {
        super( 0x02 );
    }

    @Override
    public void read(ByteBuf buf)
    {
        procolVersion = buf.readByte();
    }

    @Override
    public void write(ByteBuf buf)
    {
        buf.writeByte( procolVersion );
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }
}
