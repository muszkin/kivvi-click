import { existsSync, readFileSync, readdirSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

/**
 * PIO-118 — the typefaces are served from this origin, and stay that way.
 *
 * index.html used to carry two `preconnect` hints and a `<link rel="stylesheet">` to Google's
 * font CDN. Every visitor's browser therefore connected to fonts.googleapis.com and
 * fonts.gstatic.com and disclosed its IP address to Google on every page view, whether or not
 * anyone ever used the form — a transfer of personal data outside the EEA, and the one thing
 * `privacy.{pl,en}.ts` had to disclose. Deleting those three lines is a one-line change that a
 * later "just add a webfont" would undo just as easily, and nothing else in the build would
 * notice, so this file is the thing that notices.
 *
 * It asserts three separable properties:
 *
 *   1. index.html references no outside host at all — not only the two Google ones.
 *   2. every @font-face resolves to a .woff2 committed in this repository, with the OFL that
 *      permits us to host it sitting next to the files.
 *   3. the unicode-ranges cover Polish. This is the one that would otherwise fail in
 *      production rather than here: `ó`/`Ó` come from the `latin` subset and `ą ć ę ł ń ś ź ż`
 *      from `latin-ext`, so dropping the latin-ext files leaves a page that looks perfectly
 *      fine in English and renders half of every Polish word in the fallback face.
 *
 * The companion assertion against a real browser and a running stack — that loading /pl and /en
 * issues no request off-host — lives in tests/e2e/specs/public.spec.ts.
 */

// Resolved from the Vitest root (frontend/) the same way consentClause.spec.ts does it, rather
// than through import.meta.url, which Vite refuses to resolve outside its own fs root.
const FONT_DIR = resolve(process.cwd(), "src/assets/fonts");

const indexHtml = readFileSync(resolve(process.cwd(), "index.html"), "utf8");
const fontsCss = readFileSync(resolve(FONT_DIR, "fonts.css"), "utf8");

const POLISH_LOWER = "ąćęłńóśźż";
const POLISH_UPPER = "ĄĆĘŁŃÓŚŹŻ";
const POLISH = [...POLISH_LOWER, ...POLISH_UPPER];

interface FontFace {
    family: string;
    style: string;
    weight: string;
    display: string;
    urls: string[];
    formats: string[];
    codepoints: Set<number>;
}

function declarationValue(block: string, property: string): string {
    const match = block.match(
        new RegExp(`(?:^|[;{\\s])${property}\\s*:\\s*([^;]+);`),
    );
    if (match === null) {
        throw new Error(`@font-face block declares no ${property}:\n${block}`);
    }
    return match[1].replace(/\s+/g, " ").trim();
}

/**
 * Expands one `unicode-range` value into the code points it covers. Deliberately strict: the
 * `U+2??` wildcard form is legal CSS but nothing here uses it, and silently ignoring a form this
 * parser does not understand is how a coverage test comes to pass while covering nothing.
 */
function codepointsOf(unicodeRange: string): Set<number> {
    const covered = new Set<number>();
    for (const part of unicodeRange.split(",")) {
        const token = part.trim();
        const single = token.match(/^U\+([0-9A-Fa-f]{1,6})$/);
        const span = token.match(/^U\+([0-9A-Fa-f]{1,6})-([0-9A-Fa-f]{1,6})$/);
        if (single !== null) {
            covered.add(parseInt(single[1], 16));
            continue;
        }
        if (span === null) {
            throw new Error(`unrecognised unicode-range token: "${token}"`);
        }
        const [from, to] = [parseInt(span[1], 16), parseInt(span[2], 16)];
        for (let point = from; point <= to; point += 1) {
            covered.add(point);
        }
    }
    return covered;
}

function parseFontFaces(css: string): FontFace[] {
    const faces: FontFace[] = [];
    for (const match of css.matchAll(/@font-face\s*\{([^}]*)\}/g)) {
        const block = match[1];
        const src = declarationValue(block, "src");
        faces.push({
            family: declarationValue(block, "font-family").replace(/["']/g, ""),
            style: declarationValue(block, "font-style"),
            weight: declarationValue(block, "font-weight"),
            display: declarationValue(block, "font-display"),
            urls: [...src.matchAll(/url\(\s*["']?([^"')]+)["']?\s*\)/g)].map(
                (url) => url[1],
            ),
            formats: [
                ...src.matchAll(/format\(\s*["']?([^"')]+)["']?\s*\)/g),
            ].map((format) => format[1]),
            codepoints: codepointsOf(declarationValue(block, "unicode-range")),
        });
    }
    return faces;
}

const faces = parseFontFaces(fontsCss);

function facesOf(family: string, style: string): FontFace[] {
    return faces.filter(
        (face) => face.family === family && face.style === style,
    );
}

describe("PIO-118 index.html reaches no host but our own", () => {
    it("carries no reference to Google's font CDN", () => {
        expect(indexHtml).not.toContain("fonts.googleapis.com");
        expect(indexHtml).not.toContain("fonts.gstatic.com");
    });

    it("carries no absolute URL to any outside host, and no preconnect at all", () => {
        // Broader than the assertion above on purpose: the ticket's acceptance criterion is that
        // loading a page issues no request off-host, not that one particular vendor is gone.
        const absoluteUrls = indexHtml.match(/https?:\/\/[^"'\s>]+/g) ?? [];
        expect(absoluteUrls).toEqual([]);
        expect(indexHtml).not.toMatch(
            /rel\s*=\s*["']?(preconnect|dns-prefetch)/,
        );
    });

    it("still loads only the SPA entry point and an inline data: favicon", () => {
        // The favicon is a data: URI, so it costs no request either. Asserted so that replacing
        // it with a hosted file is a deliberate act that has to come here first.
        expect(indexHtml).toContain('href="data:image/svg+xml,');
        expect(indexHtml).toContain('src="/src/main.ts"');
    });
});

describe("PIO-118 the self-hosted @font-face declarations", () => {
    it("declares exactly the three families the design tokens name", () => {
        const tokens = readFileSync(
            resolve(process.cwd(), "src/styles/01-tokens.css"),
            "utf8",
        );
        const named = ["--font-sans", "--font-mono", "--font-display"].map(
            (token) => {
                const match = tokens.match(
                    new RegExp(`${token}:\\s*"([^"]+)"`),
                );
                if (match === null) {
                    throw new Error(`01-tokens.css declares no ${token}`);
                }
                return match[1];
            },
        );

        expect(named).toEqual(["Geist", "Geist Mono", "Instrument Serif"]);
        expect([...new Set(faces.map((face) => face.family))].sort()).toEqual(
            [...named].sort(),
        );
    });

    it("points every src at a woff2 committed in this repository", () => {
        expect(faces.length).toBeGreaterThan(0);
        for (const face of faces) {
            expect(face.urls).toHaveLength(1);
            expect(face.formats).toEqual(["woff2"]);

            const [url] = face.urls;
            expect(url).not.toMatch(/^(https?:)?\/\//);
            expect(url).toMatch(/^\.\/[a-z0-9-]+\.woff2$/);

            const file = resolve(FONT_DIR, url);
            expect(
                existsSync(file),
                `${url} is missing from src/assets/fonts`,
            ).toBe(true);
            // "wOF2" — a truncated or LFS-pointer-shaped file would otherwise pass every
            // assertion above and fail only in a browser.
            expect(readFileSync(file).subarray(0, 4).toString("latin1")).toBe(
                "wOF2",
            );
        }
    });

    it("ships no font file that nothing references", () => {
        const referenced = new Set(
            faces.map((face) => face.urls[0].replace("./", "")),
        );
        const onDisk = readdirSync(FONT_DIR).filter((name) =>
            name.endsWith(".woff2"),
        );

        expect([...onDisk].sort()).toEqual([...referenced].sort());
    });

    it("swaps rather than blocking paint while a face downloads", () => {
        for (const face of faces) {
            expect(face.display).toBe("swap");
        }
    });

    it("keeps the weight range index.html used to request from Google", () => {
        // The old link asked Google for Geist wght@300;400;500;600;700, Geist Mono
        // wght@400;500;600 and Instrument Serif ital@0;1. These variable files span the same
        // ranges, so every weight the design system already uses still resolves to a real
        // instance instead of being rounded to the nearest one shipped.
        const weightOf = (family: string, style: string) =>
            new Set(facesOf(family, style).map((face) => face.weight));

        expect(weightOf("Geist", "normal")).toEqual(new Set(["300 700"]));
        expect(weightOf("Geist Mono", "normal")).toEqual(new Set(["400 600"]));
        expect(weightOf("Instrument Serif", "normal")).toEqual(
            new Set(["400"]),
        );
        expect(weightOf("Instrument Serif", "italic")).toEqual(
            new Set(["400"]),
        );
    });
});

describe("PIO-118 Polish diacritics survive the subsetting", () => {
    const subsetted: ReadonlyArray<[string, string]> = [
        ["Geist", "normal"],
        ["Geist Mono", "normal"],
        ["Instrument Serif", "normal"],
        ["Instrument Serif", "italic"],
    ];

    it.each(subsetted)(
        "%s (%s) covers ąćęłńóśźż and their capitals across its subsets",
        (family, style) => {
            const covering = facesOf(family, style);
            expect(
                covering.length,
                `no @font-face for ${family} ${style}`,
            ).toBeGreaterThan(0);

            const covered = new Set<number>();
            for (const face of covering) {
                for (const point of face.codepoints) {
                    covered.add(point);
                }
            }

            const missing = POLISH.filter(
                (letter) => !covered.has(letter.codePointAt(0) as number),
            );
            expect(missing).toEqual([]);
        },
    );

    it("needs both subsets to do it, which is why neither may be dropped", () => {
        // Guards against the plausible "one file per family is tidier" edit: it would keep every
        // English page perfect and break every Polish one. `ó` is the only Polish accented letter
        // inside latin's U+0000-00FF; the other eight are only in latin-ext.
        const geistLatin = faces.find(
            (face) => face.urls[0] === "./geist-latin.woff2",
        );
        const geistLatinExt = faces.find(
            (face) => face.urls[0] === "./geist-latin-ext.woff2",
        );

        expect(geistLatin?.codepoints.has("ó".codePointAt(0) as number)).toBe(
            true,
        );
        expect(geistLatin?.codepoints.has("ł".codePointAt(0) as number)).toBe(
            false,
        );
        expect(
            geistLatinExt?.codepoints.has("ł".codePointAt(0) as number),
        ).toBe(true);
    });
});

describe("PIO-118 the licence travels with the files", () => {
    const licences: ReadonlyArray<[string, string]> = [
        ["Geist", "OFL-Geist.txt"],
        ["Geist Mono", "OFL-Geist-Mono.txt"],
        ["Instrument Serif", "OFL-Instrument-Serif.txt"],
    ];

    it.each(licences)(
        "%s ships the SIL Open Font License that permits self-hosting",
        (_family, file) => {
            const text = readFileSync(resolve(FONT_DIR, file), "utf8");

            expect(text).toContain("SIL OPEN FONT LICENSE Version 1.1");
            expect(text).toContain("PERMISSION & CONDITIONS");
            expect(text).toMatch(/^Copyright \d{4} The .+ Project Authors/);
        },
    );
});
