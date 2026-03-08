package com.def4alt.executor.application

class JobNotFoundException(id: String) : RuntimeException("Job $id not found")
