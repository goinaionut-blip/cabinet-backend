package ro.stoma.efactura.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(prefix = "efactura", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EfacturaSecurityConfig {
  @Bean
  @Order(0)
  public SecurityFilterChain efacturaOauthChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/efactura/oauth/**")
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }
}
