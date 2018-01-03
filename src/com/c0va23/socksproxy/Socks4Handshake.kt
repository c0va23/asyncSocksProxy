package com.c0va23.socksproxy

import java.net.Inet4Address
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.logging.Logger

class Socks4Handshake(
    private val sourceChannel: SocketChannel
) : AcceptConnection.SocksHandshake {
    private val NULL_BYTE: Byte = 0x00
    private val REQUEST_GRANTED: Byte = 0x5A
    private val REQUEST_REJECTED: Byte = 0x5B

    private val BUFFER_SIZE = 128

    private val logger = Logger.getLogger(javaClass.name)
    private val buffer = ByteBuffer.allocate(BUFFER_SIZE)

    override fun parseRequest(): AcceptConnection.Socks4RequestData {
        try {
            val readBytes = sourceChannel.read(buffer)
            logger.fine("Read $readBytes bytes")

            buffer.flip()

            val command = buffer.get()

            val port = buffer.short.toInt()
            val address = ByteArray(4)
            buffer.get(address)
            logger.fine("Address: ${address.joinToString(".")}:$port")

            val userId = buffer.slice().asCharBuffer().toString()
            logger.fine("User ID: $userId")

            return AcceptConnection.Socks4RequestData(
                    address = Inet4Address.getByAddress(address),
                    port = port,
                    command = AcceptConnection.Command.fromByte(command),
                    userId = userId)
        }
        finally {
            buffer.clear()
        }
    }

    override fun writeResponse(targetChannel: SocketChannel?) {
        val granted = null != targetChannel
        try {
            buffer.put(NULL_BYTE)
            buffer.put(
                if(granted) REQUEST_GRANTED
                else REQUEST_REJECTED
            )
            buffer.putShort(NULL_BYTE.toShort())
            buffer.putInt(NULL_BYTE.toInt())
            buffer.flip()

            sourceChannel.write(buffer)
            if (granted)
                logger.info("Request granted")
            else
                logger.warning("Request rejected")
        }
        finally {
            buffer.clear()
        }
    }
}