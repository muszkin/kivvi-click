package click.kivvi.fixtures;

import click.kivvi.domain.SupportedLocale;
import java.util.List;

/**
 * The four-step customer import: file, column mapping, rules, preview — ported from {@code
 * App\Panel\Content\ImportWizard}. Every step is reachable by URL, so a half-configured import
 * survives a reload and the back button behaves like the stepper.
 *
 * <p>Raw, unformatted data only — {@link click.kivvi.application.ImportViewService} derives the
 * display strings (confidence colours are a frontend concern; number/money formatting is {@link
 * click.kivvi.domain.Format}'s), exactly like {@link FeedsFixtures} splits from {@code
 * FeedsViewService}.
 */
public final class ImportFixtures {

  public static final int FIRST_STEP = 1;
  public static final int LAST_STEP = 4;

  private static final String DEFAULT_FILE_NAME_PL = "klienci.csv";
  private static final String DEFAULT_FILE_NAME_EN = "customers.csv";
  private static final String FILE_META_PL = "8 420 wierszy · CSV UTF-8 · ; jako separator";
  private static final String FILE_META_EN = "8,420 rows · CSV UTF-8 · ; as the separator";

  private static final int ROW_COUNT = 8420;
  private static final int ERROR_COUNT = 44;

  public record StepDef(int n, String label) {}

  public record Target(String value, String label) {}

  /**
   * One detected column, before {@link click.kivvi.application.ImportViewService} assigns it a
   * spreadsheet letter and a confidence colour.
   */
  public record RawColumn(String name, List<String> samples, String mapped, int confidence) {}

  public record ValidationCheck(String label, String tone) {}

  public record DedupStrategy(String value, String label, boolean checked) {}

  /**
   * One row of the step-4 preview, before {@link click.kivvi.application.ImportViewService} assigns
   * the status chip tone/label and formats the LTV. {@code ltv} is {@code null} for the one row
   * whose e-mail fails validation, mirroring {@code ImportWizard::PREVIEW_ROWS_PL}.
   */
  public record RawPreviewRow(
      String email,
      String name,
      String optIn,
      String orders,
      Double ltv,
      String status,
      List<String> segments,
      String error) {}

  public record RecentImport(
      String file, int rows, String date, String who, boolean ok, String note) {}

  private static final List<StepDef> STEPS_PL =
      List.of(
          new StepDef(1, "Plik"),
          new StepDef(2, "Mapowanie kolumn"),
          new StepDef(3, "Reguły i segmenty"),
          new StepDef(4, "Podgląd i start"));

  private static final List<Target> TARGETS_PL =
      List.of(
          new Target("", "— wybierz pole —"),
          new Target("email", "Email (klucz dedup.)"),
          new Target("first_name", "Imię"),
          new Target("last_name", "Nazwisko"),
          new Target("phone", "Telefon"),
          new Target("created_at", "Data utworzenia"),
          new Target("opt_in", "Zgoda marketingowa"),
          new Target("orders_count", "Liczba zamówień"),
          new Target("lifetime_value", "Wartość życiowa (LTV)"),
          new Target("locale", "Język"),
          new Target("segments", "Segmenty (lista)"),
          new Target("tags", "Tagi (lista)"),
          new Target("__custom", "Atrybut własny…"),
          new Target("__skip", "Pomiń kolumnę"));

  private static final List<RawColumn> COLUMNS_PL =
      List.of(
          new RawColumn(
              "Adres e-mail",
              List.of("hania.k@aurea.pl", "marta.w@example.com", "tomek.s@gmail.com"),
              "email",
              99),
          new RawColumn("Imię", List.of("Hania", "Marta", "Tomek"), "first_name", 98),
          new RawColumn("Nazwisko", List.of("Kowalska", "Wiśniewska", "Sobczak"), "last_name", 97),
          new RawColumn("Telefon", List.of("+48 600 100 200", "+48 502 311 882", "—"), "phone", 95),
          new RawColumn(
              "Data rejestracji",
              List.of("2024-01-14", "2024-08-22", "2025-02-03"),
              "created_at",
              92),
          new RawColumn("Status zgody mkt.", List.of("Tak", "Tak", "Nie"), "opt_in", 88),
          new RawColumn("Liczba zamówień", List.of("7", "2", "0"), "orders_count", 90),
          new RawColumn(
              "Łączna wartość PLN", List.of("1248.00", "384.00", "0"), "lifetime_value", 85),
          new RawColumn("Język", List.of("pl", "pl", "en"), "locale", 80),
          new RawColumn("Tag CRM", List.of("vip,herbata", "newsletter", "—"), "__custom", 42),
          new RawColumn("Identyfikator Shoper", List.of("83128", "83410", "84001"), "__skip", 0));

  private static final List<ValidationCheck> VALIDATIONS_PL =
      List.of(
          new ValidationCheck("Format email (RFC 5322)", "ok"),
          new ValidationCheck(
              "Format daty (auto-wykrycie: <span class=\"mono\">YYYY-MM-DD</span>)", "ok"),
          new ValidationCheck("Numer telefonu (E.164 + heurystyka PL)", "ok"),
          new ValidationCheck("Konwersja zgody marketingowej (Tak/Nie → bool)", "ok"),
          new ValidationCheck("Konwersja waluty (kropka/przecinek → liczba)", "ok"),
          new ValidationCheck("Wykryte 4 duplikaty po email — patrz krok 3", "info"));

