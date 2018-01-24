package com.github.c0va23.asyncSocksProxy.socks5AuthMethods

import java.nio.channels.ByteChannel

object NoAuthenticationRequired : AuthMethodInterface {
    override val code: Byte = 0x00

    override fun authenticate(byteChannel: ByteChannel) =
            AuthMethodInterface.Result.Success
}
