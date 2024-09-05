package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.shipsystems.xel_VoidShiftStats;
import data.utils.xel.xel_Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Vector;


public class xel_DebugHullmod extends xel_BaseHullmod {
    private static final String DATA_KEY = "xel_DebugHullmod_data_key";
    private static final IntervalUtil interval = new IntervalUtil(2f, 2f);

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
            CombatEngineAPI engine = Global.getCombatEngine();
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
                ShipAPI target = xel_Misc.findTarget(Global.getCombatEngine(), ship, 2000f);
                if (target == null) return;


                engine.maintainStatusForPlayerShip(new Object(), null, "Debug", "done!", true);
            }

        }
    }

    private void getInfo(String str) {
        Global.getLogger(this.getClass()).info(str);
    }
}
