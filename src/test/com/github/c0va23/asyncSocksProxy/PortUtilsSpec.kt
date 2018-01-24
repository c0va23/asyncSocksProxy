package com.github.c0va23.asyncSocksProxy

import io.kotlintest.matchers.shouldEqual
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.properties.forAll
import io.kotlintest.properties.headers
import io.kotlintest.properties.row
import io.kotlintest.properties.table
import io.kotlintest.specs.FreeSpec

class PortUtilsSpec : FreeSpec({
    "Int.toPortBytes()" - {
        "return valid bytes for valid int-s" {
            forAll(table(
                    headers("port", "first byte", "last byte"),
                    row(0, 0, 0),
                    row(80, 0, 80),
                    row(255, 0, 0xFF),
                    row(256, 0x01, 0x00),
                    row(443, 0x01, 0xBB),
                    row(0xFFFF, 0xFF, 0xFF)
            )) { port: Int, firstByte: Int, secondByte: Int ->
                val bytes = port.toPortBytes()
                bytes[0] shouldEqual firstByte.toByte()
                bytes[1] shouldEqual secondByte.toByte()
            }
        }

        "throw error for invalid port" {
            forAll(table(
                    headers("port"),
                    row(0x10000),
                    row(0x7F_FF_FF)
            )) { port: Int ->
                shouldThrow<InvalidPort> { port.toPortBytes() }
            }
        }
    }

    "Int.companion.fromPortBytes()" - {
        "return valid port from low bytes" {
            val port = Int.fromPortBytes(byteArrayOf(0, 80))
            port shouldEqual 80
        }

        "return valid port from high bytes" {
            val bytes = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
            val port = Int.fromPortBytes(bytes)
            port shouldEqual MAX_PORT
        }
    }
})
