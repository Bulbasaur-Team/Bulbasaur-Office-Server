package ru.bulbasaur.office.infra.ws.dto;

/**
 * Состояние партии аэрохоккея в координатах ВИДА получателя:
 * своя бита всегда внизу поля, чужая — сверху (для синего стол перевёрнут на 180°).
 * Поле {@code 420×700}. Координаты плоские — без вложенных объектов.
 * {@code rematchBy} — сторона ({@code red}/{@code blue}), предложившая реванш, или null.
 */
public record AirHockeyStateOut(
        String type,
        String phase,
        String mySide,
        int redScore,
        int blueScore,
        long remainingMs,
        double puckX,
        double puckY,
        double puckVx,
        double puckVy,
        double myX,
        double myY,
        double oppX,
        double oppY,
        String redLogin,
        String blueLogin,
        boolean redConnected,
        boolean blueConnected,
        String winnerSide,
        String winnerLogin,
        String rematchBy
) {

    public static AirHockeyStateOut of(
            String phase,
            String mySide,
            int redScore,
            int blueScore,
            long remainingMs,
            double puckX,
            double puckY,
            double puckVx,
            double puckVy,
            double myX,
            double myY,
            double oppX,
            double oppY,
            String redLogin,
            String blueLogin,
            boolean redConnected,
            boolean blueConnected,
            String winnerSide,
            String winnerLogin,
            String rematchBy
    ) {
        return new AirHockeyStateOut(
                "airhockeyState",
                phase,
                mySide,
                redScore,
                blueScore,
                remainingMs,
                puckX, puckY, puckVx, puckVy,
                myX, myY,
                oppX, oppY,
                redLogin,
                blueLogin,
                redConnected,
                blueConnected,
                winnerSide,
                winnerLogin,
                rematchBy
        );
    }
}
