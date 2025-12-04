package model;

public record ModInfo(String name, String url) {
    
    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
