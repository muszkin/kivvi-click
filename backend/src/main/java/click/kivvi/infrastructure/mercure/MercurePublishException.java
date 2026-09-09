package click.kivvi.infrastructure.mercure;

/** The Mercure hub could not be reached or refused a publish. */
public final class MercurePublishException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MercurePublishException(String message, Throwable cause) {
    super(message, cause);
  }
}
