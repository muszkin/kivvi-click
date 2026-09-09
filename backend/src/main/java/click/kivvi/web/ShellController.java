package click.kivvi.web;

import click.kivvi.application.ShellView;
import click.kivvi.application.ShellViewService;
import click.kivvi.domain.SupportedLocale;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The panel shell view-model every SPA page bootstraps from: navigation, workspace, identity and
 * view preferences. {@code locale} is constrained to {@code pl|en} in the mapping itself, so an
 * unsupported prefix never reaches the handler and 404s the same way an unmatched route does
 * everywhere else in the panel.
 */
@RestController
public class ShellController {

  private final ShellViewService shellViewService;

  public ShellController(ShellViewService shellViewService) {
    this.shellViewService = shellViewService;
  }

  @GetMapping("/api/v1/{locale:pl|en}/shell")
  public ShellView shell(
      @PathVariable String locale,
      @RequestParam(name = "route", required = false) String route,
      HttpServletRequest request) {
    SupportedLocale supported = SupportedLocale.fromCode(locale).orElseThrow();
    // A read: never force a session into existence just to answer a GET.
    return shellViewService.build(request.getSession(false), supported, route);
  }
}
