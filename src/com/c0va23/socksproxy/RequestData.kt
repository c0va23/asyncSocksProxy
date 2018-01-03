package com.c0va23.socksproxy

import java.net.InetAddress

abstract class RequestData(
        val address: InetAddress,
        val port: Int,
        val command: Command
)

class Socks4RequestData(
        address: InetAddress,
        port : Int,
        command: Command,
        val userId : String

) : RequestData(
        address = address,
        port = port,
        command = command
)