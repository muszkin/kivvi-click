package click.kivvi.web.dto;

/** {@code POST /collect}'s 400 body: {@code {"error": "<message>"}}, the exact Polish text. */
public record EventErrorResponse(String error) {}
