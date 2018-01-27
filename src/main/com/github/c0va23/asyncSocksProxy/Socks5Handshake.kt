package com.github.c0va23.asyncSocksProxy

import com.github.c0va23.asyncSocksProxy.socks5AuthMethods.AuthMethodInterface
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.charset.Charset
import java.util.logging.Logger

class Socks5Handshake(
        allowedMethods: Iterable<AuthMethodInterface>
) : SocksHandshakeInterface {
    private val nullByte: Byte = 0x00

    private val bufferSize = 128
    private val buffer = ByteBuffer.allocate(bufferSize)
    private val logger = Logger.getLogger(javaClass.name)

    private val ipv4Size = 4
    private val ipv6Size = 16

    private val allowedMethodMap = allowedMethods.associateBy { it.code }

    enum class Response(val code: Byte) {
        SUCCEEDED(0x00),
        FAILURE(0x01);
    }

    enum class AddressType(val code: Byte) {
        Ipv4(0x01),
        DomainName(0x03),
        Ipv6(0x04),
    }

    companion object {
        const val NO_ACCEPTABLE_METHODS = 0xFF.toByte()
    }

    override val version: Byte = 0x05

    @Throws(SocksException::class)
    override fun parseRequest(byteChannel: ByteChannel): RequestData {
        try {
            val methods = parseMethods(byteChannel)

            val selectedMethod = methods.mapNotNull(allowedMethodMap::get).elementAtOrNull(0)

            writeSelectedMethod(byteChannel, selectedMethod?.code ?: NO_ACCEPTABLE_METHODS)

            selectedMethod ?: throw NoAcceptableMethods(methods.map { it.toString() })

            selectedMethod.authenticate(byteChannel)

            return parseCommand(byteChannel)
        } finally {
            buffer.clear()
        }
    }

    private fun parseMethods(byteChannel: ByteChannel): List<Byte> {
        try {
            val readBytes = byteChannel.read(buffer)
            logger.fine("Read $readBytes bytes")
            buffer.flip()

            val numberMethods = buffer.get()
            logger.fine("$numberMethods methods")

            return (0 until numberMethods)
                    .map { buffer.get() }
        } finally {
            buffer.clear()
        }
    }

    private fun writeSelectedMethod(byteChannel: ByteChannel, methodCode: Byte) {
        try {
            buffer.put(version)
            buffer.put(methodCode)
            buffer.flip()

            byteChannel.write(buffer)
        } finally {
            buffer.clear()
        }
    }

    private fun parseCommand(byteChannel: ByteChannel): RequestData {
        try {
            val readBytes = byteChannel.read(buffer)
            logger.fine("Read $readBytes bytes")
            buffer.flip()

            val version = buffer.get()
            if (version != this.version) throw UnknownVersion(version)
            logger.fine("Version $version")

            val command = buffer.get()

            buffer.get() // Skip reserved

            val addressType = buffer.get()
            val address = when (addressType) {
                AddressType.Ipv4.code -> getIpAddress(buffer, ipv4Size)
                AddressType.Ipv6.code -> getIpAddress(buffer, ipv6Size)
                AddressType.DomainName.code -> getDomainName(buffer)
                else -> throw UnimplementedAddressType(addressType)
            }

            val portBytes = ByteArray(2)
            buffer.get(portBytes)
            val port = Int.fromPortBytes(portBytes)
            logger.info("Address $address:$port")

            return Socks5RequestData(
                    address = address,
                    port = port,
                    command = Command.fromByte(command)
            )

        } finally {
            buffer.clear()
        }
    }

    private fun getIpAddress(buffer: ByteBuffer, addressSize: Int): InetAddress {
        val addressBytes = ByteArray(addressSize)
        buffer.get(addressBytes)
        return InetAddress.getByAddress(addressBytes)
    }

    private fun getDomainName(buffer: ByteBuffer): InetAddress {
        val length = buffer.get().toInt()
        val domainNameBuffer = ByteArray(length)
        val charset = Charset.forName("ASCII")
        buffer.get(domainNameBuffer)
        val domainName = charset.decode(ByteBuffer.wrap(domainNameBuffer)).toString()
        return InetAddress.getByName(domainName)
    }

    override fun writeResponse(
            byteChannel: ByteChannel,
            connected: Boolean,
            requestData: RequestData
    ) {
        try {
            buffer.put(version)
            buffer.put(
                    if (connected) Response.SUCCEEDED.code
                    else Response.FAILURE.code
            )
            buffer.put(nullByte) // Reserved
            val addressType = when (requestData.address) {
                is Inet4Address -> AddressType.Ipv4.code
                is Inet6Address -> AddressType.Ipv6.code
                else -> throw Exception("Unreachable branch")
            }
            buffer.put(addressType) // Address type
            buffer.put(requestData.address.address)
            buffer.putShort(requestData.port.toShort())
            buffer.flip()

            val writeBytes = byteChannel.write(buffer)
            logger.fine("Write $writeBytes bytes")

            if(connected) logger.info("Request successful")
            else logger.warning("Request failed")

            if (buffer.hasRemaining())
                logger.warning("Remain ${buffer.remaining()} bytes")
        } finally {
            buffer.clear()
        }
    }
}
