package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import data.utils.xel.xel_Misc;

import java.util.HashMap;
import java.util.Map;

public class xel_GraviticBoosters extends xel_BaseHullmod {
	/*
	 * 重力加速器——最高速度、机动性增强
	 * +50% 舰船机动性
	 * +15/15/10/10 最大航速
	 * 降低结构，降低强制排幅速率[待商榷]
	 */

	private static final Map<ShipAPI.HullSize, Float> mag = new HashMap<>();
	private static final float ACCELERATION_BONUS = 50f;

	static {
		mag.put(ShipAPI.HullSize.FRIGATE, 15f);
		mag.put(ShipAPI.HullSize.DESTROYER, 15f);
		mag.put(ShipAPI.HullSize.CRUISER, 10f);
		mag.put(ShipAPI.HullSize.CAPITAL_SHIP, 10f);
	}

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);
		this.setNotCompatible(HullMods.AUXILIARY_THRUSTERS, HullMods.UNSTABLE_INJECTOR);
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().modifyFlat(id, mag.get(hullSize));
		stats.getMaxTurnRate().modifyMult(id, 1f + 0.01f * ACCELERATION_BONUS);
		stats.getAcceleration().modifyMult(id, 1f + 3f * 0.01f * ACCELERATION_BONUS);
		stats.getDeceleration().modifyMult(id, 1f + 2f * 0.01f * ACCELERATION_BONUS);
		stats.getMaxTurnRate().modifyMult(id, 1f + 0.01f * ACCELERATION_BONUS);
		stats.getTurnAcceleration().modifyMult(id, 1f + 3f * 0.01f * ACCELERATION_BONUS);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		removeBlockedMod(ship);
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return (int) ACCELERATION_BONUS + "%";
		else return index == 1 ? xel_Misc.getHullSizeFlatString(mag) : super.getDescriptionParam(index, hullSize);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return hasArrayMod(ship) && !hasNotCompatibleMod(ship) && !hasTooMuchResponseMod(ship);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (!hasArrayMod(ship)) return getNoArrayReason();
		else if (hasTooMuchResponseMod(ship)) return getTooMuchResponseModReason();
		else return hasNotCompatibleMod(ship) ? getNotCompatibleReason() : super.getUnapplicableReason(ship);
	}
}
