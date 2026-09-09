package click.kivvi.web.dto;

/** {@code POST /preferences/theme} response: {"theme": "dark"|"light"}. */
public record ThemeResponse(String theme) {}
