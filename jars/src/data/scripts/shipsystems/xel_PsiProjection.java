package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class xel_PsiProjection extends xel_BaseShipSystemScript{


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (getPlayerShip(stats) == null) return;

        ShipAPI ship = getPlayerShip(stats);
        if (state == State.IN||state == State.ACTIVE){
            ship.setPhased(true);

        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (getPlayerShip(stats) == null) return;

        ShipAPI ship = getPlayerShip(stats);
        ship.setPhased(false);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return new StatusData("Por zalah!",false);
    }
}
