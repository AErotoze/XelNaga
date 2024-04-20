package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;

public class xel_CyberneticsCore extends xel_BaseHullmod {
	//    控制芯核
	//    提高 50% 自动开火精度
	//    降低 5% 武器开火幅能，根据硬幅能水平至多降低 10%
	//    内置后 +5%武器伤害，武器开火幅能最大降低值额外 +10%
	//    不兼容于弹道测距仪，电子反制组件
	private static final float AUTO_ACCURACY_BONUS = 50f;
	private static final float FIRE_FLUX_DECREASE = 5f;
	private static final float SMOD_FIRE_FLUX_DECREASE = 10f;
	private static final float DAMAGE_BONUS = 5f;

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);
		this.setNotCompatible(HullMods.ECCM, "ballistic_rangefinder");
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getAutofireAimAccuracy().modifyFlat(id, 0.01f * AUTO_ACCURACY_BONUS);
		if (isSMod(stats)) {
			stats.getBallisticWeaponDamageMult().modifyMult(id, 1f + 0.01f * DAMAGE_BONUS);
			stats.getEnergyWeaponDamageMult().modifyMult(id, 1f + 0.01f * DAMAGE_BONUS);
			stats.getMissileWeaponDamageMult().modifyMult(id, 1f + 0.01f * DAMAGE_BONUS);
		}
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		String id = ship.getId();
		boolean sMod = isSMod(ship);
		MutableShipStatsAPI stats = ship.getMutableStats();
		float level = Math.min(1f, ship.getHardFluxLevel() / 0.5f);
		float bonus = FIRE_FLUX_DECREASE + (sMod ? SMOD_FIRE_FLUX_DECREASE : 0f) + level * 2f * FIRE_FLUX_DECREASE;

		stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - 0.01f * bonus);
		stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - 0.01f * bonus);
		stats.getMissileWeaponFluxCostMod().modifyMult(id, 1f - 0.01f * bonus);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		removeBlockedMod(ship);
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return (int) AUTO_ACCURACY_BONUS + "%";
		else if (index == 1) return (int) FIRE_FLUX_DECREASE + "%";
		else return index == 2 ? (int) (FIRE_FLUX_DECREASE * 3f) + "%" : super.getDescriptionParam(index, hullSize);
	}

	@Override
	public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return (int) SMOD_FIRE_FLUX_DECREASE + "%";
		else return index == 1 ? (int) DAMAGE_BONUS + "%" : super.getSModDescriptionParam(index, hullSize);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return !hasNotCompatibleMod(ship) && !hasTooMuchHarmonyMod(ship) && hasArrayMod(ship);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (hasNotCompatibleMod(ship)) return getNotCompatibleReason();
		else if (hasTooMuchHarmonyMod(ship)) return getTooMuchHarmonyModReason();
		else return !hasArrayMod(ship) ? getNoArrayReason() : super.getUnapplicableReason(ship);
	}
}
