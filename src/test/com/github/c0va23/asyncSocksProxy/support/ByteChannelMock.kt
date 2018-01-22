package com.github.c0va23.asyncSocksProxy.support

import java.nio.ByteBuffer
import java.nio.channels.ByteChannel

class ByteChannelMock(
        inputBuffers: Iterable<ByteArray>
) : ByteChannel {
    private val bufferIterator = inputBuffers.iterator()

    val outBuffers = mutableListOf<ByteArray>()

    override fun isOpen(): Boolean = true

    override fun close() {}

    override fun read(byteBuffer: ByteBuffer?): Int {
        if (null == byteBuffer) throw NullPointerException()
        val inputBuffer = bufferIterator.next()
        return byteBuffer.put(inputBuffer).limit()
    }

    override fun write(byteBuffer: ByteBuffer?): Int {
        if (null == byteBuffer) throw NullPointerException()
        val outBuffer = ByteArray(byteBuffer.limit())
        outBuffers.add(outBuffer)
        byteBuffer.get(outBuffer)
        return outBuffer.size
    }
}
