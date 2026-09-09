package click.kivvi.infrastructure.mercure;

/**
 * Publishes an update to a Mercure hub topic. The only production implementation is {@link
 * HttpMercurePublisher}; tests substitute a recording fake, mirroring how the old stack's own test
 * swapped in {@code MockHub} rather than exercising a real hub.
 */
public interface MercurePublisher {

  /**
   * @param topic the Mercure topic, e.g. {@code /accounts/1/events}
   * @param data the update's data field, already serialized (JSON in this app's only caller)
   * @throws MercurePublishException if the hub could not be reached or refused the publish
   */
  void publish(String topic, String data);
}
