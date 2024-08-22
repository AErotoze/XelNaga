package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.shipsystems.xel_VoidShiftStats;


public class xel_DebugHullmod extends xel_BaseHullmod {
    private static final String DATA_KEY = "xel_DebugHullmod_data_key";
    private static final IntervalUtil interval = new IntervalUtil(1f, 1f);

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.isAlive()) {
            String doneKey = DATA_KEY + ship.getId() + "_logger";
            Boolean done = (Boolean) ship.getCustomData().get(doneKey);
            if (done == null) {
                done = false;
            }

            if (!done) {
                ship.setCustomData(doneKey, true);

            }

            interval.advance(amount);
            if (interval.intervalElapsed()) {
                Global.getCombatEngine().spawnProjectile(ship, (WeaponAPI) null, "xel_Soul", ship.getLocation(), (float) (Math.random() * 360f), ship.getVelocity());
            }

        }
    }

    private void getInfo(String str) {
        Global.getLogger(this.getClass()).info(str);
    }
}
