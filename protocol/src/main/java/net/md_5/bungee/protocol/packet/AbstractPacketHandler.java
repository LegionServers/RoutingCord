package net.md_5.bungee.protocol.packet;

public abstract class AbstractPacketHandler
{

	public void handle(Packet0NewHandshake newHandshake) throws Exception
    {
    }
	
	public void handle(Packet01PingAdditional pingAdditional) throws Exception
    {
    }

	public void handle(Packet2Handshake handshake) throws Exception
    {
    }

    public void handle(PacketFAPluginMessage pluginMessage) throws Exception
    {
    }

    public void handle(PacketFEPing ping) throws Exception
    {
    }

    public void handle(PacketFFKick kick) throws Exception
    {
    }
}
