/* Shared navigation toggle for the static landing page and portal shell. */
document.addEventListener("DOMContentLoaded", () => {
    const toggle = document.querySelector(".site-header .menu-toggle");
    if (!toggle) return;

    toggle.addEventListener("click", () => {
        const navigation = document.querySelector(".main-nav");
        navigation.classList.toggle("open");
        toggle.setAttribute("aria-expanded", navigation.classList.contains("open"));
    });
});
