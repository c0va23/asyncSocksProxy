import java.net.Inet4Address

import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.containsAll
import io.kotlintest.matchers.should
import io.kotlintest.specs.FreeSpec

import com.github.c0va23.asyncSocksProxy.Socks4Handshake
import com.github.c0va23.asyncSocksProxy.Socks4RequestData
import com.github.c0va23.asyncSocksProxy.Command

import com.github.c0va23.asyncSocksProxy.support.ByteChannelMock

class Socks4HandshakeSpec : FreeSpec({
    fun Int.toPortBytes() : ByteArray =
        byteArrayOf(
                (this / 256).toByte(),
                (this % 256).toByte()
        )

    fun buildBuffer(command: Command, port: Int, address: Inet4Address, userId: String = "") =
        byteArrayOf(
                command.code,
                *port.toPortBytes(), // Port
                *address.address,
                *userId.toByteArray(), 0
        )

    "parseRequest()" - {
        val port = 80
        val address = Inet4Address.getByName("8.8.8.8") as Inet4Address
        "when input valid buffer without user" - {
            val inputBuffer = buildBuffer(Command.CONNECT, port, address)
            val sourceChannel = ByteChannelMock(listOf(inputBuffer))
            val socks4Handshake = Socks4Handshake(sourceChannel)
            val requestData = socks4Handshake.parseRequest()

            "return valid Socks4RequestData" {
                requestData shouldBe Socks4RequestData(
                        address =  address,
                        command = Command.CONNECT,
                        port = port
                )
            }
        }

        "when input valid buffer with user" - {
            val userName = "bob"
            val inputBuffer = buildBuffer(Command.CONNECT, port, address, userName)
            val sourceChannel = ByteChannelMock(listOf(inputBuffer))
            val socks4Handshake = Socks4Handshake(sourceChannel)
            val requestData = socks4Handshake.parseRequest()

            "return valid Socks4RequestData" {
                requestData shouldBe Socks4RequestData(
                        address = address,
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
                    address = Inet4Address.getByName("8.8.8.8"),
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
                    address = Inet4Address.getByName("0.0.0.0"),
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
