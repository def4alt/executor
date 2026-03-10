package com.def4alt.executor.persistence

import com.def4alt.executor.application.ExecutorRepository
import com.def4alt.executor.domain.Executor
import com.def4alt.executor.domain.ExecutorStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

@Repository
class JdbcExecutorRepository(
    private val jdbcClient: JdbcClient,
) : ExecutorRepository {
    override fun create(executor: Executor) {
        jdbcClient.sql(
            """
            insert into executors (
                id,
                pod_name,
                namespace,
                status,
                job_id,
                created_at,
                ready_at
            ) values (
                :id,
                :podName,
                :namespace,
                :status,
                :jobId,
                :createdAt,
                :readyAt
            )
            """.trimIndent()
        )
            .param("id", executor.id)
            .param("podName", executor.podName)
            .param("namespace", executor.namespace)
            .param("status", executor.status.name)
            .param("jobId", executor.jobId)
            .param("createdAt", executor.createdAt.toSqlTimestamp())
            .param("readyAt", executor.readyAt.toSqlTimestamp())
            .update()
    }

    override fun findById(id: String): Executor? {
        return jdbcClient.sql(
            """
            select id, pod_name, namespace, status, job_id, created_at,
                   ready_at
            from executors
            where id = :id
            """.trimIndent()
        )
            .param("id", id)
            .query { rs, _ -> rs.toExecutor() }
            .optional()
            .orElse(null)
    }

    override fun markReady(executorId: String, readyAt: Instant): Executor {
        jdbcClient.sql(
            """
            update executors
            set status = :status,
                ready_at = :readyAt
            where id = :id
            """.trimIndent()
        )
            .param("status", ExecutorStatus.READY.name)
            .param("readyAt", readyAt.toSqlTimestamp())
            .param("id", executorId)
            .update()

        return requireNotNull(findById(executorId))
    }

    override fun markTerminated(executorId: String): Executor {
        jdbcClient.sql(
            """
            update executors
            set status = :status
            where id = :id
            """.trimIndent()
        )
            .param("status", ExecutorStatus.TERMINATED.name)
            .param("id", executorId)
            .update()

        return requireNotNull(findById(executorId))
    }

    private fun ResultSet.toExecutor(): Executor {
        return Executor(
            id = getString("id"),
            podName = getString("pod_name"),
            namespace = getString("namespace"),
            status = ExecutorStatus.valueOf(getString("status")),
            jobId = getString("job_id"),
            createdAt = getTimestamp("created_at").toInstant(),
            readyAt = getTimestamp("ready_at")?.toInstant(),
        )
    }

    private fun Instant?.toSqlTimestamp(): Timestamp? = this?.let(Timestamp::from)
}
