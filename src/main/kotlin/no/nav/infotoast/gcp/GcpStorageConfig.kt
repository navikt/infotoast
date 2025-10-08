package no.nav.infotoast.gcp

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GcpStorageConfig {

    @Bean
    fun storage(): Storage {
        return StorageOptions.getDefaultInstance().service
    }
}
