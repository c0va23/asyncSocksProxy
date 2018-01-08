package com.github.c0va23.asyncSocksProxy

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
    enum class State {
        Started,
        Stopping,
        Stopped,
    }

    private val logger: Logger = Logger.getLogger(javaClass.name)

    private var serverChannel: ServerSocketChannel? = null
    private val selector: Selector = Selector.open()
    private val selectTimeout = 3000L

    private val waitStopDelay = selectTimeout

    private var state = State.Stopped

    private val connections = HashMap<SelectionKey, SelectionKey>()

    private val bufferSize = 1024
    private val buffer = ByteBuffer.allocate(bufferSize)

    fun start() {
        if(State.Stopped != state) {
            logger.warning("Can not start server in $state state")
            return
        }
        state = State.Started
        Thread {
            val serverSelectionKey = startServer()
            while (State.Started == state)
                processSelector(serverSelectionKey)
            logger.info("Stop loop")
            state = State.Stopped
        }.start()
    }

    fun stop() {
        if(State.Started != state) {
            logger.warning("Can not stop server in $state state")
            return
        }
        state = State.Stopping
        while (State.Stopped != state) {
            logger.warning("Wait $waitStopDelay")
            Thread.sleep(waitStopDelay)
        }
        stopServer()
    }

    private fun startServer(): SelectionKey {
        logger.info("Socket opened")

        val socketAddr = InetSocketAddress(address, port)
        val serverChannel = ServerSocketChannel.open()
        serverChannel.socket().bind(socketAddr)
        logger.info("Socket binded $socketAddr")

        serverChannel.configureBlocking(false)

        this.serverChannel = serverChannel

        return serverChannel.register(selector, SelectionKey.OP_ACCEPT)
    }

    private fun processSelector(serverSelectionKey: SelectionKey) {
        val selectedCount = selector.select(selectTimeout)
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
            closeConnection(sourceSelectionKey)
            closeConnection(targetSelectionKey)

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

    private fun closeConnection(selectionKey: SelectionKey) {
        val socketChannel = selectionKey.channel() as SocketChannel

        if(socketChannel.isConnected)
            socketChannel.close()

        selectionKey.cancel()

        connections.remove(selectionKey)
    }

    private fun stopServer() {
        for(selectionKey in selector.keys()) {
            val channel = selectionKey.channel()
            when (channel) {
                is SocketChannel -> channel.close()
                is ServerSocketChannel -> channel.socket().close()
            }
            selectionKey.cancel()
            connections.remove(selectionKey)
            logger.info("Close socket")
        }
        this.serverChannel = null
        if(connections.isNotEmpty())
            logger.warning("${connections.size} connections exists")
        logger.info("Server stopped")
    }
}
