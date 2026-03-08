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
                flavor,
                status,
                job_id,
                created_at,
                ready_at,
                last_heartbeat_at,
                lease_expires_at
            ) values (
                :id,
                :podName,
                :namespace,
                :flavor,
                :status,
                :jobId,
                :createdAt,
                :readyAt,
                :lastHeartbeatAt,
                :leaseExpiresAt
            )
            """.trimIndent()
        )
            .param("id", executor.id)
            .param("podName", executor.podName)
            .param("namespace", executor.namespace)
            .param("flavor", executor.flavor)
            .param("status", executor.status.name)
            .param("jobId", executor.jobId)
            .param("createdAt", executor.createdAt.toSqlTimestamp())
            .param("readyAt", executor.readyAt.toSqlTimestamp())
            .param("lastHeartbeatAt", executor.lastHeartbeatAt.toSqlTimestamp())
            .param("leaseExpiresAt", executor.leaseExpiresAt.toSqlTimestamp())
            .update()
    }

    override fun findById(id: String): Executor? {
        return jdbcClient.sql(
            """
            select id, pod_name, namespace, flavor, status, job_id, created_at,
                   ready_at, last_heartbeat_at, lease_expires_at
            from executors
            where id = :id
            """.trimIndent()
        )
            .param("id", id)
            .query { rs, _ -> rs.toExecutor() }
            .optional()
            .orElse(null)
    }

    override fun leaseReadyExecutor(flavor: String, leaseExpiresAt: Instant): Executor? {
        val executor = jdbcClient.sql(
            """
            select id, pod_name, namespace, flavor, status, job_id, created_at,
                   ready_at, last_heartbeat_at, lease_expires_at
            from executors
            where flavor = :flavor and status = 'READY'
            order by ready_at asc, created_at asc
            limit 1
            """.trimIndent()
        )
            .param("flavor", flavor)
            .query { rs, _ -> rs.toExecutor() }
            .optional()
            .orElse(null)
            ?: return null

        jdbcClient.sql(
            """
            update executors
            set status = :status,
                lease_expires_at = :leaseExpiresAt
            where id = :id
            """.trimIndent()
        )
            .param("status", ExecutorStatus.LEASED.name)
            .param("leaseExpiresAt", leaseExpiresAt.toSqlTimestamp())
            .param("id", executor.id)
            .update()

        return requireNotNull(findById(executor.id))
    }

    override fun countByFlavorAndStatuses(flavor: String, statuses: Set<ExecutorStatus>): Int {
        if (statuses.isEmpty()) {
            return 0
        }

        val placeholders = statuses.mapIndexed { index, _ -> ":status$index" }
        var statement = jdbcClient.sql(
            """
            select count(*)
            from executors
            where flavor = :flavor and status in (${placeholders.joinToString(", ")})
            """.trimIndent()
        ).param("flavor", flavor)

        statuses.toList().forEachIndexed { index, status ->
            statement = statement.param("status$index", status.name)
        }

        return statement.query(Int::class.java).single()
    }

    override fun markReady(executorId: String, readyAt: Instant): Executor {
        jdbcClient.sql(
            """
            update executors
            set status = :status,
                ready_at = :readyAt,
                last_heartbeat_at = :lastHeartbeatAt
            where id = :id
            """.trimIndent()
        )
            .param("status", ExecutorStatus.READY.name)
            .param("readyAt", readyAt.toSqlTimestamp())
            .param("lastHeartbeatAt", readyAt.toSqlTimestamp())
            .param("id", executorId)
            .update()

        return requireNotNull(findById(executorId))
    }

    override fun attachJob(executorId: String, jobId: String): Executor {
        jdbcClient.sql(
            """
            update executors
            set job_id = :jobId
            where id = :id
            """.trimIndent()
        )
            .param("jobId", jobId)
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

    override fun findByStatuses(statuses: Set<ExecutorStatus>): List<Executor> {
        if (statuses.isEmpty()) {
            return emptyList()
        }

        val placeholders = statuses.mapIndexed { index, _ -> ":status$index" }
        var statement = jdbcClient.sql(
            """
            select id, pod_name, namespace, flavor, status, job_id, created_at,
                   ready_at, last_heartbeat_at, lease_expires_at
            from executors
            where status in (${placeholders.joinToString(", ")})
            """.trimIndent()
        )

        statuses.toList().forEachIndexed { index, status ->
            statement = statement.param("status$index", status.name)
        }

        return statement.query { rs, _ -> rs.toExecutor() }.list()
    }

    private fun ResultSet.toExecutor(): Executor {
        return Executor(
            id = getString("id"),
            podName = getString("pod_name"),
            namespace = getString("namespace"),
            flavor = getString("flavor"),
            status = ExecutorStatus.valueOf(getString("status")),
            jobId = getString("job_id"),
            createdAt = getTimestamp("created_at").toInstant(),
            readyAt = getTimestamp("ready_at")?.toInstant(),
            lastHeartbeatAt = getTimestamp("last_heartbeat_at")?.toInstant(),
            leaseExpiresAt = getTimestamp("lease_expires_at")?.toInstant(),
        )
    }

    private fun Instant?.toSqlTimestamp(): Timestamp? = this?.let(Timestamp::from)
}
