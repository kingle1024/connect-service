package com.connect.service.admin

import com.connect.service.admin.security.AdminPageGateFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Base64

class AdminPageGateFilterTest {

    private val hash = BCryptPasswordEncoder(4).encode("open-sesame")

    private fun run(filter: AdminPageGateFilter, authorization: String?): Pair<MockHttpServletResponse, MockFilterChain> {
        val request = MockHttpServletRequest("GET", "/admin/").apply {
            authorization?.let { addHeader("Authorization", it) }
        }
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        return response to chain
    }

    private fun basic(user: String, password: String) =
        "Basic " + Base64.getEncoder().encodeToString("$user:$password".toByteArray())

    @Test
    fun `비밀번호 없이 오면 401 과 인증 창 헤더를 돌려준다`() {
        val (response, chain) = run(AdminPageGateFilter(hash), null)
        assertEquals(401, response.status)
        assertNotNull(response.getHeader("WWW-Authenticate"))
        assertNull(chain.request)
    }

    @Test
    fun `비밀번호가 틀리면 막는다`() {
        val (response, chain) = run(AdminPageGateFilter(hash), basic("admin", "wrong"))
        assertEquals(401, response.status)
        assertNull(chain.request)
    }

    @Test
    fun `비밀번호가 맞으면 아이디와 상관없이 통과시킨다`() {
        val filter = AdminPageGateFilter(hash)
        assertNotNull(run(filter, basic("", "open-sesame")).second.request)
        assertNotNull(run(filter, basic("아무나", "open-sesame")).second.request)
    }

    @Test
    fun `Bearer 토큰만으로는 화면을 열 수 없다`() {
        val (response, _) = run(AdminPageGateFilter(hash), "Bearer abc")
        assertEquals(401, response.status)
    }

    @Test
    fun `해시가 비어 있으면 문을 열어 둔다`() {
        assertNotNull(run(AdminPageGateFilter(""), null).second.request)
    }
}
