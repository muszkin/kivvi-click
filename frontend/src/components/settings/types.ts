// Shared shapes for the settings tabs' payload — the `settings` object GET
// /api/v1/{locale}/settings/{tab} always returns in full, mirroring SettingsResponse.Settings on
// the backend. Every field arrives pre-formatted (SettingsFixtures/SettingsController): no tab
// component ever formats a number itself.

export interface TrackedSite {
    name: string;
    color: string;
    events: string;
    key: string;
}

export interface TeamMember {
    name: string;
    email: string;
    role: string;
    roleTone: "accent" | "brown" | "neutral";
    last: string;
    invited: boolean;
    mfa: boolean;
}

export interface SettingsRole {
    name: string;
    description: string;
    count: number;
}

export interface EmailProvider {
    name: string;
    region: string;
    statusTone: "good" | "info" | "neutral";
    statusLabel: string;
    sent: string;
    bounce: string;
    bounceWarn: boolean;
    complaint: string;
    verified: boolean;
}

export interface DnsRecordData {
    record: string;
    value: string;
    ok: boolean;
}

export interface ApiKeyData {
    name: string;
    prefix: string;
    created: string;
    last: string;
    scopes: string[];
}

export interface WebhookData {
    url: string;
    events: string[];
    code: number;
    last: string;
}

export interface SettingsBar {
    label: string;
    pct: number;
    value: string;
    tone: "accent" | "brown" | "bad";
}

export interface NotificationRowData {
    label: string;
    email: boolean;
    slack: boolean;
    sms: boolean;
}

export interface InvoiceData {
    number: string;
    date: string;
    amount: string;
    status: string;
}

export interface DataSubjectRequestData {
    id: string;
    person: string;
    type: string;
    status: string;
    done: boolean;
    due: string;
}

export interface RetentionPolicyData {
    label: string;
    options: string[];
    selected: string;
}

export interface SettingsData {
    trackedSites: TrackedSite[];
    automaticEvents: string[];
    team: TeamMember[];
    roles: SettingsRole[];
    emailProviders: EmailProvider[];
    dnsRecords: DnsRecordData[];
    apiKeys: ApiKeyData[];
    webhooks: WebhookData[];
    apiLimits: SettingsBar[];
    notificationMatrix: NotificationRowData[];
    planUsage: SettingsBar[];
    invoices: InvoiceData[];
    dataSubjectRequests: DataSubjectRequestData[];
    retentionPolicies: RetentionPolicyData[];
}
