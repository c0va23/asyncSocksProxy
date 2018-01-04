package com.c0va23.socksproxy

import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.logging.Logger

class Socks5Handshake(
        private val sourceChannel: SocketChannel
) : SocksHandshake.SocksHandshake {
    private val bufferSize = 128
    private val socksVersion: Byte = 0x05

    private val buffer = ByteBuffer.allocate(bufferSize)
    private val logger = Logger.getLogger(javaClass.name)

    private enum class Response(val code: Byte) {
        SUCCEEDED(0x00),
        FAILURE(0x01);
    }

    private enum class Method(val code: Short) {
        NO_AUTHENTICATION_REQUIRED(0x00),
        GSSAPI(0x01),
        USERNAME_PASSWORD( 0x02),
        NO_ACCEPTABLE_METHODS(0xFF);

        companion object {
            private val map =
                    Method.values().associateBy { it.code }

            fun fromValue(code: Short): Method =
                    map[code] ?: throw UnknownMethod(code)
        }
    }

    private val allowedMethods = arrayOf(Method.NO_AUTHENTICATION_REQUIRED)

    override fun parseRequest(): RequestData {
        try {
            val methods = parseMethods()

            val selectedMethod = methods.find { it in allowedMethods }
                    ?: Method.NO_ACCEPTABLE_METHODS

            writeSelectedMethod(selectedMethod)

            return parseCommand()
        }
        finally {
            buffer.clear()
        }
    }

    private fun parseMethods(): List<Method> {
        try {
            val readBytes = sourceChannel.read(buffer)
            logger.fine("Read $readBytes bytes")
            buffer.flip()

            val numberMethods = buffer.get()
            logger.fine("$numberMethods methods")

            return buffer
                    .slice()
                    .array()
                    .map { Method.fromValue(it.toShort()) }
        }
        finally {
            buffer.clear()
        }
    }

    private fun writeSelectedMethod(method: Method) {
        try {
            buffer.put(socksVersion)
            buffer.put(method.code.toByte())
            buffer.flip()

            sourceChannel.write(buffer)
        }
        finally {
            buffer.clear()
        }
    }

    private fun parseCommand(): RequestData {
        try {
            val readBytes = sourceChannel.read(buffer)
            logger.fine("Read $readBytes bytes")
            buffer.flip()

            val version = buffer.get()
            if(version != socksVersion) throw UnknownVersion(version)
            logger.fine("Version $version")

            val command = buffer.get()

            buffer.get() // Skip reserved

            val addressType = buffer.get()
            if(0x01 != addressType.toInt()) throw UnimplementedAddressType(addressType)

            val address = ByteArray(4)
            buffer.get(address)

            val port = buffer.short
            logger.info("Address ${address.joinToString(",")}:$port")

            return Socks5RequestData(
                    address = InetAddress.getByAddress(address),
                    port = port.toInt(),
                    command = Command.fromByte(command)
            )

        }
        finally {
            buffer.clear()
        }
    }

    override fun writeResponse(connected: Boolean, requestData: RequestData) {
        try {
            buffer.put(socksVersion)
            buffer.put(
                    if(connected) Response.SUCCEEDED.code
                    else Response.FAILURE.code
            )
            buffer.put(0x00) // Reserved
            buffer.put(0x01) // Address type
            buffer.put(requestData.address.address)
            buffer.putShort(requestData.port.toShort())
            buffer.flip()

            val writeBytes = sourceChannel.write(buffer)
            logger.fine("Write $writeBytes bytes")

            if(buffer.hasRemaining())
                logger.warning("Remain ${buffer.remaining()} bytes")
        }
        finally {
            buffer.clear()
        }
    }
}