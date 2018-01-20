package com.github.c0va23.asyncSocksProxy

abstract class SocksException(message: String) : Exception(message)
class UnknownVersion(version: Byte) :
        SocksException("Unknown version $version")

class UnknownCommand(code: Byte) :
        SocksException("Unknown command code $code")

class UnimplementedCommand(command: Command) :
        SocksException("Unknown command $command")

class UnimplementedAddressType(addressType: Byte) :
        SocksException("Unimplemented address type $addressType")

class NoAcceptableMethods(methods: Iterable<String>) :
        Exception("No acceptable methods ${methods.joinToString(separator = ", ")}")
