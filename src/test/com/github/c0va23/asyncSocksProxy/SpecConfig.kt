package com.github.c0va23.asyncSocksProxy

import io.kotlintest.ProjectConfig
import java.io.FileInputStream
import java.util.logging.LogManager

object SpecConfig : ProjectConfig() {
    override fun beforeAll() {
        super.beforeAll()

        val inputStream = FileInputStream("src/test/logging.properties")
        LogManager.getLogManager().readConfiguration(inputStream)
    }
}
