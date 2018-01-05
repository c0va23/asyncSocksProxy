package com.c0va23.socksproxy

import java.net.InetAddress
import java.util.logging.Logger

val logger: Logger = Logger.getLogger("CLI")

fun main(args: Array<String>) {
    try {

        val host = args.getOrNull(0) ?: "0.0.0.0"
        val port = args.getOrNull(1)?.toInt() ?: 1080
        val address = InetAddress.getByName(host)

        val proxyServer = SocksProxyServer(address, port)

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
