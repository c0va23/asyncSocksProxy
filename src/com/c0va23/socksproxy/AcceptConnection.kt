package com.c0va23.socksproxy

import java.io.IOException
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

    private val COMMAND_CONNECT: Byte = 0x01
//    private val COMMAND_BINDING: Byte = 0x02

    private val NULL_BYTE: Byte = 0x00
    private val REQUEST_GRANTED: Byte = 0x5A
    private val REQUEST_REJECTED: Byte = 0x5B

    private val logger = Logger.getLogger(javaClass.name)
    private val buffer = ByteBuffer.allocate(BUFFER_SIZE)

    private class UnknownVersion(version: Byte): Exception("Unknown version $version")
    private class UnexpectedCommand(command: Byte): Exception("Unknown command $command")

    private class RequestData(
            val address : ByteArray,
            val port : Int,
            val command: Byte,
            val user : String
    )

    fun handshake(clientSocketChannel: SocketChannel) : SocketChannel? {
        return try {
            val requestData = readRequest(clientSocketChannel)
            val remoteChannel = connect(requestData)
            writeResponse(clientSocketChannel, null != remoteChannel)
            remoteChannel
        } catch(e: UnexpectedCommand) {
            logger.severe(e.message)
            null
        } catch(e: UnknownVersion) {
            logger.severe(e.message)
            null
        }
    }

    private fun connect(requestData: RequestData) : SocketChannel? {
        return try {
            val socketAddress = InetSocketAddress(
                InetAddress.getByAddress(requestData.address),
                requestData.port)
            SocketChannel.open(socketAddress)
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
        if(socksVersion != SOCKS4_VERSION) throw UnknownVersion(socksVersion)

        val command = buffer.get()
        if(COMMAND_CONNECT != command) throw UnexpectedCommand(command)

        val port = buffer.short.toInt()
        val address = ByteArray(4)
        buffer.get(address)
        logger.fine("Address: ${address.joinToString(".")}:$port")

        val userId = buffer.slice().asCharBuffer().toString()
        logger.fine("User ID: $userId")

        buffer.clear()

        return RequestData(address, port, command, userId)
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