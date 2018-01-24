package com.github.c0va23.asyncSocksProxy.socks5AuthMethods

import java.nio.channels.ByteChannel

interface AuthMethodInterface {
    val code: Byte

    enum class Result {
        Success,
        Failure
    }

    fun authenticate(byteChannel: ByteChannel): Result
}
