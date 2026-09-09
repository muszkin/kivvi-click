package click.kivvi.web.dto;

/** {@code POST /collect}'s success body: {@code {"status": "accepted"}} or {@code "duplicate"}. */
public record EventCollectResponse(String status) {

  public static final String ACCEPTED = "accepted";
  public static final String DUPLICATE = "duplicate";
}
