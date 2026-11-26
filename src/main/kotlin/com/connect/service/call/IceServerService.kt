package com.connect.service.call

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Instant

private val log = KotlinLogging.logger {}

/**
 * 통화(WebRTC)에 쓸 ICE 서버 목록을 만든다.
 *
 * 휴대폰(LTE)과 노트북(공유기 뒤)처럼 서로 직접 연결이 안 되는 경우가 많아서,
 * Cloudflare TURN 이 소리를 대신 중계하게 한다. TURN 비밀 키는 서버 환경변수에만 두고,
 * 앱에는 Cloudflare 가 발급한 임시 접속 정보(ttl 동안만 유효)만 내려준다.
 *
 * 키가 없거나 발급에 실패하면 구글 STUN 만 내려준다 (같은 와이파이 등에서는 그것만으로도 된다).
 */
@Service
class IceServerService(
    @Value("\${cloudflare.turn.key-id:}") private val keyId: String,
    @Value("\${cloudflare.turn.api-token:}") private val apiToken: String,
    @Value("\${cloudflare.turn.api-base:https://rtc.live.cloudflare.com}") private val apiBase: String,
) {
    data class IceServer(val urls: List<String>, val username: String? = null, val credential: String? = null)
    data class IceServers(val iceServers: List<IceServer>, val turn: Boolean)

    private val client = RestClient.create()

    // 요청마다 Cloudflare 를 부르지 않도록 발급받은 정보를 잠시 재사용한다.
    // 앱에 내려준 정보가 통화 도중 만료되지 않게, 유효기간(TTL)의 절반만 재사용한다.
    @Volatile private var cached: IceServers? = null
    @Volatile private var cachedUntil: Instant = Instant.EPOCH

    fun iceServers(): IceServers {
        if (keyId.isBlank() || apiToken.isBlank()) return STUN_ONLY
        cached?.let { if (Instant.now().isBefore(cachedUntil)) return it }
        synchronized(this) {
            cached?.let { if (Instant.now().isBefore(cachedUntil)) return it }
            return try {
                val fresh = IceServers(fetchFromCloudflare() + STUN_ONLY.iceServers, turn = true)
                cached = fresh
                cachedUntil = Instant.now().plus(TTL.dividedBy(2))
                fresh
            } catch (e: Exception) {
                log.warn { "Cloudflare TURN 발급 실패, STUN 만 사용: ${e.message}" }
                STUN_ONLY
            }
        }
    }

    private fun fetchFromCloudflare(): List<IceServer> {
        val body = client.post()
            .uri("$apiBase/v1/turn/keys/{keyId}/credentials/generate-ice-servers", keyId)
            .header("Authorization", "Bearer $apiToken")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("ttl" to TTL.seconds))
            .retrieve()
            .body(JsonNode::class.java) ?: error("빈 응답")

        // iceServers 는 배열로 오지만, 예전 API 처럼 객체 하나로 와도 받아준다
        val node = body.get("iceServers") ?: error("iceServers 없음")
        val entries = if (node.isArray) node.toList() else listOf(node)
        return entries.mapNotNull { entry ->
            val urlsNode = entry.get("urls") ?: return@mapNotNull null
            val urls = (if (urlsNode.isArray) urlsNode.map { it.asText() } else listOf(urlsNode.asText()))
                // 53번 포트는 브라우저가 막는 경우가 많아 연결만 늦어진다 (Cloudflare 권장)
                .filterNot { PORT_53.containsMatchIn(it) }
            if (urls.isEmpty()) null
            else IceServer(urls, entry.get("username")?.asText(), entry.get("credential")?.asText())
        }.ifEmpty { error("쓸 수 있는 ICE 서버 없음") }
    }

    companion object {
        private val TTL: Duration = Duration.ofHours(12)
        private val PORT_53 = Regex(":53(\\?|$)")
        val STUN_ONLY = IceServers(
            listOf(IceServer(listOf("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302"))),
            turn = false,
        )
    }
}
