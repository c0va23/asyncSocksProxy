package com.github.c0va23.asyncSocksProxy

import java.nio.channels.ByteChannel

interface SocksHandshakeInterface {
    val version: Byte
    fun parseRequest(byteChannel: ByteChannel): RequestData
    fun writeResponse(byteChannel: ByteChannel, connected: Boolean, requestData: RequestData)
}
