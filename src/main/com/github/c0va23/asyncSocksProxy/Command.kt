package com.github.c0va23.asyncSocksProxy

enum class Command(
        private val code: Byte
) {
    CONNECT(0x01),
    BINDING(0x02),
    UDP_ASSOCIATE(0x03);

    companion object {
        private val map =
                Command.values().associateBy { it.code }

        fun fromByte(code: Byte) =
                map[code] ?: throw UnknownCommand(code)
    }
}
