package com.github.c0va23.asyncSocksProxy

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.logging.Logger

/**
 * Created by c0va23 on 30.12.17.
 */

class SocksHandshake(
        private vararg val socksHandshakes: SocksHandshakeInterface
) {
    private val bufferSize: Int = 1

    private val logger = Logger.getLogger(javaClass.name)

    fun handshake(clientSocketChannel: SocketChannel): SocketChannel? = try {
        val buffer = ByteBuffer.allocate(bufferSize)
        val readBytes = clientSocketChannel.read(buffer)

        buffer.flip()
        logger.fine("Read $readBytes bytes")

        val socksVersion = buffer.get()
        logger.fine("SOCKS version $socksVersion")

        val socksHandshake = socksHandshakes.find { it.version == socksVersion }
            ?: throw UnknownVersion(socksVersion)

        val requestData = socksHandshake.parseRequest(clientSocketChannel)
        val remoteChannel = connect(requestData)
        val connected = null != remoteChannel
        socksHandshake.writeResponse(clientSocketChannel, connected, requestData)

        remoteChannel
    } catch (e: SocksException) {
        logger.severe(e.message)
        null
    }

    private fun connect(requestData: RequestData): SocketChannel? = try {
        val socketAddress = InetSocketAddress(
                requestData.address,
                requestData.port)
        when (requestData.command) {
            Command.CONNECT -> SocketChannel.open(socketAddress)
            else -> throw UnimplementedCommand(requestData.command)
        }
    } catch (e: IOException) {
        logger.severe(e.message)
        null
    }
}
