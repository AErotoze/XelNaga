package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import data.utils.xel.HullModUtil;
import data.utils.xel.xel_Misc;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_FluxVanes extends xel_BaseHullmod {
	/*
		熔流旋叶：
		零幅能加速激活时提高最大速度
		零幅能加速未激活时提高机动性
	 */
	private static final String DATA_KEY = "xel_MoltenBlade_data_key";
	private static final float MAX_SPEED_BONUS = 15f;
	private static final float MANEUVERABILITY_BONUS = 40f;

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);
		this.setNotCompatible(HullMods.AUXILIARY_THRUSTERS, HullMods.UNSTABLE_INJECTOR);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!ship.isAlive()) return;

		MutableShipStatsAPI stats = ship.getMutableStats();
		String id = DATA_KEY + ship.getId();
		if (ship.isEngineBoostActive()) {
			stats.getMaxSpeed().modifyFlat(id, MAX_SPEED_BONUS);
			stats.getAcceleration().unmodify(id);
			stats.getDeceleration().unmodify(id);
			stats.getTurnAcceleration().unmodify(id);
			stats.getMaxTurnRate().unmodify(id);
		} else {
			stats.getAcceleration().modifyPercent(id, MANEUVERABILITY_BONUS * 2f);
			stats.getDeceleration().modifyPercent(id, MANEUVERABILITY_BONUS);
			stats.getTurnAcceleration().modifyPercent(id, MANEUVERABILITY_BONUS * 2f);
			stats.getMaxTurnRate().modifyFlat(id, MANEUVERABILITY_BONUS);
			stats.getMaxSpeed().unmodify(id);
		}

		if (ship == Global.getCombatEngine().getPlayerShip()) {
			Global.getCombatEngine().maintainStatusForPlayerShip(
					DATA_KEY + "_01",
					"graphics/icons/hullsys/maneuvering_jets.png",
					xel_Misc.getHullmodName(HullModUtil.XEL_FLUX_VANES),
					i18n_hullmod.get(ship.isEngineBoostActive() ? "xel_fv_effect1" : "xel_fv_effect2"),
					false);
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		removeBlockedMod(ship);
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return (int) MAX_SPEED_BONUS + "";
		else if (index == 1) return (int) MANEUVERABILITY_BONUS + "%";
		else return index == 2 ? getNotCompatibleReason() : super.getDescriptionParam(index, hullSize);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return !hasNotCompatibleMod(ship);
	}
}
