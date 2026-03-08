package com.def4alt.executor.domain

data class ResourceSpec(
    val cpus: Int,
    val memory: Int,
) {
    fun fits(required: ResourceSpec): Boolean {
        return cpus >= required.cpus &&
            memory >= required.memory
    }

    fun totalFootprint(): Long {
        return cpus.toLong() * 1_000_000_000L +
            memory.toLong() * 1_000L
    }
}
