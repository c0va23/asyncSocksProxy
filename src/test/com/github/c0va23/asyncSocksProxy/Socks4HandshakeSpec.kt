import java.net.InetAddress

import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.containsAll
import io.kotlintest.matchers.should
import io.kotlintest.specs.FreeSpec

import com.github.c0va23.asyncSocksProxy.Socks4Handshake
import com.github.c0va23.asyncSocksProxy.Socks4RequestData
import com.github.c0va23.asyncSocksProxy.Command

import com.github.c0va23.asyncSocksProxy.support.ByteChannelMock

class Socks4HandshakeSpec : FreeSpec({
    "parseRequest()" - {
        "when input valid buffer without user" - {
            val inputBuffer = byteArrayOf(
                    Command.CONNECT.code,
                    0, 80, // Port
                    8, 8, 8, 8, // Address
                    0 // Empty user id
            )
            val sourceChannel = ByteChannelMock(listOf(inputBuffer))
            val socks4Handshake = Socks4Handshake(sourceChannel)
            val requestData = socks4Handshake.parseRequest()

            "return valid Socks4RequestData" {
                requestData shouldBe Socks4RequestData(
                        address =  InetAddress.getByName("8.8.8.8"),
                        command = Command.CONNECT,
                        port = 80,
                        userId = ""
                )
            }
        }

        "when input valid buffer with user" - {
            val userName = "bob"
            val inputBuffer = byteArrayOf(
                    Command.CONNECT.code,
                    0, 80, // Port
                    8, 8, 8, 8, // Address
                    *userName.toByteArray(),
                    0
            )
            val sourceChannel = ByteChannelMock(listOf(inputBuffer))
            val socks4Handshake = Socks4Handshake(sourceChannel)
            val requestData = socks4Handshake.parseRequest()

            "return valid Socks4RequestData" {
                requestData shouldBe Socks4RequestData(
                        address =  InetAddress.getByName("8.8.8.8"),
                        command = Command.CONNECT,
                        port = 80,
                        userId = userName
                )
            }
        }
    }

    "writeResponse()" - {
        "when connected" - {
            val sourceChannel = ByteChannelMock(listOf())
            val socks4Handshake = Socks4Handshake(sourceChannel)
            val requestData = Socks4RequestData(
                    address = InetAddress.getByName("8.8.8.8"),
                    port = 443,
                    command = Command.CONNECT,
                    userId = ""
            )
            socks4Handshake.writeResponse(true, requestData)

            "write valid out buffer" {
                sourceChannel.outBuffers.first().array().toList() should containsAll(byteArrayOf(
                        0,
                        Socks4Handshake.Response.GRANTED.code,
                        0, 0,
                        0, 0, 0, 0
                ).toList())
            }
        }

        "when not connected" - {
            val sourceChannel = ByteChannelMock(listOf())
            val socks4Handshake = Socks4Handshake(sourceChannel)
            val requestData = Socks4RequestData(
                    address = InetAddress.getByName("0.0.0.0"),
                    port = 443,
                    command = Command.BINDING,
                    userId = ""
            )
            socks4Handshake.writeResponse(false, requestData)

            "write valid out buffer" {
                sourceChannel.outBuffers.first().array().toList() should containsAll(byteArrayOf(
                        0,
                        Socks4Handshake.Response.REJECTED.code,
                        0, 0,
                        0, 0, 0, 0
                ).toList())
            }

        }
    }
})
