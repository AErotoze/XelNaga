package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import data.utils.xel.xel_Misc;

import java.util.HashMap;
import java.util.Map;

public class xel_ArrayBattery extends xel_BaseHullmod {
    //	提高 8000/1200/2400/4000幅容 40/60/120/200幅能
    //	提高 50% 强制排散速率
    //	内置后 额外提高10%的幅容+幅散
    //	不兼容于安全协议超驰
    private static final float VENT_RATE_BONUS = 50f;
    private static final float SMOD_FLUX_BONUS = 10f;
    private static final Map<ShipAPI.HullSize, Float> fluxCap = new HashMap<>();
    private static final Map<ShipAPI.HullSize, Float> fluxDiss = new HashMap<>();

    static {
        fluxCap.put(ShipAPI.HullSize.FRIGATE, 800f);
        fluxCap.put(ShipAPI.HullSize.DESTROYER, 1200f);
        fluxCap.put(ShipAPI.HullSize.CRUISER, 2400f);
        fluxCap.put(ShipAPI.HullSize.CAPITAL_SHIP, 4000f);

        fluxDiss.put(ShipAPI.HullSize.FRIGATE, 40f);
        fluxDiss.put(ShipAPI.HullSize.DESTROYER, 60f);
        fluxDiss.put(ShipAPI.HullSize.CRUISER, 120f);
        fluxDiss.put(ShipAPI.HullSize.CAPITAL_SHIP, 200f);
    }


    @Override
    public void init(HullModSpecAPI spec) {
        super.init(spec);
        this.setNotCompatible(HullMods.SAFETYOVERRIDES);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFluxCapacity().modifyFlat(id, fluxCap.get(hullSize));
        stats.getFluxDissipation().modifyFlat(id, fluxDiss.get(hullSize));
        stats.getVentRateMult().modifyPercent(id, VENT_RATE_BONUS);
        if (isSMod(stats)) {
            stats.getFluxCapacity().modifyMult(id, SMOD_FLUX_BONUS);
            stats.getFluxDissipation().modifyMult(id, SMOD_FLUX_BONUS);
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        removeBlockedMod(ship);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return xel_Misc.getHullSizeFlatString(fluxCap);
        else if (index == 1) return xel_Misc.getHullSizeFlatString(fluxDiss);
        else return index == 2 ? (int) VENT_RATE_BONUS + "%" : super.getDescriptionParam(index, hullSize);
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
