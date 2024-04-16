package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;

public class xel_VoidArray extends xel_BaseHullmod {
    // 萨尔纳加虚空阵列——航母插，抑制战备下降，提高舰载机火力，根据辐能点数提供相应的额外加成
    /**
     * 萨尔纳伽-虚空阵列
     * 降低5%战备下降速率，提高5%战备恢复速率
     * 每一点辐能点数都提高1%两项加成，至多额外提供20%（总计25%）
     * 每艘战机并入虚空阵列，阵亡后驾驶员可安全返回母舰，降低100%舰载机船员损失
     * 处于虚空阵列的战机因空间不稳定，越远离母舰最高速度越小，火力越小，最大降低20%最高航速以及12%火力
     * 母舰碰撞半径+1000su以内无惩罚，惩罚在[1000,3000]线性增加至最大
     * <p>
     * 需要 星灵能量矩阵
     * 不兼容 改装机库
     */
    private static final float REPLACEMENT_RATE_BONUS = 5f;
    private static final float MAX_REPLACEMENT_RATE_BONUS = 25f;
    private static final float CREW_LOST_MULT = 0f;
    private static final float MAX_RANGE = 2000f;
    private static final float FIGHTER_SPEED_DECREASE = 20f;
    private static final float FIGHTER_DAMAGE_DECREASE = 12f;

    private static final String DATA_KEY = "xel_VoidArray_key";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getStat(Stats.FIGHTER_CREW_LOSS_MULT).modifyMult(id, CREW_LOST_MULT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        float bonus = Math.min(REPLACEMENT_RATE_BONUS + ship.getVariant().getNumFluxCapacitors() + ship.getVariant().getNumFluxVents(), MAX_REPLACEMENT_RATE_BONUS);
        ship.getMutableStats().getDynamic().getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).modifyMult(id, 1f - bonus * 0.01f);
        ship.getMutableStats().getDynamic().getStat(Stats.REPLACEMENT_RATE_INCREASE_MULT).modifyPercent(id, bonus);
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        fighter.addListener(new distBonus(fighter, ship));
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        int fluxNum = Math.min(20, ship.getVariant().getNumFluxCapacitors() + ship.getVariant().getNumFluxVents());
        if (index == 0) return "" + (int) REPLACEMENT_RATE_BONUS + "%";
        else if (index == 1) return "" + fluxNum + "%";
        else if (index == 2) return "" + (int) MAX_REPLACEMENT_RATE_BONUS + "%";
        else if (index == 3) return "100%";
        else if (index == 4) return "1000su";
        else if (index == 5) return "" + (int) FIGHTER_SPEED_DECREASE + "%";
        else
            return index == 6 ? "" + (int) FIGHTER_DAMAGE_DECREASE + "%" : super.getDescriptionParam(index, hullSize, ship);

    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship != null
                && hasEnergyArrayMod(ship)
                && !hasNotCompatibleMod(ship)
                && !hasTooMuchResponseMod(ship)
                && hasFighterBays(ship);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (!hasEnergyArrayMod(ship)) return getNoEnergyArrayReason();
        else if (hasTooMuchResponseMod(ship)) return getTooMuchResponseModReason();
        else if (!hasFighterBays(ship)) return getNoBaysReason();
        else return hasNotCompatibleMod(ship) ? getNotCompatibleReason() : super.getUnapplicableReason(ship);
    }

    private static class distBonus implements AdvanceableListener {
        private final ShipAPI fighter;
        private final ShipAPI source;
        private final float INEFFECTIVE_RANGE;

        public distBonus(ShipAPI fighter, ShipAPI source) {
            this.fighter = fighter;
            this.source = source;
            this.INEFFECTIVE_RANGE = source.getCollisionRadius() + 1000f;
        }

        @Override
        public void advance(float amount) {
            if (fighter.isAlive()) {
                float dist = Misc.getDistance(fighter.getLocation(), source.getLocation());
                if (dist > INEFFECTIVE_RANGE) {
                    dist -= INEFFECTIVE_RANGE;
                    String id = fighter.getId();
                    float factors = dist / MAX_RANGE;
                    float bonus = FIGHTER_SPEED_DECREASE * 0.01f * factors;

                    fighter.getMutableStats().getMaxSpeed().modifyMult(id, 1f - bonus);
                    bonus = FIGHTER_DAMAGE_DECREASE * 0.01f * factors;
                    fighter.getMutableStats().getEnergyWeaponDamageMult().modifyMult(id, 1f - bonus);
                    fighter.getMutableStats().getBallisticWeaponDamageMult().modifyMult(id, 1f - bonus);
                    fighter.getMutableStats().getMissileWeaponDamageMult().modifyMult(id, 1f - bonus);
                }
            }
        }
    }
}
