package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/emails/{id}} response: the composer model behind the e-mail editor
 * (envelope, block library, placeholder variables, the 600px document's sections and the
 * inspector's selected block) — the wire shape {@link click.kivvi.web.CampaignsController} builds
 * from {@link click.kivvi.application.CampaignsViewService}'s computed view-model.
 */
public record CampaignEmailResponse(
    Template template,
    List<Block> blocks,
    List<Variable> variables,
    List<Section> sections,
    SelectedBlock selectedBlock) {

  public record Template(String id, String name, String meta, String subject, String sender) {}

  public record Block(String icon, String label, String type) {}

  public record Variable(String token, String description) {}

  public record Product(String name, String price) {}

  /**
   * One block of the 600px e-mail document. Flat and nullable-by-type: {@code type} selects which
   * of the other fields are populated ({@code hero}: kicker/title/body; {@code coupon}:
   * code/note/cta/href; {@code products}: kicker/items; {@code footer}: body) — mirrors {@link
   * click.kivvi.fixtures.CampaignsFixtures.Section} 1:1. Jackson's {@code non_null} inclusion
   * (application.yml) drops every field a given section type does not use, so the wire payload
   * never carries a stray {@code null}.
   */
  public record Section(
      String type,
      String kicker,
      String title,
      String body,
      String code,
      String note,
      String cta,
      String href,
      List<Product> items) {}

  public record SelectedBlock(
      String blockName,
      String blockId,
      String title,
      List<String> placeholders,
      List<String> backgrounds,
      String alignment,
      String padding,
      String visibility) {}
}
