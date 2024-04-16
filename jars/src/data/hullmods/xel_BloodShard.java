package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.utils.xel.xel_Misc;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static data.utils.xel.xel_Misc.findTarget;

public class xel_BloodShard extends xel_BaseHullmod {
    /*
     * 血水晶共鸣
     * 扩大战斗视野，提高武器射程，对目标有额外伤害
     * 根据舰船级别，战斗视野范围提高 800/1200/1600/2000
     * 提高射程 20%/30%/50%/70%
     * 额外造成10%伤害
     * 提高100%每月维护需求
     * 降低25%峰值时间
     */

    private static final Map<ShipAPI.HullSize, Float> visionMap = new HashMap<>();
    private static final Map<ShipAPI.HullSize, Float> rangeMap = new HashMap<>();
    private static final float DAMAGE_BONUS = 10f;
    private static final float SUPPLY_MULT = 2f;
    private static final float PEAK_DECEASE = 25f;

    private boolean done = false;

    static {
        visionMap.put(ShipAPI.HullSize.FRIGATE, 800f);
        visionMap.put(ShipAPI.HullSize.DESTROYER, 1200f);
        visionMap.put(ShipAPI.HullSize.CRUISER, 1600f);
        visionMap.put(ShipAPI.HullSize.CAPITAL_SHIP, 2000f);

        rangeMap.put(ShipAPI.HullSize.FRIGATE, 20f);
        rangeMap.put(ShipAPI.HullSize.DESTROYER, 30f);
        rangeMap.put(ShipAPI.HullSize.CRUISER, 50f);
        rangeMap.put(ShipAPI.HullSize.CAPITAL_SHIP, 70f);
    }

    @Override
    public void init(HullModSpecAPI spec) {
        super.init(spec);
        this.setNotCompatible(HullMods.ADVANCED_TARGETING_CORE,
                HullMods.DEDICATED_TARGETING_CORE,
                HullMods.INTEGRATED_TARGETING_UNIT);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSightRadiusMod().modifyFlat(id, visionMap.get(hullSize));
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, rangeMap.get(hullSize));
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, rangeMap.get(hullSize));
        stats.getSuppliesPerMonth().modifyMult(id, SUPPLY_MULT);
        stats.getPeakCRDuration().modifyMult(id, 1f - 0.01f * PEAK_DECEASE);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new damageToTargetModifer(ship));
        removeBlockedMod(ship);


    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return xel_Misc.getHullSizeFlatString(visionMap);
        else if (index == 1) return xel_Misc.getHullSizePercentString(rangeMap);
        else if (index == 2) return "" + (int) DAMAGE_BONUS + "%";
        else if (index == 3) return "100%";
        else return index == 4 ? "" + (int) PEAK_DECEASE + "%" : super.getDescriptionParam(index, hullSize);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return hasEnergyArrayMod(ship) && !hasNotCompatibleMod(ship) && !hasTooMuchResponseMod(ship);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (!hasEnergyArrayMod(ship)) return getNoEnergyArrayReason();
        else if (hasTooMuchResponseMod(ship)) return getTooMuchResponseModReason();
        else return hasNotCompatibleMod(ship) ? getNotCompatibleReason() : super.getUnapplicableReason(ship);
    }

    private final class damageToTargetModifer implements DamageDealtModifier, AdvanceableListener {
        private final ShipAPI source;
        private final IntervalUtil interva = new IntervalUtil(0.03f, 0.03f);
        private final CombatEngineAPI engine = Global.getCombatEngine();
        private final String DAMAGE_MODIFIER_KEY = "xel_bs_damage_modifier_key";

        public damageToTargetModifer(ShipAPI ship) {
            this.source = ship;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (target instanceof ShipAPI) {
                ShipAPI targetLocked = findTarget(engine, source, 2500f);
                if (targetLocked != null && target == targetLocked) {
                    damage.getModifier().modifyMult(DAMAGE_MODIFIER_KEY, 1f + 0.01f * DAMAGE_BONUS);
                    return DAMAGE_MODIFIER_KEY;
                }
            }
            return null;
        }

        @Override
        public void advance(float amount) {
            if (source.isAlive() && engine.getPlayerShip() == source) {
                ShipAPI targetLocked = findTarget(engine, source, 2500f);
                interva.advance(amount);

                if (targetLocked != null && interva.intervalElapsed()) {
                    SpriteAPI sprite = targetLocked.getSpriteAPI();
                    float offsetX = sprite.getWidth() / 2f - sprite.getCenterX();
                    float offsetY = sprite.getHeight() / 2f - sprite.getCenterY();
                    float trueOffsetX = (float) FastTrig.cos(Math.toRadians(targetLocked.getFacing() - 90f)) * offsetX - (float) FastTrig.sin(Math.toRadians(targetLocked.getFacing() - 90f)) * offsetY;
                    float trueOffsetY = (float) FastTrig.sin(Math.toRadians(targetLocked.getFacing() - 90f)) * offsetX + (float) FastTrig.cos(Math.toRadians(targetLocked.getFacing() - 90f)) * offsetY;
                    float size = Math.max(sprite.getWidth(), sprite.getHeight());
                    size = Math.min(200f, size);
                    MagicRender.battlespace(
                            Global.getSettings().getSprite("fx", "xel_blood_shard_lock"),
                            new Vector2f(targetLocked.getLocation().getX() + trueOffsetX, targetLocked.getLocation().getY() + trueOffsetY),
                            new Vector2f(0f, 0f),
                            new Vector2f(size, size),
                            new Vector2f(0f, 0f),
                            -90f,
                            0f,
                            new Color(255, 255, 255, 153),
                            true,
                            0f, 0f, 0f, 0f, 0f, 0f, 0.033f, 0f,
                            CombatEngineLayers.ABOVE_SHIPS_LAYER
                    );
                    engine.maintainStatusForPlayerShip("xel_bs_key1", null, "血水晶共鸣", "是否锁定目标", false);

                }
            }
        }
    }

}
