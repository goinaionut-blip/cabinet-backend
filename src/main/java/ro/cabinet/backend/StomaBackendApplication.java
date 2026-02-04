package ro.cabinet.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EntityScan(basePackages = {"ro.cabinet.backend", "ro.stoma.efactura"})
@EnableJpaRepositories(basePackages = {"ro.cabinet.backend", "ro.stoma.efactura"})
@EnableScheduling
public class StomaBackendApplication {
  public static void main(String[] args) {
    SpringApplication.run(StomaBackendApplication.class, args);
  }
}
