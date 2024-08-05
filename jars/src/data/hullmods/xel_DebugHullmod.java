package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;

public class xel_DebugHullmod extends xel_BaseHullmod {
	private static final String DATA_KEY = "xel_DebugHullmod_data_key";
	private static final IntervalUtil interval = new IntervalUtil(1f, 1f);

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

				MutableStat fluxStats = ship.getMutableStats().getFluxDissipation();

				Global.getLogger(this.getClass()).info("ship flux dissipation computeMultMod:" + fluxStats.computeMultMod());
				Global.getLogger(this.getClass()).info("ship flux dissipation ModifiedValue:" + fluxStats.getModifiedValue());
				Global.getLogger(this.getClass()).info("ship flux dissipation BaseValue:" + fluxStats.getBaseValue());
				Global.getLogger(this.getClass()).info("ship flux dissipation modified:" + fluxStats.modified);
			}

			interval.advance(amount);
			if (interval.intervalElapsed()) {

			}
		}
	}
}
