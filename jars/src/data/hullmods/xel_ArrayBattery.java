package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class xel_ArrayBattery extends xel_BaseHullmod {
	// 护盾充能器——护盾开启时增强辐能排散，增加护盾下线时间
	/*
	 * 能量充能器
	 * +10%辐散 10%辐容 不存在护盾则 +15%幅散辐容
	 *
	 * 星灵能量矩阵
	 * +0.3硬辐能排散 根据硬辐能水平能力减弱
	 * 灵能水晶矩阵
	 * 降低 15%相位维持 提高最低速度幅能阈值至75%(x1.5)
	 *
	 * s增益
	 * 星灵能量矩阵——额外提高 15% 硬辐能开盾排散能力，能力降低阈值提高至 50%
	 * 灵能水晶矩阵——额外降低 15% 相位维持，相位状态下的软幅能排散能力x1.5
	 */

	private static final float FLUX_BONUS = 10f;
	private static final float NO_SHIELD_FLUX_BONUS = 15f;
	private static final float BASE_HARDFLUX_VENT = 0.3f;
	private static final float SMOD_HARDFLUX_VENT_BONUS = 0.15f;
	private static final float HARDFLUX_THRESHOLD = 35f;
	private static final float SMOD_HARDFLUX_THRESHOLD = 50f;

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		float bonus = ship.getShield() == null ? NO_SHIELD_FLUX_BONUS : FLUX_BONUS;
		MutableShipStatsAPI stats = ship.getMutableStats();
		stats.getFluxCapacity().modifyMult(id, 1f + bonus * 0.01f);
		stats.getFluxDissipation().modifyMult(id, 1f + bonus * 0.01f);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship.getShield() != null) {
			float bonus = BASE_HARDFLUX_VENT + (isSMod(ship) ? SMOD_HARDFLUX_VENT_BONUS : 0f);
			if (ship.getHardFluxLevel() > (isSMod(ship) ? SMOD_HARDFLUX_THRESHOLD * 0.01f : HARDFLUX_THRESHOLD * 0.01f)) {
				bonus *= 2f / 3f;
			}
			ship.getMutableStats().getHardFluxDissipationFraction().modifyFlat(ship.getId(), bonus);
		}
	}

	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return (int) FLUX_BONUS + "%";
		else if (index == 1) return (int) NO_SHIELD_FLUX_BONUS + "%";
		else if (index == 2) return (int) (BASE_HARDFLUX_VENT * 100f) + "%";
		else if (index == 3) return (int) HARDFLUX_THRESHOLD + "%";
		else return index == 4 ? "33%" : super.getDescriptionParam(index, hullSize);
	}

	@Override
	public String getSModDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return (int) (SMOD_HARDFLUX_VENT_BONUS * 100f) + "%";
		else
			return index == 1 ? (int) SMOD_HARDFLUX_THRESHOLD + "%" : super.getSModDescriptionParam(index, hullSize);
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (!hasEnergyArrayMod(ship)) return getNoEnergyArrayReason();
		else return hasTooMuchHarmonyMod(ship) ? getTooMuchHarmonyModReason() : super.getUnapplicableReason(ship);
	}


	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return hasEnergyArrayMod(ship) && !hasTooMuchHarmonyMod(ship);
	}
}
