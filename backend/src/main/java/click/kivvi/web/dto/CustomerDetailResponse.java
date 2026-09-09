package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/customers/{id}} response: the 360 profile — the wire shape {@link
 * click.kivvi.web.CustomersController} builds from {@link
 * click.kivvi.application.CustomersViewService}'s computed view-model. Absent for an unknown id, in
 * which case the endpoint answers 404 instead (see {@link click.kivvi.web.CustomersController}).
 */
public record CustomerDetailResponse(
    Customer customer,
    String profileSub,
    List<Fact> facts,
    List<Automation> automations,
    List<Tab> tabs,
    List<Score> scores,
    List<TimelineEntry> timeline) {

  public record Customer(String id, String name, String initials, String email, List<Tag> tags) {}

  /** {@code tone} is {@code null} for the plain "PL" tag, exactly like the old stack's chip. */
  public record Tag(String label, String tone) {}

  public record Fact(String label, String value, boolean small) {}

  public record Automation(String name) {}

  public record Tab(String label, boolean active) {}

  public record Score(
      String label, String value, String unit, String delta, String dir, String deltaIcon) {}

  public record TimelineEntry(String time, String title, String detail, String icon) {}
}
