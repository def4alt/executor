package com.def4alt.executor.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourceSpecTest {
    @Test
    fun `fits returns true when executor satisfies every resource requirement`() {
        val executor = ResourceSpec(cpuCores = 2, memoryMb = 4096, gpuCount = 0)
        val job = ResourceSpec(cpuCores = 1, memoryMb = 2048, gpuCount = 0)

        assertTrue(executor.fits(job))
    }

    @Test
    fun `fits returns false when any required resource exceeds executor capacity`() {
        val executor = ResourceSpec(cpuCores = 2, memoryMb = 4096, gpuCount = 0)

        assertFalse(executor.fits(ResourceSpec(cpuCores = 3, memoryMb = 2048, gpuCount = 0)))
        assertFalse(executor.fits(ResourceSpec(cpuCores = 2, memoryMb = 8192, gpuCount = 0)))
        assertFalse(executor.fits(ResourceSpec(cpuCores = 2, memoryMb = 4096, gpuCount = 1)))
    }
}
