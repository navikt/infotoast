package no.nav.infotoast.config

import java.time.Duration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator

@Configuration
@EnableCaching
class ValkeyConfiguration {

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory

        // Use String serializer for keys
        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()

        // Use the GenericJacksonJsonRedisSerializer builder to create a serializer with default
        // typing
        val jsonSerializer =
            GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(
                    BasicPolymorphicTypeValidator.builder().allowIfBaseType(Any::class.java).build()
                )
                .build()

        template.valueSerializer = jsonSerializer
        template.hashValueSerializer = jsonSerializer

        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        // Use the GenericJacksonJsonRedisSerializer builder to create a serializer with default
        // typing
        val jsonSerializer =
            GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(
                    BasicPolymorphicTypeValidator.builder().allowIfBaseType(Any::class.java).build()
                )
                .build()

        val cacheConfig =
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24)) // Default TTL for cache entries
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        StringRedisSerializer()
                    )
                )
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                )

        return RedisCacheManager.builder(connectionFactory).cacheDefaults(cacheConfig).build()
    }
}
