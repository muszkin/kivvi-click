package click.kivvi.fixtures;

import click.kivvi.domain.Identity;

/**
 * Sample data the panel shell renders until the domain model replaces it — mirrors {@code
 * Workspace} and {@code PanelIdentity}'s hard-coded defaults. Later journeys add their own fixtures
 * here as their pages stop being empty placeholders; this class only carries what the wave-0 shell
 * needs.
 */
public final class ShellFixtures {

  /** The single tenant every panel screen is scoped to. */
  public record Workspace(String name, String meta, String mark) {}

  public static final Identity DEFAULT_IDENTITY =
      new Identity("maciej@aureashop.pl", "Maciej Kowalczyk");

  private static final Workspace WORKSPACE =
      new Workspace("aureashop.pl", "Plan Pro · 3 strony", "AS");

  private ShellFixtures() {}

  public static Workspace workspace() {
    return WORKSPACE;
  }
}
