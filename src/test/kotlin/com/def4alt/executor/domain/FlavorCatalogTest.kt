package com.def4alt.executor.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class FlavorCatalogTest {
    private val catalog = FlavorCatalog(
        listOf(
            ExecutorFlavor("small-linux", ResourceSpec(cpus = 1, memory = 512)),
            ExecutorFlavor("medium-linux", ResourceSpec(cpus = 1, memory = 1024)),
            ExecutorFlavor("large-linux", ResourceSpec(cpus = 2, memory = 2048)),
        )
    )

    @Test
    fun `findSmallestMatching chooses the smallest flavor that fits`() {
        val flavor = catalog.findSmallestMatching(ResourceSpec(cpus = 1, memory = 900))

        assertEquals("medium-linux", flavor.name)
    }

    @Test
    fun `findSmallestMatching picks the smallest sufficient flavor`() {
        val flavor = catalog.findSmallestMatching(ResourceSpec(cpus = 1, memory = 256))

        assertEquals("small-linux", flavor.name)
    }

    @Test
    fun `findSmallestMatching rejects requests that no flavor can satisfy`() {
        assertThrows<NoSuchElementException> {
            catalog.findSmallestMatching(ResourceSpec(cpus = 4, memory = 4096))
        }
    }
}
