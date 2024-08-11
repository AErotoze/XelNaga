package data.hullmods;

import com.fs.graphics.G;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import data.utils.xel.HullModUtil;
import data.utils.xel.ShipSystemUtil;

import java.lang.reflect.Field;

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
                for (String str : ShipSystemUtil.XEL_UPGRADABLE_SYSTEMS) {
//                    getInfo("upgradable ship system: " + str);
                    if (ship.getSystem() != null){
                        getInfo("ship system ID: "+ ship.getSystem().getId());
                        getInfo("ship system SpecID: "+ ship.getSystem().getSpecAPI().getId());
                    }
                }
            }

            interval.advance(amount);
            if (interval.intervalElapsed()) {

            }

        }
    }

    private void getInfo(String str) {
        Global.getLogger(this.getClass()).info(str);
    }
}
