package com.github.c0va23.asyncSocksProxy.socks5AuthMethods

import com.github.c0va23.asyncSocksProxy.support.ByteChannelMock
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldEqual
import io.kotlintest.specs.FreeSpec

class NoAuthenticationRequiredSpec : FreeSpec({
    "have code 0" {
        NoAuthenticationRequired.code.toInt() shouldBe 0x00
    }

    "return success result" {
        val byteChannel = ByteChannelMock(emptyList())
        val result = NoAuthenticationRequired.authenticate(byteChannel)

        result shouldEqual AuthMethodInterface.Result.Success
    }

    "not write payload" {
        val byteChannel = ByteChannelMock(emptyList())
        NoAuthenticationRequired.authenticate(byteChannel)

        byteChannel.outBuffers.size shouldBe 0
    }
})
