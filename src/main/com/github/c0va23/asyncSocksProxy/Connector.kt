package com.github.c0va23.asyncSocksProxy

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.nio.channels.spi.AbstractSelectableChannel

interface Connector {
    @Throws(UnimplementedCommand::class)
    fun connect(requestData: RequestData): AbstractSelectableChannel?
}

object SocketConnector : Connector {
    override fun connect(requestData: RequestData): AbstractSelectableChannel? = try {
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
