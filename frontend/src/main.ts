import { createPinia } from "pinia";
import { createApp } from "vue";
import App from "./App.vue";
import { i18n } from "./i18n";
import { router } from "./router";
// PIO-118: the @font-face declarations that replaced the Google Fonts <link> in index.html.
// Imported here rather than added to styles/app.css because src/styles/ is the design system
// copied byte-for-byte from the original handoff (see frontend/.prettierignore) and stays that
// way; the families these declarations define are the ones 01-tokens.css names in --font-sans,
// --font-mono and --font-display.
import "./assets/fonts/fonts.css";
import "./styles/app.css";

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(i18n);

app.mount("#app");
