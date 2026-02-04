package ro.stoma.efactura;

import ro.stoma.efactura.config.EfacturaProperties;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@EnableConfigurationProperties(EfacturaProperties.class)
@EntityScan(basePackages = "ro.stoma.efactura")
@EnableJpaRepositories(basePackages = "ro.stoma.efactura")
@ComponentScan(basePackages = "ro.stoma.efactura")
@ConditionalOnProperty(prefix = "efactura", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EfacturaAutoConfiguration {
}
