package click.kivvi.infrastructure.mail;

/** A message that did not leave, with whatever the transport had to say about why. */
public class MailTransportException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MailTransportException(String message, Throwable cause) {
    super(message, cause);
  }
}
