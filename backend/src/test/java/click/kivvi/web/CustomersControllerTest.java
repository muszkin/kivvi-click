package click.kivvi.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.CustomersViewService;
import click.kivvi.application.SpaDocumentService;
import click.kivvi.infrastructure.IndexHtmlTemplate;
import click.kivvi.infrastructure.SessionPreferencesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sliced (no full application context) coverage for the wire shape of {@code GET
 * /api/v1/{locale}/customers[/{id}]} and the {@code /{locale}/customers/{id}} document route.
 * Neither {@link CustomersViewService} nor {@link SpaDocumentService} needs a database, so both are
 * imported as real beans rather than mocked.
 */
@WebMvcTest(CustomersController.class)
@Import({
  CustomersViewService.class,
  SpaDocumentService.class,
  IndexHtmlTemplate.class,
  SessionPreferencesStore.class
})
class CustomersControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("B25 GET /api/v1/pl/customers returns 24 rows and the pager fields")
  void listPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/customers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customersRows.length()").value(24))
        .andExpect(jsonPath("$.segments.length()").value(6))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.pages").value(192))
        .andExpect(jsonPath("$.customersRows[0].name").value("Anna K."))
        .andExpect(jsonPath("$.customersRows[0].revenue").value("49 zł"));
  }

  @Test
  @DisplayName("B25 ?page= only moves the pager; the same 24 rows render on every page")
  void pageOnlyMovesThePager() throws Exception {
    mvc.perform(get("/api/v1/pl/customers").param("page", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.customersRows.length()").value(24))
        .andExpect(jsonPath("$.customersRows[0].name").value("Anna K."));
  }

  @Test
  @DisplayName("B25 GET /api/v1/pl/customers/{id} returns the 360 profile shape")
  void detailPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/customers/c_1000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customer.name").value("Anna K."))
        .andExpect(jsonPath("$.customer.tags.length()").value(3))
        .andExpect(jsonPath("$.facts.length()").value(7))
        .andExpect(jsonPath("$.automations.length()").value(3))
        .andExpect(jsonPath("$.tabs.length()").value(5))
        .andExpect(jsonPath("$.scores.length()").value(3))
        .andExpect(jsonPath("$.timeline.length()").value(9));
  }

  @Test
  @DisplayName("B05 GET /api/v1/pl/customers/c_9999 is not found")
  void unknownCustomerApiIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/pl/customers/c_9999")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/customers is not found")
  void unsupportedLocaleApiIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/de/customers")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("B25 GET /pl/customers/{id} renders the SPA document for a known customer")
  void knownCustomerDocumentRenders200() throws Exception {
    mvc.perform(get("/pl/customers/c_1000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<html")));
  }

  @Test
  @DisplayName("B05/DEV-12 GET /pl/customers/c_9999 is a 404 document, not the 200 SPA shell")
  void unknownCustomerDocumentIsNotFound() throws Exception {
    mvc.perform(get("/pl/customers/c_9999")).andExpect(status().isNotFound());
  }
}
