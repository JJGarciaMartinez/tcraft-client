package model;

/**
 * Record that contains metadata information about a mod.
 * This data typically describes a mod's name, version, author, and related details.
 * It is intended for use in scenarios where detailed mod metadata is needed.
 *
 * @param name        The name of the mod.
 * @param version     The version of the mod.
 * @param description A short description of the mod's functionality or purpose.
 * @param author      The author or creator of the mod.
 * @param url         A URL pointing to additional information, downloads, or documentation related to the mod.
 * @param configUrl   A URL pointing to the configuration file for the mod (optional, can be null).
 * @param configName  The name of the configuration file (optional, can be null).
 */
public record ModInfo(String name, String version, String description, String author, String url, String configUrl, String configName) {
    // No additional methods or fields are necessary for this record.
}
