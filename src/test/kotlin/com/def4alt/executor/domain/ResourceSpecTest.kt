package com.def4alt.executor.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourceSpecTest {
    @Test
    fun `fits returns true when executor satisfies every resource requirement`() {
        val executor = ResourceSpec(cpus = 2, memory = 4096)
        val job = ResourceSpec(cpus = 1, memory = 2048)

        assertTrue(executor.fits(job))
    }

    @Test
    fun `fits returns false when any required resource exceeds executor capacity`() {
        val executor = ResourceSpec(cpus = 2, memory = 4096)

        assertFalse(executor.fits(ResourceSpec(cpus = 3, memory = 2048)))
        assertFalse(executor.fits(ResourceSpec(cpus = 2, memory = 8192)))
    }
}
