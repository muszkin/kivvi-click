/**
 * The landing page's contact form — "let's talk about your deployment" — as a link target.
 *
 * PIO-125 pointed the login page's "No account?" line here, because there is no registration to
 * send anyone to: an account on kivvi·click is something you get by deploying it. The id lives on
 * the form in LandingView.vue, and LandingView scrolls to it once the page has rendered, since the
 * browser's own jump to a fragment happens before the landing content exists.
 */
export const WAITLIST_FORM_ANCHOR = "waitlist";

export function waitlistFormHref(locale: string): string {
    return `/${locale}#${WAITLIST_FORM_ANCHOR}`;
}
