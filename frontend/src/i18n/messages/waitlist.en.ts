/**
 * English half of the waitlist confirmation pages (PIO-71). See waitlist.pl.ts for why the
 * top-level key is `waitlistPage` and why the unknown-link copy never reveals whether an address
 * is on the list.
 */
export default {
    waitlistPage: {
        confirm: {
            ok: {
                title: "Done — you are on the list",
                body: "Your address is confirmed. We will write exactly once: when kivvi·click launches.",
            },
            already: {
                title: "This address is already confirmed",
                body: "There is nothing else to do — you are on the list, waiting for a single launch notification.",
            },
            expired: {
                title: "This link has expired",
                body: "A confirmation link is good for 7 days. We will send a new one to the same address — just click below.",
                action: "Send me a new link",
            },
            unknown: {
                title: "We do not know this link",
                body: "Your mail client may have shortened or truncated it. Copy the whole link from the message, or sign up again on the home page.",
            },
            sent: {
                title: "Check your inbox",
                body: "If that address is waiting to be confirmed, a new link is on its way. Look in your spam folder too — a first message from a new domain often lands there.",
            },
        },
        unsubscribe: {
            ok: {
                title: "You are unsubscribed",
                body: "Your address is off the waiting list and we will send you nothing. If you change your mind, you can sign up again on the home page.",
            },
            unknown: {
                title: "We do not know this link",
                body: "Your mail client may have truncated it. Copy the whole link from the message, and if that does not help, write to piotr{'@'}kivvi.click and we will remove the address by hand.",
            },
        },
        backHome: "Back to the home page",
    },
};
