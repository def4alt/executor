package com.def4alt.executor.domain

class FlavorCatalog(
    flavors: List<ExecutorFlavor>,
) {
    private val sortedFlavors = flavors.sortedBy { it.resources.totalFootprint() }

    fun findSmallestMatching(requiredResources: ResourceSpec): ExecutorFlavor {
        return sortedFlavors.firstOrNull { it.resources.fits(requiredResources) }
            ?: throw NoSuchElementException("No executor flavor satisfies requested resources")
    }
}
