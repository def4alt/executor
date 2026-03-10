package com.def4alt.executor.application

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
class ProcessShellCommandRunner : ShellCommandRunner {
    override fun run(script: String): ShellCommandResult {
        val process = ProcessBuilder("sh", "-lc", script)
            .start()

        val stdout = process.inputStream.readAllBytes().toString(StandardCharsets.UTF_8)
        val stderr = process.errorStream.readAllBytes().toString(StandardCharsets.UTF_8)
        val exitCode = process.waitFor()

        return ShellCommandResult(
            stdout = stdout,
            stderr = stderr,
            exitCode = exitCode,
        )
    }
}
