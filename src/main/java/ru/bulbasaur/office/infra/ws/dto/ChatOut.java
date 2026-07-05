package ru.bulbasaur.office.infra.ws.dto;

public record ChatOut(String type, String id, String login, String text) {

    public static ChatOut of(String id, String login, String text) {
        return new ChatOut("chat", id, login, text);
    }
}
