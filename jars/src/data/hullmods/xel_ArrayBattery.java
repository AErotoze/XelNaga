package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;

public class xel_ArrayBattery extends xel_BaseHullmod {
	//    提高5%的辐容+幅散
	//    内置后 提高15%的幅容+幅散
	//    不兼容于安全协议超驰
	private static final float FLUX_BONUS = 5f;
	private static final float SMOD_FLUX_BONUS = 10f;


	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);
		this.setNotCompatible(HullMods.SAFETYOVERRIDES);
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		float bonus = FLUX_BONUS + (isSMod(stats) ? SMOD_FLUX_BONUS : 0f);
		stats.getFluxDissipation().modifyMult(id, 1f + 0.01f * bonus);
		stats.getFluxCapacity().modifyMult(id, 1f + 0.01f * bonus);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		removeBlockedMod(ship);
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		return index == 0 ? (int) FLUX_BONUS + "%" : super.getDescriptionParam(index, hullSize);
	}

	@Override
	public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		return index == 0 ? (int) SMOD_FLUX_BONUS + "%" : super.getSModDescriptionParam(index, hullSize);
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
