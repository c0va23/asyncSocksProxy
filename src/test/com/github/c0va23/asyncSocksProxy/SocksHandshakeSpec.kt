package com.github.c0va23.asyncSocksProxy

import com.github.c0va23.asyncSocksProxy.support.ByteChannelMock
import io.kotlintest.matchers.shouldBe
import io.kotlintest.mock.mock
import io.kotlintest.mock.`when`
import io.kotlintest.specs.FreeSpec
import java.nio.channels.spi.AbstractSelectableChannel

class SocksHandshakeSpec : FreeSpec({
    "handshake()" - {
        "when SocksHandshake constructed without socksHandshakes" - {
            val connector = mock<Connector>()
            val socksHandshake = SocksHandshake(connector)

            "throw UnknownVersion" {
                val byteChannel = ByteChannelMock(listOf(byteArrayOf(0x04)))

                socksHandshake.handshake(byteChannel) shouldBe null
            }
        }

        "when socksHandshake construct with two socksHandshake" - {
            val firstSocksHandshake = mock<SocksHandshakeInterface>()
            `when`(firstSocksHandshake.version).thenReturn(0x10)

            val secondSocksHandshake = mock<SocksHandshakeInterface>()
            `when`(secondSocksHandshake.version).thenReturn(0x11)

            val requestData = mock<RequestData>()
            val resultChannel = mock<AbstractSelectableChannel>()

            val connector = mock<Connector>()
            `when`(connector.connect(requestData)).thenReturn(resultChannel)
            val socksHandshake = SocksHandshake(connector, firstSocksHandshake, secondSocksHandshake)

            "when byte channel have request from first socks handshake" - {
                val inputChannel = ByteChannelMock(listOf(byteArrayOf(0x10)))

                "then perform first handshake" {
                    `when`(firstSocksHandshake.parseRequest(inputChannel)).thenReturn(requestData)
                    `when`(firstSocksHandshake.writeResponse(inputChannel, true, requestData))
                            .then {}

                    socksHandshake.handshake(inputChannel) shouldBe resultChannel
                }
            }

            "when byte channel have request from first socks handshake" - {
                val inputChannel = ByteChannelMock(listOf(byteArrayOf(0x11)))

                "then perform first handshake" {
                    `when`(secondSocksHandshake.parseRequest(inputChannel)).thenReturn(requestData)
                    `when`(secondSocksHandshake.writeResponse(inputChannel, true, requestData))
                            .then {}

                    socksHandshake.handshake(inputChannel) shouldBe resultChannel
                }
            }
        }

        "when socksHandshake construct with one socksHandshake" - {
            val inputChannel = ByteChannelMock(listOf(byteArrayOf(0x10)))

            val resultChannel = mock<AbstractSelectableChannel>()

            "when parseRequest is success" - {
                val someSocksHandshake = mock<SocksHandshakeInterface>()
                `when`(someSocksHandshake.version).thenReturn(0x10)

                val requestData = mock<RequestData>()
                `when`(someSocksHandshake.parseRequest(inputChannel)).thenReturn(requestData)


                "when connect success" - {
                    `when`(someSocksHandshake.writeResponse(inputChannel, true, requestData))
                            .then {}

                    val connector = mock<Connector>()
                    `when`(connector.connect(requestData)).thenReturn(resultChannel)

                    val socksHandshake = SocksHandshake(connector, someSocksHandshake)

                    "then return new connection" {
                        socksHandshake.handshake(inputChannel) shouldBe resultChannel
                    }
                }

                "when connect failure" - {
                    `when`(someSocksHandshake.writeResponse(inputChannel, false, requestData))
                            .then {}

                    val connector = mock<Connector>()
                    `when`(connector.connect(requestData)).thenReturn(null)

                    val socksHandshake = SocksHandshake(connector, someSocksHandshake)

                    "then return null" {
                        socksHandshake.handshake(inputChannel) shouldBe null
                    }
                }

                "when connect throw UnimplementedCommand" - {
                    val connector = mock<Connector>()
                    `when`(connector.connect(requestData)).thenThrow(UnimplementedCommand(Command.BINDING))

                    val socksHandshake = SocksHandshake(connector, someSocksHandshake)

                    "then return null" {
                        socksHandshake.handshake(inputChannel) shouldBe null
                    }
                }
            }

            "when parseRequest throw SocketException" - {
                val someSocksHandshake = mock<SocksHandshakeInterface>()
                `when`(someSocksHandshake.version).thenReturn(0x10)

                val socksException = mock<SocksException>()
                `when`(someSocksHandshake.parseRequest(inputChannel)).thenThrow(socksException)

                val connector = mock<Connector>()
                val socksHandshake = SocksHandshake(connector, someSocksHandshake)

                "return null" {
                    socksHandshake.handshake(inputChannel) shouldBe null
                }
            }
        }
    }
})
