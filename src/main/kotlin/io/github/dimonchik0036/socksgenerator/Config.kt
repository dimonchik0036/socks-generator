/*
 * Copyright 2018 Dmitry Gridin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.dimonchik0036.socksgenerator

import java.io.FileInputStream
import java.io.IOException
import java.util.*

class Config(configPath: String) {
    private val properties = Properties()

    init {
        try {
            properties.load(FileInputStream(configPath))
        } catch (e: IOException) {
            error("Couldn't read properties: ${e.message}")
        }
    }

    data class SocksConfig(
        val port: Int,
        val address: String
    )

    data class ApplicationConfig(
        val port: Int,
        val host: String,
        val secretKey: String,
        val keysPath: String,
        val scriptPath: String,
        val usersPath: String
    )

    fun loadSocks(): SocksConfig =
        with(this) {
            return SocksConfig(
                port = readInt("socks_port"),
                address = readValue("socks_address")
            )
        }

    fun loadApplication(): ApplicationConfig =
        with(this) {
            return ApplicationConfig(
                port = readInt("port"),
                host = readValue("host"),
                secretKey = readValue("secret_key"),
                keysPath = readValue("keys_path"),
                scriptPath = readValue("script_path"),
                usersPath = readValue("users_path")
            )
        }

    private fun readValue(key: String): String = properties.getProperty(key) ?: error("Couldn't find `$key`")

    private fun readInt(key: String): Int = try {
        this.readValue(key).toInt()
    } catch (e: NumberFormatException) {
        error("Couldn't parse `$key`")
    }
}


