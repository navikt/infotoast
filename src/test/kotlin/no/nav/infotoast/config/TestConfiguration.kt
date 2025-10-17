package no.nav.infotoast.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate

//@TestConfiguration
class ValkeyTestConfiguration {

    @Bean
    @Primary
    fun testRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val valkeyConfig = ValkeyConfiguration()
        return valkeyConfig.redisTemplate(connectionFactory)
    }
}
