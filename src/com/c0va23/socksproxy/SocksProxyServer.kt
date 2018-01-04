package com.c0va23.socksproxy

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.util.logging.Logger

/**
 * Created by c0va23 on 30.12.17.
 */
class SocksProxyServer(
    private val address: InetAddress,
    private val port: Int
) {
    private val logger: Logger = Logger.getLogger(javaClass.name)

    private val serverChannel = ServerSocketChannel.open()
    private val selector: Selector = Selector.open()

    private var loopEnabled = false

    private val connections = HashMap<SelectionKey, SelectionKey>()

    private val bufferSize = 1024
    private val buffer = ByteBuffer.allocate(bufferSize)

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

        val socketAddr = InetSocketAddress(address, port)
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
        val remoteChannel = SocksHandshake.handshake(clientChannel)
        if(null == remoteChannel) {
            clientChannel.close()
            return
        }
        registerConnections(clientChannel, remoteChannel)
    }

    private fun copy(sourceSelectionKey: SelectionKey) {
        if(!sourceSelectionKey.isValid) return

        val sourceChannel =  sourceSelectionKey.channel() as SocketChannel
        val targetSelectionKey = connections[sourceSelectionKey]
        if(null == targetSelectionKey) {
            logger.warning("Not registered selection key $sourceSelectionKey")
            return
        }
        val targetChannel = targetSelectionKey.channel() as SocketChannel

        try {
            val readSize = sourceChannel.read(buffer)
            logger.fine("Read $readSize bytes")
            if(-1 == readSize) throw IOException("EOF")
            buffer.flip()

            while(buffer.hasRemaining()) {
                val writeSize = targetChannel.write(buffer)
                logger.fine("Write $writeSize bytes")
                logger.fine("Buffer ${buffer.remaining()} bytes")
            }
        }
        catch(e: IOException) {
            closeConnection(sourceSelectionKey, sourceChannel)
            closeConnection(targetSelectionKey, targetChannel)

            logger.info("Close tunnel")
        }
        finally {
            buffer.clear()
        }
    }

    private fun registerConnections(sourceSocketChannel: SocketChannel,
                                   targetSocketChannel: SocketChannel) {
        sourceSocketChannel.configureBlocking(false)
        targetSocketChannel.configureBlocking(false)
        val clientSelectionKey = sourceSocketChannel.register(selector, SelectionKey.OP_READ)
        val remoteSelectionKey = targetSocketChannel.register(selector, SelectionKey.OP_READ)
        connections.put(clientSelectionKey, remoteSelectionKey)
        connections.put(remoteSelectionKey, clientSelectionKey)
    }

    private fun closeConnection(selectionKey: SelectionKey, socketChannel: SocketChannel) {
        if(socketChannel.isConnected)
            socketChannel.close()

        selectionKey.cancel()

        connections.remove(selectionKey)
    }
}