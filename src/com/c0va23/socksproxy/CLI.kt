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

        SocksProxyServer(address, port).start()
    } catch (e: ArrayIndexOutOfBoundsException) {
        logger.severe("Require port argument")
    }
}
