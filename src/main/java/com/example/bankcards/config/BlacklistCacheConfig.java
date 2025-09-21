package com.example.bankcards.config;

import com.example.bankcards.util.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class BlacklistCacheConfig {
	@Value("${jwt.expiration.accessInMinutes}")
	private int jwtDuration;
	
	@Value("${spring.data.redis.host}")
	private String redisHost;
	@Value("${spring.data.redis.port}")
	private int redisPort;
	
	@Bean
	public LettuceConnectionFactory redisConnectionFactory() {
		return new LettuceConnectionFactory(new RedisStandaloneConfiguration(redisHost, redisPort));
	}
	
	@Bean
	public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
		return (builder) -> {
			RedisCacheConfiguration config = RedisCacheConfiguration
					.defaultCacheConfig()
					.entryTtl(Duration.ofMinutes(jwtDuration));
			
			Map<String, RedisCacheConfiguration> configurationMap = new HashMap<>() {{
				put(Constants.BLACKLIST_CACHE_NAME, config);
			}};
			
			builder.withInitialCacheConfigurations(configurationMap);
		};
	}
}
