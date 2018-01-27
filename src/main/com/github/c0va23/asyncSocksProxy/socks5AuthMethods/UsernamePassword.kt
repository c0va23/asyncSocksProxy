package com.github.c0va23.asyncSocksProxy.socks5AuthMethods

import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.charset.Charset
import java.util.logging.Logger

class UsernamePassword(
        private val validator: UsernamePasswordValidator
) : AuthMethodInterface {
    override val code: Byte = 0x02

    private val logger = Logger.getLogger(javaClass.name)

    private val bufferSize = 512
    private val buffer = ByteBuffer.allocate(bufferSize)

    private val charset = Charset.forName("UTF-8")

    companion object {
        const val VERSION: Byte = 0x01
        const val SUCCESS_CODE: Byte = 0x00
        const val FAILURE_CODE: Byte = 0x01
    }

    @Synchronized
    override fun authenticate(byteChannel: ByteChannel): AuthMethodInterface.Result {
        val result = parseAndCheckRequest(byteChannel)

        writeResponse(result, byteChannel)

        return result
    }

    private fun readString(buffer: ByteBuffer): String {
        val length = buffer.get().toInt()
        val stringBytes = ByteArray(length)
        buffer.get(stringBytes)
        return String(stringBytes, charset)
    }

    private fun parseAndCheckRequest(byteChannel: ByteChannel) = try {
        byteChannel.read(buffer)
        buffer.flip()

        val version = buffer.get()
        if (VERSION != version) {
            logger.warning("Invalid version $version")
            AuthMethodInterface.Result.Failure
        } else {
            val username = readString(buffer)
            val password = readString(buffer)

            if (validator.validate(username, password))
                AuthMethodInterface.Result.Success
            else {
                logger.warning("Invalid username or password")
                AuthMethodInterface.Result.Failure
            }
        }
    } finally {
        buffer.clear()
    }

    private fun writeResponse(result: AuthMethodInterface.Result, byteChannel: ByteChannel) = try {
        buffer.put(VERSION)
        buffer.put(when(result) {
            AuthMethodInterface.Result.Success -> SUCCESS_CODE
            AuthMethodInterface.Result.Failure -> FAILURE_CODE
        })
        buffer.flip()

        byteChannel.write(buffer)
    } finally {
        buffer.clear()
    }

}
