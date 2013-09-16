package net.md_5.bungee.protocol;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Constructor;
import net.md_5.bungee.protocol.packet.DefinedPacket;

public interface Protocol
{

    DefinedPacket read(short packetId, ByteBuf buf, boolean parsePackets);

    Class<? extends DefinedPacket>[] getClasses();

    Constructor<? extends DefinedPacket>[] getConstructors();
}
