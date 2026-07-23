package ru.bulbasaur.office.infra.ws.dto;

/** Ответ кота одному игроку (совет или реплика). */
public record CatSayOut(String type, String text) {

    public static CatSayOut of(String text) {
        return new CatSayOut("catSay", text);
    }
}
