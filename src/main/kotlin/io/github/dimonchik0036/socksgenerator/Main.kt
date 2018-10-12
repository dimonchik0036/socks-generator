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

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        if (args.size != 1) error("The first argument must be the path to the configuration.")
        val config = Config(args.first())

        val socksConfig = config.loadSocks()
        val applicationConfig = config.loadApplication()
        Application(applicationConfig, socksConfig).start()
    } catch (e: IllegalStateException) {
        System.err.println(e.message)
        exitProcess(1)
    }
}
