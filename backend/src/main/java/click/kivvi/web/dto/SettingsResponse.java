package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/settings/{tab}} response — the whole settings catalogue, exactly like
 * the Twig page received the injected {@code SettingsCatalog} service and every partial could call
 * any of its methods: every collection is always serialized, regardless of which tab is active, so
 * a tab switch never needs a second shape. {@code trackerSnippet} is the raw, unescaped source;
 * highlighting is a presentation concern the SPA owns (see {@code frontend/src/highlight.ts}).
 */
public record SettingsResponse(
    String tab, List<Tab> tabs, String tabSubtitle, Settings settings, String trackerSnippet) {

  public record Tab(String id, String icon, String label, String href, boolean active) {}

  public record TrackedSite(String name, String color, String events, String key) {}

  public record TeamMember(
      String name,
      String email,
      String role,
      String roleTone,
      String last,
      boolean invited,
      boolean mfa) {}

  public record Role(String name, String description, int count) {}

  public record EmailProvider(
      String name,
      String region,
      String statusTone,
      String statusLabel,
      String sent,
      String bounce,
      boolean bounceWarn,
      String complaint,
      boolean verified) {}

  public record DnsRecord(String record, String value, boolean ok) {}

  public record ApiKey(
      String name, String prefix, String created, String last, List<String> scopes) {}

  public record Webhook(String url, List<String> events, int code, String last) {}

  public record Bar(String label, double pct, String value, String tone) {}

  public record NotificationRow(String label, boolean email, boolean slack, boolean sms) {}

  public record Invoice(String number, String date, String amount, String status) {}

  public record DataSubjectRequest(
      String id, String person, String type, String status, boolean done, String due) {}

  public record RetentionPolicy(String label, List<String> options, String selected) {}

  public record Settings(
      List<TrackedSite> trackedSites,
      List<String> automaticEvents,
      List<TeamMember> team,
      List<Role> roles,
      List<EmailProvider> emailProviders,
      List<DnsRecord> dnsRecords,
      List<ApiKey> apiKeys,
      List<Webhook> webhooks,
      List<Bar> apiLimits,
      List<NotificationRow> notificationMatrix,
      List<Bar> planUsage,
      List<Invoice> invoices,
      List<DataSubjectRequest> dataSubjectRequests,
      List<RetentionPolicy> retentionPolicies) {}
}
