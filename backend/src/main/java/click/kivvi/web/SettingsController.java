package click.kivvi.web;

import click.kivvi.application.SettingsViewService;
import click.kivvi.application.SpaDocumentService;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.SettingsFixtures;
import click.kivvi.web.dto.SettingsResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Eight settings tabs, one URL each — mirrors {@code SettingsController}.
 *
 * <p>The document route for one tab ({@code GET /{locale}/settings/{tab}}) is handled here rather
 * than by {@link SpaDocumentController}'s generic {@code /{locale}/**} route table, for the same
 * reason {@code CustomersController} handles {@code /{locale}/customers/{id}} itself: an unknown
 * tab is a 404 document exactly like the old stack's {@code createNotFoundException}, but {@link
 * click.kivvi.domain.RouteTable}'s {@code settings(?:/[a-z0-9-]+)?} pattern matches any
 * lowercase-and-digits segment, known tab or not. Spring's handler mapping always prefers this
 * method's exact-shape pattern over the wildcard for any request that reaches both, so no ambiguity
 * results from the two controllers overlapping on this one path. The bare {@code
 * /{locale}/settings} (no tab) still falls through to {@link SpaDocumentController}, which is
 * correct: it always renders the default tab, never a 404.
 */
@RestController
public class SettingsController {

  // Mirrors SpaDocumentController's own reasoning: an unspecified response charset defaults to
  // ISO-8859-1 in the servlet spec, which would mangle this page's Polish text.
  private static final MediaType TEXT_HTML_UTF8 =
      new MediaType("text", "html", StandardCharsets.UTF_8);

  private static final String NOT_FOUND_DOCUMENT =
      "<!doctype html><html lang=\"pl\"><head><meta charset=\"utf-8\"><title>404</title></head>"
          + "<body><h1>404</h1><p>Nie ma takiej zakładki ustawień.</p></body></html>";

  private final SettingsViewService settingsViewService;
  private final SpaDocumentService spaDocumentService;

  public SettingsController(
      SettingsViewService settingsViewService, SpaDocumentService spaDocumentService) {
    this.settingsViewService = settingsViewService;
    this.spaDocumentService = spaDocumentService;
  }

