package ro.stoma.efactura;

import ro.stoma.efactura.config.EfacturaProperties;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@EnableConfigurationProperties(EfacturaProperties.class)
@ComponentScan(basePackages = "ro.stoma.efactura")
@ConditionalOnProperty(prefix = "efactura", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EfacturaAutoConfiguration {
}
