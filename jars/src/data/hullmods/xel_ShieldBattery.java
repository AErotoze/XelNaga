package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.utils.xel.Constants;
import data.utils.xel.HullModUtil;
import data.utils.xel.xel_Misc;

public class xel_ShieldBattery extends BaseHullMod {
    // 护盾充能器——护盾开启时增强辐能排散，增加护盾下线时间
    /**
     * 护盾充能器
     * +10%辐散 10%辐容 +0.3硬辐能排散
     * 根据硬辐能水平降低开盾时硬辐能排散
     * <p>
     * 内置后开盾排散硬辐能能力上限 +0.15
     */

    private static final float FLUX_BONUS = 10f;
    private static final float BASE_HARDFLUX_VENT = 0.3f;
    private static final float SMOD_HARDFLUX_VENT_BONUS = 0.15f;
    private static final float HARDFLUX_THRESHOLD = 35f;
    private static final float SMOD_HARDFLUX_THRESHOLD = 50f;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFluxCapacity().modifyMult(id, 1f + FLUX_BONUS * 0.01f);
        stats.getFluxDissipation().modifyMult(id, 1f + FLUX_BONUS * 0.01f);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        float bonus = BASE_HARDFLUX_VENT + (isSMod(ship) ? SMOD_HARDFLUX_VENT_BONUS : 0f);
        if (ship.getHardFluxLevel() > (isSMod(ship) ? SMOD_HARDFLUX_THRESHOLD * 0.01f : HARDFLUX_THRESHOLD * 0.01f)) {
            bonus *= 2f / 3f;
        }
        ship.getMutableStats().getHardFluxDissipationFraction().modifyFlat(ship.getId(), bonus);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int) FLUX_BONUS + "%";
        else if (index == 1) return "" + (int) (BASE_HARDFLUX_VENT * 100f) + "%";
        else if (index == 2) return "" + (int) HARDFLUX_THRESHOLD + "%";
        else return index == 3 ? "33%" : super.getDescriptionParam(index, hullSize);
    }

    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int) (SMOD_HARDFLUX_VENT_BONUS * 100f) + "%";
        else
            return index == 1 ? "" + (int) SMOD_HARDFLUX_THRESHOLD + "%" : super.getSModDescriptionParam(index, hullSize);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship.getVariant().hasHullMod(HullModUtil.XEL_CYBERNETICS_CORE))
            return Constants.i18n_hullmod.format("notCompatibleWith", xel_Misc.getHullmodName(HullModUtil.XEL_CYBERNETICS_CORE));
        if (ship.getVariant().hasHullMod(HullModUtil.XEL_RESONANANCE_COIL))
            return Constants.i18n_hullmod.format("notCompatibleWith", xel_Misc.getHullmodName(HullModUtil.XEL_RESONANANCE_COIL));
        return Constants.i18n_hullmod.format("needSupportWith", xel_Misc.getHullmodName(HullModUtil.XEL_PROTOSS_ENERGY_ARRAY));
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return !ship.getVariant().hasHullMod(HullModUtil.XEL_CYBERNETICS_CORE)
                && !ship.getVariant().hasHullMod(HullModUtil.XEL_RESONANANCE_COIL)
                && ship.getVariant().hasHullMod(HullModUtil.XEL_PROTOSS_ENERGY_ARRAY);
    }
}
