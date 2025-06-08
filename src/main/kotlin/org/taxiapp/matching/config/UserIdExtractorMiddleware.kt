package org.taxiapp.matching.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class UserIdExtractorMiddleware : OncePerRequestFilter() {

    companion object {
        const val USER_ID_ATTRIBUTE = "userId"
        const val USERNAME_HEADER = "username"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val usernameHeader = request.getHeader(USERNAME_HEADER)

        if (!usernameHeader.isNullOrBlank()) {
            val userId = extractUserIdFromHeader(usernameHeader)
            if (userId != null) {
                request.setAttribute(USER_ID_ATTRIBUTE, userId)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractUserIdFromHeader(headerValue: String): String? {
        return try {
            // Expected format: "username {userId}"
            val regex = Regex("""username\s+\{(.+)\}""")
            val matchResult = regex.find(headerValue.trim())
            matchResult?.groupValues?.get(1)
        } catch (e: Exception) {
            logger.warn("Failed to extract userId from header: $headerValue", e)
            null
        }
    }
}

fun HttpServletRequest.getUserId(): String? {
    return this.getAttribute(UserIdExtractorMiddleware.USER_ID_ATTRIBUTE) as? String
}