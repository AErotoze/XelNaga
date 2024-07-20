package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;

public class xel_GraviticBoosters extends xel_BaseHullmod {
	/*
	 * 重力加速器——最高速度、机动性增强
	 * +50% 舰船机动性
	 * +15/15/10/10 最大航速
	 * 降低结构，降低强制排幅速率[待商榷]
	 */

	//	private static final Map<ShipAPI.HullSize, Float> mag = new HashMap<>();
	private static final float ACCELERATION_MAX_BONUS = 100f;
	private static final float ACCELERATION_MIN_BONUS = 10f;
	private static final float MAX_SPEED_MAX_BONUS = 25f;
	private static final float MAX_SPEED_MIN_BONUS = 5f;
	private static final float ZERO_FLUX_SPEED_BONUS = 10f;
	private static final float MIN_MASS = 200f;
	private static final float MAX_MASS = 3500f;

//	static {
//		mag.put(ShipAPI.HullSize.FRIGATE, 15f);
//		mag.put(ShipAPI.HullSize.DESTROYER, 15f);
//		mag.put(ShipAPI.HullSize.CRUISER, 10f);
//		mag.put(ShipAPI.HullSize.CAPITAL_SHIP, 10f);
//	}

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);
		this.setNotCompatible(HullMods.AUXILIARY_THRUSTERS, HullMods.UNSTABLE_INJECTOR);
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getZeroFluxSpeedBoost().modifyFlat(id, ZERO_FLUX_SPEED_BONUS);
//		stats.getMaxSpeed().modifyFlat(id, mag.get(hullSize));
//		stats.getMaxTurnRate().modifyMult(id, 1f + 0.01f * ACCELERATION_BONUS);
//		stats.getAcceleration().modifyMult(id, 1f + 3f * 0.01f * ACCELERATION_BONUS);
//		stats.getDeceleration().modifyMult(id, 1f + 2f * 0.01f * ACCELERATION_BONUS);
//		stats.getMaxTurnRate().modifyMult(id, 1f + 0.01f * ACCELERATION_BONUS);
//		stats.getTurnAcceleration().modifyMult(id, 1f + 3f * 0.01f * ACCELERATION_BONUS);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		removeBlockedMod(ship);
		MutableShipStatsAPI stats = ship.getMutableStats();

		float maxSpeedBonus = getMaxSpeedBonus(ship.getMass());
		float accelerationBonus = getAccelerationBonus(ship.getMass());

		stats.getMaxSpeed().modifyFlat(id, maxSpeedBonus);
		stats.getMaxTurnRate().modifyMult(id, 1f + 0.01f * accelerationBonus);
		stats.getAcceleration().modifyMult(id, 1f + 3f * 0.01f * accelerationBonus);
		stats.getDeceleration().modifyMult(id, 1f + 2f * 0.01f * accelerationBonus);
		stats.getMaxTurnRate().modifyMult(id, 1f + 0.01f * accelerationBonus);
		stats.getTurnAcceleration().modifyMult(id, 1f + 3f * 0.01f * accelerationBonus);
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
		float maxSpeedBonus = getMaxSpeedBonus(ship.getMass());
		float accelerationBonus = getAccelerationBonus(ship.getMass());

		if (index == 0) return (int) MAX_SPEED_MAX_BONUS + "";
		else if (index == 1) return (int) ACCELERATION_MAX_BONUS + "%";
		else if (index == 2) return String.format("%.0f", maxSpeedBonus);
		else if (index == 3) return String.format("%.1f", accelerationBonus) + "%";
		else return index == 4 ? (int) ZERO_FLUX_SPEED_BONUS + "" : super.getDescriptionParam(index, hullSize);
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

	private float getMaxSpeedBonus(float mass) {
		mass = Math.min(Math.max(200f, mass), 3500f);
		float fraction = (mass - MIN_MASS) / (MAX_MASS - MIN_MASS);
		return (1f - fraction) * (MAX_SPEED_MAX_BONUS - MAX_SPEED_MIN_BONUS) + MAX_SPEED_MIN_BONUS;
	}

	private float getAccelerationBonus(float mass) {
		mass = Math.min(Math.max(200f, mass), 3500f);
		float fraction = (mass - MIN_MASS) / (MAX_MASS - MIN_MASS);
		return fraction * (ACCELERATION_MAX_BONUS - ACCELERATION_MIN_BONUS) + ACCELERATION_MIN_BONUS;
	}
}
