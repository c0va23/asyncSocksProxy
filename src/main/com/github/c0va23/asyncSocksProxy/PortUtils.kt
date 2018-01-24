package com.github.c0va23.asyncSocksProxy

internal const val MAX_PORT = 0xFFFF

internal class InvalidPort(port: Int) : Exception("Invalid port $port")

internal fun Int.toPortBytes() : ByteArray {
    if(this > MAX_PORT) throw InvalidPort(this)
    return byteArrayOf(
            (this / 256).toByte(),
            (this % 256).toByte()
    )
}

private fun byteToPort(byte: Byte) : Int =
    if(byte >= 0) byte.toInt()
    else 256 + byte

internal fun Int.Companion.fromPortBytes(bytes: ByteArray) : Int =
    byteToPort(bytes[0]) * 256 + byteToPort(bytes[1])
