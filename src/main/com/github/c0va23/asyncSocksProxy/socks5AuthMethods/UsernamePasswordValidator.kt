package com.github.c0va23.asyncSocksProxy.socks5AuthMethods

interface UsernamePasswordValidator {
    fun validate(username: String, password: String): Boolean
}

class SimpleUsernamePasswordValidator(
        private val username: String,
        private val password: String
) : UsernamePasswordValidator {
    override fun validate(username: String, password: String): Boolean =
        username == this.username && password == this.password
}

typealias Username = String
typealias Password = String

class TableUsernamePasswordValidator(
        private val usersTable: Map<Username, Password>
) : UsernamePasswordValidator {
    override fun validate(username: String, password: String) =
        usersTable[username]?.equals(password) ?: false
}
