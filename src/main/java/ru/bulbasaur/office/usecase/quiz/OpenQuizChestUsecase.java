package ru.bulbasaur.office.usecase.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.domain.model.QuizPlayerState;
import ru.bulbasaur.office.usecase.CreditBulbaCoinsUsecase;
import ru.bulbasaur.office.usecase.EventLogService;
import ru.bulbasaur.office.usecase.dto.WardrobeItemView;
import ru.bulbasaur.office.usecase.port.out.QuizRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.WardrobeRepositoryPort;
import ru.bulbasaur.office.usecase.quiz.dto.QuizViews;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OpenQuizChestUsecase {

    private final QuizRepositoryPort quiz;
    private final QuizStateHelper helper;
    private final WardrobeRepositoryPort wardrobe;
    private final CreditBulbaCoinsUsecase credit;
    private final EventLogService eventLog;

    @Transactional
    public QuizViews.ChestRewardView execute(UUID playerId, String login) {
        Instant now = Instant.now();
        QuizPlayerState state = requirePendingChest(playerId, now);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int chestLevel = resolveChestLevel(state.getLevel());
        String chestRef = "quiz:chest:" + chestLevel + ":" + playerId;
        int coins = awardCoins(playerId, chestRef, rnd);
        int energy = awardEnergy(state, now, rnd);
        WardrobeReward wardrobeReward = awardWardrobe(playerId, chestRef, rnd);

        closeChest(state);
        String rewardLog = formatRewardLog(coins, energy, wardrobeReward);
        eventLog.quizChestOpened(login, rewardLog);

        QuizViews.StateView stateView = helper.toView(state, playerId);
        return QuizViews.ChestRewardView.builder()
                .item(wardrobeReward.item())
                .duplicateSold(wardrobeReward.duplicateSold())
                .sellRefund(wardrobeReward.sellRefund())
                .coins(coins)
                .energy(energy)
                .bulbaCoinBalance(stateView.bulbaCoinBalance())
                .state(stateView)
                .build();
    }

    private QuizPlayerState requirePendingChest(UUID playerId, Instant now) {
        QuizPlayerState state = helper.loadWithRegen(playerId, now);
        if (!state.isPendingChest()) {
            throw new IllegalArgumentException("Нет сундука для открытия");
        }
        return state;
    }

    private int awardCoins(UUID playerId, String chestRef, ThreadLocalRandom rnd) {
        int coinSteps = (QuizConstants.CHEST_COINS_MAX - QuizConstants.CHEST_COINS_MIN)
                / QuizConstants.CHEST_COINS_STEP + 1;
        int coins = QuizConstants.CHEST_COINS_MIN
                + rnd.nextInt(coinSteps) * QuizConstants.CHEST_COINS_STEP;
        credit.execute(
                playerId,
                coins,
                BulbaCoinKind.QUIZ_CHEST_REWARD,
                chestRef + ":coins",
                "Bulba Quiz: сундук (монеты)");
        return coins;
    }

    private static int awardEnergy(QuizPlayerState state, Instant now, ThreadLocalRandom rnd) {
        int rolledEnergy = rnd.nextInt(QuizConstants.CHEST_ENERGY_MIN, QuizConstants.CHEST_ENERGY_MAX + 1);
        int room = Math.max(0, QuizConstants.MAX_ENERGY - state.getEnergy());
        int energy = Math.min(rolledEnergy, room);
        if (energy > 0) {
            state.setEnergy(state.getEnergy() + energy);
            if (state.getEnergy() >= QuizConstants.MAX_ENERGY) {
                state.setEnergyUpdatedAt(now);
            }
        }
        return energy;
    }

    private WardrobeReward awardWardrobe(UUID playerId, String chestRef, ThreadLocalRandom rnd) {
        List<WardrobeItemView> candidates = wardrobe.catalog(playerId).stream()
                .filter(i -> i.sellable() && i.price() > 0)
                .toList();
        if (candidates.isEmpty() || rnd.nextDouble() >= QuizConstants.CHEST_WARDROBE_CHANCE) {
            return WardrobeReward.empty();
        }

        WardrobeItemView picked = weightedPick(candidates, rnd);
        if (!picked.owned()) {
            wardrobe.grantItem(playerId, picked.code());
            return new WardrobeReward(toChestItem(picked), false, null);
        }

        long sellRefund = picked.price() / 2;
        credit.execute(
                playerId,
                sellRefund,
                BulbaCoinKind.WARDROBE_SELL,
                chestRef + ":dup:" + picked.code(),
                "Bulba Quiz: дубликат «" + picked.name() + "»");
        return new WardrobeReward(toChestItem(picked), true, sellRefund);
    }

    private void closeChest(QuizPlayerState state) {
        state.setPendingChest(false);
        state.setChestsOpened(state.getChestsOpened() + 1);
        quiz.saveState(state);
    }

    private static String formatRewardLog(int coins, int energy, WardrobeReward wardrobeReward) {
        QuizViews.ChestItemView item = wardrobeReward.item();
        return coins + " BC"
                + (energy > 0 ? ", +" + energy + "⚡" : "")
                + (item != null
                        ? ", «" + item.name() + "»"
                            + (wardrobeReward.duplicateSold()
                                ? " (дубликат → " + wardrobeReward.sellRefund() + " BC)"
                                : "")
                        : "");
    }

    private static QuizViews.ChestItemView toChestItem(WardrobeItemView item) {
        return new QuizViews.ChestItemView(item.code(), item.name(), item.price());
    }

    private static int resolveChestLevel(int level) {
        int chestLevel = level - (level % QuizConstants.CHEST_EVERY);
        return chestLevel <= 0 ? level : chestLevel;
    }

    /**
     * Выбирает предмет с весом, обратно пропорциональным цене:
     * дешёвые предметы выпадают чаще, а дорогие реже.
     */
    private static WardrobeItemView weightedPick(List<WardrobeItemView> candidates, ThreadLocalRandom rnd) {
        List<Double> weights = new ArrayList<>(candidates.size());
        double sum = 0;
        for (WardrobeItemView c : candidates) {
            double w = 1.0 / Math.max(1, c.price());
            weights.add(w);
            sum += w;
        }
        double r = rnd.nextDouble() * sum;
        double acc = 0;
        for (int i = 0; i < candidates.size(); i++) {
            acc += weights.get(i);
            if (r <= acc) {
                return candidates.get(i);
            }
        }
        return candidates.getLast();
    }

    private record WardrobeReward(
            QuizViews.ChestItemView item,
            boolean duplicateSold,
            Long sellRefund
    ) {
        private static WardrobeReward empty() {
            return new WardrobeReward(null, false, null);
        }
    }
}