  private static final List<DedupStrategy> DEDUP_STRATEGIES_PL =
      List.of(
          new DedupStrategy("merge", "Nadpisz polami z pliku, gdy nie są puste (zalecane)", true),
          new DedupStrategy(
              "fill", "Wypełnij tylko brakujące pola — nie nadpisuj istniejących", false),
          new DedupStrategy("skip", "Pomiń duplikat — nie zmieniaj nic", false),
          new DedupStrategy("replace", "Zastąp wszystkie pola pełną zawartością z pliku", false));

  private static final List<RawPreviewRow> PREVIEW_ROWS_PL =
      List.of(
          new RawPreviewRow(
              "hania.k@aurea.pl",
              "Hania Kowalska",
              "Tak",
              "7",
              1248d,
              "new",
              List.of("Newsletter", "VIP", "Import — marzec 2026"),
              null),
          new RawPreviewRow(
              "marta.w@example.com",
              "Marta Wiśniewska",
              "Tak",
              "2",
              384d,
              "update",
              List.of("Newsletter", "Import — marzec 2026"),
              null),
          new RawPreviewRow(
              "tomek.s@gmail.com",
              "Tomek Sobczak",
              "Nie",
              "0",
              0d,
              "new",
              List.of("Newsletter", "Do reaktywacji", "Import — marzec 2026"),
              null),
          new RawPreviewRow(
              "olek.b@example.pl",
              "Olek Borowski",
              "Tak",
              "14",
              3490d,
              "update",
              List.of("Newsletter", "VIP", "Import — marzec 2026"),
              null),
          new RawPreviewRow(
              "invalid@@@bad",
              "(nieznane)",
              "?",
              "?",
              null,
              "error",
              List.of(),
              "Email nie przechodzi walidacji"),
          new RawPreviewRow(
              "iga.p@example.com",
              "Iga Pszczółkowska",
              "Tak",
              "0",
              0d,
              "new",
              List.of("Newsletter", "Do reaktywacji", "Import — marzec 2026"),
              null));

  private static final List<RecentImport> RECENT_IMPORTS_PL =
      List.of(
          new RecentImport("newsletter-2026-feb.csv", 4280, "14 lut 2026", "Maciej K.", true, "OK"),
          new RecentImport("klienci-shoper.xml", 1284, "02 sty 2026", "Anna B.", true, "OK"),
          new RecentImport(
              "mailerlite-archiwum.csv", 8120, "18 gru 2025", "Maciej K.", false, "12 błędów"));

  private static final List<StepDef> STEPS_EN =
      List.of(
          new StepDef(1, "File"),
          new StepDef(2, "Column mapping"),
          new StepDef(3, "Rules and segments"),
          new StepDef(4, "Preview and start"));

  private static final List<Target> TARGETS_EN =
      List.of(
          new Target("", "— choose a field —"),
          new Target("email", "Email (dedup key)"),
          new Target("first_name", "First name"),
          new Target("last_name", "Last name"),
          new Target("phone", "Phone"),
          new Target("created_at", "Created at"),
          new Target("opt_in", "Marketing consent"),
          new Target("orders_count", "Order count"),
          new Target("lifetime_value", "Lifetime value (LTV)"),
          new Target("locale", "Language"),
          new Target("segments", "Segments (list)"),
          new Target("tags", "Tags (list)"),
          new Target("__custom", "Custom attribute…"),
          new Target("__skip", "Skip this column"));

  private static final List<RawColumn> COLUMNS_EN =
      List.of(
          new RawColumn(
              "E-mail address",
              List.of("hannah.k@aurea.pl", "martha.w@example.com", "tom.s@gmail.com"),
              "email",
              99),
          new RawColumn("First name", List.of("Hannah", "Martha", "Tom"), "first_name", 98),
          new RawColumn("Last name", List.of("Brooks", "Whitfield", "Sanders"), "last_name", 97),
          new RawColumn("Phone", List.of("+44 7700 900123", "+44 7700 900456", "—"), "phone", 95),
          new RawColumn(
              "Sign-up date", List.of("2024-01-14", "2024-08-22", "2025-02-03"), "created_at", 92),
          new RawColumn("Consent status", List.of("Yes", "Yes", "No"), "opt_in", 88),
          new RawColumn("Order count", List.of("7", "2", "0"), "orders_count", 90),
          new RawColumn("Total value EUR", List.of("1248.00", "384.00", "0"), "lifetime_value", 85),
          new RawColumn("Language", List.of("pl", "pl", "en"), "locale", 80),
          new RawColumn("CRM tag", List.of("vip,tea", "newsletter", "—"), "__custom", 42),
          new RawColumn("Shoper id", List.of("83128", "83410", "84001"), "__skip", 0));

