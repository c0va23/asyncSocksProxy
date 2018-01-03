package com.c0va23.socksproxy

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.logging.Logger

/**
 * Created by c0va23 on 30.12.17.
 */
class SocksProxyServer(
    private val inetAddress: InetAddress,
    private val port: Int
) {
    private val logger: Logger = Logger.getLogger(javaClass.name)

    private val serverChannel = ServerSocketChannel.open()
    private val selector: Selector = Selector.open()

    private val acceptConnection = SocksHandshake()

    private var loopEnabled = false

    private val connections = HashMap<SelectionKey, SelectionKey>()

    private val BUFFER_SIZE = 1024
    private val buffer = ByteBuffer.allocate(BUFFER_SIZE)

    fun start() {
        loopEnabled = true
        val serverSelectionKey = startServer()
        while (loopEnabled)
            processSelector(serverSelectionKey)
    }

    fun stop() {
        loopEnabled = false
    }

    private fun startServer(): SelectionKey {
        logger.info("Socket opened")

        val socketAddr = InetSocketAddress(inetAddress, port)
        serverChannel.socket().bind(socketAddr)
        logger.info("Socket binded $socketAddr")

        serverChannel.configureBlocking(false)

        return serverChannel.register(selector, SelectionKey.OP_ACCEPT)
    }

    private fun processSelector(serverSelectionKey: SelectionKey) {
        val selectedCount = selector.select()
        logger.fine("Selected keys $selectedCount")

        val selectedKeys = selector.selectedKeys()
        for(selectionKey in selectedKeys) {
            when(selectionKey) {
                serverSelectionKey -> accept(selectionKey)
                else -> copy(selectionKey)
            }
        }
        selectedKeys.clear()
    }

    private fun accept(selectionKey: SelectionKey) {
        val serverSocketChannel = selectionKey.channel() as ServerSocketChannel
        val clientChannel = serverSocketChannel.accept()
        val remoteChannel = acceptConnection.handshake(clientChannel)
        if(null == remoteChannel) {
            clientChannel.close()
            return
        }
        clientChannel.configureBlocking(false)
        remoteChannel.configureBlocking(false)
        val clientSelectionKey = clientChannel.register(selector, SelectionKey.OP_READ)
        val remoteSelectionKey = remoteChannel.register(selector, SelectionKey.OP_READ)
        connections.put(clientSelectionKey, remoteSelectionKey)
        connections.put(remoteSelectionKey, clientSelectionKey)
    }

    private fun copy(sourceSelectionKey: SelectionKey) {
        val sourceChannel =  sourceSelectionKey.channel() as SocketChannel
        val targetSelectionKey = connections[sourceSelectionKey]
        if(null == targetSelectionKey) {
            logger.warning("Not registered selection key $targetSelectionKey")
            return
        }
        val targetChannel = targetSelectionKey.channel() as SocketChannel

        try {
            val readSize = sourceChannel.read(buffer)
            logger.info("Read $readSize bytes")
            if(-1 == readSize) throw IOException("EOF")
            buffer.flip()

            while(buffer.hasRemaining()) {
                val writeSize = targetChannel.write(buffer)
                logger.info("Write $writeSize bytes")
                logger.info("Buffer ${buffer.remaining()} bytes")
            }
        }
        catch(e: IOException) {
            if(sourceChannel.isConnected) sourceChannel.close()
            if(targetChannel.isConnected) targetChannel.close()

            sourceSelectionKey.cancel()
            targetSelectionKey.cancel()

            logger.info("Close tunnel")
        }
        finally {
            buffer.clear()
        }
    }
}