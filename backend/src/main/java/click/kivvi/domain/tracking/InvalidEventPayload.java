package click.kivvi.domain.tracking;

/**
 * Raised when the tracking script sends something {@code /collect} cannot accept — ported from
 * {@code App\Tracking\InvalidEventPayload}. The message is the exact Polish text {@code
 * CollectController} returns as the {@code error} field, byte for byte.
 */
public final class InvalidEventPayload extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidEventPayload(String message) {
    super(message);
  }

  public InvalidEventPayload(String message, Throwable cause) {
    super(message, cause);
  }
}