  @GetMapping("/api/v1/{locale:pl|en}/settings/{tab}")
  public ResponseEntity<SettingsResponse> settings(
      @PathVariable String locale, @PathVariable String tab) {
    SupportedLocale supported = SupportedLocale.fromCode(locale).orElseThrow();
    try {
      SettingsViewService.Payload payload = settingsViewService.build(tab, supported);
      return ResponseEntity.ok(toResponse(payload));
    } catch (NoSuchElementException unknownTab) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/{locale:pl|en}/settings/{tab}")
  public ResponseEntity<String> document(
      @PathVariable String locale, @PathVariable String tab, HttpServletRequest request) {
    SupportedLocale supported = SupportedLocale.fromCode(locale).orElseThrow();
    if (!SettingsFixtures.isKnownTab(tab)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .contentType(TEXT_HTML_UTF8)
          .body(NOT_FOUND_DOCUMENT);
    }
    // A read: never force a session into existence just to render a page.
    String document = spaDocumentService.render(request.getSession(false), supported);
    return ResponseEntity.ok().contentType(TEXT_HTML_UTF8).body(document);
  }

  private static SettingsResponse toResponse(SettingsViewService.Payload payload) {
    return new SettingsResponse(
        payload.tab(),
        payload.tabs().stream().map(SettingsController::toTab).toList(),
        payload.tabSubtitle(),
        toSettings(),
        payload.trackerSnippet());
  }

  private static SettingsResponse.Tab toTab(SettingsViewService.TabView tab) {
    return new SettingsResponse.Tab(tab.id(), tab.icon(), tab.label(), tab.href(), tab.active());
  }

  private static SettingsResponse.Settings toSettings() {
    return new SettingsResponse.Settings(
        SettingsFixtures.trackedSites().stream().map(SettingsController::toTrackedSite).toList(),
        SettingsFixtures.automaticEvents(),
        SettingsFixtures.team().stream().map(SettingsController::toTeamMember).toList(),
        SettingsFixtures.roles().stream().map(SettingsController::toRole).toList(),
        SettingsFixtures.emailProviders().stream()
            .map(SettingsController::toEmailProvider)
            .toList(),
        SettingsFixtures.dnsRecords().stream().map(SettingsController::toDnsRecord).toList(),
        SettingsFixtures.apiKeys().stream().map(SettingsController::toApiKey).toList(),
        SettingsFixtures.webhooks().stream().map(SettingsController::toWebhook).toList(),
        SettingsFixtures.apiLimits().stream().map(SettingsController::toBar).toList(),
        SettingsFixtures.notificationMatrix().stream()
            .map(SettingsController::toNotificationRow)
            .toList(),
        SettingsFixtures.planUsage().stream().map(SettingsController::toBar).toList(),
        SettingsFixtures.invoices().stream().map(SettingsController::toInvoice).toList(),
        SettingsFixtures.dataSubjectRequests().stream()
            .map(SettingsController::toDataSubjectRequest)
            .toList(),
        SettingsFixtures.retentionPolicies().stream()
            .map(SettingsController::toRetentionPolicy)
            .toList());
  }

  private static SettingsResponse.TrackedSite toTrackedSite(SettingsFixtures.TrackedSite site) {
    return new SettingsResponse.TrackedSite(site.name(), site.color(), site.events(), site.key());
  }

  private static SettingsResponse.TeamMember toTeamMember(SettingsFixtures.TeamMember member) {
    return new SettingsResponse.TeamMember(
        member.name(),
        member.email(),
        member.role(),
        member.roleTone(),
        member.last(),
        member.invited(),
        member.mfa());
  }

  private static SettingsResponse.Role toRole(SettingsFixtures.Role role) {
    return new SettingsResponse.Role(role.name(), role.description(), role.count());
  }

  private static SettingsResponse.EmailProvider toEmailProvider(
      SettingsFixtures.EmailProvider provider) {
    return new SettingsResponse.EmailProvider(
        provider.name(),
        provider.region(),
        provider.statusTone(),
        provider.statusLabel(),
        provider.sent(),
        provider.bounce(),
        provider.bounceWarn(),
        provider.complaint(),
        provider.verified());
  }

  private static SettingsResponse.DnsRecord toDnsRecord(SettingsFixtures.DnsRecord record) {
    return new SettingsResponse.DnsRecord(record.record(), record.value(), record.ok());
  }

  private static SettingsResponse.ApiKey toApiKey(SettingsFixtures.ApiKey key) {
    return new SettingsResponse.ApiKey(
        key.name(), key.prefix(), key.created(), key.last(), key.scopes());
  }

  private static SettingsResponse.Webhook toWebhook(SettingsFixtures.Webhook webhook) {
    return new SettingsResponse.Webhook(
        webhook.url(), webhook.events(), webhook.code(), webhook.last());
  }

  private static SettingsResponse.Bar toBar(SettingsFixtures.Bar bar) {
    return new SettingsResponse.Bar(bar.label(), bar.pct(), bar.value(), bar.tone());
  }

  private static SettingsResponse.NotificationRow toNotificationRow(
      SettingsFixtures.NotificationRow row) {
    return new SettingsResponse.NotificationRow(row.label(), row.email(), row.slack(), row.sms());
  }

  private static SettingsResponse.Invoice toInvoice(SettingsFixtures.Invoice invoice) {
    return new SettingsResponse.Invoice(
        invoice.number(), invoice.date(), invoice.amount(), invoice.status());
  }

  private static SettingsResponse.DataSubjectRequest toDataSubjectRequest(
      SettingsFixtures.DataSubjectRequest request) {
    return new SettingsResponse.DataSubjectRequest(
        request.id(),
        request.person(),
        request.type(),
        request.status(),
        request.done(),
        request.due());
  }

  private static SettingsResponse.RetentionPolicy toRetentionPolicy(
      SettingsFixtures.RetentionPolicy policy) {
    return new SettingsResponse.RetentionPolicy(
        policy.label(), List.copyOf(policy.options()), policy.selected());
  }
}
