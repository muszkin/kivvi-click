package click.kivvi.fixtures;

import click.kivvi.domain.Identity;
import click.kivvi.domain.SupportedLocale;

/**
 * Sample data the panel shell renders until the domain model replaces it — mirrors {@code
 * Workspace} and {@code PanelIdentity}'s hard-coded defaults. Later journeys add their own fixtures
 * here as their pages stop being empty placeholders; this class only carries what the wave-0 shell
 * needs.
 *
 * <p>PIO-129: the workspace's {@code meta} line counts tracked sites, so it is interface text and
 * has a version per language. {@link #DEFAULT_IDENTITY} does not — it is one demonstration person's
 * name and address, and a person is not translated. The name shown in the top bar is in any case
 * derived from the e-mail by {@code Identity.derive}, so a locale-dependent default would mean the
 * same session answering to two different people depending on the URL prefix.
 */
public final class ShellFixtures {

  /** The single tenant every panel screen is scoped to. */
  public record Workspace(String name, String meta, String mark) {}

  public static final Identity DEFAULT_IDENTITY =
      new Identity("maciej@aureashop.pl", "Maciej Kowalczyk");

  private static final Workspace WORKSPACE_PL = new Workspace("aureashop.pl", "3 strony", "AS");
  private static final Workspace WORKSPACE_EN = new Workspace("aureashop.pl", "3 sites", "AS");

  private ShellFixtures() {}

  public static Workspace workspace(SupportedLocale locale) {
    return switch (locale) {
      case PL -> WORKSPACE_PL;
      case EN -> WORKSPACE_EN;
    };
  }
}
