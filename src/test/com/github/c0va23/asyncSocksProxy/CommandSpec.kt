package com.github.c0va23.asyncSocksProxy

import io.kotlintest.matchers.shouldEqual
import io.kotlintest.properties.forAll
import io.kotlintest.properties.headers
import io.kotlintest.properties.row
import io.kotlintest.properties.table
import io.kotlintest.specs.FreeSpec

class CommandSpec : FreeSpec({
    "companion.fromByte()" - {
        "return valid command" {
            val table = table(
                    headers("Command"),
                    row(Command.CONNECT),
                    row(Command.BINDING),
                    row(Command.UDP_ASSOCIATE)
            )

            forAll(table) { command: Command ->
                Command.fromByte(command.code) shouldEqual command
            }
        }
    }
})
