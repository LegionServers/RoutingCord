package net.md_5.bungee.protocol.packet;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public class Packet0NewHandshake extends DefinedPacket
{
	@Getter
	private long protocolVersion;
	@Getter
	@Setter
	private String serverAddress;
	private int port;
	private long currentState;
	
	public Packet0NewHandshake() {
		super( 0x00 );
	}

	@Override
	public void read(ByteBuf buf) {
		readVarInt(buf);
		readVarInt(buf);
		protocolVersion = readVarInt(buf);
		serverAddress = readVarIntString(buf);
		port = buf.readUnsignedShort();
		currentState = readVarInt(buf);
	}

	@Override
	public void write(ByteBuf buf) {
		int addressLength = serverAddress.getBytes( Charset.forName( "UTF-8" ) ).length;
		long length = 1
				+ getVarIntLength( protocolVersion )
				+ getVarIntLength( addressLength )
				+ addressLength
				+ 2
				+ getVarIntLength( currentState );
		writeVarInt( length, buf );
		writeVarInt( 0x00, buf );
		writeVarInt( protocolVersion, buf );
		writeVarIntString( serverAddress, buf );
		buf.writeShort( port );
		writeVarInt( currentState, buf );
	}

	@Override
	public void handle(AbstractPacketHandler handler) throws Exception {
		handler.handle( this );
	}
	
}
