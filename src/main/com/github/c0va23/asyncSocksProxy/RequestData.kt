package com.github.c0va23.asyncSocksProxy

import java.net.InetAddress

abstract class RequestData(
        val address: InetAddress,
        val port: Int,
        val command: Command
)

class Socks4RequestData(
        address: InetAddress,
        port: Int,
        command: Command,
        val userId: String

) : RequestData(
        address = address,
        port = port,
        command = command
)

class Socks5RequestData(
        address: InetAddress,
        port: Int,
        command: Command
) : RequestData(
        address = address,
        port = port,
        command = command
)
