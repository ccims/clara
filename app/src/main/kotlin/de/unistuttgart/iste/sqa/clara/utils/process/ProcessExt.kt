package de.unistuttgart.iste.sqa.clara.utils.process

import arrow.core.*
import java.io.BufferedWriter
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

data class ProcessExecutionError(val description: String)

fun ProcessBuilder.startChecked(): Either<ProcessExecutionError, Process> {
    return try {
        this.start().right()
    } catch (ex: IOException) {
        ProcessExecutionError("Cannot execute '${this.command().first()}'! Check if the program is installed correctly!").left()
    } catch (ex: SecurityException) {
        ProcessExecutionError("Cannot execute '${this.command().first()}'! Check if the program has the correct permissions!").left()
    }
}

fun Either<ProcessExecutionError, Process>.readError(timeout: Duration): Either<ProcessExecutionError, String> {
    return this.flatMap { it.readError(timeout) }
}

fun Process.readError(timeout: Duration): Either<ProcessExecutionError, String> {
    if (this.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS).not()) {
        return ProcessExecutionError("Timout of $timeout expired").left()
    }

    if (this.exitValue() != 0) {
        return ProcessExecutionError("Process did not exit successfully. Exit code ${this.exitValue()}. Error: ${this.errorReader().readText()}").left()
    }

    this.errorReader()
        .use {
            return it.readText().right()
        }
}

fun Either<ProcessExecutionError, Process>.writeOutput(timeout: Duration, writeFunc: BufferedWriter.() -> Unit): Option<ProcessExecutionError> {
    return this.map { it.writeOutput(timeout, writeFunc) }.getOrNone().flatten()
}

fun Process.writeOutput(timeout: Duration, writeFunc: BufferedWriter.() -> Unit): Option<ProcessExecutionError> {
    this.outputWriter()
        .use {
            it.run(writeFunc).right()
        }

    if (this.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS).not()) {
        return ProcessExecutionError("Timout of $timeout expired").some()
    }

    if (this.exitValue() != 0) {
        return ProcessExecutionError("Process did not exit successfully. Exit code ${this.exitValue()}. Error: ${this.errorReader().readText()}").some()
    }

    return None
}
