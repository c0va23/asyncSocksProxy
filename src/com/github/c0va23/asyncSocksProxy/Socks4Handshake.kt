package com.github.c0va23.asyncSocksProxy

import java.net.Inet4Address
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.logging.Logger

class Socks4Handshake(
        private val sourceChannel: SocketChannel
) : SocksHandshake {
    private val nullByte: Byte = 0x00
    private val nullShort: Short = 0x00
    private val nullInt: Int = 0x00

    private val bufferSize = 128

    private val logger = Logger.getLogger(javaClass.name)
    private val buffer = ByteBuffer.allocate(bufferSize)

    private enum class Response(val code: Byte) {
        GRANTED(0x5A),
        REJECTED(0x5B);
    }

    override fun parseRequest(): Socks4RequestData {
        try {
            val readBytes = sourceChannel.read(buffer)
            logger.fine("Read $readBytes bytes")

            buffer.flip()

            val command = buffer.get()

            val port = buffer.short.toInt()
            val addressBytes = ByteArray(4)
            buffer.get(addressBytes)
            val address = Inet4Address.getByAddress(addressBytes)
            logger.fine("Address: $address:$port")

            val userId = buffer.slice().asCharBuffer().toString()
            logger.fine("User ID: $userId")

            return Socks4RequestData(
                    address = address,
                    port = port,
                    command = Command.fromByte(command),
                    userId = userId)
        } finally {
            buffer.clear()
        }
    }

    override fun writeResponse(connected: Boolean, requestData: RequestData) {
        try {
            buffer.put(nullByte)
            buffer.put(
                    (if (connected) Response.GRANTED else Response.REJECTED).code
            )
            buffer.putShort(nullShort)
            buffer.putInt(nullInt)
            buffer.flip()

            sourceChannel.write(buffer)
            if (connected)
                logger.info("Request granted")
            else
                logger.warning("Request rejected")
        } finally {
            buffer.clear()
        }
    }
}