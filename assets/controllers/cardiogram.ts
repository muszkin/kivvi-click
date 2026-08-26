/**
 * Cardiogram — events/second canvas.
 *
 * Why canvas: the series updates every second and would thrash the DOM as SVG.
 * Colours are READ FROM COMPUTED CUSTOM PROPERTIES, so the chart re-themes
 * for free — but that also means it must be redrawn when [data-theme] changes.
 */

const BUCKETS = 60;

export function registerCardiogram(canvas: HTMLElement): void {
    if (!(canvas instanceof HTMLCanvasElement)) return;
    if (canvas.dataset.mounted) return;
    canvas.dataset.mounted = "true";

    const buckets: number[] = new Array(BUCKETS).fill(0);
    let eventsThisSecond = 0;

    const draw = (): void => {
        const dpr = window.devicePixelRatio || 1;
        const w = canvas.clientWidth;
        const h = canvas.clientHeight;
        canvas.width = w * dpr;
        canvas.height = h * dpr;

        const ctx = canvas.getContext("2d");
        if (!ctx) return;
        ctx.scale(dpr, dpr);
        ctx.clearRect(0, 0, w, h);

        const cs = getComputedStyle(document.documentElement);
        const accent = cs.getPropertyValue("--accent").trim();
        const accentSoft = cs.getPropertyValue("--accent-soft").trim();
        const line = cs.getPropertyValue("--line").trim();
        const muted = cs.getPropertyValue("--fg-subtle").trim();

        const pad = 12;
        const max = Math.max(8, ...buckets) * 1.2;
        const xStep = (w - pad * 2) / (BUCKETS - 1);
        const yOf = (v: number): number => h - pad - (v / max) * (h - pad * 2);

        ctx.strokeStyle = line;
        ctx.lineWidth = 1;
        for (let i = 0; i < 4; i++) {
            const y = pad + ((h - pad * 2) / 3) * i;
            ctx.beginPath();
            ctx.moveTo(pad, y);
            ctx.lineTo(w - pad, y);
            ctx.stroke();
        }

        ctx.beginPath();
        ctx.moveTo(pad, h - pad);
        buckets.forEach((v, i) => ctx.lineTo(pad + i * xStep, yOf(v)));
        ctx.lineTo(w - pad, h - pad);
        ctx.closePath();
        ctx.fillStyle = accentSoft;
        ctx.fill();

        ctx.beginPath();
        buckets.forEach((v, i) =>
            i === 0
                ? ctx.moveTo(pad, yOf(v))
                : ctx.lineTo(pad + i * xStep, yOf(v)),
        );
        ctx.strokeStyle = accent;
        ctx.lineWidth = 2;
        ctx.stroke();

        const last = buckets[BUCKETS - 1] ?? 0;
        ctx.fillStyle = accent;
        ctx.beginPath();
        ctx.arc(pad + (BUCKETS - 1) * xStep, yOf(last), 4, 0, Math.PI * 2);
        ctx.fill();

        ctx.font = '500 11px "Geist Mono", monospace';
        ctx.fillStyle = muted;
        ctx.textAlign = "right";
        ctx.fillText(String(last) + " ev/s", w - pad - 8, pad + 14);
    };

    // Count events pushed by the Mercure stream.
    document.addEventListener("kivvi:event", () => {
        eventsThisSecond++;
    });

    window.setInterval(() => {
        buckets.push(eventsThisSecond);
        buckets.shift();
        eventsThisSecond = 0;
        draw();
    }, 1000);

    new MutationObserver(draw).observe(document.documentElement, {
        attributes: true,
        attributeFilter: ["data-theme", "style"],
    });
    window.addEventListener("resize", draw);
    draw();
}
