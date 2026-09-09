package click.kivvi.web.dto;

/** {@code POST /preferences/sidebar} response: {"state": "collapsed"|"expanded"}. */
public record SidebarResponse(String state) {}