  private static final List<ValidationCheck> VALIDATIONS_EN =
      List.of(
          new ValidationCheck("E-mail format (RFC 5322)", "ok"),
          new ValidationCheck(
              "Date format (detected automatically: <span class=\"mono\">YYYY-MM-DD</span>)", "ok"),
          new ValidationCheck("Phone number (E.164 plus a Polish heuristic)", "ok"),
          new ValidationCheck("Marketing consent converted (Yes/No → bool)", "ok"),
          new ValidationCheck("Currency converted (full stop or comma → number)", "ok"),
          new ValidationCheck("Found 4 duplicates by e-mail — see step 3", "info"));

  private static final List<DedupStrategy> DEDUP_STRATEGIES_EN =
      List.of(
          new DedupStrategy(
              "merge",
              "Overwrite with the file's fields where they are not empty (recommended)",
              true),
          new DedupStrategy(
              "fill", "Fill in only the missing fields — never overwrite an existing one", false),
          new DedupStrategy("skip", "Skip the duplicate — change nothing", false),
          new DedupStrategy("replace", "Replace every field with what the file holds", false));

  private static final List<RawPreviewRow> PREVIEW_ROWS_EN =
      List.of(
          new RawPreviewRow(
              "hannah.k@aurea.pl",
              "Hannah Brooks",
              "Yes",
              "7",
              1248d,
              "new",
              List.of("Newsletter", "VIP", "Import — March 2026"),
              null),
          new RawPreviewRow(
              "martha.w@example.com",
              "Martha Whitfield",
              "Yes",
              "2",
              384d,
              "update",
              List.of("Newsletter", "Import — March 2026"),
              null),
          new RawPreviewRow(
              "tom.s@gmail.com",
              "Tom Sanders",
              "No",
              "0",
              0d,
              "new",
              List.of("Newsletter", "To win back", "Import — March 2026"),
              null),
          new RawPreviewRow(
              "ollie.b@example.com",
              "Ollie Bradley",
              "Yes",
              "14",
              3490d,
              "update",
              List.of("Newsletter", "VIP", "Import — March 2026"),
              null),
          new RawPreviewRow(
              "invalid@@@bad",
              "(unknown)",
              "?",
              "?",
              null,
              "error",
              List.of(),
              "The e-mail does not pass validation"),
          new RawPreviewRow(
              "ivy.p@example.com",
              "Ivy Pemberton",
              "Yes",
              "0",
              0d,
              "new",
              List.of("Newsletter", "To win back", "Import — March 2026"),
              null));

  private static final List<RecentImport> RECENT_IMPORTS_EN =
      List.of(
          new RecentImport(
              "newsletter-2026-feb.csv", 4280, "14 Feb 2026", "Michael K.", true, "OK"),
          new RecentImport("customers-shoper.xml", 1284, "02 Jan 2026", "Amy B.", true, "OK"),
          new RecentImport(
              "mailerlite-archive.csv", 8120, "18 Dec 2025", "Michael K.", false, "12 errors"));

  private ImportFixtures() {}

  public static boolean isValidStep(int step) {
    return step >= FIRST_STEP && step <= LAST_STEP;
  }

  /** The name of the file the demonstration import walks through. */
  public static String defaultFileName(SupportedLocale locale) {
    return switch (locale) {
      case PL -> DEFAULT_FILE_NAME_PL;
      case EN -> DEFAULT_FILE_NAME_EN;
    };
  }

  public static String fileMeta(SupportedLocale locale) {
    return switch (locale) {
      case PL -> FILE_META_PL;
      case EN -> FILE_META_EN;
    };
  }

  public static List<StepDef> steps(SupportedLocale locale) {
    return switch (locale) {
      case PL -> STEPS_PL;
      case EN -> STEPS_EN;
    };
  }

  public static List<Target> targets(SupportedLocale locale) {
    return switch (locale) {
      case PL -> TARGETS_PL;
      case EN -> TARGETS_EN;
    };
  }

  public static List<RawColumn> rawColumns(SupportedLocale locale) {
    return switch (locale) {
      case PL -> COLUMNS_PL;
      case EN -> COLUMNS_EN;
    };
  }

  public static List<ValidationCheck> validations(SupportedLocale locale) {
    return switch (locale) {
      case PL -> VALIDATIONS_PL;
      case EN -> VALIDATIONS_EN;
    };
  }

  public static List<DedupStrategy> dedupStrategies(SupportedLocale locale) {
    return switch (locale) {
      case PL -> DEDUP_STRATEGIES_PL;
      case EN -> DEDUP_STRATEGIES_EN;
    };
  }

  public static List<RawPreviewRow> rawPreviewRows(SupportedLocale locale) {
    return switch (locale) {
      case PL -> PREVIEW_ROWS_PL;
      case EN -> PREVIEW_ROWS_EN;
    };
  }

  public static List<RecentImport> recentImports(SupportedLocale locale) {
    return switch (locale) {
      case PL -> RECENT_IMPORTS_PL;
      case EN -> RECENT_IMPORTS_EN;
    };
  }

  public static int rowCount() {
    return ROW_COUNT;
  }

  public static int errorCount() {
    return ERROR_COUNT;
  }
}
