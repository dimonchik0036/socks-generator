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

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.escapeHTML
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import kotlin.collections.set

typealias KeysMap = ConcurrentSkipListMap<String, String>

class Application(
    private val applicationConfig: Config.ApplicationConfig,
    private val socksConfig: Config.SocksConfig
) {
    private val logger = LoggerFactory.getLogger("application")
    private val keys = loadMap(applicationConfig.keysPath)
    private val users = loadMap(applicationConfig.usersPath)

    fun start() {
        val env = applicationEngineEnvironment {
            log = logger
            module {
                routing {
                    registerHandler("/*") {
                        ERROR_PAGE
                    }
                    registerHandler("generate") {
                        handleGeneration()
                    }
                    registerHandler("stats") {
                        handleStatus()
                    }
                    registerHandler("remove") {
                        handleRemove()
                    }
                    registerHandler("auth") {
                        handleAuth()
                    }
                }
            }

            connector {
                host = applicationConfig.host
                port = applicationConfig.port
            }
        }
        embeddedServer(Netty, env).start(wait = true)
    }

    private fun Routing.registerHandler(path: String, handler: ApplicationCall.() -> String) {
        get(path) {
            with(call) {
                respondText(handler(this), ContentType.Text.Html)
            }
        }
    }

    private fun ApplicationCall.handleGeneration(): String {
        val key = getSecretKey() ?: return SECRET_KEY_NEED_ENTER
        if (key != applicationConfig.secretKey) return SECRET_KEY_INVALID

        val uuid = UUID.randomUUID().toString()
        val comment = parameters["comment"] ?: ""
        keys[uuid] = comment

        launch { saveKeys() }
        logger.info("New key `$key` with `$comment`")
        return uuid
    }

    private fun ApplicationCall.handleStatus(): String {
        val key = getSecretKey() ?: return SECRET_KEY_NEED_ENTER
        if (key != applicationConfig.secretKey) return SECRET_KEY_INVALID

        val br = "<br/>"
        val keys = keys.entries.joinToString(
            separator = br,
            transform = { "${it.key} : ${it.value}" }
        )
        val users = users.entries.joinToString(
            separator = br,
            transform = { "${it.key} : ${it.value.substringBefore(':')}" }
        )
        logger.info("Get stats")
        return "Unused keys:$br $keys$br$br" +
                "Users:$br $users"
    }

    private fun ApplicationCall.handleRemove(): String {
        val key = getSecretKey() ?: return SECRET_KEY_NEED_ENTER
        return keys.remove(key)?.let {
            launch { saveKeys() }
            logger.info("Remove key `$key` with `$it`")
            SECRET_KEY_REMOVED
        } ?: SECRET_KEY_NOT_FOUND
    }

    private fun ApplicationCall.handleAuth(): String {
        val key = getSecretKey() ?: return SECRET_KEY_NEED_ENTER
        val login = parameters["login"] ?: return "Enter login"
        val password = parameters["password"] ?: return "Enter password"
        val value = keys.remove(key)
            ?: return SECRET_KEY_INVALID.let { logger.info("Failed login attempt: $key:$login:$password"); it }

        val result = auth(login, password)
        if (result.first) {
            users[key] = "$login:$password"
            launch {
                saveUsers()
                saveKeys()
            }
            logger.info("New user: $key:$login:$password")
        } else {
            keys[key] = value
            logger.error("Crash when creating: $key:$login:$password")
        }

        return result.second
    }

    private fun auth(login: String, password: String): Pair<Boolean, String> {
        val regex = Regex("^[a-zA-Z0-9\\-_]+$")
        if (!login.matches(regex)) return Pair(false, "Invalid login ${login.escapeHTML()}, allowed ${regex.pattern}")
        if (!password.matches(regex)) return Pair(
            false,
            "Invalid password ${password.escapeHTML()}, allowed ${regex.pattern}"
        )

        val process = Runtime.getRuntime().exec("${applicationConfig.scriptPath} $login $password")
            ?: return Pair(false, "Couldn't exec process")
        val exitCode = process.waitFor()
        if (exitCode != 0) return Pair(false, "Couldn't create account")

        return Pair(
            true,
            "<a href=\"tg://socks?server=${socksConfig.address}&port=${socksConfig.port}&user=$login&pass=$password\">Click me!</a>"
        )
    }

    companion object {
        const val ERROR_PAGE =
            "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/7OBx-YwPl8g?autoplay=1\" frameborder=\"0\" allowfullscreen></iframe>"
        const val SECRET_KEY_NEED_ENTER = "Enter the secret key"
        const val SECRET_KEY_INVALID =
            ERROR_PAGE // "Invalid secret key"
        const val SECRET_KEY_NOT_FOUND = "Secret key not found"
        const val SECRET_KEY_REMOVED = "Secret key removed"
        private const val SECRET_KEY_PARAM_NAME = "key"

        private fun ApplicationCall.getSecretKey(): String? = this.parameters[SECRET_KEY_PARAM_NAME]
    }

    private fun saveKeys() {
        keys.saveMap(applicationConfig.keysPath)
    }

    private fun saveUsers() {
        users.saveMap(applicationConfig.usersPath)
    }

    private fun KeysMap.saveMap(filePath: String) {
        try {
            val file = File(filePath)
            file.writeText(this.entries.joinToString(separator = "\n") { "${it.key}:${it.value}" })
        } catch (e: Throwable) {
            logger.error("Couldn't save map: ${e.message}")
        }
    }

    private fun loadMap(keysPath: String): KeysMap {
        try {
            val file = File(keysPath)
            return KeysMap(file.readLines().asSequence().associate {
                it.substringBefore(':') to it.substringAfter(':')
            })
        } catch (e: Throwable) {
            logger.error("Couldn't load keys: ${e.message}")
        }

        return KeysMap()
    }
}

