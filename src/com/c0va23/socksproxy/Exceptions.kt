package com.c0va23.socksproxy

abstract class SocksException(message: String) : Exception(message)
class UnknownVersion(version: Byte) :
        SocksException("Unknown version $version")

class UnknownCommand(code: Byte) :
        SocksException("Unknown command code $code")

class UnknownMethod(code: Short) :
        SocksException("Unknown method $code")

class UnimplementedCommand(command: Command) :
        SocksException("Unknown command $command")

class UnimplementedAddressType(addressType: Byte) :
        SocksException("Unimplemented address type $addressType")
