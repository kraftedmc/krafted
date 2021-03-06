package protocol

import chat.ChatComponent
import io.netty.buffer.ByteBuf
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private const val SEGMENT_BITS = 0x7F
private const val CONTINUE_BIT = 0x80

fun Int.varIntSize(): Int {
    var i = this
    var size = 0

    while (i and -0x80 != 0x0) {
        size++
        i = i ushr 7

        if (size > 5) {
            throw IllegalArgumentException("VarInt is longer than 5 bytes.")
        }
    }

    return size
}

fun ByteBuf.readVarInt(): Int {
    var value = 0
    var position = 0
    var currentByte: Byte

    while (true) {
        currentByte = readByte()
        value = value or (currentByte.toInt() and SEGMENT_BITS shl position)

        if (currentByte.toInt() and CONTINUE_BIT == 0)
            break

        position += 7

        if (position >= 32)
            throw RuntimeException("VarInt is too big")
    }

    return value
}

fun ByteBuf.readVarLong(): Long {
    var value: Long = 0
    var position = 0
    var currentByte: Byte

    while (true) {
        currentByte = readByte()
        value = value or ((currentByte.toInt() and SEGMENT_BITS shl position).toLong())

        if (currentByte.toInt() and CONTINUE_BIT == 0)
            break

        position += 7

        if (position >= 64)
            throw RuntimeException("VarInt is too big")
    }

    return value
}

fun ByteBuf.readChatComponent(): ChatComponent {
    return Json.decodeFromString(
        readString()
    )
}

fun ByteBuf.writeChatComponent(component: ChatComponent) {
    writeString(Json.encodeToString(component))
}

fun ByteBuf.readString(): String {
    val length = readVarInt()
    val bytes = readBytes(length)

    return bytes.toString(Charsets.UTF_8)
}

fun ByteBuf.writeString(data: String) {
    writeVarInt(data.length)
    this.writeBytes(data.toByteArray(Charsets.UTF_8))
}

fun ByteBuf.writeVarInt(data: Int) {
    var value = data
    while (true) {
        if (value and SEGMENT_BITS == value) {
            writeByte(value)
            return
        }

        writeByte(value and SEGMENT_BITS or CONTINUE_BIT)
        value = value ushr 7
    }
}

fun ByteBuf.writeVarLong(data: Long) {
    var value = data
    while (true) {
        if (value and SEGMENT_BITS.toLong() == value) {
            writeByte(value.toInt())
            return
        }

        writeByte((value and SEGMENT_BITS.toLong() or CONTINUE_BIT.toLong()).toInt())
        value = value ushr 7
    }
}

fun ByteBuf.readVarBoolean(): Boolean {
    return this.readByte() == 0x01.toByte()
}

fun ByteBuf.writeVarBoolean(boolean: Boolean) {
    this.writeByte(if (boolean) 0x01 else 0x00)
}

fun ByteBuf.readUniqueId(): UUID {
    return UUID(readVarLong(), readVarLong())
}
