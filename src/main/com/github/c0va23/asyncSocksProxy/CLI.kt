package com.github.c0va23.asyncSocksProxy

import java.net.InetAddress
import java.util.logging.Logger

val logger: Logger = Logger.getLogger("CLI")

fun main(args: Array<String>) {
    try {

        val host = args.getOrNull(0) ?: "0.0.0.0"
        val port = args.getOrNull(1)?.toInt() ?: 1080
        val address = InetAddress.getByName(host)

        val connector = SocketConnector
        val socksHandshake = SocksHandshake(
                connector,
                Socks4Handshake(),
                Socks5Handshake()
        )
        val proxyServer = SocksProxyServer(socksHandshake, address, port)

        while(true) {
            val command = readLine()
            when (command) {
                "start" -> proxyServer.start()
                "stop" -> proxyServer.stop()
                else -> println("Unkown command '$command'")
            }
        }

    } catch (e: ArrayIndexOutOfBoundsException) {
        logger.severe("Require port argument")
    }
}
