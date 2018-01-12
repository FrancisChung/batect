/*
   Copyright 2017 Charles Korn.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package batect.cli.commands

import batect.TaskRunner
import batect.config.Configuration
import batect.config.Task
import batect.config.io.ConfigurationLoader
import batect.logging.Logger
import batect.model.RunOptions
import batect.model.TaskExecutionOrderResolutionException
import batect.model.TaskExecutionOrderResolver
import batect.ui.Console
import batect.ui.ConsoleColor
import batect.updates.UpdateNotifier

class RunTaskCommand(
    val configFile: String,
    val runOptions: RunOptions,
    val configLoader: ConfigurationLoader,
    val taskExecutionOrderResolver: TaskExecutionOrderResolver,
    val taskRunner: TaskRunner,
    val updateNotifier: UpdateNotifier,
    val console: Console,
    val errorConsole: Console,
    val logger: Logger) : Command {

    override fun run(): Int {
        val config = configLoader.loadConfig(configFile)

        try {
            val tasks = taskExecutionOrderResolver.resolveExecutionOrder(config, runOptions.taskName)

            updateNotifier.run()

            return runTasks(config, tasks)

        } catch (e: TaskExecutionOrderResolutionException) {
            logger.error {
                message("Could not resolve task execution order.")
                exception(e)
            }

            errorConsole.withColor(ConsoleColor.Red) {
                println(e.message ?: "")
            }

            return -1
        }
    }

    private fun runTasks(config: Configuration, tasks: List<Task>): Int {
        for (task in tasks) {
            val exitCode = taskRunner.run(config, task, runOptions)

            if (exitCode != 0) {
                return exitCode
            }

            if (task != tasks.last()) {
                console.println()
            }
        }

        return 0
    }
}
