package click.kivvi.domain.mail;

import java.util.Objects;
import java.util.Optional;

/**
 * One message waiting to go out: everything the transport needs and nothing about how it gets
 * there. The same record is what {@code mail_outbox} stores and what the SMTP transport sends, so a
 * message that survived a restart is byte-for-byte the message that was composed.
 *
 * @param recipient the address to deliver to, already normalized by whoever built this
 * @param subject the subject line, in the recipient's language
 * @param htmlBody the HTML alternative, with every style inlined — mail clients fetch no
 *     stylesheets
 * @param textBody the plain-text alternative; never blank, because a message without one scores
 *     worse with spam filters and is unreadable in a text client
 * @param dedupKey optional: when present, queueing the same key twice stores one row. Absent when
 *     the caller genuinely wants another copy — a resend the visitor asked for.
 */
public record OutboundMail(
    String recipient, String subject, String htmlBody, String textBody, String dedupKey) {

  public OutboundMail {
    Objects.requireNonNull(recipient, "recipient");
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(htmlBody, "htmlBody");
    Objects.requireNonNull(textBody, "textBody");
    if (textBody.isBlank()) {
      throw new IllegalArgumentException("An outbound mail must carry a plain-text alternative.");
    }
  }

  /** A message that may safely be queued more than once. */
  public static OutboundMail repeatable(
      String recipient, String subject, String htmlBody, String textBody) {
    return new OutboundMail(recipient, subject, htmlBody, textBody, null);
  }

  public Optional<String> deduplicationKey() {
    return Optional.ofNullable(dedupKey);
  }
}
