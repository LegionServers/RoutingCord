package net.md_5.bungee.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public class Packet01PingAdditional extends DefinedPacket
{

    private byte version;

    private Packet01PingAdditional()
    {
        super( 0x01 );
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }

	@Override
	public void read(ByteBuf buf) {}

	@Override
	public void write(ByteBuf buf) {}
}
