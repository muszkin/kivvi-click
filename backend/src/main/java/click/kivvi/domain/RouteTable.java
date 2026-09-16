package click.kivvi.domain;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every path the SPA document is served for, ported from {@code inventory/routes.json} (the 27
 * Symfony routes minus {@code /_storybook}, {@code /collect}, {@code /preferences/*} and {@code
 * /import/upload}, which are not SPA documents). A path outside this table — or with an unsupported
 * locale prefix — is unknown to the panel and gets a 404, matching the old stack's routing exactly:
 * no requirement here ever matched, no route ever rendered.
 */
public final class RouteTable {

  /** Which layout wraps the page: the marketing shell, the auth split screen, or the app shell. */
  public enum Layout {
    PUBLIC,
    AUTH,
    APP
  }

  /** A matched request: the route name (mirrors the Symfony route name) and its layout. */
  public record Match(String routeName, Layout layout, SupportedLocale locale) {}

  private record Route(String name, Pattern pattern, Layout layout) {
    private Route(String name, String localeRelativePattern, Layout layout) {
      this(name, Pattern.compile("^/(?<locale>pl|en)" + localeRelativePattern + "$"), layout);
    }
  }

  private static final List<Route> LOCALE_PREFIXED =
      List.of(
          new Route("home", "", Layout.PUBLIC),
          // PIO-70: the first route here that is not a port of a Symfony route. The waitlist
          // form's consent clause links to it, so it has to answer a direct hit and a reload,
          // not only an in-SPA transition — which is exactly what an entry here provides.
          new Route("privacy", "/privacy", Layout.PUBLIC),
          // The waitlist form's own POST target. A refused submission re-renders the landing
          // document without redirecting, which leaves the browser on this URL — so a reload
          // from there has to serve the landing page rather than a 404. GET here is just the
          // landing page; only POST (WaitlistController) does anything.
          new Route("waitlist", "/waitlist", Layout.PUBLIC),
          // PIO-71. The three pages a confirmation mail leads to. Entries here matter for the
          // "sent" page above all: the other two are answered by WaitlistConfirmationController,
          // but /waitlist/confirm/sent is a plain document and without a route it would 404 the
          // moment the browser followed the redirect. The token routes are listed for the same
          // reason every other route is — one table describes what this application serves.
          //
          // The token pattern is the token's own shape (64 hex characters), so a mistyped or
          // truncated link 404s here rather than reaching the controller to be told it is unknown.
          new Route("waitlist_confirm_sent", "/waitlist/confirm/sent", Layout.PUBLIC),
          new Route("waitlist_confirm", "/waitlist/confirm/[0-9a-f]{64}", Layout.PUBLIC),
          new Route("waitlist_unsubscribe", "/waitlist/unsubscribe/[0-9a-f]{64}", Layout.PUBLIC),
          new Route("login", "/login", Layout.AUTH),
          new Route("dashboard", "/dashboard", Layout.APP),
          new Route("events", "/events", Layout.APP),
          new Route("customers", "/customers", Layout.APP),
          new Route("customer_show", "/customers/c_\\d+", Layout.APP),
          new Route("automations", "/automations", Layout.APP),
          new Route("automation_new", "/automations/new", Layout.APP),
          new Route("automation_edit", "/automations/a\\d+", Layout.APP),
          new Route("campaigns", "/campaigns", Layout.APP),
          new Route("email_new", "/emails/new", Layout.APP),
          new Route("email_edit", "/emails/k\\d+", Layout.APP),
          new Route("popups", "/popups", Layout.APP),
          new Route("popup_new", "/popups/new", Layout.APP),
          new Route("popup_edit", "/popups/p\\d+", Layout.APP),
          new Route("feeds", "/feeds", Layout.APP),
          new Route("import", "/import(?:/[1-4])?", Layout.APP),
          new Route("settings", "/settings(?:/[a-z0-9-]+)?", Layout.APP));

  private static final Pattern ROOT = Pattern.compile("^/$");

  private RouteTable() {}

  /**
   * Matches a request path against the table, resolving its locale from the URL segment. The bare
   * root has no segment to read, so it is the landing page in {@link SupportedLocale#DEFAULT} —
   * English since PIO-125.
   */
  public static Optional<Match> match(String path) {
    if (ROOT.matcher(path).matches()) {
      return Optional.of(new Match("home", Layout.PUBLIC, SupportedLocale.DEFAULT));
    }
    for (Route route : LOCALE_PREFIXED) {
      Matcher matcher = route.pattern().matcher(path);
      if (matcher.matches()) {
        SupportedLocale locale = SupportedLocale.fromCode(matcher.group("locale")).orElseThrow();
        return Optional.of(new Match(route.name(), route.layout(), locale));
      }
    }
    return Optional.empty();
  }
}
