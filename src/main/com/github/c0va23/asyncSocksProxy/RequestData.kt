package com.github.c0va23.asyncSocksProxy

import java.net.InetAddress

interface RequestData {
        val address: InetAddress
        val port: Int
        val command: Command
}

data class Socks4RequestData(
        override val address: InetAddress,
        override val port: Int,
        override val command: Command,
        val userId: String

) : RequestData

data class Socks5RequestData(
        override val address: InetAddress,
        override val port: Int,
        override val command: Command
) : RequestData
