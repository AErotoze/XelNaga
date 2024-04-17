package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.HullModUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_PhaseReactor extends xel_BaseHullmod {
    /*
    相位反应堆
    反应堆受到的伤害
    反应堆受到超过伤害50的攻击触发超限
    每秒恢复 6.67% 额度
    反应堆需要 25秒 重启
    超限：
    [守护之壳]免疫所有伤害 2秒 该效果冷却时间 5秒
    [天界太阳能]排散12%当前软幅能和6%当前硬幅能，在 2秒 内排空 冷却时间 8秒
    [圣堂表象]在 2秒 内提供300%时流，冷却时间12秒
     */

    private static final float DAMAGE_THRESHOLD = 50f;
    private static final float RECOVER_PERCENT_PER_SEC = 100f / 15f;
    private static final float MAX_RESTART_TIME = 25f;
    private static final float TIME_MULT = 4f;

    private static final Map<ShipAPI.HullSize, Float> HEALTH_MAP = new HashMap<>();
    private static final Map<String, effectData> EFFECT_DATA = new HashMap<>();

    {
        HEALTH_MAP.put(ShipAPI.HullSize.FIGHTER, 0f);
        HEALTH_MAP.put(ShipAPI.HullSize.FRIGATE, 500f);
        HEALTH_MAP.put(ShipAPI.HullSize.DESTROYER, 600f);
        HEALTH_MAP.put(ShipAPI.HullSize.CRUISER, 800f);
        HEALTH_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, 1000f);

        EFFECT_DATA.put(HullModUtil.XEL_RESONANCE_COIL, new effectData(2f, 5f, i18n_hullmod.get("xel_pr_effect_gs"), HullModUtil.XEL_RESONANCE_COIL));
        EFFECT_DATA.put(HullModUtil.XEL_ARRAY_BATTERY, new effectData(2f, 8f, i18n_hullmod.get("xel_pr_effect_sc"), HullModUtil.XEL_ARRAY_BATTERY));
        EFFECT_DATA.put(HullModUtil.XEL_CYBERNETICS_CORE, new effectData(2f, 12f, i18n_hullmod.get("xel_pr_effect_ta"), HullModUtil.XEL_CYBERNETICS_CORE));
    }

    @Override
    public void init(HullModSpecAPI spec) {
        this.setNotCompatible(HullMods.HEAVYARMOR, HullMods.SAFETYOVERRIDES);
        super.init(spec);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        removeBlockedMod(ship);
        effectData data = null;
        for (String tmp : ship.getVariant().getHullMods()) {
            HullModSpecAPI mod = Global.getSettings().getHullModSpec(tmp);
            if (mod.hasUITag(i18n_hullmod.get("harmony_mod_tag"))) {
                data = EFFECT_DATA.get(mod.getId());
            }
        }
        if (data != null) {
            ship.addListener(new PRmanager(ship, data.maxEffectTime, data.maxCooldown, data.effectName, data.hullmods));
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        return null;
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        return hasEnergyArrayMod(ship) && !hasNotCompatibleMod(ship) && !hasTooMuchResponseMod(ship);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (!hasEnergyArrayMod(ship)) return getNoEnergyArrayReason();
        else if (hasTooMuchResponseMod(ship)) return getTooMuchResponseModReason();
        else return hasNotCompatibleMod(ship) ? getNotCompatibleReason() : super.getUnapplicableReason(ship);
    }

    private class effectData {
        private float maxEffectTime;
        private float maxCooldown;
        private String effectName;
        private String hullmods;

        public effectData(float maxEffectTime, float maxCooldown, String effectName, String hullmods) {
            this.maxCooldown = maxCooldown;
            this.maxEffectTime = maxEffectTime;
            this.effectName = effectName;
            this.hullmods = hullmods;
        }
    }

    private class PRmanager implements DamageTakenModifier, AdvanceableListener {
        private final ShipAPI ship;
        private final float MAX_EFFECT_TIME;
        private final float MAX_EFFECT_COOLDOWN;
        private final float MAX_HEALTH;
        private final String HULLMODS_KEY;
        private final String EFFECT_NAME;
        private final String ID = "xel_PR_damage_taken_modifier";
        private float fluxDeceased = 0f;
        private float recoverTime = 0f;
        private float effectTime = 0f;
        private float effectCooldown = 0f;
        private float health;


        // 计时皆用减法计算
        public PRmanager(ShipAPI ship, float maxEffectTime, float maxEffectCooldown, String effectName, String hullmods) {
            this.ship = ship;
            this.MAX_EFFECT_TIME = maxEffectTime;
            this.MAX_EFFECT_COOLDOWN = maxEffectCooldown;
            this.EFFECT_NAME = effectName;
            this.HULLMODS_KEY = hullmods;
            this.MAX_HEALTH = HEALTH_MAP.get(ship.getHullSize());
//            this.effectTime = MAX_EFFECT_TIME;
            this.health = this.MAX_HEALTH;
        }

        @Override
        public void advance(float amount) {
            if (ship.isAlive()) {
                CombatEngineAPI engine = Global.getCombatEngine();

                // 判断是正常运行还是正在重启
                if (recoverTime <= 0f) {
                    // 判断额度是否有剩
                    if (health > 0f) {
                        // 有剩则回复
                        health = Math.min(MAX_HEALTH, health + RECOVER_PERCENT_PER_SEC * amount);
                        // 判断效果是否启动
                        // 启动后若额度满载则会提前结束效果
                        if (effectTime > 0f) {
                            // 判断需要执行的效果
                            if (Objects.equals(HULLMODS_KEY, HullModUtil.XEL_ARRAY_BATTERY)) {
                                // 天界太阳能
                                // 取开始运行时这一帧的幅能
                                if (effectTime >= MAX_EFFECT_TIME - amount) {
                                    fluxDeceased = ship.getMaxFlux() * (ship.getHardFluxLevel() * 0.06f + (ship.getFluxLevel() - ship.getHardFluxLevel()) * 0.012f);
                                }
                                solariteCelestial(fluxDeceased, amount);
                            }
                            if (Objects.equals(HULLMODS_KEY, HullModUtil.XEL_CYBERNETICS_CORE)) {
                                // 圣堂表象
                                templarApparent(effectTime, amount);
                            }
                            // 效果启动，开始计时 启动后effectCooldown已经重置
                            effectTime = Math.max(0f, effectTime - amount);
                        } else {
                            // 效果结束后 开始冷却计时
                            effectCooldown = Math.max(0f, effectCooldown - amount);
                        }
                    } else {
                        //反之 进入重启状态 重置效果时间和冷却时间
                        recoverTime = MAX_RESTART_TIME;
                        effectTime = 0f;
                        effectCooldown = 0f;
                    }
                } else {
                    // 处于重启
                    recoverTime = Math.max(0f, recoverTime - amount);
                    // 若重启完成
                    if (recoverTime <= 0f) {
                        // 重置health
                        health = MAX_HEALTH;
                    }
                }
                if (engine.getPlayerShip() == ship) {
                    String data = health > 0f ? i18n_hullmod.format("xel_pr_show_health", (int) health) : i18n_hullmod.format("xel_pr_restarting", (int) recoverTime);
                    engine.maintainStatusForPlayerShip(
                            "xel_pr_manager1",
                            Global.getSettings().getSpriteName("hullsys", "xel_PhaseReactor_active"),
                            i18n_hullmod.get("xel_pr_name"),
                            data,
                            health < 0f);
                    data = effectCooldown > 0f ? i18n_hullmod.format("xel_pr_effect_cooldown", (int) effectCooldown) : i18n_hullmod.get("xel_pr_effect_ready");
                    engine.maintainStatusForPlayerShip(
                            "xel_pr_manager2",
                            Global.getSettings().getSpriteName("hullsys", "xel_PhaseReactor_active"),
                            effectTime > 0f ? i18n_hullmod.get(EFFECT_NAME) : i18n_hullmod.get("xel_pr_name"),
                            effectTime > 0f ? i18n_hullmod.get("xel_pr_effect_active") : data,
                            effectCooldown > 0f);
                }
            }
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!shieldHit) {
                float damageNum = damage.getDamage();
                ShipAPI source = null;
                if (damage.getStats().getEntity() != null && damage.getStats().getEntity() instanceof ShipAPI) {
                    source = (ShipAPI) damage.getStats().getEntity();
                }

                //若冷却完毕 若受到大于的50伤害 若效果未激活 重置效果时间和冷却时间
                if (effectCooldown == 0f && damageNum > 50f && effectTime == 0f) {
                    effectTime = MAX_EFFECT_TIME;
                    effectCooldown = MAX_EFFECT_COOLDOWN;
                }
                // 若效果持续时间未结束 且 效果是 守护之壳
                if (effectTime > 0f && Objects.equals(HULLMODS_KEY, HullModUtil.XEL_RESONANCE_COIL)) {
                    // 运行守护之壳 在被击中的地方生成粒子特效
                    Global.getCombatEngine().addNebulaSmoothParticle(
                            point,
                            MathUtils.getPointOnCircumference((Vector2f) null, MathUtils.getRandomNumberInRange(10f, 50f), MathUtils.getRandomNumberInRange(0f, 360f)),
                            MathUtils.getRandomNumberInRange(10f, 40f),
                            MathUtils.getRandomNumberInRange(1.5f, 3f),
                            0.5f,
                            0f,
                            MathUtils.getRandomNumberInRange(0.1f, 1f),
                            Misc.scaleAlpha(new Color(84, 0, 129, 255), MathUtils.getRandomNumberInRange(0.3f, 0.6f))
                    );
                    return guardianShell(damage, ID);
                }

                ship.setJitterShields(false);
                // 额度是否大于等于吸收伤害
                if (health >= damageNum) {
                    health -= damageNum;
                    damage.getModifier().modifyMult(ID, 0f);
                    ship.setJitter(ship, new Color(90, 167, 73, 128), 0.6f, 5, 10f, 30f);
                    ship.setJitterUnder(ship, new Color(90, 167, 73, 191), 0.6f, 10, 10f);
                    Global.getCombatEngine().addFloatingDamageText(point, damageNum, new Color(255, 255, 255), target, source);
                } else {
                    float reduction = damageNum - health;
                    damage.getModifier().modifyMult(ID, 1f - reduction / damageNum);
                    ship.setJitter(ship, new Color(0, 255, 166, 128), 0.6f, 5, 10f, 30f);
                    ship.setJitterUnder(ship, new Color(0, 255, 166, 191), 0.6f, 10, 10f);
                }
                return ID;
            }
            return null;
        }

        // 守护之壳 0伤害
        private String guardianShell(DamageAPI damage, String id) {
            damage.getModifier().modifyMult(id, 0f);
            ship.setJitter(ship, new Color(84, 0, 129, 128), 0.6f, 5, 10f, 30f);
            ship.setJitterUnder(ship, new Color(84, 0, 129, 191), 0.6f, 10, 10f);
            return id;
        }

        // 天界太阳能 排幅
        private void solariteCelestial(float fluxDecreased, float amount) {
            ship.getFluxTracker().decreaseFlux(fluxDecreased * amount / MAX_EFFECT_TIME);
            ship.setJitterShields(false);
            ship.setJitter(ship, new Color(254, 138, 14, 128), 0.6f, 5, 10f, 30f);
            ship.setJitterUnder(ship, new Color(254, 138, 14, 191), 0.6f, 10, 10f);
        }

        // 圣堂表象 时流
        private void templarApparent(float effectTime, float amount) {
            float level = 1f - effectTime / MAX_EFFECT_TIME;
            if (effectTime > amount + amount) {
                ship.getMutableStats().getTimeMult().modifyMult(ID, TIME_MULT * level);
                Global.getCombatEngine().getTimeMult().modifyMult(ID, 1f / TIME_MULT / effectTime);
                ship.setJitterShields(false);
                ship.setJitter(ship, new Color(0, 178, 255, 128), 0.6f, 5, 10f, 30f);
                ship.setJitterUnder(ship, new Color(0, 178, 255, 191), 0.6f, 10, 10f);
            } else {
                ship.getMutableStats().getTimeMult().unmodify(ID);
                Global.getCombatEngine().getTimeMult().unmodify(ID);
            }
        }
    }
}
