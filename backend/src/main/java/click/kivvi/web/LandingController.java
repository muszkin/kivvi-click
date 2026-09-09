package click.kivvi.web;

import click.kivvi.application.LandingView;
import click.kivvi.application.LandingViewService;
import click.kivvi.domain.SupportedLocale;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public marketing page: its view-model endpoint and the shortcut into the panel demo — mirrors
 * {@code LandingController::index()} and {@code LandingController::demo()}.
 */
@RestController
public class LandingController {

  private final LandingViewService landingViewService;

  public LandingController(LandingViewService landingViewService) {
    this.landingViewService = landingViewService;
  }

  @GetMapping("/api/v1/{locale:pl|en}/landing")
  public LandingView landing(@PathVariable String locale) {
    // The mapping regex already gates pl|en; this mirrors ShellController's redundant
    // validation so an unsupported locale can never silently fall through.
    SupportedLocale.fromCode(locale).orElseThrow();
    return landingViewService.build();
  }

  @GetMapping("/{locale:pl|en}/demo")
  public ResponseEntity<Void> demo(@PathVariable String locale) {
    SupportedLocale supported = SupportedLocale.fromCode(locale).orElseThrow();
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create("/" + supported.code() + "/dashboard"))
        .build();
  }
}
