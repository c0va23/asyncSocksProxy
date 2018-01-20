package com.github.c0va23.asyncSocksProxy

import io.kotlintest.specs.FreeSpec

import com.github.c0va23.asyncSocksProxy.Socks5Handshake.Method
import com.github.c0va23.asyncSocksProxy.Socks5Handshake.AddressType
import com.github.c0va23.asyncSocksProxy.support.ByteChannelMock
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldEqual
import io.kotlintest.properties.forAll
import io.kotlintest.properties.headers
import io.kotlintest.properties.row
import io.kotlintest.properties.table
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

class Socks5HandshakeSpec : FreeSpec({
    "parseRequest()" - {
        fun buildAuthRequestPayload(vararg methods: Method) =
                byteArrayOf(
                    methods.size.toByte(),
                    *methods.map { it.code }.toByteArray()
                )

        fun buildCommandRequestPayload(
                command: Command,
                addressType: AddressType,
                addressHost: String,
                port: Int
        ): ByteArray {
            val addressPayload = when(addressType) {
                AddressType.Ipv4, AddressType.Ipv6 -> InetAddress.getByName(addressHost).address
                AddressType.DomainName -> byteArrayOf(addressHost.length.toByte()) + addressHost.toByteArray()
            }
            return byteArrayOf(
                    0x05, // Version
                    command.code,
                    0, // Reserved
                    addressType.code,
                    *addressPayload,
                    *port.toPortBytes()
            )
        }

        "when request NO_AUTHENTICATION_REQUIRED" {
            val authRequestPayload = buildAuthRequestPayload(Method.NO_AUTHENTICATION_REQUIRED)

            forAll(table(
                    headers("Command", "Address type", "Address", "Port"),
                    row(Command.CONNECT, AddressType.Ipv4, "8.8.8.8", 80),
                    row(Command.BINDING, AddressType.Ipv6, "2001:4860:4860::8888", 443),
                    row(Command.CONNECT, AddressType.DomainName, "localhost", 80),
                    row(Command.UDP_ASSOCIATE, AddressType.Ipv4, "8.8.8.8", 0xFFFF)
            )) { command: Command, addressType: AddressType, addressHost: String, port: Int ->
                val address = InetAddress.getByName(addressHost)
                val commandRequestPayload = buildCommandRequestPayload(
                        command, addressType, addressHost, port)
                val byteChannel = ByteChannelMock(listOf(authRequestPayload, commandRequestPayload))
                val socks5Handshake = Socks5Handshake(byteChannel)

                socks5Handshake.parseRequest() shouldBe Socks5RequestData(
                        command = command,
                        address = address,
                        port = port
                )

                val methodResponsePayload = byteChannel.outBuffers[0].array()
                methodResponsePayload[0] shouldEqual 0x05.toByte()
                methodResponsePayload[1] shouldEqual Method.NO_AUTHENTICATION_REQUIRED.code
            }
        }
    }

    "writeResponse()" - {
        val socketTable =table(
                headers("IP", "Address type", "Port"),
                row(Inet4Address.getByName("127.0.0.1"), AddressType.Ipv4, 80),
                row(Inet6Address.getByName("::1"), AddressType.Ipv6, 1080)
        )
        "when connected" - {
            "write success response" {
                forAll(socketTable) { address: InetAddress, addressType: AddressType, port: Int ->
                    val byteChannel = ByteChannelMock(listOf())
                    val socks5Handshake = Socks5Handshake(byteChannel)

                    val requestData = Socks5RequestData(
                            address = address,
                            port = port,
                            command = Command.CONNECT
                    )

                    socks5Handshake.writeResponse(true, requestData)

                    val responseBytes = byteChannel.outBuffers.first().array()

                    responseBytes[0] shouldEqual 0x05.toByte()
                    responseBytes[1] shouldEqual Socks5Handshake.Response.SUCCEEDED.code
                    responseBytes[2] shouldEqual 0.toByte()
                    responseBytes[3] shouldEqual addressType.code

                    val size = responseBytes.size
                    val addressBytes = responseBytes.slice(4 until size-2)
                    addressBytes shouldEqual address.address.toList()

                    val portBytes = responseBytes.slice(size-2 until size)
                    portBytes shouldBe port.toPortBytes().toList()
                }
            }
        }

        "when not connected" - {
            "write reject response" {
                forAll(socketTable) { address: InetAddress, addressType: AddressType, port: Int ->
                    val byteChannel = ByteChannelMock(listOf())
                    val socks5Handshake = Socks5Handshake(byteChannel)

                    val requestData = Socks5RequestData(
                            address = address,
                            port = port,
                            command = Command.CONNECT
                    )

                    socks5Handshake.writeResponse(false, requestData)

                    val responseBytes = byteChannel.outBuffers.first().array()

                    responseBytes[0] shouldEqual 0x05.toByte()
                    responseBytes[1] shouldEqual Socks5Handshake.Response.FAILURE.code
                    responseBytes[2] shouldEqual 0.toByte()
                    responseBytes[3] shouldEqual addressType.code

                    val size = responseBytes.size
                    val addressBytes = responseBytes.slice(4 until size-2)
                    addressBytes shouldEqual address.address.toList()

                    val portBytes = responseBytes.slice(size-2 until size)
                    portBytes shouldBe port.toPortBytes().toList()
                }
            }
        }
    }
})
