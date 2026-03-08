package com.def4alt.executor.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class FlavorCatalogTest {
    private val catalog = FlavorCatalog(
        listOf(
            ExecutorFlavor("small-linux", ResourceSpec(cpuCores = 1, memoryMb = 2048, gpuCount = 0)),
            ExecutorFlavor("medium-linux", ResourceSpec(cpuCores = 2, memoryMb = 4096, gpuCount = 0)),
            ExecutorFlavor("gpu-linux", ResourceSpec(cpuCores = 4, memoryMb = 16384, gpuCount = 1)),
        )
    )

    @Test
    fun `findSmallestMatching chooses the smallest flavor that fits`() {
        val flavor = catalog.findSmallestMatching(ResourceSpec(cpuCores = 2, memoryMb = 3072, gpuCount = 0))

        assertEquals("medium-linux", flavor.name)
    }

    @Test
    fun `findSmallestMatching prefers non-gpu flavor when gpu is not needed`() {
        val flavor = catalog.findSmallestMatching(ResourceSpec(cpuCores = 1, memoryMb = 1024, gpuCount = 0))

        assertEquals("small-linux", flavor.name)
    }

    @Test
    fun `findSmallestMatching rejects requests that no flavor can satisfy`() {
        assertThrows<NoSuchElementException> {
            catalog.findSmallestMatching(ResourceSpec(cpuCores = 8, memoryMb = 32768, gpuCount = 0))
        }
    }
}
