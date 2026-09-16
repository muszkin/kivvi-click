package click.kivvi.infrastructure.mail;

import click.kivvi.domain.mail.OutboundMail;

/**
 * How a message actually leaves. Two implementations, chosen by profile and never both present:
 * {@link SmtpMailTransport} outside {@code dev}, {@link FilesystemMailTransport} inside it.
 *
 * <p>The seam exists so the sender can be tested against a transport that throws on demand, and so
 * the {@code dev} stack can close the double opt-in loop without a mail container — Postgres stays
 * the only backing service (CLAUDE.md, "Architecture decisions").
 */
public interface MailTransport {

  /**
   * @throws MailTransportException when the message did not leave. The sender treats any failure as
   *     retryable; a permanent one simply exhausts its attempts a little more slowly than it might.
   */
  void send(OutboundMail mail);
}
