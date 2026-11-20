/* Page rendering and interactions for the session-based student frontend. */
(function () {
    const api = window.ScholarAssistApi;
    const pageName = document.body.dataset.page;
    const app = document.getElementById("app");
    let currentStudentProfile = null;

    const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[character]);
    const dateText = (value) => value ? new Date(`${String(value).slice(0, 10)}T00:00:00`).toLocaleDateString(window.ScholarAssistI18n?.locale() || "en-IN") : "Not specified";
    const moneyText = (value) => value === null || value === undefined ? "Not specified" : `₹${Number(value).toLocaleString(window.ScholarAssistI18n?.locale() || "en-IN")}`;
    const localDateToday = () => {
        const today = new Date();
        const month = String(today.getMonth() + 1).padStart(2, "0");
        const day = String(today.getDate()).padStart(2, "0");
        return `${today.getFullYear()}-${month}-${day}`;
    };
    const authenticated = () => sessionStorage.getItem("scholarAssistAuthenticated") === "true";
    const isReviewer = () => ["REVIEWER", "ADMIN"].includes(sessionStorage.getItem("scholarAssistRole"));
    const studentId = () => sessionStorage.getItem("scholarAssistStudentId") || "";
    const userId = () => sessionStorage.getItem("scholarAssistUserId") || "";
    const displayName = () => currentStudentProfile?.fullName
        || sessionStorage.getItem("scholarAssistDisplayName")
        || sessionStorage.getItem("scholarAssistEmail")
        || "";
    const initials = (name) => {
        const words = String(name || "").trim().split(/[\s@._-]+/).filter(Boolean);
        return (words.length > 1 ? words[0][0] + words[1][0] : (words[0] || "S").slice(0, 2)).toUpperCase();
    };
    const toast = (message) => {
        const element = document.querySelector(".toast");
        if (!element) return;
        element.textContent = message;
        element.classList.add("show");
        window.setTimeout(() => element.classList.remove("show"), 3200);
    };
    const showError = (container, error) => {
        const fields = Object.entries(error.fields || {}).map(([name, message]) => `${name}: ${message}`);
        container.innerHTML = `<div class="api-message api-error" role="alert">${escapeHtml(error.message)}${fields.length ? `<ul>${fields.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ul>` : ""}</div>`;
    };
    const loading = `<div class="api-message" role="status">Loading…</div>`;
    const empty = (message) => `<div class="api-message">${escapeHtml(message)}</div>`;
    const needId = (which) => empty(which === "user" ? "Sign in to view your notifications." : "Sign in and create your student profile to use this feature.");
    const idsPresent = (which) => which === "user" ? Boolean(userId()) : Boolean(studentId());
    const statusClass = (status) => ({
        ELIGIBLE: "eligible", POSSIBLY_ELIGIBLE: "possible", NOT_ELIGIBLE: "ineligible",
        PRELIMINARY_MATCH: "eligible", MORE_INFORMATION_REQUIRED: "possible", DOES_NOT_MATCH: "ineligible",
        DRAFT: "blue-badge", SUBMITTED: "possible", UNDER_REVIEW: "possible",
        APPROVED: "eligible", REJECTED: "ineligible", PENDING: "possible", VERIFIED: "eligible",
        EXTERNAL_STARTED: "possible", APPLIED: "possible", NEEDS_INFORMATION: "possible",
        CANCELLED: "neutral"
    })[String(status || "").toUpperCase()] || "blue-badge";
    const badge = (value) => `<span class="status-badge ${statusClass(value)}">${escapeHtml(value || "Unknown")}</span>`;
    const factLabel = (value) => String(value || "").replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
    const dateTimeText = (value) => value ? new Date(value).toLocaleString() : "Not recorded";
    const historyActor = (entry) => entry.changeSource === "MIGRATION"
        ? "System · initial status recorded when history tracking began"
        : entry.changeSource === "REVIEWER" ? "Reviewer" : "Student";
    const historyMarkup = (entry) => `<li><div>${badge(entry.newStatus)} <small>${escapeHtml(historyActor(entry))}</small></div><p>${escapeHtml(entry.note || "Status updated")}</p><time>${escapeHtml(dateTimeText(entry.changedAt))}</time></li>`;

    const nav = [
        ["⌂", "Dashboard", "dashboard"], ["⌕", "Scholarships", "scholarships"],
        ["▦", "Deadline calendar", "deadlines"],
        ["▤", "Documents", "documents"],
        ["▥", "Applications", "applications"], ["◷", "Notifications", "notifications"],
        ["◎", "Profile", "profile"]
    ];
    const reviewerNav = [
        ["▣", "Application review", "reviewer-applications"],
        ["✓", "Document review", "reviewer-documents"]
    ];

    function shell(title, subtitle, body) {
        app.innerHTML = `
            <header class="portal-header">
                <a class="skip-link" href="#page-content">Skip to main content</a>
                <button class="menu-toggle mobile-only sidebar-toggle" aria-label="Open workspace navigation" aria-controls="workspace-navigation" aria-expanded="false"><span aria-hidden="true">☰</span></button>
                <a class="brand" href="../index.html"><span class="brand-mark">S</span> ScholarAssist</a>
                <div class="portal-user"><span class="user-avatar" id="session-avatar" aria-hidden="true">D</span><span class="portal-identity"><span id="session-user">Guest</span><small id="session-role"></small></span><span id="auth-action"></span></div>
            </header>
            <div class="portal-layout">
                <aside class="sidebar" id="workspace-navigation" aria-label="Workspace navigation"><div class="sidebar-label">WORKSPACE</div>
                    ${(isReviewer() ? reviewerNav : nav).map(([icon, label, key]) => `<a class="side-link ${pageName === key ? "active" : ""}" href="${key}.html"${pageName === key ? ' aria-current="page"' : ""}><span class="side-icon" aria-hidden="true">${icon}</span>${label}${key === "notifications" ? '<span class="status-badge possible" id="unread-notification-count" hidden></span>' : ""}</a>`).join("")}
                    <div class="sidebar-bottom"><a class="side-link" href="../index.html"><span class="side-icon" aria-hidden="true">↪</span>Back to home</a></div>
                </aside>
                <main class="portal-main" id="page-content" tabindex="-1">
                    <div class="page-heading"><div><h1>${escapeHtml(title)}</h1><p>${escapeHtml(subtitle)}</p></div></div>
                    <div id="page-content">${body}</div>
                </main>
            </div>
            <div class="toast" role="status" aria-live="polite"></div>`;
        const isSignedIn = authenticated();
        const role = String(sessionStorage.getItem("scholarAssistRole") || "STUDENT").toUpperCase();
        document.querySelector("#session-user").textContent = isSignedIn
            ? (displayName() || (role === "ADMIN" ? "Administrator" : role === "REVIEWER" ? "Reviewer" : "Student"))
            : "Guest";
        document.querySelector("#session-role").textContent = isSignedIn
            ? (role === "ADMIN" ? "Admin" : role === "REVIEWER" ? "Reviewer" : "Student")
            : "";
        document.querySelector("#session-avatar").textContent = isSignedIn
            ? initials(displayName() || role)
            : "G";
        if (isSignedIn && !isReviewer() && userId()) {
            api.get(`/api/notifications/${encodeURIComponent(userId())}/unread`)
                .then((unread) => {
                    const count = document.querySelector("#unread-notification-count");
                    if (count && unread.length) {
                        count.textContent = String(unread.length);
                        count.hidden = false;
                    }
                })
                .catch(() => {});
        }
        const authAction = document.querySelector("#auth-action");
        authAction.innerHTML = isSignedIn
            ? '<button class="button button-quiet button-small" id="sign-out" type="button">Sign out</button>'
            : '<a class="button button-quiet button-small" href="login.html">Sign in</a>';
        document.querySelector(".sidebar-toggle")?.addEventListener("click", (event) => {
            const sidebar = document.querySelector(".sidebar");
            sidebar.classList.toggle("open");
            event.currentTarget.setAttribute("aria-expanded", sidebar.classList.contains("open"));
        });
        document.querySelector("#sign-out")?.addEventListener("click", async (event) => {
            event.currentTarget.disabled = true;
            try {
                await api.logout();
                sessionStorage.removeItem(preliminaryChecksKey(userId()));
                sessionStorage.removeItem("scholarAssistAuthenticated");
                sessionStorage.removeItem("scholarAssistUserId");
                sessionStorage.removeItem("scholarAssistStudentId");
                sessionStorage.removeItem("scholarAssistRole");
                sessionStorage.removeItem("scholarAssistEmail");
                sessionStorage.removeItem("scholarAssistDisplayName");
                location.href = "login.html";
            } catch (error) {
                event.currentTarget.disabled = false;
                toast(error.message);
            }
        });
    }

    function applicationLink(item, buttonClass = "button button-primary button-small", label = "Apply now", track = false) {
        try {
            const destination = new URL(item.applicationUrl || "");
            if (destination.protocol !== "https:") return "";
            const tracking = track ? ` data-track-application="true" data-scholarship-id="${escapeHtml(item.id)}"` : "";
            return `<a class="${buttonClass}" href="${escapeHtml(destination.href)}" target="_blank" rel="noopener noreferrer"${tracking}>${escapeHtml(label)}</a>`;
        } catch (error) {
            return "";
        }
    }

    function scholarshipCard(item) {
        return `<article class="scholarship-card" data-id="${escapeHtml(item.id)}">
            <div class="scholarship-main"><span class="provider-mark">✦</span><div>
                <h3>${escapeHtml(item.name)}</h3><p>${escapeHtml(item.provider)}</p>
                <div class="result-summary">${escapeHtml(item.description || "")}</div>
                <div class="scholarship-meta"><span><b>${moneyText(item.amount)}</b> award</span><span>Apply by <b>${dateText(item.deadline)}</b></span><span>${escapeHtml(item.educationLevel || "Education level not specified")}</span></div>
                <p class="eligibility-summary">Check your answers against this scholarship's listed criteria.</p>
            </div></div><div class="card-actions">
                <a class="button button-outline button-small" href="scholarship-details.html?id=${encodeURIComponent(item.id)}">Details</a>
            </div></article>`;
    }

    function isSspPortalScholarship(item) {
        try {
            const destination = new URL(item.applicationUrl || "");
            return destination.hostname === "ssp.postmatric.karnataka.gov.in";
        } catch (error) {
            return false;
        }
    }

    function scholarshipCards(items) {
        const sspItems = (items || []).filter(isSspPortalScholarship);
        let sspCardRendered = false;
        const cards = [];

        for (const item of items || []) {
            if (!isSspPortalScholarship(item)) {
                cards.push(scholarshipCard(item));
                continue;
            }

            if (!sspCardRendered) {
                cards.push(sspPortalCard(sspItems));
                sspCardRendered = true;
            }
        }

        return cards;
    }

    function sspPortalCard(items) {
        const portalScholarship = items[0];
        const programLinks = items.map((item) => `
            <li class="ssp-program-row">
                <span><b>${escapeHtml(item.name)}</b><small>${escapeHtml(item.provider)} · Deadline ${dateText(item.deadline)}</small></span>
                <a class="text-link" href="scholarship-details.html?id=${encodeURIComponent(item.id)}">Check criteria →</a>
            </li>`).join("");

        return `<article class="scholarship-card ssp-portal-card">
            <div class="scholarship-main"><span class="provider-mark">✦</span><div>
                <h3>Karnataka SSP scholarship portal</h3>
                <p>Government of Karnataka · one central application portal</p>
                <div class="result-summary">The SSP portal serves multiple department schemes. Each scheme has its own student category and eligibility criteria. Choose the relevant scheme below to check its requirements.</div>
                <ul class="ssp-program-list">${programLinks}</ul>
            </div></div>
            <div class="card-actions">${applicationLink(portalScholarship, "button button-primary button-small", "Open SSP portal")}</div>
        </article>`;
    }

    async function fetchScholarships(filters = {}) {
        const query = api.query(filters);
        return api.get(`/api/scholarships/search${query ? `?${query}` : ""}`);
    }

    const stateFilterValues = [
        "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat",
        "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh",
        "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
        "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand",
        "West Bengal", "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu",
        "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
    ];
    const educationFilterValues = [
        ["PRIMARY_SCHOOL", "Primary School"], ["UPPER_PRIMARY", "Upper Primary / Middle School"],
        ["SECONDARY", "Secondary School"], ["HIGHER_SECONDARY", "Higher Secondary / Senior Secondary"],
        ["DIPLOMA", "Diploma"], ["UNDERGRADUATE", "Undergraduate (UG)"],
        ["POSTGRADUATE", "Postgraduate (PG)"], ["DOCTORAL", "Doctoral / PhD"],
        ["POSTDOCTORAL", "Postdoctoral"], ["OTHER", "Other"]
    ];
    function canonicalEducation(value) {
        const normalized = String(value || "").trim().toUpperCase().replace(/[\s/-]+/g, "_");
        if (["UG", "UNDERGRAD", "UNDERGRADUATE"].includes(normalized)) return "UNDERGRADUATE";
        if (["PG", "POSTGRAD", "POSTGRADUATE"].includes(normalized)) return "POSTGRADUATE";
        if (["PHD", "DOCTORATE", "DOCTORAL", "DOCTORAL_PHD"].includes(normalized)) return "DOCTORAL";
        if (["MIDDLE", "MIDDLE_SCHOOL", "UPPER_PRIMARY", "UPPER_PRIMARY_MIDDLE_SCHOOL"].includes(normalized)) return "UPPER_PRIMARY";
        if (["SENIOR_SECONDARY", "HIGHER_SECONDARY", "HIGHER_SECONDARY_SENIOR_SECONDARY"].includes(normalized)) return "HIGHER_SECONDARY";
        if (["PRIMARY", "PRIMARY_SCHOOL"].includes(normalized)) return "PRIMARY_SCHOOL";
        if (["SECONDARY", "SECONDARY_SCHOOL"].includes(normalized)) return "SECONDARY";
        if (["POST_DOCTORAL", "POSTDOCTORAL"].includes(normalized)) return "POSTDOCTORAL";
        if (normalized === "DIPLOMA") return "DIPLOMA";
        return "OTHER";
    }
    function scholarshipFilterOptions(scholarships, fieldName) {
        if (fieldName === "state") {
            const values = [...new Set([...stateFilterValues, ...scholarships.map((item) => item.state).filter(Boolean)])]
                .sort((a, b) => a.localeCompare(b));
            return values.map((value) => `<option value="${escapeHtml(value)}">${escapeHtml(value)}</option>`).join("");
        }
        if (fieldName === "educationLevel") {
            const observed = scholarships.map((item) => item.educationLevel).filter(Boolean);
            const canonicalValues = new Set([...educationFilterValues.map(([value]) => value), ...observed.map(canonicalEducation)]);
            return [...canonicalValues].map((value) => {
                const label = educationFilterValues.find(([key]) => key === value)?.[1] || value;
                return `<option value="${escapeHtml(value)}">${escapeHtml(label)}</option>`;
            }).join("");
        }
        const values = [...new Set(scholarships
            .map((scholarship) => scholarship[fieldName])
            .filter((value) => typeof value === "string" && value.trim()))]
            .sort((first, second) => first.localeCompare(second));

        return values.map((value) =>
            `<option value="${escapeHtml(value)}">${escapeHtml(value)}</option>`)
            .join("");
    }
    async function scholarshipsPage() {
        shell("Discover scholarships",
            "Browse scholarships and quickly check your answers against each provider's listed criteria.", loading);
        const target = document.getElementById("page-content");
        try {
            const items = await fetchScholarships();
            const rows = items || [];
            target.innerHTML = `
                <form id="scholarship-search" class="filter-row">
                    <input class="field" name="q" placeholder="Search scholarships or providers" aria-label="Search scholarships">
                    <select class="field" name="state" aria-label="State"><option value="">All states</option>${scholarshipFilterOptions(rows, "state")}</select>
                    <select class="field" name="category" aria-label="Category"><option value="">All categories</option>${scholarshipFilterOptions(rows, "category")}</select>
                    <select class="field" name="educationLevel" aria-label="Education level"><option value="">All education levels</option>${scholarshipFilterOptions(rows, "educationLevel")}</select>
                    <button class="button button-primary button-small" type="submit">Search</button>
                </form>
                <div class="panel-title"><h2><span id="result-count">${rows.length}</span> available scholarships</h2></div>
                <div class="scholarship-list" id="scholarship-list">${rows.length ? scholarshipCards(rows).join("") : empty("No scholarships are currently available.")}</div>`;
            target.querySelector("#scholarship-search").addEventListener("submit", async (event) => {
                event.preventDefault();
                const values = Object.fromEntries(new FormData(event.currentTarget));
                const list = target.querySelector("#scholarship-list");
                list.innerHTML = loading;
                try {
                    const filtered = await fetchScholarships(values);
                    const visible = filtered || [];
                    list.innerHTML = visible.length ? scholarshipCards(visible).join("") : empty("No scholarships match these filters.");
                    target.querySelector("#result-count").textContent = visible.length;
                } catch (error) { showError(list, error); }
            });
        } catch (error) { showError(target, error); }
    }

    async function scholarshipDetails() {
        const id = new URLSearchParams(location.search).get("id");
        shell("Scholarship details", "Review the details, answer a few questions, and get an instant preliminary check.", loading);
        const target = document.getElementById("page-content");
        try {
            const item = await api.get(`/api/scholarships/${encodeURIComponent(id || "")}`);
            const rules = await api.get(`/api/scholarships/${encodeURIComponent(id)}/rules`);
            const inputRules = (rules || []).filter((rule) => normalizeFactType(rule.ruleType) !== "MANUAL_REVIEW");
            const manualRules = (rules || []).filter((rule) => normalizeFactType(rule.ruleType) === "MANUAL_REVIEW");
            const questions = new Map();
            inputRules.forEach((rule) => questions.set(normalizeFactType(rule.ruleType), rule));
            if (item.state) {
                questions.set("STATE", questions.get("STATE") || {
                    ruleType: "STATE",
                    ruleValue: item.state
                });
            }
            if (item.category) {
                questions.set("CATEGORY", questions.get("CATEGORY") || {
                    ruleType: "CATEGORY",
                    ruleValue: item.category
                });
            }
            if (item.educationLevel) {
                questions.set("EDUCATION_LEVEL", questions.get("EDUCATION_LEVEL") || {
                    ruleType: "EDUCATION_LEVEL",
                    ruleValue: item.educationLevel
                });
            }
            target.innerHTML = `<div class="dashboard-grid"><section class="panel">
                <div class="page-heading"><div><p class="eyebrow">SCHOLARSHIP DETAILS</p><h2>${escapeHtml(item.name)}</h2><p>${escapeHtml(item.provider)}</p></div>${badge(item.status)}</div>
                <div class="stats-grid"><div><span class="stat-label">Award amount</span><b>${moneyText(item.amount)}</b></div><div><span class="stat-label">Deadline</span><b>${dateText(item.deadline)}</b></div><div><span class="stat-label">Education level</span><b>${escapeHtml(item.educationLevel || "Not specified")}</b></div></div>
                <h3>About this scholarship</h3><p>${escapeHtml(item.description || "No description provided.")}</p>
                <h3>Quick eligibility check</h3>
                <p class="form-hint">Answer using your current information. This preliminary check is not document verification or an award decision.</p>
                <form id="quick-eligibility-form" class="form-grid">
                    ${[...questions.entries()].map(([type, rule]) => quickFactField(type, rule)).join("")}
                    ${manualRules.map((rule) => `<label class="form-group"><span class="form-label">${escapeHtml(rule.description || "Do you meet this additional condition?")}</span><select class="field" name="condition-${escapeHtml(rule.id)}" required><option value="">Choose an answer</option><option value="true">Yes</option><option value="false">No</option></select></label>`).join("")}
                    <button class="button button-primary" type="submit">Check eligibility</button>
                </form><div id="quick-check-result" aria-live="polite"></div>
                <h3>Listed criteria</h3>${rules.length ? `<ul>${rules.map((rule) => `<li>${escapeHtml(rule.ruleType)} ${escapeHtml(rule.operator)} ${escapeHtml(rule.ruleValue)}${rule.description ? ` — ${escapeHtml(rule.description)}` : ""}</li>`).join("")}</ul>` : empty("No additional criteria are currently listed.")}
                <p class="form-hint">If your answers match, you can continue on the scholarship provider's official site.</p>
                ${!item.applicationUrl ? `<p class="api-message">No official application link is available for this scholarship yet.</p>` : ""}
            </section></div>`;

            target.querySelector("#quick-eligibility-form").addEventListener("submit", async (event) => {
                event.preventDefault();
                const form = event.currentTarget;
                if (!form.reportValidity()) return;
                const facts = {};
                const additionalConditions = {};
                questions.forEach((rule, type) => {
                    const value = form.elements[type]?.value.trim();
                    if (value) facts[type] = value;
                });
                manualRules.forEach((rule) => {
                    const value = form.elements[`condition-${rule.id}`].value;
                    if (value !== "") additionalConditions[rule.id] = value === "true";
                });
                const submit = form.querySelector("button[type=submit]");
                const resultArea = target.querySelector("#quick-check-result");
                submit.disabled = true;
                resultArea.innerHTML = loading;
                try {
                    const result = await api.post(`/api/eligibility/quick-check/${encodeURIComponent(id)}`, {
                        facts,
                        additionalConditions
                    });
                    rememberPreliminaryCheck(userId(), item, result);
                    const match = result.status === "PRELIMINARY_MATCH";
                    resultArea.innerHTML = `<div class="api-message" role="status"><h3>${badge(result.status)} ${escapeHtml(quickStatusLabel(result.status))}</h3><ul>${result.reasons.map((reason) => `<li>${escapeHtml(reason)}</li>`).join("")}</ul><p>${escapeHtml(result.notice)}</p>${match ? applicationLink(item, "button button-primary", "Apply now", true) : ""}<div class="tracking-feedback"></div></div>`;
                    resultArea.querySelector("[data-track-application]")?.addEventListener("click", async (clickEvent) => {
                        clickEvent.preventDefault();
                        const link = clickEvent.currentTarget;
                        const feedback = resultArea.querySelector(".tracking-feedback");
                        if (!studentId()) {
                            feedback.innerHTML = empty("Create your student profile before starting an application.");
                            return;
                        }
                        const providerTab = window.open("about:blank", "_blank");
                        if (!providerTab) {
                            feedback.innerHTML = empty("Allow pop-ups to open the scholarship provider after tracking starts.");
                            return;
                        }
                        link.setAttribute("aria-disabled", "true");
                        try {
                            const tracker = await api.post(`/api/applications/${encodeURIComponent(studentId())}/external`, {
                                scholarshipId: Number(link.dataset.scholarshipId)
                            });
                            providerTab.location.href = link.href;
                            feedback.innerHTML = `<p>Tracking started as ${badge(tracker.status)}. After submitting on the provider’s site, return and confirm it in <a class="text-link" href="application-details.html?id=${encodeURIComponent(tracker.id)}">application details</a>.</p>`;
                        } catch (error) {
                            providerTab.close();
                            showError(feedback, error);
                        } finally {
                            link.removeAttribute("aria-disabled");
                        }
                    });
                } catch (error) {
                    showError(resultArea, error);
                } finally {
                    submit.disabled = false;
                }
            });
        } catch (error) { showError(target, error); }
    }

    function normalizeFactType(type) {
        const value = String(type || "").trim().toUpperCase().replaceAll(" ", "_");
        return ["INCOME", "ANNUAL_FAMILY_INCOME"].includes(value) ? "ANNUAL_FAMILY_INCOME" : value;
    }

    function quickFactField(type, rule) {
        const labels = {
            CGPA: "Your CGPA", ANNUAL_FAMILY_INCOME: "Annual family income (₹)",
            STATE: "Your state", CATEGORY: "Your category", COURSE: "Your course",
            EDUCATION_LEVEL: "Your education level", GENDER: "Your gender",
            DISABILITY_STATUS: "Do you have a recognized disability?",
            DISABILITY_PERCENTAGE: "Disability percentage",
            SEAT_TYPE: "Admission / seat type",
            PREVIOUS_MARKS_PERCENTAGE: "Marks in your previous qualifying exam (%)",
            PUC_MARKS_PERCENTAGE: "PUC / Class 12 marks (%)",
            HOSTEL_STATUS: "Minority hostel seat status",
            YEAR_OF_STUDY: "Current year of study"
        };
        const label = labels[type] || type.replaceAll("_", " ").toLowerCase();
        const numeric = [
            "CGPA", "ANNUAL_FAMILY_INCOME", "DISABILITY_PERCENTAGE",
            "PREVIOUS_MARKS_PERCENTAGE", "PUC_MARKS_PERCENTAGE", "YEAR_OF_STUDY"
        ].includes(type);
        const attributes = type === "CGPA" ? 'min="0" max="10" step="0.01"'
            : type === "YEAR_OF_STUDY" ? 'min="1" max="10" step="1"'
                : ["PREVIOUS_MARKS_PERCENTAGE", "PUC_MARKS_PERCENTAGE", "DISABILITY_PERCENTAGE"].includes(type)
                    ? 'min="0" max="100" step="0.01"'
                    : type === "ANNUAL_FAMILY_INCOME" ? 'min="0" step="1"' : "";
        if (!numeric) {
            const suggestions = quickFactOptions(type, rule);
            return `<label class="form-group"><span class="form-label">${escapeHtml(label)}</span><select class="field" name="${escapeHtml(type)}" required><option value="">Select ${escapeHtml(label.toLowerCase())}</option>${suggestions.map((value) => `<option value="${escapeHtml(value)}">${escapeHtml(value)}</option>`).join("")}</select></label>`;
        }
        return `<label class="form-group"><span class="form-label">${escapeHtml(label)}</span><input class="field" name="${escapeHtml(type)}" type="${numeric ? "number" : "text"}" ${attributes} required></label>`;
    }

    function quickFactOptions(type, rule) {
        const commonOptions = {
            STATE: [
                "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
                "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka",
                "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram",
                "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu",
                "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal",
                "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu",
                "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
            ],
            CATEGORY: [
                "General", "General EWS", "SC", "ST", "OBC", "EWS", "Minority",
                "OBC/EBC/DNT", "Brahmin"
            ],
            GENDER: ["Female", "Male", "Other"],
            DISABILITY_STATUS: ["Yes", "No"],
            SEAT_TYPE: ["Merit", "Regular", "Management", "COMEDK"],
            HOSTEL_STATUS: ["Not allotted", "Allotted", "Not applied", "Not applicable"],
            COURSE: [
                "B.A.", "B.Com.", "B.Sc.", "B.E.", "B.Tech", "BBA", "BCA", "MBBS",
                "B.Ed.", "Diploma", "M.A.", "M.Com.", "M.Sc.", "M.E.", "M.Tech",
                "MBA", "MCA", "PhD"
            ],
            EDUCATION_LEVEL: [
                "School", "Higher Secondary", "Diploma", "Undergraduate", "Postgraduate", "Doctoral"
            ]
        };
        const options = new Set(commonOptions[type] || []);
        if (rule?.ruleValue) {
            const ruleValues = rule.operator === "IN"
                ? rule.ruleValue.split(",")
                : [rule.ruleValue];
            ruleValues.forEach((value) => options.add(value.trim()));
        }
        return [...options].sort((first, second) => first.localeCompare(second));
    }

    function quickStatusLabel(status) {
        return ({
            PRELIMINARY_MATCH: "Your answers appear to match",
            DOES_NOT_MATCH: "Your answers do not match the listed criteria",
            MORE_INFORMATION_REQUIRED: "More information is needed"
        })[status] || "Quick check result";
    }

    async function profilePage() {
        shell("My profile", "Edit the student profile associated with your signed-in account.", loading);
        const target = document.getElementById("page-content");
        if (!idsPresent("user")) { target.innerHTML = needId("user"); return; }
        let profile = null;
        try { profile = await api.get(`/api/student-profile/${encodeURIComponent(userId())}`); }
        catch (error) {
            if (error.status !== 404) { showError(target, error); return; }
        }
        currentStudentProfile = profile;
        const fields = [
            ["fullName", "Full name", "text", true], ["phone", "Phone", "tel"],
            ["dateOfBirth", "Date of birth", "date"], ["gender", "Gender", "text"],
            ["state", "State", "text"], ["district", "District", "text"],
            ["collegeName", "College", "text"], ["course", "Course", "text"],
            ["yearOfStudy", "Year of study", "number"], ["cgpa", "CGPA", "number"],
            ["annualFamilyIncome", "Annual family income", "number"], ["category", "Category", "text"]
        ];
        target.innerHTML = `<form id="profile-form" class="panel"><div class="form-grid">${fields.map(([name, label, type, required]) => `<label class="form-group"><span class="form-label">${label}</span><input class="field" name="${name}" type="${type}" ${required ? "required" : ""} ${type === "number" && name === "cgpa" ? 'min="0" max="10" step="0.01"' : ""} value="${escapeHtml(profile?.[name] ?? "")}"></label>`).join("")}</div><button class="button button-primary" type="submit">${profile ? "Save profile" : "Create profile"}</button><div class="form-feedback"></div></form>`;
        target.querySelector("#profile-form").addEventListener("submit", async (event) => {
            event.preventDefault();
            const form = event.currentTarget;
            if (!form.reportValidity()) return;
            const body = Object.fromEntries(new FormData(form));
            Object.keys(body).forEach((key) => {
                if (typeof body[key] === "string") body[key] = body[key].trim() || null;
            });
            ["yearOfStudy", "cgpa", "annualFamilyIncome"].forEach((key) => {
                if (body[key] === "" || body[key] === null) body[key] = null;
                else if (body[key] !== undefined) body[key] = Number(body[key]);
            });
            const button = form.querySelector("button[type=submit]");
            button.disabled = true;
            try {
                const savedProfile = await api.request(`/api/student-profile/${encodeURIComponent(userId())}`, {
                    method: profile ? "PUT" : "POST",
                    headers: { "Content-Type": "application/json", Accept: "application/json" },
                    body: JSON.stringify(body)
                });
                profile = savedProfile;
                currentStudentProfile = savedProfile;
                if (profile?.id) {
                    sessionStorage.setItem("scholarAssistStudentId", String(profile.id));
                }
                form.querySelector("button[type=submit]").textContent = "Save profile";
                toast("Profile saved.");
            } catch (error) { showError(form.querySelector(".form-feedback"), error); }
            finally { button.disabled = false; }
        });
    }

    async function applicationsPage() {
        shell("My applications", "Track provider applications and follow status updates from the reviewer.", loading);
        const target = document.getElementById("page-content");
        if (!idsPresent("student")) { target.innerHTML = needId("student"); return; }
        try {
            const items = await api.get(`/api/applications/${encodeURIComponent(studentId())}`);
            target.innerHTML = `<p class="application-sync-note">Provider-site status is not synchronized automatically. You or a reviewer must report updates here.</p>` + (items.length ? `<div class="panel table-wrap"><table><thead><tr><th>Scholarship</th><th>Started</th><th>Status</th><th>Provider reference</th><th>Latest update</th><th></th></tr></thead><tbody>${items.map((item) => `<tr><td class="table-title">${escapeHtml(item.scholarshipName)}</td><td>${dateText(item.applicationDate)}</td><td>${badge(item.status)}</td><td>${escapeHtml(item.providerApplicationId || "—")}</td><td>${escapeHtml(item.latestUpdate ? `${item.latestUpdateSource === "REVIEWER" ? "Reviewer: " : item.latestUpdateSource === "MIGRATION" ? "System: " : "Student: "}${item.latestUpdate}` : item.remarks || "—")}</td><td><a class="text-link" href="application-details.html?id=${encodeURIComponent(item.id)}">Track →</a></td></tr>`).join("")}</tbody></table></div>` : empty("No tracked applications yet. Check a scholarship and choose Apply now to start tracking."));
        } catch (error) { showError(target, error); }
    }

    async function applicationDetails() {
        const id = new URLSearchParams(location.search).get("id");
        shell("Application details", "Follow the application timeline and record provider submission details.", loading);
        const target = document.getElementById("page-content");
        try {
            const item = await api.get(`/api/applications/details/${encodeURIComponent(id || "")}`);
            if (item.status !== "DRAFT") {
                const [history, supportRequests] = await Promise.all([
                    api.get(`/api/applications/${encodeURIComponent(id || "")}/history`),
                    api.get(`/api/applications/${encodeURIComponent(id || "")}/support-requests`)
                ]);
                const canConfirmSubmission = item.status === "EXTERNAL_STARTED";
                const withinDeadline = !item.scholarshipDeadline || item.scholarshipDeadline >= localDateToday();
                const canRequestHelp = withinDeadline && ["EXTERNAL_STARTED", "APPLIED"].includes(item.status);
                const pendingSupportRequest = supportRequests.some((request) => request.status === "PENDING");
                const supportHistory = supportRequests.length ? `<div class="support-request-list">${supportRequests.map((request) => `<article class="application-context"><b>${escapeHtml(factLabel(request.requestType))} request · ${badge(request.status)}</b><span>${escapeHtml(request.studentMessage)}</span>${request.reviewerMessage ? `<span>Reviewer: ${escapeHtml(request.reviewerMessage)}</span>` : ""}<small>${escapeHtml(dateTimeText(request.requestedAt))}</small></article>`).join("")}</div>` : empty("No correction or stop requests have been made.");
                target.innerHTML = `<section class="panel"><div class="panel-title"><h2>${escapeHtml(item.scholarshipName)}</h2>${badge(item.status)}</div>
                    <p>Started: ${dateText(item.applicationDate)} · Submitted: ${escapeHtml(item.submittedAt || "Not confirmed")}</p>
                    <p>Provider reference: ${escapeHtml(item.providerApplicationId || "Not provided")}</p>
                    <p>Student notes: ${escapeHtml(item.remarks || "None")}</p>
                    ${canConfirmSubmission ? `<form id="external-submission-form" class="form-grid"><label class="form-group"><span class="form-label">Provider application number (optional)</span><input class="field" name="providerApplicationId" maxlength="150" autocomplete="off"></label><label class="form-group"><span class="form-label">Notes (optional)</span><textarea class="field" name="remarks" maxlength="2000" rows="3"></textarea></label><button class="button button-primary" type="submit">I submitted this application</button><div class="form-feedback"></div></form>` : ""}
                    ${item.status === "APPLIED" ? `<form id="external-submission-form" class="form-grid"><p class="form-hint">You can correct the provider reference before a reviewer begins updating this application.</p><label class="form-group"><span class="form-label">Update provider application number</span><input class="field" name="providerApplicationId" value="${escapeHtml(item.providerApplicationId || "")}" maxlength="150" autocomplete="off"></label><label class="form-group"><span class="form-label">Correct student note (optional)</span><textarea class="field" name="remarks" maxlength="2000" rows="3">${escapeHtml(item.remarks || "")}</textarea></label><button class="button button-outline" type="submit">Save correction</button><div class="form-feedback"></div></form>` : ""}
                    <section class="support-request-panel"><h3>Need to correct or stop this application?</h3><p class="form-hint">ScholarAssist can send a request to its reviewer. It cannot change or cancel the form on the scholarship provider’s website. Requests are available until the scholarship deadline.</p>${supportHistory}${canRequestHelp && !pendingSupportRequest ? `<form id="support-request-form" class="form-grid"><label class="form-group"><span class="form-label">Request type</span><select class="field" name="requestType" required><option value="">Choose a request</option>${item.status === "APPLIED" ? '<option value="CORRECTION">Request a correction</option>' : ""}<option value="CANCELLATION">Request to stop tracking this application</option></select></label><label class="form-group"><span class="form-label">Explain what you need</span><textarea class="field" name="message" rows="3" maxlength="2000" required></textarea></label><button class="button button-outline" type="submit">Send request to reviewer</button><div class="form-feedback"></div></form>` : canRequestHelp && pendingSupportRequest ? empty("A request is already waiting for reviewer action.") : item.scholarshipDeadline && !withinDeadline ? empty("The scholarship deadline has passed. New correction or stop requests are closed.") : ""}</section>
                    <h3>Status history</h3>${history.length ? `<ol class="application-timeline">${history.map(historyMarkup).join("")}</ol>` : empty("No status updates yet.")}
                    <div class="form-feedback" id="application-feedback"></div></section>`;
                target.querySelector("#external-submission-form")?.addEventListener("submit", async (event) => {
                    event.preventDefault();
                    const form = event.currentTarget;
                    const values = Object.fromEntries(new FormData(form));
                    const button = form.querySelector("button[type=submit]");
                    button.disabled = true;
                    try {
                        await api.patch(`/api/applications/${encodeURIComponent(id)}/external-submission`, values);
                        toast("Submission confirmation saved. Provider decisions are not synced automatically.");
                        await applicationDetails();
                    } catch (error) { showError(form.querySelector(".form-feedback"), error); }
                    finally { button.disabled = false; }
                });
                target.querySelector("#support-request-form")?.addEventListener("submit", async (event) => {
                    event.preventDefault();
                    const form = event.currentTarget;
                    if (!form.reportValidity()) return;
                    const button = form.querySelector("button[type=submit]");
                    button.disabled = true;
                    try {
                        await api.post(`/api/applications/${encodeURIComponent(id)}/support-requests`, Object.fromEntries(new FormData(form)));
                        toast("Request sent to the reviewer.");
                        await applicationDetails();
                    } catch (error) { showError(form.querySelector(".form-feedback"), error); }
                    finally { button.disabled = false; }
                });
                return;
            }
            const docs = idsPresent("student") ? await api.get(`/api/documents/${encodeURIComponent(studentId())}`) : [];
            target.innerHTML = `<section class="panel"><div class="panel-title"><h2>${escapeHtml(item.scholarshipName)}</h2>${badge(item.status)}</div>
                <p>Application date: ${dateText(item.applicationDate)} · Submitted: ${escapeHtml(item.submittedAt || "Not submitted")}</p>
                <form id="remarks-form"><label class="form-group"><span class="form-label">Remarks</span><textarea class="field" name="remarks" rows="3">${escapeHtml(item.remarks || "")}</textarea></label><button class="button button-outline" ${item.status !== "DRAFT" ? "disabled" : ""}>Save remarks</button><span class="form-feedback"></span></form>
                ${item.status === "DRAFT" ? `<div class="form-grid"><label class="form-group"><span class="form-label">Existing document</span><select class="field" id="document-id"><option value="">Select a document</option>${docs.map((doc) => `<option value="${escapeHtml(doc.id)}">${escapeHtml(doc.fileName)} (${escapeHtml(doc.documentType)})</option>`).join("")}</select></label><button class="button button-outline" id="attach-document" type="button">Attach as required and mark submitted</button></div><button class="button button-primary" id="submit-application" type="button">Submit application</button>` : ""}
                <div class="form-feedback" id="application-feedback"></div></section>`;
            target.querySelector("#remarks-form").addEventListener("submit", async (event) => {
                event.preventDefault();
                try {
                    await api.put(`/api/applications/${encodeURIComponent(id)}`, { remarks: new FormData(event.currentTarget).get("remarks") });
                    toast("Remarks saved.");
                } catch (error) { showError(event.currentTarget.querySelector(".form-feedback"), error); }
            });
            target.querySelector("#attach-document")?.addEventListener("click", async () => {
                const documentId = target.querySelector("#document-id").value;
                if (!documentId) { toast("Choose a document first."); return; }
                try {
                    await api.post(`/api/applications/${encodeURIComponent(id)}/documents`, {
                        applicationId: Number(id),
                        documentId: Number(documentId),
                        required: true
                    });
                    await api.patch(`/api/documents/applications/${encodeURIComponent(id)}/${encodeURIComponent(documentId)}/submitted?submitted=true`);
                    toast("Document attached and marked submitted.");
                } catch (error) { showError(target.querySelector("#application-feedback"), error); }
            });
            target.querySelector("#submit-application")?.addEventListener("click", async () => {
                try {
                    await api.patch(`/api/applications/${encodeURIComponent(id)}/submit`);
                    toast("Application submitted.");
                    applicationDetails();
                } catch (error) { showError(target.querySelector("#application-feedback"), error); }
            });
        } catch (error) { showError(target, error); }
    }

    async function reviewerApplicationsPage() {
        shell("Application review", "Review student-reported submissions and record decisions that trigger notifications.", loading);
        const target = document.getElementById("page-content");
        if (!authenticated() || !isReviewer()) {
            target.innerHTML = empty("Sign in with a reviewer or admin account to access application review.");
            return;
        }
        try {
            const [items, supportRequests] = await Promise.all([
                api.get("/api/reviewer/applications"),
                api.get("/api/reviewer/applications/support-requests")
            ]);
            const queue = items.length ? `<div class="panel table-wrap"><table><thead><tr><th>ScholarAssist ID</th><th>Student</th><th>Scholarship</th><th>Submitted</th><th>Provider reference</th><th>Status</th><th></th></tr></thead><tbody>${items.map((item) => `<tr><td>#${escapeHtml(item.id)}</td><td class="table-title">${escapeHtml(item.studentName)}<small>${escapeHtml(item.studentEmail)} · Student ${escapeHtml(item.studentId)}</small></td><td>${escapeHtml(item.scholarshipName)}<small>${escapeHtml(item.scholarshipProvider || "")}</small></td><td>${escapeHtml(dateTimeText(item.submittedAt))}</td><td>${escapeHtml(item.providerApplicationId || "Not provided")}</td><td>${badge(item.status)}</td><td><a class="button button-outline button-small" href="reviewer-application-details.html?id=${encodeURIComponent(item.id)}">Open request</a></td></tr>`).join("")}</tbody></table></div>` : empty("No applications are waiting for review. A request appears after the student starts tracking a scholarship and confirms submission.");
            const supportQueue = supportRequests.length ? `<section class="panel"><h2>Student correction and stop requests (${supportRequests.length})</h2><p class="form-hint">Approval records a ScholarAssist decision. It cannot edit or cancel the application at the external provider.</p><div class="support-request-list">${supportRequests.map((request) => `<article class="panel support-request-card" data-request-id="${escapeHtml(request.id)}"><div class="panel-title"><div><h3>${escapeHtml(factLabel(request.requestType))} · ${escapeHtml(request.scholarshipName)}</h3><p>${escapeHtml(request.studentName)} · ${escapeHtml(request.studentEmail)}</p></div>${badge(request.status)}</div><p>${escapeHtml(request.studentMessage)}</p><a class="text-link" href="reviewer-application-details.html?id=${encodeURIComponent(request.applicationId)}">Open application #${escapeHtml(request.applicationId)}</a><form class="support-decision-form form-grid"><label class="form-group"><span class="form-label">Decision</span><select class="field" name="decision" required><option value="">Choose</option><option value="APPROVED">Approve request</option><option value="DECLINED">Decline request</option></select></label><label class="form-group"><span class="form-label">Message to student</span><textarea class="field" name="message" maxlength="2000" required rows="2"></textarea></label><button class="button button-primary button-small" type="submit">Save decision and notify student</button><div class="form-feedback"></div></form></article>`).join("")}</div></section>` : `<section class="panel"><h2>Student correction and stop requests</h2>${empty("There are no pending requests.")}</section>`;
            target.innerHTML = `<p class="form-hint reviewer-workflow-note">Record updates based on information received from the scholarship provider. Saving creates history and notifies the student.</p>${supportQueue}${queue}`;
            target.querySelectorAll(".support-decision-form").forEach((form) => form.addEventListener("submit", async (event) => {
                event.preventDefault();
                if (!form.reportValidity()) return;
                const card = form.closest(".support-request-card");
                const button = form.querySelector("button[type=submit]");
                button.disabled = true;
                try {
                    await api.patch(`/api/reviewer/applications/support-requests/${encodeURIComponent(card.dataset.requestId)}`, Object.fromEntries(new FormData(form)));
                    toast("Request decision saved and student notified.");
                    await reviewerApplicationsPage();
                } catch (error) { showError(form.querySelector(".form-feedback"), error); }
                finally { button.disabled = false; }
            }));
        } catch (error) { showError(target, error); }
    }

    async function reviewerApplicationDetails() {
        const id = new URLSearchParams(location.search).get("id");
        shell("Student application request", "Review the student’s information and record provider updates.", loading);
        const target = document.getElementById("page-content");
        if (!authenticated() || !isReviewer()) {
            target.innerHTML = empty("Sign in with a reviewer or admin account to access this request.");
            return;
        }
        if (!id) {
            target.innerHTML = empty("No application request was selected.");
            return;
        }
        try {
            const item = await api.get(`/api/reviewer/applications/${encodeURIComponent(id)}`);
            const [history, studentDocs] = await Promise.all([
                api.get(`/api/applications/${encodeURIComponent(id)}/history`),
                api.get(`/api/reviewer/documents/students/${encodeURIComponent(item.studentId)}/documents`)
            ]);
            const documents = studentDocs.length
                ? `<div class="review-queue">${studentDocs.map((doc) => `<article class="panel reviewer-document" data-document-id="${escapeHtml(doc.id)}"><div class="panel-title"><div><h3>${escapeHtml(doc.fileName)}</h3><p>${escapeHtml(doc.documentType)} · Uploaded ${escapeHtml(doc.uploadedAt || "")}</p></div>${badge(doc.verificationStatus)}</div>${doc.reviewFeedback ? `<p>Reviewer feedback: ${escapeHtml(doc.reviewFeedback)}</p>` : ""}<button type="button" class="button button-outline open-student-document" data-id="${escapeHtml(doc.id)}">Inspect document</button><div class="review-preview" hidden></div>${doc.verificationStatus === "PENDING" ? `<label class="form-group"><span class="form-label">Feedback (required if rejecting)</span><textarea class="field document-feedback" rows="2" maxlength="2000"></textarea></label><div class="card-actions"><button type="button" class="button button-primary decide-student-document" data-id="${escapeHtml(doc.id)}" data-status="VERIFIED">Confirm document</button><button type="button" class="button button-outline decide-student-document" data-id="${escapeHtml(doc.id)}" data-status="REJECTED">Reject with feedback</button></div>` : ""}<div class="form-feedback"></div></article>`).join("")}</div>`
                : empty("No uploaded documents are available for this student.");
            const canUpdateStatus = ["SUBMITTED", "APPLIED", "PENDING", "UNDER_REVIEW", "NEEDS_INFORMATION"]
                .includes(String(item.status).toUpperCase());
            target.innerHTML = `<p><a class="text-link" href="reviewer-applications.html">← Back to requests</a></p>
                <section class="panel"><div class="panel-title"><div><h2>${escapeHtml(item.scholarshipName)}</h2><p>${escapeHtml(item.scholarshipProvider || "Provider not listed")}</p></div>${badge(item.status)}</div><p>ScholarAssist application ID: #${escapeHtml(item.id)}</p><p>Student: ${escapeHtml(item.studentName)} · Student ${escapeHtml(item.studentId)} · ${escapeHtml(item.studentEmail)}</p><p>Student-reported submission: ${escapeHtml(dateTimeText(item.submittedAt))}</p><p>Provider reference: ${escapeHtml(item.providerApplicationId || "Not provided")}</p><p>Student notes: ${escapeHtml(item.remarks || "None")}</p></section>
                <section class="panel"><h2>Student documents</h2><p>These files were uploaded to the student profile; they may not be specific to this scholarship application.</p>${documents}</section>
                <section class="panel"><h2>Record a provider update</h2><p>Enter a status only after receiving information from the scholarship provider. Saving sends your message as an in-app notification.</p>${canUpdateStatus ? `<form id="review-application-form" class="form-grid"><label class="form-group"><span class="form-label">Status</span><select class="field" name="status" required><option value="PENDING">Pending</option><option value="UNDER_REVIEW">Under review</option><option value="NEEDS_INFORMATION">Needs information</option><option value="APPROVED">Selected</option><option value="REJECTED">Not selected</option></select></label><label class="form-group"><span class="form-label">Provider update for the student</span><textarea class="field" name="remarks" rows="4" maxlength="2000" required></textarea></label><button class="button button-primary" type="submit">Save status and notify student</button><div class="form-feedback"></div></form>` : empty("This application has reached a final status and cannot be updated here.")}</section>
                <section class="panel"><h2>Status history</h2>${history.length ? `<ol>${history.map(historyMarkup).join("")}</ol>` : empty("No status history yet.")}</section>`;
            target.querySelector("#review-application-form")?.addEventListener("submit", async (event) => {
                event.preventDefault();
                const form = event.currentTarget;
                const button = form.querySelector("button[type=submit]");
                button.disabled = true;
                try {
                    await api.patch(`/api/applications/${encodeURIComponent(id)}/status`, Object.fromEntries(new FormData(form)));
                    toast("Update saved and student notified.");
                    await reviewerApplicationDetails();
                } catch (error) { showError(form.querySelector(".form-feedback"), error); }
                finally { button.disabled = false; }
            });
            target.querySelectorAll(".open-student-document").forEach((button) => button.addEventListener("click", async () => {
                const preview = button.closest(".reviewer-document").querySelector(".review-preview");
                preview.hidden = false;
                preview.innerHTML = loading;
                try {
                    const blob = await api.getBlob(`/api/reviewer/documents/${encodeURIComponent(button.dataset.id)}/content`);
                    const previousUrl = preview.dataset.objectUrl;
                    if (previousUrl) URL.revokeObjectURL(previousUrl);
                    const objectUrl = URL.createObjectURL(blob);
                    preview.dataset.objectUrl = objectUrl;
                    preview.innerHTML = blob.type.startsWith("image/")
                        ? `<img class="review-file-image" src="${objectUrl}" alt="Student document">`
                        : `<iframe class="review-file-frame" src="${objectUrl}" title="Student document"></iframe>`;
                } catch (error) { showError(preview, error); }
            }));
            target.querySelectorAll(".decide-student-document").forEach((button) => button.addEventListener("click", async () => {
                const card = button.closest(".reviewer-document");
                const feedback = card.querySelector(".document-feedback").value.trim();
                if (button.dataset.status === "REJECTED" && !feedback) {
                    showError(card.querySelector(".form-feedback"), new Error("Add clear feedback before rejecting this document."));
                    return;
                }
                button.disabled = true;
                try {
                    await api.patch(`/api/reviewer/documents/${encodeURIComponent(button.dataset.id)}/review`, {
                        status: button.dataset.status,
                        feedback
                    });
                    toast(button.dataset.status === "VERIFIED" ? "Document confirmed." : "Document rejected with feedback.");
                    await reviewerApplicationDetails();
                } catch (error) { showError(card.querySelector(".form-feedback"), error); }
                finally { button.disabled = false; }
            }));
        } catch (error) { showError(target, error); }
    }

    async function documentsPage() {
        shell("Document vault", "Upload documents and follow reviewer decisions.", loading);
        const target = document.getElementById("page-content");
        if (!idsPresent("student")) { target.innerHTML = needId("student"); return; }
        try {
            const docs = await api.get(`/api/documents/${encodeURIComponent(studentId())}`);
            target.innerHTML = `<section class="panel"><h2>Document verification steps</h2><ol><li>Choose a clear PDF, JPEG, or PNG file up to 10 MB and select the document type.</li><li>Wait for a reviewer to inspect it. The state will show <b>PENDING</b>.</li><li>If rejected, read the reviewer feedback and replace the file. Verified or application-linked documents can’t be replaced.</li><li>A verified file alone does not update eligibility. A reviewer must separately confirm the specific facts visible in the document.</li></ol></section><section class="panel"><form id="upload-form" class="form-grid"><label class="form-group"><span class="form-label">Document type</span><input class="field" name="documentType" required maxlength="100" placeholder="INCOME_CERTIFICATE"></label><label class="form-group"><span class="form-label">PDF, JPEG, or PNG (max 10 MB)</span><input class="field" name="file" type="file" accept="application/pdf,image/jpeg,image/png" required></label><button class="button button-primary">Upload document</button><div class="form-feedback"></div></form></section>
                ${docs.length ? `<div class="panel table-wrap"><table><thead><tr><th>File</th><th>Type</th><th>Uploaded</th><th>Review state</th><th>Reviewer feedback</th><th>Actions</th></tr></thead><tbody>${docs.map((doc) => {
                    const editable = ["PENDING", "REJECTED"].includes(String(doc.verificationStatus).toUpperCase());
                    return `<tr><td>${escapeHtml(doc.fileName)}</td><td>${escapeHtml(doc.documentType)}</td><td>${escapeHtml(doc.uploadedAt || "")}</td><td>${badge(doc.verificationStatus)}${doc.reviewedAt ? `<small class="document-review-time">Reviewed ${escapeHtml(doc.reviewedAt)}</small>` : ""}</td><td>${doc.verificationStatus === "REJECTED" ? escapeHtml(doc.reviewFeedback || "No feedback was provided.") : "—"}</td><td>${editable ? `<button class="button button-quiet replace-document" data-id="${escapeHtml(doc.id)}" data-type="${escapeHtml(doc.documentType)}">Replace</button><button class="button button-quiet delete-document" data-id="${escapeHtml(doc.id)}">Delete</button>` : "—"}</td></tr>`;
                }).join("")}</tbody></table></div>` : empty("No documents uploaded yet.")}`;
            target.querySelector("#upload-form").addEventListener("submit", async (event) => {
                event.preventDefault();
                const form = event.currentTarget;
                const file = form.elements.file.files[0];
                if (!file) return;
                if (file.size > 10 * 1024 * 1024) { showError(form.querySelector(".form-feedback"), new Error("File is larger than the 10 MB limit.")); return; }
                if (!["application/pdf", "image/jpeg", "image/png"].includes(file.type)) { showError(form.querySelector(".form-feedback"), new Error("Choose a PDF, JPEG, or PNG file.")); return; }
                const data = new FormData();
                data.append("documentType", form.elements.documentType.value);
                data.append("file", file);
                try {
                    await api.upload(`/api/documents/${encodeURIComponent(studentId())}`, data);
                    documentsPage();
                    toast("Document uploaded.");
                } catch (error) { showError(form.querySelector(".form-feedback"), error); }
            });
            target.querySelectorAll(".delete-document").forEach((button) => button.addEventListener("click", async () => {
                try { await api.delete(`/api/documents/${encodeURIComponent(button.dataset.id)}`); documentsPage(); }
                catch (error) { toast(error.message); }
            }));
            target.querySelectorAll(".replace-document").forEach((button) => button.addEventListener("click", () => {
                const picker = document.createElement("input");
                picker.type = "file";
                picker.accept = "application/pdf,image/jpeg,image/png";
                picker.addEventListener("change", async () => {
                    const file = picker.files[0];
                    if (!file) return;
                    if (file.size > 10 * 1024 * 1024 || !["application/pdf", "image/jpeg", "image/png"].includes(file.type)) {
                        toast("Choose a PDF, JPEG, or PNG file no larger than 10 MB.");
                        return;
                    }
                    const formData = new FormData();
                    formData.append("documentType", button.dataset.type);
                    formData.append("file", file);
                    try {
                        await api.request(`/api/documents/${encodeURIComponent(button.dataset.id)}`, {
                            method: "PUT", body: formData
                        });
                        documentsPage();
                        toast("Replacement uploaded. It is pending reviewer verification.");
                    } catch (error) { toast(error.message); }
                }, { once: true });
                picker.click();
            }));
        } catch (error) { showError(target, error); }
    }

    async function reviewerDocumentsPage() {
        shell("Document review", "Review uploads and confirm eligibility facts only from verified evidence.", loading);
        const target = document.getElementById("page-content");
        if (!authenticated() || !isReviewer()) {
            target.innerHTML = empty("Sign in with a reviewer or admin account to access this queue.");
            return;
        }
        try {
            let [items, verifiedItems] = await Promise.all([
                api.get("/api/reviewer/documents"),
                api.get("/api/reviewer/documents/verified")
            ]);
            const selectedStudentId = new URLSearchParams(location.search).get("studentId");
            if (selectedStudentId) {
                items = items.filter((item) => String(item.studentId) === selectedStudentId);
                verifiedItems = verifiedItems.filter((item) => String(item.studentId) === selectedStudentId);
            }
            const studentIds = [...new Set(verifiedItems.map((item) => String(item.studentId)))];
            const verifiedFacts = await Promise.all(studentIds.map(async (id) => [
                id,
                await api.get(`/api/reviewer/documents/students/${encodeURIComponent(id)}/facts`)
            ]));
            const factsByStudent = new Map(verifiedFacts);
            const pendingSection = items.length ? `<div class="review-queue">${items.map((item) => `<article class="panel reviewer-document" data-id="${escapeHtml(item.id)}">
                <div class="panel-title"><div><h2>${escapeHtml(item.fileName)}</h2><p>${escapeHtml(item.studentName)} · ${escapeHtml(item.documentType)}</p></div>${badge(item.verificationStatus)}</div>
                <p>Uploaded ${escapeHtml(item.uploadedAt || "")}</p>
                <button class="button button-outline open-review-file" data-id="${escapeHtml(item.id)}">Open document</button>
                <div class="review-preview" hidden></div>
                <label class="form-group"><span class="form-label">Feedback (required when rejecting)</span><textarea class="field reviewer-feedback" rows="3" maxlength="2000"></textarea></label>
                <div class="card-actions"><button class="button button-primary decide-document" data-status="VERIFIED" data-id="${escapeHtml(item.id)}">Verify</button><button class="button button-outline decide-document" data-status="REJECTED" data-id="${escapeHtml(item.id)}">Reject</button></div>
                <div class="form-feedback"></div>
            </article>`).join("")}</div>` : empty("There are no documents waiting for review.");
            const verifiedSection = verifiedItems.length ? `<div class="review-queue">${verifiedItems.map((item) => {
                const facts = (factsByStudent.get(String(item.studentId)) || [])
                    .filter((fact) => String(fact.sourceDocumentId) === String(item.id));
                const factHistory = facts.length ? `<ul class="confirmed-facts">${facts.map((fact) => `<li><div class="fact-history-field"><small>Confirmed fact</small><strong>${escapeHtml(factLabel(fact.factType))}</strong></div><div class="fact-history-field"><small>Verified value</small><strong>${escapeHtml(fact.value)}</strong></div><div class="fact-history-field"><small>Source document</small><strong>${escapeHtml(fact.sourceFileName)}</strong></div><div class="fact-history-field"><small>Verified</small><strong>${escapeHtml(dateTimeText(fact.verifiedAt))}</strong></div></li>`).join("")}</ul>` : empty("No facts have been confirmed for this student yet.");
                return `<article class="panel reviewer-document verified-document" data-id="${escapeHtml(item.id)}" data-student-id="${escapeHtml(item.studentId)}">
                <div class="panel-title"><div><h2>${escapeHtml(item.fileName)}</h2><p>${escapeHtml(item.studentName)} · ${escapeHtml(item.documentType)}</p></div>${badge(item.verificationStatus)}</div>
                <p>Verified ${escapeHtml(item.reviewedAt || "")}. Confirm only facts that are visibly supported by this document.</p>
                <button class="button button-outline open-review-file" data-id="${escapeHtml(item.id)}">Open document</button>
                <div class="review-preview" hidden></div>
                <h3>Confirmed fact history</h3>${factHistory}
                <form class="verified-fact-form">
                    <label class="form-group"><span class="form-label">Fact supported by this document</span><select class="field fact-type" required><option value="CGPA">CGPA</option><option value="ANNUAL_FAMILY_INCOME">Annual family income</option><option value="STATE">State</option><option value="CATEGORY">Category</option><option value="COURSE">Course</option><option value="EDUCATION_LEVEL">Education level</option><option value="GENDER">Gender</option><option value="DISABILITY_STATUS">Disability status</option><option value="DISABILITY_PERCENTAGE">Disability percentage</option><option value="SEAT_TYPE">Admission / seat type</option><option value="PREVIOUS_MARKS_PERCENTAGE">Previous exam marks percentage</option><option value="PUC_MARKS_PERCENTAGE">PUC / Class 12 marks percentage</option><option value="HOSTEL_STATUS">Hostel seat status</option><option value="YEAR_OF_STUDY">Year of study</option></select></label>
                    <label class="form-group"><span class="form-label">Value read from the document</span><input class="field fact-value" maxlength="255" required></label>
                    <button class="button button-primary button-small" type="submit">Confirm fact</button>
                </form>
                <div class="fact-feedback" aria-live="polite"></div>
            </article>`;
            }).join("")}</div>` : empty("No verified documents are available for fact confirmation.");
            target.innerHTML = `${selectedStudentId ? '<p><a class="text-link" href="reviewer-applications.html">← Back to all application requests</a></p>' : ""}<section class="panel"><h2>Review student documents</h2><p>Open each pending document. Verify it if the file is readable and matches its type; otherwise reject it and explain what the student should correct.</p></section><section class="panel"><h2>Waiting for review (${items.length})</h2>${pendingSection}</section><details class="panel"><summary>Optional: confirm eligibility facts from verified documents (${verifiedItems.length})</summary><p>Only use this section if the project is using reviewer-confirmed eligibility. A verified document does not automatically confirm its contents.</p>${verifiedSection}</details>`;

            target.querySelectorAll(".open-review-file").forEach((button) => button.addEventListener("click", async () => {
                const preview = button.closest(".reviewer-document").querySelector(".review-preview");
                preview.hidden = false;
                preview.innerHTML = loading;
                try {
                    const blob = await api.getBlob(`/api/reviewer/documents/${encodeURIComponent(button.dataset.id)}/content`);
                    const url = URL.createObjectURL(blob);
                    preview.innerHTML = blob.type.startsWith("image/")
                        ? `<img class="review-file-image" src="${url}" alt="Document being reviewed">`
                        : `<iframe class="review-file-frame" src="${url}" title="Document being reviewed"></iframe>`;
                } catch (error) { showError(preview, error); }
            }));

            target.querySelectorAll(".decide-document").forEach((button) => button.addEventListener("click", async () => {
                const card = button.closest(".reviewer-document");
                const feedback = card.querySelector(".reviewer-feedback").value.trim();
                if (button.dataset.status === "REJECTED" && !feedback) {
                    toast("Add feedback before rejecting this document.");
                    return;
                }
                try {
                    await api.patch(`/api/reviewer/documents/${encodeURIComponent(button.dataset.id)}/review`, {
                        status: button.dataset.status,
                        feedback
                    });
                    reviewerDocumentsPage();
                    toast(`Document ${button.dataset.status.toLowerCase()}.`);
                } catch (error) { showError(card.querySelector(".form-feedback"), error); }
            }));

            target.querySelectorAll(".verified-fact-form").forEach((form) => form.addEventListener("submit", async (event) => {
                event.preventDefault();
                const card = form.closest(".verified-document");
                const feedback = card.querySelector(".fact-feedback");
                const button = form.querySelector("button[type='submit']");
                button.disabled = true;
                feedback.innerHTML = loading;
                try {
                    const saved = await api.put(`/api/reviewer/documents/${encodeURIComponent(card.dataset.id)}/facts`, {
                        facts: [{ factType: form.querySelector(".fact-type").value, value: form.querySelector(".fact-value").value.trim() }]
                    });
                    feedback.innerHTML = `<div class="api-message" role="status">${escapeHtml(saved[0].factType)} confirmed from ${escapeHtml(saved[0].sourceFileName)} at ${escapeHtml(saved[0].verifiedAt)}.</div>`;
                } catch (error) { showError(feedback, error); }
                finally { button.disabled = false; }
            }));
        } catch (error) { showError(target, error); }
    }

    async function notificationsPage() {
        shell("Notifications", "Application notifications for your account.", loading);
        const target = document.getElementById("page-content");
        if (!idsPresent("user")) { target.innerHTML = needId("user"); return; }
        try {
            const items = await api.get(`/api/notifications/${encodeURIComponent(userId())}`);
            let emailPreference = null;
            if (!isReviewer()) {
                try { emailPreference = await api.get("/api/notifications/preferences/email"); }
                catch (error) { emailPreference = { error: error.message }; }
            }
            const preferencePanel = isReviewer() ? "" : `<section class="panel email-preference-panel"><div class="panel-title"><h2>Email notifications</h2></div><p>Choose whether ScholarAssist sends you an email when a new in-app update is created. Email messages contain a link to sign in; application and document details stay in ScholarAssist.</p>${emailPreference?.error ? `<div class="api-message api-error" role="alert">${escapeHtml(emailPreference.error)}</div>` : `<label class="email-preference-control"><input type="checkbox" id="email-notifications-enabled" ${emailPreference?.enabled ? "checked" : ""}><span>Send me email alerts for new application and document updates</span></label><div class="email-status-row" aria-live="polite"><span>Preference: <strong id="email-preference-state">${emailPreference?.enabled ? "On" : "Off"}</strong></span><span>Delivery: <strong id="email-delivery-state" class="${emailPreference?.deliveryAvailable ? "configured" : "unavailable"}">${emailPreference?.deliveryAvailable ? "Configured" : "Not configured"}</strong></span></div><div class="form-hint" id="email-preference-hint">${emailPreference?.deliveryAvailable ? "Email delivery is configured. New updates will be emailed when your preference is on." : emailPreference?.enabled ? "Your preference is saved, but email delivery is not configured on this server. Updates remain available in the Notifications page." : "You can save your preference now. Email delivery is not configured on this server, so updates will not be emailed yet."}</div><div class="form-feedback" id="email-preference-feedback" role="status" aria-live="polite"></div>`}</section>`;
            target.innerHTML = preferencePanel + (items.length ? `<div class="notification-list">${items.map((item) => `<article class="notification-card ${item.isRead ? "read" : ""}"><i class="notification-dot"></i><div><h3>${escapeHtml(item.title)}</h3><p>${escapeHtml(item.message)}</p><small>${escapeHtml(item.createdAt || "")} · ${escapeHtml(item.type || "")}</small></div>${item.isRead ? "" : `<button class="button button-quiet button-small mark-read" data-id="${escapeHtml(item.id)}">Mark read</button>`}</article>`).join("")}</div>` : empty("No notifications yet."));
            target.querySelector("#email-notifications-enabled")?.addEventListener("change", async (event) => {
                const checkbox = event.currentTarget;
                const feedback = target.querySelector("#email-preference-feedback");
                checkbox.disabled = true;
                feedback.textContent = "Saving preference…";
                try {
                    const saved = await api.put("/api/notifications/preferences/email", { enabled: checkbox.checked });
                    checkbox.checked = saved.enabled;
                    target.querySelector("#email-preference-state").textContent = saved.enabled ? "On" : "Off";
                    target.querySelector("#email-delivery-state").textContent = saved.deliveryAvailable ? "Configured" : "Not configured";
                    target.querySelector("#email-delivery-state").className = saved.deliveryAvailable ? "configured" : "unavailable";
                    target.querySelector("#email-preference-hint").textContent = saved.deliveryAvailable
                        ? (saved.enabled ? "Email delivery is configured. New updates will be emailed when your preference is on." : "Email delivery is configured, but alerts are off. In-app notifications continue to work.")
                        : (saved.enabled ? "Your preference is saved, but email delivery is not configured on this server. Updates remain available in the Notifications page." : "You can save your preference now. Email delivery is not configured on this server, so updates will not be emailed yet.");
                    feedback.textContent = saved.enabled ? "Email preference saved: On." : "Email preference saved: Off.";
                } catch (error) {
                    checkbox.checked = !checkbox.checked;
                    feedback.textContent = error.message;
                    feedback.classList.add("api-error");
                } finally {
                    checkbox.disabled = false;
                }
            });
            target.querySelectorAll(".mark-read").forEach((button) => button.addEventListener("click", async () => {
                try { await api.patch(`/api/notifications/${encodeURIComponent(button.dataset.id)}/read`); notificationsPage(); }
                catch (error) { toast(error.message); }
            }));
        } catch (error) { showError(target, error); }
    }

    function preliminaryChecksKey(ownerId) {
        return `scholarAssistPreliminaryChecks:${ownerId}`;
    }

    function readPreliminaryChecks(ownerId) {
        if (!ownerId) return [];
        try {
            const saved = JSON.parse(sessionStorage.getItem(preliminaryChecksKey(ownerId)) || "[]");
            return Array.isArray(saved) ? saved : [];
        } catch (error) {
            return [];
        }
    }

    function rememberPreliminaryCheck(ownerId, scholarship, result) {
        if (!ownerId) return;
        const previous = readPreliminaryChecks(ownerId)
            .filter((entry) => String(entry.scholarshipId) !== String(result.scholarshipId));
        previous.unshift({
            scholarshipId: result.scholarshipId,
            scholarshipName: scholarship.name,
            status: result.status,
            reasons: Array.isArray(result.reasons) ? result.reasons.slice(0, 4) : [],
            checkedAt: new Date().toISOString()
        });
        sessionStorage.setItem(preliminaryChecksKey(ownerId), JSON.stringify(previous.slice(0, 12)));
    }

    async function dashboardData(request, fallback = []) {
        try {
            return { value: await request, error: null };
        } catch (error) {
            return { value: fallback, error };
        }
    }

    function dashboardError(error) {
        return error
            ? `<div class="api-message api-error" role="alert">${escapeHtml(error.message)}</div>`
            : "";
    }

    function dashboardLink(label, href) {
        return `<a class="text-link" href="${escapeHtml(href)}">${escapeHtml(label)} →</a>`;
    }

    function formatDateTime(value) {
        if (!value) return "Time not available";
        const parsed = new Date(value);
        return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString();
    }

    async function dashboardPage() {
        shell("Dashboard", "Your scholarships, applications, and next steps in one place.", loading);
        const target = document.getElementById("page-content");

        if (!authenticated() || isReviewer()) {
            target.innerHTML = empty("Sign in with a student account to view your personalized dashboard.")
                + '<p><a class="button button-primary" href="login.html">Student sign in</a></p>';
            return;
        }

        const profile = currentStudentProfile;
        const hasStudentProfile = Boolean(studentId());
        const preliminaryChecks = readPreliminaryChecks(userId());
        const [scholarshipState, applicationState, documentState, notificationState, eligibilityState] =
            await Promise.all([
                dashboardData(fetchScholarships()),
                hasStudentProfile
                    ? dashboardData(api.get(`/api/applications/${encodeURIComponent(studentId())}`))
                    : Promise.resolve({ value: [], error: null }),
                hasStudentProfile
                    ? dashboardData(api.get(`/api/documents/${encodeURIComponent(studentId())}`))
                    : Promise.resolve({ value: [], error: null }),
                dashboardData(api.get(`/api/notifications/${encodeURIComponent(userId())}/unread`)),
                hasStudentProfile
                    ? dashboardData(api.get(`/api/eligibility/results/${encodeURIComponent(studentId())}`))
                    : Promise.resolve({ value: [], error: null })
            ]);

        const scholarships = scholarshipState.value || [];
        const applications = applicationState.value || [];
        const documents = documentState.value || [];
        const unreadNotifications = notificationState.value || [];
        const verifiedResults = eligibilityState.value || [];
        const scholarshipById = new Map(scholarships.map((item) => [String(item.id), item]));

        const recentApplications = [...applications]
            .sort((first, second) => String(second.updatedAt || "").localeCompare(String(first.updatedAt || "")))
            .slice(0, 4);
        const historyPairs = await Promise.all(recentApplications.map(async (application) => {
            const state = await dashboardData(
                api.get(`/api/applications/${encodeURIComponent(application.id)}/history`));
            return [String(application.id), state];
        }));
        const historyByApplication = new Map(historyPairs);

        const preliminaryCards = preliminaryChecks
            .filter((check) => scholarshipById.has(String(check.scholarshipId)))
            .slice(0, 4)
            .map((check) => `<article class="dashboard-record">
                <div class="dashboard-record-heading"><strong>${escapeHtml(check.scholarshipName)}</strong>${badge(check.status)}</div>
                <p>Self-reported preliminary check · ${escapeHtml(formatDateTime(check.checkedAt))}</p>
                ${(check.reasons || []).slice(0, 2).map((reason) => `<p>${escapeHtml(reason)}</p>`).join("")}
                ${dashboardLink("Review scholarship and check again", `scholarship-details.html?id=${encodeURIComponent(check.scholarshipId)}`)}
            </article>`).join("");

        const verifiedMatches = verifiedResults
            .filter((result) => ["ELIGIBLE", "POSSIBLY_ELIGIBLE"].includes(result.status))
            .filter((result) => scholarshipById.has(String(result.scholarshipId)))
            .slice(0, 4)
            .map((result) => {
                const scholarship = scholarshipById.get(String(result.scholarshipId));
                return `<article class="dashboard-record">
                    <div class="dashboard-record-heading"><strong>${escapeHtml(scholarship.name)}</strong>${badge(result.status)}</div>
                    <p>Based on reviewer-confirmed facts · checked ${escapeHtml(formatDateTime(result.evaluatedAt))}</p>
                    <p>${escapeHtml(result.reason || "No explanation was provided.")}</p>
                    ${dashboardLink("View scholarship", `scholarship-details.html?id=${encodeURIComponent(scholarship.id)}`)}
                </article>`;
            }).join("");

        const applicationCards = recentApplications.map((application) => {
            const historyState = historyByApplication.get(String(application.id));
            const history = historyState?.value || [];
            const latest = history.length ? history[history.length - 1] : null;
            const message = latest?.note || application.remarks;
            const messageLabel = latest?.changeSource === "REVIEWER" ? "Reviewer update" : "Latest update";
            return `<article class="dashboard-record">
                <div class="dashboard-record-heading"><strong>${escapeHtml(application.scholarshipName)}</strong>${badge(application.status)}</div>
                <p>Last changed ${escapeHtml(formatDateTime(application.updatedAt))}</p>
                ${message ? `<p><b>${escapeHtml(messageLabel)}:</b> ${escapeHtml(message)}</p>` : "<p>No reviewer message is recorded yet.</p>"}
                ${historyState?.error ? dashboardError(historyState.error) : ""}
                ${dashboardLink("Open application tracker", `application-details.html?id=${encodeURIComponent(application.id)}`)}
            </article>`;
        }).join("");

        const documentCards = [...documents]
            .sort((first, second) => String(second.uploadedAt || "").localeCompare(String(first.uploadedAt || "")))
            .slice(0, 4)
            .map((document) => `<article class="dashboard-record">
                <div class="dashboard-record-heading"><strong>${escapeHtml(document.fileName)}</strong>${badge(document.verificationStatus)}</div>
                <p>${escapeHtml(document.documentType)} · uploaded ${escapeHtml(formatDateTime(document.uploadedAt))}</p>
                ${document.verificationStatus === "REJECTED"
                    ? `<p><b>Reviewer feedback:</b> ${escapeHtml(document.reviewFeedback || "No feedback was provided.")}</p>`
                    : ""}
            </article>`).join("");

        const unreadCards = unreadNotifications.slice(0, 3).map((notification) =>
            `<article class="dashboard-record"><strong>${escapeHtml(notification.title)}</strong>
                <p>${escapeHtml(notification.message)}</p><small>${escapeHtml(formatDateTime(notification.createdAt))}</small></article>`
        ).join("");

        const trackedScholarshipIds = new Set(applications.map((application) => String(application.scholarshipId)));
        verifiedResults.filter((result) => ["ELIGIBLE", "POSSIBLY_ELIGIBLE"].includes(result.status))
            .forEach((result) => trackedScholarshipIds.add(String(result.scholarshipId)));
        preliminaryChecks.filter((check) => check.status === "PRELIMINARY_MATCH")
            .forEach((check) => trackedScholarshipIds.add(String(check.scholarshipId)));
        const upcomingDeadlines = [...trackedScholarshipIds]
            .map((id) => scholarshipById.get(id))
            .filter((scholarship) => scholarship?.deadline && scholarship.deadline >= localDateToday())
            .sort((first, second) => first.deadline.localeCompare(second.deadline))
            .slice(0, 5)
            .map((scholarship) => `<article class="dashboard-record dashboard-deadline">
                <strong>${escapeHtml(scholarship.name)}</strong><span>Apply by ${escapeHtml(dateText(scholarship.deadline))}</span>
                ${dashboardLink("View details", `scholarship-details.html?id=${encodeURIComponent(scholarship.id)}`)}
            </article>`).join("");

        const preliminarySection = preliminaryCards || scholarshipState.error
            ? `<section class="panel"><div class="panel-title"><h2>Recent quick checks</h2>${dashboardLink("Browse scholarships", "scholarships.html")}</div>
                <p class="form-hint">Self-reported preliminary results; not document verification or an award decision. Recent checks are stored in this browser only.</p>
                ${dashboardError(scholarshipState.error)}${preliminaryCards}</section>`
            : "";

        target.innerHTML = `
            <div class="stats-grid">
                <article class="stat-card"><span class="stat-label">Matching scholarships</span><strong>${verifiedResults.filter((result) => result.status === "ELIGIBLE").length + preliminaryChecks.filter((check) => check.status === "PRELIMINARY_MATCH").length}</strong></article>
                <article class="stat-card"><span class="stat-label">Tracked applications</span><strong>${applications.length}</strong></article>
                <article class="stat-card"><span class="stat-label">Documents</span><strong>${documents.length}</strong></article>
                <article class="stat-card"><span class="stat-label">Unread notifications</span><strong>${unreadNotifications.length}</strong></article>
            </div>
            <div class="dashboard-overview-grid">
                ${preliminarySection}
                <section class="panel"><div class="panel-title"><h2>Reviewer-confirmed eligibility</h2>${dashboardLink("View scholarships", "scholarships.html")}</div>
                    <p class="form-hint">These saved results use facts a reviewer confirmed from documents. They are not an award decision.</p>
                    ${dashboardError(eligibilityState.error)}
                    ${verifiedMatches || empty(hasStudentProfile ? "No eligible or incomplete verified results are saved yet." : "Create a student profile before reviewer-confirmed eligibility can be checked.")}
                </section>
                <section class="panel"><div class="panel-title"><h2>Application tracking</h2>${dashboardLink("All applications", "applications.html")}</div>
                    ${dashboardError(applicationState.error)}
                    ${applicationCards || empty(hasStudentProfile ? "No tracked applications yet. Apply now opens the provider website and starts a local tracker." : "Create a profile to start tracking applications.")}
                </section>
                <section class="panel"><div class="panel-title"><h2>Document review</h2>${dashboardLink("All documents", "documents.html")}</div>
                    ${dashboardError(documentState.error)}
                    ${documentCards || empty(hasStudentProfile ? "No documents uploaded yet." : "Create a profile before uploading documents.")}
                </section>
                <section class="panel"><div class="panel-title"><h2>Upcoming deadlines</h2>${dashboardLink("Find scholarships", "scholarships.html")}</div>
                    ${upcomingDeadlines || empty("No upcoming deadlines for tracked or matching scholarships.")}
                </section>
                <section class="panel"><div class="panel-title"><h2>Unread notifications (${unreadNotifications.length})</h2>${dashboardLink("Open notifications", "notifications.html")}</div>
                    ${dashboardError(notificationState.error)}
                    ${unreadCards || empty("You have no unread notifications.")}
                </section>
            </div>`;
    }

    function calendarDateKey(year, monthIndex, day) {
        return `${year}-${String(monthIndex + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
    }

    function deadlineCalendarMarkup(events, displayedMonth, selectedDate) {
        const year = displayedMonth.getFullYear();
        const monthIndex = displayedMonth.getMonth();
        const firstWeekday = new Date(year, monthIndex, 1).getDay();
        const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
        const today = localDateToday();
        const eventsByDate = new Map();
        events.forEach((event) => {
            if (!eventsByDate.has(event.deadline)) eventsByDate.set(event.deadline, []);
            eventsByDate.get(event.deadline).push(event);
        });
        const cells = [];
        for (let index = 0; index < firstWeekday; index += 1) {
            cells.push('<div class="deadline-calendar-cell outside" aria-hidden="true"></div>');
        }
        for (let day = 1; day <= daysInMonth; day += 1) {
            const key = calendarDateKey(year, monthIndex, day);
            const dayEvents = eventsByDate.get(key) || [];
            const classes = ["deadline-calendar-cell", key === today ? "today" : "", key === selectedDate ? "selected" : ""]
                .filter(Boolean).join(" ");
            cells.push(`<button type="button" class="${classes}" data-calendar-date="${key}" aria-label="${escapeHtml(new Date(year, monthIndex, day).toLocaleDateString(undefined, { month: "long", day: "numeric", year: "numeric" }))}${dayEvents.length ? `, ${dayEvents.length} deadline${dayEvents.length === 1 ? "" : "s"}` : ", no deadlines"}" aria-pressed="${key === selectedDate}"><span>${day}</span>${dayEvents.length ? `<i aria-hidden="true"></i><small>${dayEvents.length} due</small>` : ""}</button>`);
        }
        while (cells.length % 7) cells.push('<div class="deadline-calendar-cell outside" aria-hidden="true"></div>');
        const selectedEvents = (eventsByDate.get(selectedDate) || []).sort((a, b) => a.name.localeCompare(b.name));
        const upcoming = events.filter((event) => event.deadline >= today)
            .sort((a, b) => a.deadline.localeCompare(b.deadline) || a.name.localeCompare(b.name)).slice(0, 8);
        const dayList = selectedEvents.length
            ? `<div class="deadline-event-list">${selectedEvents.map((event) => `<article class="deadline-event"><div><strong>${escapeHtml(event.name)}</strong><p>${escapeHtml(event.provider || "Scholarship")}${event.applicationStatus ? ` · Tracked: ${escapeHtml(factLabel(event.applicationStatus))}` : ""}</p></div><a class="text-link" href="${event.applicationId ? `application-details.html?id=${encodeURIComponent(event.applicationId)}` : `scholarship-details.html?id=${encodeURIComponent(event.scholarshipId)}`}" aria-label="Open ${escapeHtml(event.name)}">Open →</a></article>`).join("")}</div>`
            : empty("No scholarship deadlines on this date.");
        const upcomingList = upcoming.length
            ? `<div class="deadline-event-list">${upcoming.map((event) => `<article class="deadline-event"><div><time datetime="${escapeHtml(event.deadline)}">${escapeHtml(dateText(event.deadline))}</time><strong>${escapeHtml(event.name)}</strong><p>${escapeHtml(event.provider || "Scholarship")}${event.applicationStatus ? ` · Tracked: ${escapeHtml(factLabel(event.applicationStatus))}` : ""}</p></div><a class="text-link" href="${event.applicationId ? `application-details.html?id=${encodeURIComponent(event.applicationId)}` : `scholarship-details.html?id=${encodeURIComponent(event.scholarshipId)}`}">Open →</a></article>`).join("")}</div>`
            : empty("No upcoming deadlines are available in the current scholarship catalog or your trackers.");
        return `<section class="panel deadline-calendar-panel">
            <div class="deadline-calendar-toolbar"><button class="button button-outline button-small" type="button" data-calendar-move="-1" aria-label="Previous month">‹</button><h2>${escapeHtml(displayedMonth.toLocaleDateString(undefined, { month: "long", year: "numeric" }))}</h2><button class="button button-outline button-small" type="button" data-calendar-move="1" aria-label="Next month">›</button><button class="button button-quiet button-small" type="button" data-calendar-today>Today</button></div>
            <p class="form-hint">Deadlines come from active scholarship records and your application trackers. Dates are reminders; always confirm the final deadline with the official provider.</p>
            <div class="deadline-calendar-grid" role="grid" aria-label="Scholarship deadline calendar">
                ${["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((day) => `<div class="deadline-calendar-weekday" role="columnheader">${day}</div>`).join("")}
                ${cells.join("")}
            </div>
        </section><section class="panel" aria-live="polite"><div class="panel-title"><h2>Deadlines on ${escapeHtml(dateText(selectedDate))}</h2></div>${dayList}</section>
        <section class="panel"><div class="panel-title"><h2>Upcoming deadlines</h2></div>${upcomingList}</section>`;
    }

    async function deadlinesPage() {
        shell("Deadline calendar", "See scholarship deadlines and deadlines for applications you are tracking.", loading);
        const target = document.getElementById("page-content");
        if (!authenticated() || isReviewer()) {
            target.innerHTML = empty("Sign in with a student account to view scholarship deadlines.") + '<p><a class="button button-primary" href="login.html">Student sign in</a></p>';
            return;
        }
        let displayedMonth = new Date();
        let selectedDate = localDateToday();
        target.innerHTML = loading;
        try {
            const [scholarships, applications] = await Promise.all([
                fetchScholarships(),
                studentId() ? api.get(`/api/applications/${encodeURIComponent(studentId())}`) : Promise.resolve([])
            ]);
            const byScholarship = new Map();
            (scholarships || []).forEach((item) => {
                if (!item.deadline) return;
                byScholarship.set(String(item.id), {
                    scholarshipId: item.id, name: item.name, provider: item.provider,
                    deadline: String(item.deadline).slice(0, 10), applicationStatus: "", applicationId: ""
                });
            });
            (applications || []).forEach((item) => {
                if (!item.scholarshipDeadline) return;
                const key = String(item.scholarshipId);
                const existing = byScholarship.get(key) || {
                    scholarshipId: item.scholarshipId, name: item.scholarshipName,
                    provider: item.scholarshipProvider || "", deadline: String(item.scholarshipDeadline).slice(0, 10)
                };
                existing.applicationStatus = item.status;
                existing.applicationId = item.id;
                existing.name = item.scholarshipName || existing.name;
                existing.deadline = String(item.scholarshipDeadline).slice(0, 10);
                byScholarship.set(key, existing);
            });
            const events = [...byScholarship.values()].filter((item) => /^\d{4}-\d{2}-\d{2}$/.test(item.deadline));
            const draw = () => {
                target.innerHTML = deadlineCalendarMarkup(events, displayedMonth, selectedDate);
                target.querySelectorAll("[data-calendar-move]").forEach((button) => button.addEventListener("click", () => {
                    displayedMonth = new Date(displayedMonth.getFullYear(), displayedMonth.getMonth() + Number(button.dataset.calendarMove), 1);
                    selectedDate = calendarDateKey(displayedMonth.getFullYear(), displayedMonth.getMonth(), 1);
                    draw();
                }));
                target.querySelector("[data-calendar-today]")?.addEventListener("click", () => {
                    displayedMonth = new Date(); selectedDate = localDateToday(); draw();
                });
                target.querySelectorAll("[data-calendar-date]").forEach((button) => button.addEventListener("click", () => {
                    selectedDate = button.dataset.calendarDate; draw();
                }));
            };
            draw();
        } catch (error) {
            showError(target, error);
        }
    }

    function authPage(isRegistration, reviewerMode = false) {
        const title = isRegistration
            ? "Create your account"
            : reviewerMode ? "Reviewer and admin sign in" : "Welcome back";
        const intro = isRegistration
            ? "Create a ScholarAssist account to manage your scholarship journey."
            : reviewerMode
                ? "Sign in with your existing reviewer or admin account to review applications and documents."
                : "Sign in to continue to your student workspace.";
        const passwordHint = isRegistration
            ? '<small class="form-hint">Use at least 8 characters.</small>'
            : "";

        app.innerHTML = '<main class="auth-page"><section class="auth-card">' +
            '<a class="brand" href="../index.html"><span class="brand-mark">S</span> ScholarAssist</a>' +
            '<h1>' + title + '</h1><p>' + intro + '</p>' +
            (reviewerMode
                ? '<div class="development-note">Reviewer accounts are provisioned by the project administrator. There is no public reviewer registration. If you were granted access, sign in using the email and password configured for your reviewer account.</div>'
                : '') +
            '<form id="auth-form">' +
            '<label class="form-group"><span class="form-label">Email</span>' +
            '<input class="field" name="email" type="email" autocomplete="email" required maxlength="255"></label>' +
            '<label class="form-group"><span class="form-label">Password</span>' +
            '<span class="password-field"><input class="field" id="auth-password" name="password" type="password" autocomplete="' +
            (isRegistration ? "new-password" : "current-password") + '" required ' +
            (isRegistration ? 'minlength="8"' : "") + '><button class="password-visibility" type="button" aria-label="Show password" aria-pressed="false"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg></button></span>' + passwordHint + '</label>' +
            '<div class="form-feedback" id="auth-feedback" role="alert"></div>' +
            '<button class="button button-primary" type="submit">' +
            (isRegistration ? "Create account" : "Sign in") + '</button></form>' +
            '<p class="auth-switch">' + (isRegistration
                ? 'Already registered? <a href="login.html">Sign in</a>'
                : reviewerMode
                    ? 'Student account? <a href="login.html">Student sign in</a>'
                    : 'New to ScholarAssist? <a href="register.html">Create an account</a>') + '</p>' +
            (!isRegistration && !reviewerMode
                ? '<p class="auth-switch">Reviewer or admin? <a href="reviewer-login.html">Use reviewer sign in</a></p>'
                : '') +
            '<a class="text-link" href="scholarships.html">Return to scholarships</a>' +
            '</section></main>';

        const form = app.querySelector("#auth-form");
        const passwordInput = form.elements.password;
        const visibilityButton = form.querySelector(".password-visibility");
        visibilityButton.addEventListener("click", () => {
            const reveal = passwordInput.type === "password";
            passwordInput.type = reveal ? "text" : "password";
            visibilityButton.setAttribute("aria-label", reveal ? "Hide password" : "Show password");
            visibilityButton.setAttribute("aria-pressed", String(reveal));
        });
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (!form.reportValidity()) return;

            const submitButton = form.querySelector("button[type=submit]");
            const feedback = form.querySelector("#auth-feedback");
            submitButton.disabled = true;
            feedback.textContent = "";
            const requestBody = {
                email: form.elements.email.value.trim(),
                password: form.elements.password.value
            };

            try {
                const result = await api.post(
                    isRegistration ? "/api/auth/register" : "/api/auth/login",
                    requestBody);
                if (isRegistration) {
                    feedback.innerHTML = '<div class="api-message" role="status">Account created. You can now sign in.</div>';
                    form.reset();
                    return;
                }

                const role = String(result.user.role || "STUDENT").toUpperCase();
                sessionStorage.setItem("scholarAssistEmail", String(result.user.email || requestBody.email));
                sessionStorage.setItem("scholarAssistDisplayName", String(result.user.fullName || ""));
                if (reviewerMode && !["REVIEWER", "ADMIN"].includes(role)) {
                    await api.logout();
                    sessionStorage.removeItem("scholarAssistAuthenticated");
                    sessionStorage.removeItem("scholarAssistUserId");
                    sessionStorage.removeItem("scholarAssistStudentId");
                    sessionStorage.removeItem("scholarAssistRole");
                    feedback.textContent = "This account does not have reviewer or admin access. Sign in with an authorized account.";
                    return;
                }

                sessionStorage.setItem("scholarAssistAuthenticated", "true");
                sessionStorage.setItem("scholarAssistUserId", String(result.user.id));
                sessionStorage.setItem("scholarAssistRole", role);
                sessionStorage.setItem("scholarAssistEmail", String(result.user.email || requestBody.email));
                sessionStorage.setItem("scholarAssistDisplayName", String(result.user.fullName || ""));
                sessionStorage.removeItem("scholarAssistStudentId");

                if (role === "STUDENT") {
                    try {
                        const profile = await api.get(
                            "/api/student-profile/" + encodeURIComponent(result.user.id));
                        if (profile && profile.id) {
                            sessionStorage.setItem("scholarAssistStudentId", String(profile.id));
                        }
                    } catch (profileError) {
                        if (profileError.status !== 404) throw profileError;
                    }
                }

                location.href = role === "STUDENT"
                    ? "dashboard.html"
                    : "reviewer-applications.html";
            } catch (error) {
                showError(feedback, error);
            } finally {
                submitButton.disabled = false;
            }
        });
    }

    const routes = {
        dashboard: dashboardPage,
        deadlines: deadlinesPage,
        scholarships: () => scholarshipsPage(false),
        "scholarship-details": scholarshipDetails,
        profile: profilePage,
        applications: applicationsPage,
        "application-details": applicationDetails,
        "reviewer-applications": reviewerApplicationsPage,
        "reviewer-application-details": reviewerApplicationDetails,
        documents: documentsPage,
        "reviewer-documents": reviewerDocumentsPage,
        notifications: notificationsPage,
        login: () => authPage(false),
        "reviewer-login": () => authPage(false, true),
        register: () => authPage(true)
    };

    if (!pageName || !app) return;

    async function initializePortal() {
        if (pageName !== "login" && pageName !== "register" && pageName !== "reviewer-login") {
            try {
                const user = await api.get("/api/auth/me");
                currentStudentProfile = null;
                sessionStorage.setItem("scholarAssistAuthenticated", "true");
                sessionStorage.setItem("scholarAssistUserId", String(user.id));
                sessionStorage.setItem("scholarAssistRole", String(user.role || "STUDENT"));
                sessionStorage.setItem("scholarAssistEmail", String(user.email || ""));
                sessionStorage.removeItem("scholarAssistStudentId");

                if (user.role === "STUDENT") {
                    try {
                        const profile = await api.get(
                            "/api/student-profile/" + encodeURIComponent(user.id));
                        if (profile && profile.id) {
                            currentStudentProfile = profile;
                            sessionStorage.setItem("scholarAssistDisplayName", String(profile.fullName || ""));
                            sessionStorage.setItem(
                                "scholarAssistStudentId", String(profile.id));
                        }
                    } catch (error) {
                        if (error.status !== 404) {
                            throw error;
                        }
                    }
                }
            } catch (error) {
                sessionStorage.removeItem("scholarAssistAuthenticated");
                sessionStorage.removeItem("scholarAssistUserId");
                sessionStorage.removeItem("scholarAssistStudentId");
                sessionStorage.removeItem("scholarAssistRole");
            }
        }

        (routes[pageName] || dashboardPage)();
    }

    initializePortal();
})();
