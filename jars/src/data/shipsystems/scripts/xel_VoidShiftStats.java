package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;

public class xel_VoidShiftStats extends BaseShipSystemScript {
    public static final float VULNERABLE_FRACTION = 0f;

    private boolean isUnapplied = false;

    private void maintainStatus(CombatEngineAPI engine, ShipAPI playership, float effectLevel, float timeMultiplier) {
        ShipSystemAPI cloak = playership.getPhaseCloak();
        if (cloak == null) cloak = playership.getSystem();
        if (cloak == null) return;

        if (effectLevel > VULNERABLE_FRACTION) {
            engine.maintainStatusForPlayerShip(this, cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), Misc.getRoundedValue(timeMultiplier), false);
        }
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship;
        boolean player;
        CombatEngineAPI engine;
        if (stats.getEntity() instanceof ShipAPI) {
            engine = Global.getCombatEngine();
            if (engine == null) return;
            ship = (ShipAPI) stats.getEntity();
            player = ship == engine.getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        if (engine.isPaused()) {
            return;
        }
        if (state == State.COOLDOWN || state == State.IDLE) {
            if (!isUnapplied) {
                unapply(stats, id);
                isUnapplied = true;
            }
            return;
        }


    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        super.unapply(stats, id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return super.getStatusData(index, state, effectLevel);
    }
}
