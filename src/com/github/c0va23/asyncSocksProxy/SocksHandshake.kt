package com.github.c0va23.asyncSocksProxy

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.logging.Logger

/**
 * Created by c0va23 on 30.12.17.
 */

interface SocksHandshake {
    fun parseRequest(): RequestData
    fun writeResponse(connected: Boolean, requestData: RequestData)

    companion object {
        private val bufferSize: Int = 1

        private val socks4Version: Byte = 0x04
        private val socks5Version: Byte = 0x05

        private val logger = Logger.getLogger(SocksHandshake.toString())

        fun handshake(clientSocketChannel: SocketChannel): SocketChannel? {
            val buffer = ByteBuffer.allocate(bufferSize)
            return try {
                val readBytes = clientSocketChannel.read(buffer)

                buffer.flip()
                logger.fine("Read $readBytes bytes")

                val socksVersion = buffer.get()
                logger.fine("SOCKS version $socksVersion")

                val socksHandshake = when (socksVersion) {
                    socks4Version -> Socks4Handshake(clientSocketChannel)
                    socks5Version -> Socks5Handshake(clientSocketChannel)
                    else -> throw UnknownVersion(socksVersion)
                }

                val requestData = socksHandshake.parseRequest()
                val remoteChannel = connect(requestData)
                val connected = null != remoteChannel
                socksHandshake.writeResponse(connected, requestData)

                remoteChannel
            } catch (e: SocksException) {
                logger.severe(e.message)
                null
            } finally {
                buffer.clear()
            }
        }

        private fun connect(requestData: RequestData): SocketChannel? {
            return try {
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
    }
}