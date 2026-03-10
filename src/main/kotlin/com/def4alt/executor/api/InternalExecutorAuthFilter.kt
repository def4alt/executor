package com.def4alt.executor.api

import com.def4alt.executor.ExecutorProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class InternalExecutorAuthFilter(
    private val properties: ExecutorProperties,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return !request.requestURI.startsWith("/internal/executors/")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val providedToken = request.getHeader("X-Executor-Token")
        if (providedToken != properties.internalAuthToken) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }

        filterChain.doFilter(request, response)
    }
}
