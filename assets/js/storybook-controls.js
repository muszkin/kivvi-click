/**
 * Storybook chrome controls — theme and accent switching.
 * Plain JS, no build step: the storybook must run even if the asset
 * pipeline is broken, because that is exactly when you need it.
 */
(function () {
    "use strict";

    var ACCENTS = {
        green: {
            light: [
                "oklch(0.42 0.06 150)",
                "oklch(0.37 0.065 150)",
                "oklch(0.985 0.008 85)",
                "oklch(0.90 0.04 150)",
                "oklch(0.32 0.06 150)",
            ],
            dark: [
                "oklch(0.78 0.09 150)",
                "oklch(0.84 0.10 150)",
                "oklch(0.18 0.04 150)",
                "oklch(0.32 0.05 150)",
                "oklch(0.88 0.08 150)",
            ],
        },
        brown: {
            light: [
                "oklch(0.45 0.07 60)",
                "oklch(0.40 0.075 60)",
                "oklch(0.985 0.008 85)",
                "oklch(0.90 0.04 65)",
                "oklch(0.34 0.07 60)",
            ],
            dark: [
                "oklch(0.78 0.08 60)",
                "oklch(0.84 0.09 60)",
                "oklch(0.18 0.04 60)",
                "oklch(0.32 0.05 60)",
                "oklch(0.88 0.08 60)",
            ],
        },
        rust: {
            light: [
                "oklch(0.52 0.12 32)",
                "oklch(0.47 0.13 32)",
                "oklch(0.985 0.008 85)",
                "oklch(0.90 0.05 32)",
                "oklch(0.36 0.12 32)",
            ],
            dark: [
                "oklch(0.78 0.12 32)",
                "oklch(0.83 0.13 32)",
                "oklch(0.18 0.05 32)",
                "oklch(0.32 0.06 32)",
                "oklch(0.88 0.10 32)",
            ],
        },
        plum: {
            light: [
                "oklch(0.40 0.08 320)",
                "oklch(0.35 0.085 320)",
                "oklch(0.985 0.008 85)",
                "oklch(0.90 0.05 320)",
                "oklch(0.32 0.08 320)",
            ],
            dark: [
                "oklch(0.78 0.10 320)",
                "oklch(0.84 0.11 320)",
                "oklch(0.20 0.05 320)",
                "oklch(0.32 0.06 320)",
                "oklch(0.88 0.10 320)",
            ],
        },
    };

    var VARS = [
        "--accent",
        "--accent-hover",
        "--accent-fg",
        "--accent-soft",
        "--accent-soft-fg",
    ];
    var state = { theme: "light", accent: "green" };

    function applyAccent() {
        var palette = ACCENTS[state.accent] || ACCENTS.green;
        var colors = state.theme === "dark" ? palette.dark : palette.light;
        VARS.forEach(function (name, i) {
            document.documentElement.style.setProperty(name, colors[i]);
        });
    }

    function setTheme(theme) {
        state.theme = theme;
        document.documentElement.setAttribute("data-theme", theme);
        applyAccent();
        mark("[data-sb-theme]", "sbTheme", theme);
    }

    function setAccent(accent) {
        state.accent = accent;
        applyAccent();
        mark("[data-sb-accent]", "sbAccent", accent);
    }

    function mark(selector, key, value) {
        document.querySelectorAll(selector).forEach(function (btn) {
            btn.setAttribute("data-active", String(btn.dataset[key] === value));
        });
    }

    document.addEventListener("click", function (ev) {
        var themeBtn = ev.target.closest("[data-sb-theme]");
        if (themeBtn) {
            setTheme(themeBtn.dataset.sbTheme);
            return;
        }
        var accentBtn = ev.target.closest("[data-sb-accent]");
        if (accentBtn) {
            setAccent(accentBtn.dataset.sbAccent);
        }
    });

    applyAccent();
    window.KivviStorybookControls = {
        setTheme: setTheme,
        setAccent: setAccent,
        state: state,
    };
})();
