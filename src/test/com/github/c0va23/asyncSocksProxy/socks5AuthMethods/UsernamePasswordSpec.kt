package com.github.c0va23.asyncSocksProxy.socks5AuthMethods

import com.github.c0va23.asyncSocksProxy.support.ByteChannelMock
import io.kotlintest.matchers.*
import io.kotlintest.properties.forAll
import io.kotlintest.properties.headers
import io.kotlintest.properties.row
import io.kotlintest.properties.table
import io.kotlintest.specs.FreeSpec
import java.nio.charset.Charset

class UsernamePasswordSpec : FreeSpec({
    fun buildRequest(username: String, password: String, version: Byte = 0x01): ByteArray {
        val charset = Charset.forName("UTF-8")
        val usernameBytes = username.toByteArray(charset)
        val passwordBytes = password.toByteArray(charset)
        return byteArrayOf(
                version,
                usernameBytes.size.toByte(),
                *usernameBytes,
                passwordBytes.size.toByte(),
                *passwordBytes
        )
    }

    "when pass valid username and password" - {
        "return success result" {
            forAll(table(
                    headers("Username", "Password"),
                    row("John", "password"),
                    row("Иван", "Пароль")
            )) { username: String, password: String ->
                val validator = SimpleUsernamePasswordValidator(username, password)
                val usernamePassword = UsernamePassword(validator)

                val requestPayload = buildRequest(username, password)
                val byteChannel = ByteChannelMock(listOf(requestPayload))

                val result = usernamePassword.authenticate(byteChannel)

                result shouldBe AuthMethodInterface.Result.Success

                byteChannel.outBuffers.size shouldBe 1
                byteChannel.outBuffers.first().toList() shouldBe listOf(
                        UsernamePassword.VERSION,
                        UsernamePassword.SUCCESS_CODE
                )
            }
        }
    }

    "when pass valid username, but invalid password" - {
        val username = "John"
        val validator = SimpleUsernamePasswordValidator(username, "password")
        val usernamePassword = UsernamePassword(validator)

        val requestPayload = buildRequest(username, "invalid")
        val byteChannel = ByteChannelMock(listOf(requestPayload))

        val result = usernamePassword.authenticate(byteChannel)

        "return failure result" {
            result shouldBe AuthMethodInterface.Result.Failure

            byteChannel.outBuffers.size shouldBe 1
            byteChannel.outBuffers.first().toList() shouldBe listOf(
                    UsernamePassword.VERSION,
                    UsernamePassword.FAILURE_CODE
            )
        }
    }

    "when pass invalid version" - {
        val username = "John"
        val password = "Pass"
        val requestPayload = buildRequest(username, password, version = 0x02)
        val byteChannel = ByteChannelMock(listOf(requestPayload))

        val validator = SimpleUsernamePasswordValidator(username, password)
        val usernamePassword = UsernamePassword(validator)

        "return failure result" {
            val result = usernamePassword.authenticate(byteChannel)

            result shouldBe AuthMethodInterface.Result.Failure

            byteChannel.outBuffers.size shouldBe 1
            byteChannel.outBuffers.first().toList() shouldBe listOf(
                    UsernamePassword.VERSION,
                    UsernamePassword.FAILURE_CODE
            )
        }
    }
})
