package ru.bulbasaur.office.infra.security;

import org.springframework.stereotype.Service;
import ru.bulbasaur.office.infra.config.AppProperties;
import ru.bulbasaur.office.usecase.port.out.WotdSeedPort;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.HexFormat;

/**
 * Сид слова дня = усечённый HMAC-SHA256(секрет, namespace:дата). Детерминирован по дате,
 * так что «генерируется» ежедневно без планировщика и хранилища; открытого слова сервер
 * не знает и не хранит.
 */
@Service
public class HmacWotdSeedService implements WotdSeedPort {

    private static final int SEED_HEX_LEN = 16;

    private final byte[] key;

    public HmacWotdSeedService(AppProperties properties) {
        this.key = properties.wotd().secret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String seed(String namespace, LocalDate date) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal((namespace + ":" + date).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, SEED_HEX_LEN);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("не удалось посчитать сид слова дня", e);
        }
    }
}
