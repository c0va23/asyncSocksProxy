package com.c0va23.socksproxy

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.logging.Logger

/**
 * Created by c0va23 on 30.12.17.
 */

class AcceptConnection {
    private val BUFFER_SIZE: Int = 1
    private val SOCKS4_VERSION: Byte = 0x04

    private val logger = Logger.getLogger(javaClass.name)
    private val buffer = ByteBuffer.allocate(BUFFER_SIZE)

    interface SocksHandshake {
        fun parseRequest(): RequestData
        fun writeResponse(targetChannel: SocketChannel?)
    }

    fun handshake(clientSocketChannel: SocketChannel) : SocketChannel? {
        return try {
            val readBytes = clientSocketChannel.read(buffer)

            buffer.flip()
            logger.fine("Read $readBytes bytes")

            val socksVersion = buffer.get()
            logger.fine("SOCKS version $socksVersion")

            val socksHandshake = when(socksVersion) {
                SOCKS4_VERSION -> Socks4Handshake(clientSocketChannel)
                else -> throw UnknownVersion(socksVersion)
            }

            val requestData = socksHandshake.parseRequest()
            val remoteChannel = connect(requestData)
            socksHandshake.writeResponse(remoteChannel)

            remoteChannel
        } catch(e: SocksException) {
            logger.severe(e.message)
            null
        }
        finally {
            buffer.clear()
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
}