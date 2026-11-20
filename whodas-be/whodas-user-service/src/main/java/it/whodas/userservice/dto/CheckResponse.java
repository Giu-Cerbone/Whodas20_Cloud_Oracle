package it.whodas.userservice.dto;

public record CheckResponse(boolean exists, String message) {

    public static CheckResponse available() {
        return new CheckResponse(false, "Disponibile");
    }

    public static CheckResponse duplicate(String message) {
        return new CheckResponse(true, message);
    }
}
