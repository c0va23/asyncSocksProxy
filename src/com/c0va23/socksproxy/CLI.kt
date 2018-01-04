package com.c0va23.socksproxy

import java.net.InetAddress
import java.util.logging.Logger

val logger: Logger = Logger.getLogger("CLI")

fun main(args: Array<String>) {
    try {
        val port = args[0].toInt()
        val host = args[1]
        logger.info("Port $port")
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
