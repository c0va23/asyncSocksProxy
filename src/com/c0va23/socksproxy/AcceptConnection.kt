package com.c0va23.socksproxy

import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.logging.Logger

/**
 * Created by c0va23 on 30.12.17.
 */

class AcceptConnection {
    private val BUFFER_SIZE: Int = 128
    private val SOCKS4_VERSION: Byte = 0x04

    private val NULL_BYTE: Byte = 0x00
    private val REQUEST_GRANTED: Byte = 0x5A
    private val REQUEST_REJECTED: Byte = 0x5B

    private val logger = Logger.getLogger(javaClass.name)
    private val buffer = ByteBuffer.allocate(BUFFER_SIZE)

    private abstract class SocksException(message: String) : Exception(message)
    private class UnknownVersion(version: Byte): SocksException("Unknown version $version")
    private class UnexpectedCommand(code: Byte): SocksException("Unknown command code $code")
    private class UnimplementedCommand(command: Command): SocksException("Unknown command $command")

    private enum class Command(
            private val code: Byte
    ) {
        CONNECT(0x01),
        BINDING(0x02),
        UDP_ASSOCIATE(0x03);

        companion object {
            private val map = Command.values().associateBy { it.code }
            fun fromByte(code: Byte) = map[code] ?: throw UnexpectedCommand(code)
        }
    }

    private abstract class RequestData(
            val address: InetAddress,
            val port: Int,
            val command: Command
    )

    private class Socks4RequestData(
            address: InetAddress,
            port : Int,
            command: Command,
            val userId : String

    ) : RequestData(
            address = address,
            port = port,
            command = command
    )

    fun handshake(clientSocketChannel: SocketChannel) : SocketChannel? {
        return try {
            val requestData = readRequest(clientSocketChannel)
            val remoteChannel = connect(requestData)
            writeResponse(clientSocketChannel, null != remoteChannel)
            remoteChannel
        } catch(e: SocksException) {
            logger.severe(e.message)
            null
        }
    }

    private fun connect(requestData: RequestData) : SocketChannel? {
        return try {
            val socketAddress = InetSocketAddress(
                requestData.address,
                requestData.port)
            when(requestData.command) {
                Command.CONNECT -> SocketChannel.open(socketAddress)
                else -> throw UnimplementedCommand(requestData.command)
            }
        } catch(e: IOException) {
            logger.severe(e.message)
            null
        }
    }

    private fun readRequest(clientSocketChannel : SocketChannel) : RequestData {
        val readBytes = clientSocketChannel.read(buffer)
        buffer.flip()
        logger.fine("Read $readBytes bytes")

        val socksVersion = buffer.get()
        logger.fine("SOCKS version $socksVersion")
        if(socksVersion != SOCKS4_VERSION) throw UnknownVersion(socksVersion)

        val command = buffer.get()

        val port = buffer.short.toInt()
        val address = ByteArray(4)
        buffer.get(address)
        logger.fine("Address: ${address.joinToString(".")}:$port")

        val userId = buffer.slice().asCharBuffer().toString()
        logger.fine("User ID: $userId")

        buffer.clear()

        return Socks4RequestData(
                address = Inet4Address.getByAddress(address),
                port = port,
                command = Command.fromByte(command),
                userId = userId)
    }

    private fun writeResponse(clientSocketChannel: SocketChannel, granted : Boolean) {
        buffer.put(NULL_BYTE)
        if(granted)
            buffer.put(REQUEST_GRANTED)
        else
            buffer.put(REQUEST_REJECTED)
        buffer.putShort(NULL_BYTE.toShort())
        buffer.putInt(NULL_BYTE.toInt())
        buffer.flip()
        clientSocketChannel.write(buffer)
        if(granted)
            logger.info("Request granted")
        else
            logger.warning("Request rejected")

        buffer.clear()
    }
}