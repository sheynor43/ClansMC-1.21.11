package io.github.sheynor43.clans.config;

/** How the clan tag is shown in the tab list. */
public enum TabMode {
    /** The plugin sets the tab-list name itself. */
    INTERNAL,
    /** The plugin only exposes placeholders; an external tab plugin renders the tag. */
    PLACEHOLDER_ONLY
}
