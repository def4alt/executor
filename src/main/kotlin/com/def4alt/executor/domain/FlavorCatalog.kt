package com.def4alt.executor.domain

class FlavorCatalog(
    flavors: List<ExecutorFlavor>,
) {
    private val sortedFlavors = flavors.sortedBy { it.resources.totalFootprint() }
    private val flavorsByName = flavors.associateBy { it.name }

    fun findSmallestMatching(requiredResources: ResourceSpec): ExecutorFlavor {
        return sortedFlavors.firstOrNull { it.resources.fits(requiredResources) }
            ?: throw NoSuchElementException("No executor flavor satisfies requested resources")
    }

    fun getByName(name: String): ExecutorFlavor {
        return flavorsByName[name] ?: throw NoSuchElementException("Unknown executor flavor $name")
    }
}
