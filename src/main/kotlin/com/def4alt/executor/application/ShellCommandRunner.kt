package com.def4alt.executor.application

interface ShellCommandRunner {
    fun run(script: String): ShellCommandResult
}

data class ShellCommandResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
)
