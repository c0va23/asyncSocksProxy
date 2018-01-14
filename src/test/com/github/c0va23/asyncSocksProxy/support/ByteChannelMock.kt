package com.github.c0va23.asyncSocksProxy.support

import java.nio.ByteBuffer
import java.nio.channels.ByteChannel

class ByteChannelMock(
        inputBuffers: Iterable<ByteArray>
) : ByteChannel {
    private val bufferIterator = inputBuffers.iterator()

    val outBuffers = mutableListOf<ByteBuffer>()

    override fun isOpen(): Boolean = true

    override fun close() {}

    override fun read(byteBuffer: ByteBuffer?): Int {
        if (null == byteBuffer) throw NullPointerException()
        val inputBuffer = bufferIterator.next()
        return byteBuffer.put(inputBuffer).limit()
    }

    override fun write(byteBuffer: ByteBuffer?): Int {
        if (null == byteBuffer) throw NullPointerException()
        val outBuffer = ByteBuffer.allocate(byteBuffer.limit())
        outBuffers.add(outBuffer)
        return outBuffer.put(byteBuffer).limit()
    }
}
