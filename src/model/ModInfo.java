package model;

public record ModInfo(String name, String version, String description, String author, String url) {
    // Los records generan automáticamente los métodos name(), version(), description(), author() y url()
}
