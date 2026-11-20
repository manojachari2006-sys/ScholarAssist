/* Shared API client for JSON, multipart uploads, sessions, and CSRF protection. */
(function () {
    const config = window.SCHOLARASSIST_CONFIG || {};
    let csrfToken = null;
    let csrfHeaderName = "X-CSRF-TOKEN";

    function baseUrl() {
        return String(config.apiBaseUrl || "http://localhost:8082").replace(/\/$/, "");
    }

    async function fetchCsrfToken() {
        let response;
        try {
            response = await fetch(baseUrl() + "/api/auth/csrf", {
                method: "GET",
                credentials: "include",
                headers: { Accept: "application/json" }
            });
        } catch (error) {
            throw new Error("Could not reach the backend while requesting a security token. Check that Spring Boot is running and CORS allows this frontend origin.");
        }

        if (!response.ok) {
            throw new Error("Could not obtain a security token. Check that the backend is running.");
        }

        const token = await response.json();
        csrfToken = token.token;
        csrfHeaderName = token.headerName || "X-CSRF-TOKEN";
    }

    function clearLocalSession() {
        sessionStorage.removeItem("scholarAssistAuthenticated");
        sessionStorage.removeItem("scholarAssistUserId");
        sessionStorage.removeItem("scholarAssistStudentId");
        sessionStorage.removeItem("scholarAssistRole");
    }

    async function request(path, options = {}) {
        const method = String(options.method || "GET").toUpperCase();
        const headers = new Headers(options.headers || {});
        const isFormData = options.body instanceof FormData;
        const changesState = !["GET", "HEAD", "OPTIONS"].includes(method);

        if (options.body && !isFormData) {
            headers.set("Content-Type", "application/json");
        }
        headers.set("Accept", "application/json");

        if (changesState) {
            if (!csrfToken) {
                await fetchCsrfToken();
            }
            headers.set(csrfHeaderName, csrfToken);
        }

        let response;
        try {
            response = await fetch(baseUrl() + path, {
                ...options,
                method,
                credentials: "include",
                headers
            });
        } catch (error) {
            throw new Error(
                "Could not reach the backend. Check that Spring Boot is running and that its CORS configuration allows this frontend origin."
            );
        }

        let payload = null;
        if (response.status !== 204) {
            const contentType = response.headers.get("content-type") || "";
            payload = contentType.includes("application/json")
                ? await response.json()
                : await response.text();
        }

        if (!response.ok) {
            if (response.status === 401) {
                clearLocalSession();
            }

            let message = payload && typeof payload === "object"
                ? payload.message || payload.error
                : payload;

            if (!message && response.status === 401) {
                message = "Please sign in to continue. Your session may have expired.";
            } else if (!message && response.status === 403) {
                message = "The request was rejected. Refresh the page and try again.";
            } else {
                message = message || "Request failed (" + response.status + ")";
            }

            const error = new Error(message);
            error.status = response.status;
            error.fields = payload && typeof payload === "object" ? payload.fields || {} : {};
            throw error;
        }

        if (path === "/api/auth/login" || path === "/api/auth/logout") {
            csrfToken = null;
            await fetchCsrfToken();
        }

        return payload;
    }

    async function getBlob(path) {
        let response;
        try {
            response = await fetch(baseUrl() + path, {
                method: "GET",
                credentials: "include",
                headers: { Accept: "application/pdf, image/jpeg, image/png" }
            });
        } catch (error) {
            throw new Error("Could not reach the backend while opening the document.");
        }
        if (!response.ok) {
            let payload = {};
            try { payload = await response.json(); } catch (error) { /* Non-JSON response. */ }
            const failure = new Error(payload.message || payload.error || "Could not open this document.");
            failure.status = response.status;
            throw failure;
        }
        return response.blob();
    }

    function query(values) {
        const params = new URLSearchParams();
        Object.entries(values).forEach(([key, value]) => {
            if (value !== undefined && value !== null && String(value).trim() !== "") {
                params.set(key, String(value).trim());
            }
        });
        return params.toString();
    }

    window.ScholarAssistApi = {
        request,
        getBlob,
        query,
        get(path) { return request(path); },
        post(path, body) {
            return request(path, {
                method: "POST",
                body: body === undefined ? undefined : JSON.stringify(body)
            });
        },
        put(path, body) {
            return request(path, { method: "PUT", body: JSON.stringify(body) });
        },
        patch(path, body) {
            return request(path, {
                method: "PATCH",
                body: body === undefined ? undefined : JSON.stringify(body)
            });
        },
        delete(path) { return request(path, { method: "DELETE" }); },
        upload(path, formData) {
            return request(path, { method: "POST", body: formData });
        },
        logout() { return request("/api/auth/logout", { method: "POST" }); }
    };
})();
