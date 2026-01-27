package ro.cabinet.backend.licensing.web;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LicensingWebConfig implements WebMvcConfigurer {
  private final LicensingRateLimitInterceptor rateLimitInterceptor;

  public LicensingWebConfig(LicensingRateLimitInterceptor rateLimitInterceptor) {
    this.rateLimitInterceptor = rateLimitInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(rateLimitInterceptor)
        .addPathPatterns("/api/licensing/v1/installs/touch", "/api/licensing/v1/licenses/activate");
  }

  @Bean
  public Clock licensingClock() {
    return Clock.systemUTC();
  }
}
