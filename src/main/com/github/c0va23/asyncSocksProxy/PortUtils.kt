package com.github.c0va23.asyncSocksProxy

val maxPort = 0xFFFF

class InvalidPort(port: Int) : Exception("Invalid port $port")

fun Int.toPortBytes() : ByteArray {
    if(this > maxPort) throw InvalidPort(this)
    return byteArrayOf(
            (this / 256).toByte(),
            (this % 256).toByte()
    )
}

private fun byteToPort(byte: Byte) : Int =
    if(byte >= 0) byte.toInt()
    else 256 + byte

fun Int.Companion.fromPortBytes(bytes: ByteArray) : Int =
    byteToPort(bytes[0]) * 256 + byteToPort(bytes[1])
