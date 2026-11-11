package hopsync.model;

/**
 * Modpack metadata from the manifest's {@code pack} section.
 *
 * @param serverIp the server address players connect to, or {@code null} when the pack has none
 */
public record PackInfo(String name, String version, String description, String serverIp,
                       String minecraft, String loader) {
}
