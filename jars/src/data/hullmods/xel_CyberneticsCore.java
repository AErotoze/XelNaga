package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

/**
 * 控制芯核
 * 降低武器后坐力，提高ai射击精度
 * 根据硬辐能水平，降低武器开火辐能
 * <p>
 * 内置后 +5%武器伤害，武器开火辐能最大降低值额外+10%
 */

public class xel_CyberneticsCore extends xel_BaseHullmod {
    private static final float RECOIL_BONUS = 20f;
    private static final float AUTO_ACCURACY_BONUS = 100f;
    private static final float SMOD_DAMAGE_BONUS = 5f;

    private static final float FIRE_FLUX_DECREASE = 5f;
    private static final float SMOD_FIRE_FLUX_DECREASE = 10f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        boolean sMod = isSMod(stats);
        stats.getRecoilPerShotMult().modifyMult(id, 1f - RECOIL_BONUS * 0.01f);
        stats.getRecoilDecayMult().modifyMult(id, 1f - RECOIL_BONUS * 0.01f);
        stats.getMaxRecoilMult().modifyMult(id, 1f - RECOIL_BONUS * 0.01f);
        stats.getAutofireAimAccuracy().modifyFlat(id, AUTO_ACCURACY_BONUS * 0.01f);
        if (sMod) {
            stats.getBallisticWeaponDamageMult().modifyMult(id, 1f + SMOD_DAMAGE_BONUS * 0.01f);
            stats.getEnergyWeaponDamageMult().modifyMult(id, 1f + SMOD_DAMAGE_BONUS * 0.01f);
            stats.getMissileWeaponDamageMult().modifyMult(id, 1f + SMOD_DAMAGE_BONUS * 0.01f);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        String id = ship.getId();
        boolean sMod = isSMod(ship);
        float level = Math.min(ship.getHardFluxLevel() / 0.5f, 1f);
        float bonus = FIRE_FLUX_DECREASE + (level * 2f * FIRE_FLUX_DECREASE) + (sMod ? SMOD_FIRE_FLUX_DECREASE : 0f);

        ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyMult(id, 1f - bonus);
        ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyMult(id, 1f - bonus);
        ship.getMutableStats().getMissileWeaponFluxCostMod().modifyMult(id, 1f - bonus);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return (int) RECOIL_BONUS + "%";
        else if (index == 1) return (int) AUTO_ACCURACY_BONUS + "%";
        else if (index == 2) return (int) FIRE_FLUX_DECREASE + "%";
        else
            return index == 3 ? (int) (FIRE_FLUX_DECREASE * 3f) + "%" : super.getDescriptionParam(index, hullSize);
    }

    @Override
    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return (int) SMOD_FIRE_FLUX_DECREASE + "%";
        else return index == 1 ? (int) SMOD_DAMAGE_BONUS + "%" : super.getSModDescriptionParam(index, hullSize);
    }

    public String getUnapplicableReason(ShipAPI ship) {
        return getTooMuchHarmonyModReason();
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return hasTooMuchHarmonyMod(ship);
    }
}
