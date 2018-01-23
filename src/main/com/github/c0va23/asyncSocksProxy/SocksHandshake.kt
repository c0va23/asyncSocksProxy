package com.github.c0va23.asyncSocksProxy

import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.channels.spi.AbstractSelectableChannel
import java.util.logging.Logger

/**
 * Created by c0va23 on 30.12.17.
 */

class SocksHandshake(
        private val connector: Connector,
        private vararg val socksHandshakes: SocksHandshakeInterface
) {
    private val bufferSize: Int = 1

    private val logger = Logger.getLogger(javaClass.name)

    fun handshake(clientSocketChannel: ByteChannel): AbstractSelectableChannel? = try {
        val buffer = ByteBuffer.allocate(bufferSize)
        val readBytes = clientSocketChannel.read(buffer)

        buffer.flip()
        logger.fine("Read $readBytes bytes")

        val socksVersion = buffer.get()
        logger.fine("SOCKS version $socksVersion")

        val socksHandshake = socksHandshakes.find { it.version == socksVersion }
            ?: throw UnknownVersion(socksVersion)

        val requestData = socksHandshake.parseRequest(clientSocketChannel)
        val remoteChannel = connector.connect(requestData)
        val connected = null != remoteChannel
        socksHandshake.writeResponse(clientSocketChannel, connected, requestData)

        remoteChannel
    } catch (e: SocksException) {
        logger.warning(e.message)
        null
    }
}
