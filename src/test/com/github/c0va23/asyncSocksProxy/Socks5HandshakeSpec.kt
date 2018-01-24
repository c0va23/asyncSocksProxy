package com.github.c0va23.asyncSocksProxy

import io.kotlintest.specs.FreeSpec

import com.github.c0va23.asyncSocksProxy.Socks5Handshake.AddressType
import com.github.c0va23.asyncSocksProxy.socks5AuthMethods.AuthMethodInterface
import com.github.c0va23.asyncSocksProxy.support.ByteChannelMock
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldEqual
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.mock.mock
import io.kotlintest.mock.`when`
import io.kotlintest.properties.forAll
import io.kotlintest.properties.headers
import io.kotlintest.properties.row
import io.kotlintest.properties.table
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

class Socks5HandshakeSpec : FreeSpec() {
    private fun buildAuthRequestPayload(vararg methods: Byte) =
            byteArrayOf(
                    methods.size.toByte(),
                    *methods
            )

    private fun buildCommandRequestPayload(
            command: Command,
            addressType: AddressType,
            addressHost: String,
            port: Int
    ): ByteArray {
        val addressPayload = when (addressType) {
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

    override val oneInstancePerTest = true

    init {
        "parseRequest()" - {
            val allowedMethod = mock<AuthMethodInterface>()
            val allowedMethodCode: Byte = 10
            `when`(allowedMethod.code).thenReturn(allowedMethodCode)

            val socks5Handshake = Socks5Handshake(listOf(allowedMethod))

            "when request with allowed method" - {
                val authRequestPayload = buildAuthRequestPayload(allowedMethodCode)

                "then write allowed method" {
                    val commandRequestPayload = buildCommandRequestPayload(
                            Command.CONNECT, AddressType.Ipv4, "127.0.0.1", 80)
                    val byteChannel = ByteChannelMock(listOf(authRequestPayload, commandRequestPayload))

                    socks5Handshake.parseRequest(byteChannel)

                    val methodResponsePayload = byteChannel.outBuffers[0]
                    methodResponsePayload[0] shouldEqual 0x05.toByte()
                    methodResponsePayload[1] shouldEqual allowedMethodCode
                }

                "then return valid Socks5RequestData" {
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

                        socks5Handshake.parseRequest(byteChannel) shouldBe Socks5RequestData(
                                command = command,
                                address = address,
                                port = port
                        )
                    }
                }
            }

            "when request with not allowed method" - {
                "write only NO_ACCEPTABLE_METHODS" {
                    val authRequestPayload = buildAuthRequestPayload(20)
                    val byteChannel = ByteChannelMock(listOf(authRequestPayload))

                    shouldThrow<NoAcceptableMethods> { socks5Handshake.parseRequest(byteChannel) }

                    byteChannel.outBuffers.size shouldEqual 1

                    val methodResponsePayload = byteChannel.outBuffers[0]
                    methodResponsePayload[0] shouldEqual 0x05.toByte()
                    methodResponsePayload[1] shouldEqual 0xFF.toByte()
                }
            }
        }

        "writeResponse()" - {
            val socketTable = table(
                    headers("IP", "Address type", "Port"),
                    row(Inet4Address.getByName("127.0.0.1"), AddressType.Ipv4, 80),
                    row(Inet6Address.getByName("::1"), AddressType.Ipv6, 1080)
            )
            val socks5Handshake = Socks5Handshake(listOf())

            "when connected" - {
                "write success response" {
                    forAll(socketTable) { address: InetAddress, addressType: AddressType, port: Int ->
                        val requestData = Socks5RequestData(
                                address = address,
                                port = port,
                                command = Command.CONNECT
                        )

                        val byteChannel = ByteChannelMock(listOf())
                        socks5Handshake.writeResponse(byteChannel, true, requestData)

                        val responseBytes = byteChannel.outBuffers.first()

                        responseBytes[0] shouldEqual 0x05.toByte()
                        responseBytes[1] shouldEqual Socks5Handshake.Response.SUCCEEDED.code
                        responseBytes[2] shouldEqual 0.toByte()
                        responseBytes[3] shouldEqual addressType.code

                        val size = responseBytes.size
                        val addressBytes = responseBytes.slice(4 until size - 2)
                        addressBytes shouldEqual address.address.toList()

                        val portBytes = responseBytes.slice(size - 2 until size)
                        portBytes shouldBe port.toPortBytes().toList()
                    }
                }
            }

            "when not connected" - {
                "write reject response" {
                    forAll(socketTable) { address: InetAddress, addressType: AddressType, port: Int ->
                        val requestData = Socks5RequestData(
                                address = address,
                                port = port,
                                command = Command.CONNECT
                        )

                        val byteChannel = ByteChannelMock(listOf())
                        socks5Handshake.writeResponse(byteChannel, false, requestData)

                        val responseBytes = byteChannel.outBuffers.first()

                        responseBytes[0] shouldEqual 0x05.toByte()
                        responseBytes[1] shouldEqual Socks5Handshake.Response.FAILURE.code
                        responseBytes[2] shouldEqual 0.toByte()
                        responseBytes[3] shouldEqual addressType.code

                        val size = responseBytes.size
                        val addressBytes = responseBytes.slice(4 until size - 2)
                        addressBytes shouldEqual address.address.toList()

                        val portBytes = responseBytes.slice(size - 2 until size)
                        portBytes shouldBe port.toPortBytes().toList()
                    }
                }
            }
        }
    }
}
