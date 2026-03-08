package com.def4alt.executor.persistence

import com.def4alt.executor.application.SchedulingJobRepository
import com.def4alt.executor.domain.Job
import com.def4alt.executor.domain.JobStatus
import com.def4alt.executor.domain.ResourceSpec
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant

@Repository
class JdbcJobRepository(
    private val jdbcClient: JdbcClient,
) : JobRepository, SchedulingJobRepository {
    override fun create(job: Job) {
        insert(job)
    }

    override fun createOrReplace(job: Job) {
        jdbcClient.sql("delete from jobs where id = :id")
            .param("id", job.id)
            .update()

        insert(job)
    }

    private fun insert(job: Job) {
        jdbcClient.sql(
            """
            insert into jobs (
                id,
                script,
                status,
                cpus,
                memory,
                flavor,
                executor_id,
                stdout,
                stderr,
                exit_code,
                created_at,
                started_at,
                finished_at
            ) values (
                :id,
                :script,
                :status,
                :cpus,
                :memory,
                :flavor,
                :executorId,
                :stdout,
                :stderr,
                :exitCode,
                :createdAt,
                :startedAt,
                :finishedAt
            )
            """.trimIndent()
        )
            .param("id", job.id)
            .param("script", job.script)
            .param("status", job.status.name)
            .param("cpus", job.requiredResources.cpus)
            .param("memory", job.requiredResources.memory)
            .param("flavor", job.flavor)
            .param("executorId", job.executorId)
            .param("stdout", job.stdout)
            .param("stderr", job.stderr)
            .param("exitCode", job.exitCode)
            .param("createdAt", job.createdAt)
            .param("startedAt", job.startedAt)
            .param("finishedAt", job.finishedAt)
            .update()
    }

    override fun findById(id: String): Job? {
        return jdbcClient.sql(
            """
            select id, script, status, cpus, memory, flavor, executor_id,
                   stdout, stderr, exit_code, created_at, started_at, finished_at
            from jobs
            where id = :id
            """.trimIndent()
        )
            .param("id", id)
            .query { rs, _ -> rs.toJob() }
            .optional()
            .orElse(null)
    }

    override fun findNextQueuedJob(): Job? {
        return jdbcClient.sql(
            """
            select id, script, status, cpus, memory, flavor, executor_id,
                   stdout, stderr, exit_code, created_at, started_at, finished_at
            from jobs
            where status = 'QUEUED'
            order by created_at asc
            limit 1
            """.trimIndent()
        )
            .query { rs, _ -> rs.toJob() }
            .optional()
            .orElse(null)
    }

    override fun markInProgress(jobId: String, executorId: String, startedAt: Instant): Job {
        jdbcClient.sql(
            """
            update jobs
            set status = :status,
                executor_id = :executorId,
                started_at = :startedAt
            where id = :id
            """.trimIndent()
        )
            .param("status", JobStatus.IN_PROGRESS.name)
            .param("executorId", executorId)
            .param("startedAt", startedAt)
            .param("id", jobId)
            .update()

        return requireNotNull(findById(jobId))
    }

    fun markFinished(jobId: String, stdout: String, stderr: String, exitCode: Int, finishedAt: Instant): Job {
        jdbcClient.sql(
            """
            update jobs
            set status = :status,
                stdout = :stdout,
                stderr = :stderr,
                exit_code = :exitCode,
                finished_at = :finishedAt
            where id = :id
            """.trimIndent()
        )
            .param("status", JobStatus.FINISHED.name)
            .param("stdout", stdout)
            .param("stderr", stderr)
            .param("exitCode", exitCode)
            .param("finishedAt", finishedAt)
            .param("id", jobId)
            .update()

        return requireNotNull(findById(jobId))
    }

    private fun ResultSet.toJob(): Job {
        return Job(
            id = getString("id"),
            script = getString("script"),
            status = JobStatus.valueOf(getString("status")),
            requiredResources = ResourceSpec(
                cpus = getInt("cpus"),
                memory = getInt("memory"),
            ),
            flavor = getString("flavor"),
            executorId = getString("executor_id"),
            stdout = getString("stdout"),
            stderr = getString("stderr"),
            exitCode = getObject("exit_code") as Int?,
            createdAt = getTimestamp("created_at").toInstant(),
            startedAt = getTimestamp("started_at")?.toInstant(),
            finishedAt = getTimestamp("finished_at")?.toInstant(),
        )
    }
}
