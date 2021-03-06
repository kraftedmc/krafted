package protocol.packet

import protocol.packet.impl.handshake.serverbound.HandshakePacket
import protocol.packet.impl.login.clientbound.DisconnectPacket
import protocol.packet.impl.login.clientbound.EncryptionRequestPacket
import protocol.packet.impl.login.clientbound.LoginSuccessPacket
import protocol.packet.impl.login.serverbound.LoginStartPacket
import protocol.packet.impl.status.PingPacket
import protocol.packet.impl.status.clientbound.PongPacket
import protocol.packet.impl.status.clientbound.ResponsePacket
import protocol.packet.impl.status.serverbound.RequestPacket
import server.connection.State

object PacketRegistry {

    val packets: Map<Direction, Map<State, Map<Int, () -> Packet>>> = mapOf(
        Direction.Serverbound to mapOf(
            State.Handshake to mapOf(
                0x00 to { HandshakePacket() },
            ),
            State.Status to mapOf(
                0x00 to { RequestPacket() },
                0x01 to { PingPacket() }
            ),
            State.Login to mapOf(
                0x00 to { LoginStartPacket() }
            )
        ),
        Direction.Clientbound to mapOf(
            State.Status to mapOf(
                0x00 to { ResponsePacket() },
                0x01 to { PongPacket() }
            ),
            State.Login to mapOf(
                0x00 to { DisconnectPacket() },
                0x01 to { EncryptionRequestPacket() },
                0x02 to { LoginSuccessPacket() }
            )
        ),
    )

    fun findPacket(id: Int, state: State, direction: Direction): (() -> Packet)? {
        return packets[direction]
            ?.get(state)
            ?.get(id)
    }
}
