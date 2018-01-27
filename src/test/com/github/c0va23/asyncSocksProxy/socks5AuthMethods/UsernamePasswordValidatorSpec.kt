package com.github.c0va23.asyncSocksProxy.socks5AuthMethods

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec

class SimpleUsernamePasswordValidatorSpec : FreeSpec({
    val validator = SimpleUsernamePasswordValidator(username = "foo", password = "bar")

    "when pass valid username and password" - {
        validator.validate("foo", "bar") shouldBe true
    }

    "when pass valid username, but invalid password" - {
        validator.validate("foo", "invalid") shouldBe false
    }

    "when pass invalid username, but valid password" - {

        validator.validate("invalid", "bar") shouldBe false
    }
})

class TableUsernamePasswordValidatorSpec : FreeSpec({
    val validator = TableUsernamePasswordValidator(
            mapOf(
                    "user1" to "pass1",
                    "user2" to "pass2"
            )
    )

    "when pass one valid username and password pair" - {
        val result = validator.validate("user1", "pass1")

        "return true" {
            result shouldBe true
        }
    }

    "when pass other valid username and password pair" {
        val result = validator.validate("user1", "pass1")

        "return false" {
            result shouldBe true
        }
    }

    "when pass username from one pair and password from other pair" {
        val result = validator.validate("user1", "pass2")

        "return false" {
            result shouldBe false
        }
    }

    "when pass invalid username and password" {
        val result = validator.validate("user3", "pass3")

        "return false" {
            result shouldBe false
        }
    }
})
