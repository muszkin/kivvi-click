package click.kivvi.infrastructure.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Wires {@link SessionRequestSerializationFilter} into the servlet container at an order that makes
 * it wrap <em>outside</em> Spring Boot's {@code SessionRepositoryFilter} — see that filter's class
 * Javadoc for why the ordering matters. The filter itself is exposed only as a {@code @Bean} here
 * (never {@code @Component}-scanned) so Spring Boot's automatic filter registration never sees it
 * as a second, unordered filter bean alongside this explicit {@link FilterRegistrationBean}.
 */
@Configuration
public class SessionRequestSerializationConfig {

  /**
   * Spring Boot's {@code SessionProperties.Servlet} defaults {@code SessionRepositoryFilter} to
   * {@code Ordered.HIGHEST_PRECEDENCE + 50}; ten below that is comfortably outside it while leaving
   * room for anything Boot itself registers even earlier (e.g. the character-encoding filter, at
   * {@code HIGHEST_PRECEDENCE}). {@code SessionRequestSerializationIT} reads both values back from
   * the live context and asserts this relationship directly, rather than each side hard-coding the
   * other's constant.
   */
  static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

  @Bean
  SessionRequestSerializationFilter sessionRequestSerializationFilter(
      SessionLockRegistry lockRegistry,
      @Value(
              "${kivvi.session-lock.timeout-ms:"
                  + SessionRequestSerializationFilter.DEFAULT_LOCK_TIMEOUT_MS
                  + "}")
          long lockTimeoutMillis,
      @Value("${kivvi.testing.preferences-theme-post-delay-ms:0}") long testDelayMillis,
      @Value("${kivvi.session-lock.enabled:true}") boolean lockEnabled,
      @Value("${server.servlet.session.cookie.name:SESSION}") String sessionCookieName) {
    return new SessionRequestSerializationFilter(
        lockRegistry, lockTimeoutMillis, testDelayMillis, lockEnabled, sessionCookieName);
  }

  @Bean
  FilterRegistrationBean<SessionRequestSerializationFilter>
      sessionRequestSerializationFilterRegistration(SessionRequestSerializationFilter filter) {
    FilterRegistrationBean<SessionRequestSerializationFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.setOrder(FILTER_ORDER);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
