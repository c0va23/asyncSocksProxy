# Async Socks Proxy for JVM

Implement Socks 4/5 tunnel for JVM.

[![Release](https://jitpack.io/v/c0va23/asyncSocksProxy.svg)](https://jitpack.io/#c0va23/asyncSocksProxy)
[![Build Status](https://travis-ci.org/c0va23/asyncSocksProxy.svg?branch=master)](https://travis-ci.org/c0va23/asyncSocksProxy)

# Socks 4

Socks 4 implemented partly.

## Command

| Command  | Implemented   |
|----------|---------------|
| CONNECT  | Yes           |
| BIND     | No            |

# Socks 5

Socks 5 implement partly.

## Authentication methods

| Method                      | Implemented   |
|-----------------------------|---------------|
| NO AUTHENTICATION REQUIRED  | Yes           |
| GSSAPI                      | No            |
| USERNAME/PASSWORD           | No            |

## Command

| Command       | Implemented |
|---------------|-------------|
| CONNECT       | Yes         |
| BIND          | No          |
| UDP ASSOCIATE | No          |

## Address type

| Type       | Implemented  |
|------------|--------------|
| IP V4      | Yes          |
| DOMAINNAME | No           |
| IP V6      | Yes          |
