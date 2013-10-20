package net.md_5.bungee.protocol.packet;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class DefinedPacket
{

    private final int id;

    public final int getId()
    {
        return id;
    }

    public void writeString(String s, ByteBuf buf)
    {
        // TODO: Check len - use Guava?
        buf.writeShort( s.length() );
        for ( char c : s.toCharArray() )
        {
            buf.writeChar( c );
        }
    }

    public String readString(ByteBuf buf)
    {
        // TODO: Check len - use Guava?
        short len = buf.readShort();
        char[] chars = new char[ len ];
        for ( int i = 0; i < len; i++ )
        {
            chars[i] = buf.readChar();
        }
        return new String( chars );
    }

    public void writeArray(byte[] b, ByteBuf buf)
    {
        // TODO: Check len - use Guava?
        buf.writeShort( b.length );
        buf.writeBytes( b );
    }

    public byte[] readArray(ByteBuf buf)
    {
        // TODO: Check len - use Guava?
        short len = buf.readShort();
        byte[] ret = new byte[ len ];
        buf.readBytes( ret );
        return ret;
    }
    
    public long getVarIntLength(long in)
    {
    	long size = 0;
    	do
    	{
    		size++;
    		in >>>= 7;
    	} while( in != 0 );
    	return size;
    }
    
    public long readVarInt(ByteBuf buf)
    {
    	long varIntValue = 0;
    	boolean more = true;
    	byte bitsRead = 0;
    	while ( more && bitsRead < 64 )
    	{
    		byte currentByte = buf.readByte();
    		more = currentByte < 0; // a java byte is a signed 2's complement 8 bit number
    		varIntValue |= (currentByte & 0x7f) << bitsRead;
    		bitsRead += 7;
    	}
    	return varIntValue;
    }
    
    public String readVarIntString(ByteBuf buf)
    {
    	int len = (int) readVarInt(buf);
    	byte[] b = new byte[ len ];
    	buf.readBytes( b );
        return new String( b, Charset.forName( "UTF-8" ) );
    }
    
    public void writeVarInt(long in, ByteBuf buf)
    {
    	do
    	{
    		byte currentByte = (byte) (in & 0x7f);
    		in >>>= 7;
    		if ( in != 0 )
    			currentByte |= 1 << 7;
    		buf.writeByte( currentByte );
    	} while ( in != 0 );
    }
    
    public void writeVarIntString(String in, ByteBuf buf)
    {
    	byte[] b = in.getBytes( Charset.forName( "UTF-8" ) );
    	long len = b.length;
    	writeVarInt( len, buf );
    	buf.writeBytes( b );
    }

    public abstract void read(ByteBuf buf);

    public abstract void write(ByteBuf buf);

    public abstract void handle(AbstractPacketHandler handler) throws Exception;

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
