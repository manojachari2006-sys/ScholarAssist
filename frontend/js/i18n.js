/* Lightweight frontend localization for shared navigation and common controls. */
(function () {
    const translations = {
        hi: {
            "Language": "भाषा", "Workspace": "कार्यस्थल", "Dashboard": "डैशबोर्ड",
            "Home": "मुखपृष्ठ", "How it works": "यह कैसे काम करता है", "About": "हमारे बारे में",
            "Welcome back": "वापसी पर स्वागत है", "Find scholarships": "छात्रवृत्तियाँ खोजें",
            "Explore scholarships": "छात्रवृत्तियाँ देखें", "Open your profile": "अपनी प्रोफ़ाइल खोलें",
            "Create a student profile": "विद्यार्थी प्रोफ़ाइल बनाएँ", "Upcoming deadlines": "आगामी अंतिम तिथियाँ",
            "Next steps": "अगले कदम", "Your applications": "आपके आवेदन",
            "Your documents": "आपके दस्तावेज़", "No documents uploaded yet.": "अभी कोई दस्तावेज़ अपलोड नहीं किया गया है।",
            "Scholarships": "छात्रवृत्तियाँ", "Deadline calendar": "अंतिम तिथि कैलेंडर",
            "Documents": "दस्तावेज़", "Applications": "आवेदन", "Notifications": "सूचनाएँ",
            "Profile": "प्रोफ़ाइल", "Back to home": "होम पर वापस", "Sign in": "साइन इन",
            "Sign out": "साइन आउट", "Student": "छात्र", "Reviewer": "समीक्षक", "Admin": "व्यवस्थापक",
            "Email": "ईमेल", "Password": "पासवर्ड", "Full name": "पूरा नाम",
            "Phone": "फ़ोन", "Date of birth": "जन्म तिथि", "Gender": "लिंग", "State": "राज्य",
            "District": "ज़िला", "College name": "कॉलेज का नाम", "Course": "पाठ्यक्रम",
            "Year of study": "अध्ययन वर्ष", "CGPA": "सीजीपीए", "Annual family income": "वार्षिक पारिवारिक आय",
            "Category": "श्रेणी", "Save profile": "प्रोफ़ाइल सहेजें", "Update profile": "प्रोफ़ाइल अपडेट करें",
            "Upload document": "दस्तावेज़ अपलोड करें", "Document type": "दस्तावेज़ का प्रकार",
            "Choose file": "फ़ाइल चुनें", "Mark read": "पढ़ा हुआ चिह्नित करें",
            "Email notifications": "ईमेल सूचनाएँ", "Preference:": "प्राथमिकता:", "Delivery:": "डिलीवरी:",
            "On": "चालू", "Off": "बंद", "Configured": "कॉन्फ़िगर किया गया",
            "Not configured": "कॉन्फ़िगर नहीं", "No notifications yet.": "अभी कोई सूचना नहीं है।",
            "Create your account": "अपना खाता बनाएँ", "Create account": "खाता बनाएँ",
            "Student sign in": "छात्र साइन इन", "Reviewer and admin sign in": "समीक्षक और व्यवस्थापक साइन इन",
            "Already registered? Sign in": "पहले से खाता है? साइन इन करें",
            "Return to scholarships": "छात्रवृत्तियों पर लौटें", "My profile": "मेरी प्रोफ़ाइल",
            "Application review": "आवेदन समीक्षा", "Document review": "दस्तावेज़ समीक्षा",
            "Scholarship details": "छात्रवृत्ति विवरण", "Application details": "आवेदन विवरण",
            "My applications": "मेरे आवेदन", "My shortlist": "मेरी पसंदीदा सूची",
            "Search scholarships": "छात्रवृत्तियाँ खोजें", "Search": "खोजें", "Loading…": "लोड हो रहा है…",
            "No scholarships match these filters.": "इन फ़िल्टरों से कोई छात्रवृत्ति नहीं मिली।",
            "No applications yet. Start from a scholarship that matches your needs.": "अभी कोई आवेदन नहीं है। अपनी ज़रूरत से मेल खाने वाली छात्रवृत्ति से शुरुआत करें।",
            "Send me email alerts for new application and document updates": "आवेदन और दस्तावेज़ अपडेट के लिए ईमेल अलर्ट भेजें",
            "Preference saved: On.": "प्राथमिकता सहेजी गई: चालू।", "Preference saved: Off.": "प्राथमिकता सहेजी गई: बंद।",
            "Apply now": "अभी आवेदन करें", "View details": "विवरण देखें", "Save changes": "परिवर्तन सहेजें",
            "Choose a request": "अनुरोध चुनें", "Cancel": "रद्द करें", "Submit": "जमा करें"
        },
        kn: {
            "Language": "ಭಾಷೆ", "Workspace": "ಕಾರ್ಯಸ್ಥಳ", "Dashboard": "ಡ್ಯಾಶ್‌ಬೋರ್ಡ್",
            "Home": "ಮುಖಪುಟ", "How it works": "ಇದು ಹೇಗೆ ಕೆಲಸ ಮಾಡುತ್ತದೆ", "About": "ನಮ್ಮ ಬಗ್ಗೆ",
            "Welcome back": "ಮರಳಿ ಸ್ವಾಗತ", "Find scholarships": "ವಿದ್ಯಾರ್ಥಿವೇತನಗಳನ್ನು ಹುಡುಕಿ",
            "Explore scholarships": "ವಿದ್ಯಾರ್ಥಿವೇತನಗಳನ್ನು ವೀಕ್ಷಿಸಿ", "Open your profile": "ನಿಮ್ಮ ಪ್ರೊಫೈಲ್ ತೆರೆಯಿರಿ",
            "Create a student profile": "ವಿದ್ಯಾರ್ಥಿ ಪ್ರೊಫೈಲ್ ರಚಿಸಿ", "Upcoming deadlines": "ಮುಂಬರುವ ಕೊನೆಯ ದಿನಾಂಕಗಳು",
            "Next steps": "ಮುಂದಿನ ಹಂತಗಳು", "Your applications": "ನಿಮ್ಮ ಅರ್ಜಿಗಳು",
            "Your documents": "ನಿಮ್ಮ ದಾಖಲೆಗಳು", "No documents uploaded yet.": "ಇನ್ನೂ ಯಾವುದೇ ದಾಖಲೆಗಳನ್ನು ಅಪ್‌ಲೋಡ್ ಮಾಡಿಲ್ಲ.",
            "Scholarships": "ವಿದ್ಯಾರ್ಥಿವೇತನಗಳು", "Deadline calendar": "ಅಂತಿಮ ದಿನಾಂಕದ ಕ್ಯಾಲೆಂಡರ್",
            "Documents": "ದಾಖಲೆಗಳು", "Applications": "ಅರ್ಜಿಗಳು", "Notifications": "ಅಧಿಸೂಚನೆಗಳು",
            "Profile": "ಪ್ರೊಫೈಲ್", "Back to home": "ಮುಖಪುಟಕ್ಕೆ ಹಿಂತಿರುಗಿ", "Sign in": "ಸೈನ್ ಇನ್",
            "Sign out": "ಸೈನ್ ಔಟ್", "Student": "ವಿದ್ಯಾರ್ಥಿ", "Reviewer": "ಪರಿಶೀಲಕರು", "Admin": "ನಿರ್ವಾಹಕರು",
            "Email": "ಇಮೇಲ್", "Password": "ಪಾಸ್‌ವರ್ಡ್", "Full name": "ಪೂರ್ಣ ಹೆಸರು",
            "Phone": "ದೂರವಾಣಿ", "Date of birth": "ಜನ್ಮ ದಿನಾಂಕ", "Gender": "ಲಿಂಗ", "State": "ರಾಜ್ಯ",
            "District": "ಜಿಲ್ಲೆ", "College name": "ಕಾಲೇಜಿನ ಹೆಸರು", "Course": "ಕೋರ್ಸ್",
            "Year of study": "ಅಧ್ಯಯನದ ವರ್ಷ", "CGPA": "ಸಿಜಿಪಿಎ", "Annual family income": "ವಾರ್ಷಿಕ ಕುಟುಂಬ ಆದಾಯ",
            "Category": "ವರ್ಗ", "Save profile": "ಪ್ರೊಫೈಲ್ ಉಳಿಸಿ", "Update profile": "ಪ್ರೊಫೈಲ್ ನವೀಕರಿಸಿ",
            "Upload document": "ದಾಖಲೆ ಅಪ್‌ಲೋಡ್ ಮಾಡಿ", "Document type": "ದಾಖಲೆಯ ವಿಧ",
            "Choose file": "ಫೈಲ್ ಆಯ್ಕೆಮಾಡಿ", "Mark read": "ಓದಿದಂತೆ ಗುರುತಿಸಿ",
            "Email notifications": "ಇಮೇಲ್ ಅಧಿಸೂಚನೆಗಳು", "Preference:": "ಆದ್ಯತೆ:", "Delivery:": "ವಿತರಣೆ:",
            "On": "ಆನ್", "Off": "ಆಫ್", "Configured": "ಕಾನ್ಫಿಗರ್ ಆಗಿದೆ",
            "Not configured": "ಕಾನ್ಫಿಗರ್ ಆಗಿಲ್ಲ", "No notifications yet.": "ಇನ್ನೂ ಅಧಿಸೂಚನೆಗಳಿಲ್ಲ.",
            "Create your account": "ನಿಮ್ಮ ಖಾತೆ ರಚಿಸಿ", "Create account": "ಖಾತೆ ರಚಿಸಿ",
            "Student sign in": "ವಿದ್ಯಾರ್ಥಿ ಸೈನ್ ಇನ್", "Reviewer and admin sign in": "ಪರಿಶೀಲಕ ಮತ್ತು ನಿರ್ವಾಹಕ ಸೈನ್ ಇನ್",
            "Already registered? Sign in": "ಈಗಾಗಲೇ ಖಾತೆಯಿದೆಯೇ? ಸೈನ್ ಇನ್ ಮಾಡಿ",
            "Return to scholarships": "ವಿದ್ಯಾರ್ಥಿವೇತನಗಳಿಗೆ ಹಿಂತಿರುಗಿ", "My profile": "ನನ್ನ ಪ್ರೊಫೈಲ್",
            "Application review": "ಅರ್ಜಿ ಪರಿಶೀಲನೆ", "Document review": "ದಾಖಲೆ ಪರಿಶೀಲನೆ",
            "Scholarship details": "ವಿದ್ಯಾರ್ಥಿವೇತನದ ವಿವರಗಳು", "Application details": "ಅರ್ಜಿಯ ವಿವರಗಳು",
            "My applications": "ನನ್ನ ಅರ್ಜಿಗಳು", "My shortlist": "ನನ್ನ ಆಯ್ಕೆಪಟ್ಟಿ",
            "Search scholarships": "ವಿದ್ಯಾರ್ಥಿವೇತನಗಳನ್ನು ಹುಡುಕಿ", "Search": "ಹುಡುಕಿ", "Loading…": "ಲೋಡ್ ಆಗುತ್ತಿದೆ…",
            "No scholarships match these filters.": "ಈ ಫಿಲ್ಟರ್‌ಗಳಿಗೆ ಹೊಂದುವ ವಿದ್ಯಾರ್ಥಿವೇತನಗಳಿಲ್ಲ.",
            "No applications yet. Start from a scholarship that matches your needs.": "ಇನ್ನೂ ಅರ್ಜಿಗಳಿಲ್ಲ. ನಿಮ್ಮ ಅಗತ್ಯಕ್ಕೆ ಹೊಂದುವ ವಿದ್ಯಾರ್ಥಿವೇತನದಿಂದ ಪ್ರಾರಂಭಿಸಿ.",
            "Send me email alerts for new application and document updates": "ಅರ್ಜಿ ಮತ್ತು ದಾಖಲೆ ನವೀಕರಣಗಳಿಗೆ ಇಮೇಲ್ ಎಚ್ಚರಿಕೆ ಕಳುಹಿಸಿ",
            "Preference saved: On.": "ಆದ್ಯತೆ ಉಳಿಸಲಾಗಿದೆ: ಆನ್.", "Preference saved: Off.": "ಆದ್ಯತೆ ಉಳಿಸಲಾಗಿದೆ: ಆಫ್.",
            "Apply now": "ಈಗ ಅರ್ಜಿ ಸಲ್ಲಿಸಿ", "View details": "ವಿವರಗಳನ್ನು ನೋಡಿ", "Save changes": "ಬದಲಾವಣೆಗಳನ್ನು ಉಳಿಸಿ",
            "Choose a request": "ವಿನಂತಿಯನ್ನು ಆಯ್ಕೆಮಾಡಿ", "Cancel": "ರದ್ದುಮಾಡಿ", "Submit": "ಸಲ್ಲಿಸಿ"
        }
    };
    const languages = { en: "English", hi: "हिन्दी", kn: "ಕನ್ನಡ" };
    const storageKey = "scholarAssistLanguage";
    let activeLanguage = localStorage.getItem(storageKey) || "en";
    if (!Object.hasOwn(languages, activeLanguage)) activeLanguage = "en";

    function baseText(text) {
        if (Object.hasOwn(translations.hi, text) || Object.hasOwn(translations.kn, text)) return text;
        for (const language of ["hi", "kn"]) {
            const english = Object.keys(translations[language]).find((key) => translations[language][key] === text);
            if (english) return english;
        }
        return text;
    }

    function localize(root = document.body) {
        const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
        const nodes = [];
        while (walker.nextNode()) nodes.push(walker.currentNode);
        for (const node of nodes) {
            const parentTag = node.parentElement?.tagName;
            if (["SCRIPT", "STYLE", "TEXTAREA"].includes(parentTag)) continue;
            const trimmed = node.nodeValue.trim();
            const base = baseText(trimmed);
            const translated = translations[activeLanguage]?.[base] || base;
            if (translated !== trimmed) {
                const leading = node.nodeValue.match(/^\s*/)?.[0] || "";
                const trailing = node.nodeValue.match(/\s*$/)?.[0] || "";
                node.nodeValue = leading + translated + trailing;
            }
        }
        root.querySelectorAll?.("[aria-label], [placeholder], [title]").forEach((element) => {
            for (const attribute of ["aria-label", "placeholder", "title"]) {
                const value = element.getAttribute(attribute);
                if (value) {
                    const base = baseText(value);
                    const translated = translations[activeLanguage]?.[base];
                    if (translated) element.setAttribute(attribute, translated);
                }
            }
        });
    }

    function switcherMarkup() {
        return `<div class="language-switcher"><label for="scholarassist-language">${activeLanguage === "hi" ? "भाषा" : activeLanguage === "kn" ? "ಭಾಷೆ" : "Language"}</label><select id="scholarassist-language" aria-label="Language">${Object.entries(languages).map(([key, label]) => `<option value="${key}" ${activeLanguage === key ? "selected" : ""}>${label}</option>`).join("")}</select></div>`;
    }

    function ensureSwitcher() {
        if (document.getElementById("scholarassist-language")) return;
        const header = document.querySelector(".portal-header, .site-header");
        if (header) {
            const anchor = header.querySelector(".portal-user, .header-actions");
            if (anchor) anchor.insertAdjacentHTML("beforebegin", switcherMarkup());
            else header.insertAdjacentHTML("beforeend", switcherMarkup());
            return;
        }
        const authPage = document.querySelector(".auth-page");
        if (authPage) authPage.insertAdjacentHTML("afterbegin", switcherMarkup());
    }

    function setLanguage(language) {
        if (!Object.hasOwn(languages, language)) return;
        activeLanguage = language;
        localStorage.setItem(storageKey, language);
        document.documentElement.lang = language;
        const selector = document.getElementById("scholarassist-language");
        if (selector) selector.innerHTML = Object.entries(languages)
            .map(([key, label]) => `<option value="${key}" ${activeLanguage === key ? "selected" : ""}>${label}</option>`).join("");
        const label = document.querySelector(".language-switcher label");
        if (label) label.textContent = language === "hi" ? "भाषा" : language === "kn" ? "ಭಾಷೆ" : "Language";
        localize();
    }

    document.documentElement.lang = activeLanguage;
    document.addEventListener("change", (event) => {
        if (event.target?.id === "scholarassist-language") setLanguage(event.target.value);
    });
    const observer = new MutationObserver(() => {
        ensureSwitcher();
        localize();
    });
    observer.observe(document.body, { childList: true, subtree: true });
    ensureSwitcher();
    localize();

    window.ScholarAssistI18n = {
        language: () => activeLanguage,
        locale: () => ({ en: "en-IN", hi: "hi-IN", kn: "kn-IN" })[activeLanguage],
        setLanguage
    };
})();
