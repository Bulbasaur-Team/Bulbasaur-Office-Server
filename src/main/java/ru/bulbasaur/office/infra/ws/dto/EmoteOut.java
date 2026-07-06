package ru.bulbasaur.office.infra.ws.dto;

/** Реакция — рассылается остальным в комнате отправителя. */
public record EmoteOut(String type, String id, String emote) {

    public static EmoteOut of(String id, String emote) {
        return new EmoteOut("emote", id, emote);
    }
}
