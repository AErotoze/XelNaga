package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.HullModUtil;
import data.utils.xel.xel_Misc;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Random;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_ProtossEnergyArray extends xel_BaseHullmod {
    //    护盾无维持且永远开启
    //    舰船无法过载，但过载会导致护盾下线8s
    //    护盾被emp电弧穿透概率 x0.5
    //    护盾值（幅能）代码恢复，根据硬辐能水平降低恢复速率
    //    护盾开启时，每秒恢复 幅散值x0.4 的幅能
    //      [待商榷] 根据硬幅能改变矩阵排散能力 或 将硬幅能的一定比例转换成软幅能
    //
    //    矩阵充能器——提高恢复速度至每秒 幅散值x0.6
    //    控制芯核——护盾开启时，全射弹类武器+100基础射程，护盾关闭时，效果仅有25%
    //    谐振盘——护盾下线时，对周围1000su的战机每 0.5s 秒造成能量伤害，根据距离，最大伤害60 ，护盾重启时间降低2s，
    private static final float SHIELD_UPKEEP = 0f;
    private static final float PIERCE_MULT = 50f;
    private static final float SHIELD_CRASH_TIME = 8f;
    private static final float FLUX_VENT_RATE = 40f;
    private static final float FLUX_VENT_RATE_WITH_BATTERY = 60f;
    private static final float WEAPON_RANGE_WITH_CORE = 1000f;
    private static final float TIME_REDUCED_WITH_COIL = 2f;
    private static final float DAMAGE_WITH_COIL = 60f;
    private static final float DAMAGE_RANGE_WITH_COIL = 600f;

    private static boolean hasBattery(ShipAPI ship) {
        return ship.getVariant().hasHullMod(HullModUtil.XEL_ARRAY_BATTERY);
    }

    private static boolean hasCore(ShipAPI ship) {
        return ship.getVariant().hasHullMod(HullModUtil.XEL_CYBERNETICS_CORE);
    }

    private static boolean hasCoil(ShipAPI ship) {
        return ship.getVariant().hasHullMod(HullModUtil.XEL_RESONANCE_COIL);
    }

    @Override
    public void init(HullModSpecAPI spec) {
        super.init(spec);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getShieldUpkeepMult().modifyMult(id, SHIELD_UPKEEP);
        stats.getDynamic().getMod(Stats.SHIELD_PIERCED_MULT).modifyMult(id, 1f - 0.01f * PIERCE_MULT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new peaManager(ship));
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return (int) SHIELD_CRASH_TIME + "sec";
        else if (index == 1) return (int) PIERCE_MULT + "%";
        else return index == 2 ? (int) FLUX_VENT_RATE + "%" : super.getDescriptionParam(index, hullSize);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 5f;
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color bad = Misc.getNegativeHighlightColor();
        Color good = Misc.getPositiveHighlightColor();

        boolean flag = hasCore(ship);
        tooltip.addSectionHeading(i18n_hullmod.get("xel_array_upgrade_title"), Alignment.TMID, pad * 2f);
        TooltipMakerAPI text = tooltip.beginImageWithText(Global.getSettings().getHullModSpec(HullModUtil.XEL_CYBERNETICS_CORE).getSpriteName(), 32f);
        text.addPara("%s [%s]", pad * 2f,
                new Color[]{new Color(155, 155, 255), flag ? h : g},
                xel_Misc.getHullmodName(HullModUtil.XEL_CYBERNETICS_CORE),
                i18n_hullmod.get(flag ? "install" : "uninstall"));
        text.setBulletedListMode("--");
        text.addPara(i18n_hullmod.get("xel_pea_core_upgrade"), pad, new Color[]{flag ? good : g, flag ? bad : g}, "100su", "25%");
        text.setBulletedListMode(null);
        tooltip.addImageWithText(pad);
        flag = hasBattery(ship);
        text = tooltip.beginImageWithText(Global.getSettings().getHullModSpec(HullModUtil.XEL_ARRAY_BATTERY).getSpriteName(), 32f);
        text.addPara("%s [%s]", pad * 2f,
                new Color[]{new Color(155, 155, 255), flag ? h : g},
                xel_Misc.getHullmodName(HullModUtil.XEL_ARRAY_BATTERY),
                i18n_hullmod.get(flag ? "install" : "uninstall"));
        text.setBulletedListMode("--");
        text.addPara(i18n_hullmod.get("xel_pea_battery_upgrade"), pad, flag ? good : g, (int) FLUX_VENT_RATE_WITH_BATTERY + "%");
        text.setBulletedListMode(null);
        tooltip.addImageWithText(pad);
        flag = hasCoil(ship);
        text = tooltip.beginImageWithText(Global.getSettings().getHullModSpec(HullModUtil.XEL_RESONANCE_COIL).getSpriteName(), 32f);
        text.addPara("%s [%s]", pad * 2f,
                new Color[]{new Color(155, 155, 255), flag ? h : g},
                xel_Misc.getHullmodName(HullModUtil.XEL_RESONANCE_COIL),
                i18n_hullmod.get(flag ? "install" : "uninstall"));
        text.setBulletedListMode("--");
        text.addPara(i18n_hullmod.get("xel_pea_coil_upgrade1"), pad, flag ? good : g, (int) TIME_REDUCED_WITH_COIL + "sec");
        text.addPara(i18n_hullmod.get("xel_pea_coil_upgrade2"), pad, flag ? h : g, (int) DAMAGE_RANGE_WITH_COIL + "su");
        text.setBulletedListMode(null);
        tooltip.addImageWithText(pad);

    }

    private static class peaManager implements WeaponBaseRangeModifier, AdvanceableListener {
        private final ShipAPI ship;
        private final Object STATUSKEY1 = new Object();
        private final Object STATUSKEY2 = new Object();
        private final Object STATUSKEY3 = new Object();
        private final Object STATUSKEY4 = new Object();
        private final DecimalFormat dc = new DecimalFormat("0.0");
        private final IntervalUtil interval = new IntervalUtil(0.5f, 0.5f);
        private float time = 0f;


        public peaManager(ShipAPI ship) {
            this.ship = ship;
        }


        @Override
        public void advance(float amount) {
            if (ship.isAlive() && ship.getShield() != null) {
                CombatEngineAPI engine = Global.getCombatEngine();
                FluxTrackerAPI fluxTracker = ship.getFluxTracker();
                float fluxDecrease = ship.getMutableStats().getFluxDissipation().getModifiedValue() * 0.01f *
                        (hasBattery(ship) ? FLUX_VENT_RATE_WITH_BATTERY : FLUX_VENT_RATE);
                ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);

                if (fluxTracker.isOverloaded()) {
                    ship.getShield().toggleOff();
                    fluxTracker.stopOverload();
                    if (time <= 0f) {
                        time = SHIELD_CRASH_TIME - (hasCoil(ship) ? TIME_REDUCED_WITH_COIL : 0f);
                    }
                }
                if (time <= 0f) {
                    if (ship.getShield().isOff()) {
                        ship.getShield().toggleOn();
                    }
                    fluxTracker.decreaseFlux(fluxDecrease * amount);
                } else {
                    time -= amount;
                    if (hasCoil(ship)) interval.advance(amount);
                    if (interval.intervalElapsed()) {
                        xel_Misc.spawnFakeEmpInShipRange(
                                ship,
                                Math.max(ship.getCollisionRadius() * 2f, 200f),
                                10f,
                                new Color(65, 119, 255),
                                new Color(213, 250, 248)
                        );
                        for (ShipAPI target : Global.getCombatEngine().getShips()) {
                            if (MathUtils.getDistance(ship, target) > DAMAGE_RANGE_WITH_COIL) continue;
                            if (target.getOwner() == ship.getOwner() && !target.isFighter()) continue;
                            float rng = new Random().nextFloat();
                            float level = 1f - MathUtils.getDistance(ship, target) / DAMAGE_RANGE_WITH_COIL;
                            float damage = DAMAGE_WITH_COIL * level;
                            Global.getCombatEngine().applyDamage(
                                    target,
                                    target.getLocation(),
                                    damage,
                                    DamageType.ENERGY,
                                    0f,
                                    false,
                                    false,
                                    ship,
                                    false);
                            if (rng >= 0.3f) {
                                xel_Misc.spawnFakeEmpInShipRange(
                                        target,
                                        Math.max(target.getCollisionRadius() * 2f, 100f),
                                        10f,
                                        new Color(65, 119, 255),
                                        new Color(213, 250, 248)
                                );
                            }
                        }
                    }
                }
                if (engine.getPlayerShip() == ship) {
                    engine.maintainStatusForPlayerShip(STATUSKEY1, "graphics/icons/hullsys/fortress_shield.png",
                            xel_Misc.getHullmodName(HullModUtil.XEL_PROTOSS_ENERGY_ARRAY),
                            time > 0 ? i18n_hullmod.format("xel_pea_shield_down", dc.format(time)) : i18n_hullmod.get("xel_pea_shield_up"),
                            time > 0);
                    if (hasBattery(ship)) {
                        engine.maintainStatusForPlayerShip(STATUSKEY2, "graphics/icons/hullsys/fortress_shield.png",
                                xel_Misc.getHullmodName(HullModUtil.XEL_PROTOSS_ENERGY_ARRAY),
                                i18n_hullmod.get("xel_pea_with_array_battery"),
                                false);
                    }
                    if (hasCore(ship)) {
                        engine.maintainStatusForPlayerShip(STATUSKEY3, "graphics/icons/hullsys/fortress_shield.png",
                                xel_Misc.getHullmodName(HullModUtil.XEL_PROTOSS_ENERGY_ARRAY),
                                i18n_hullmod.get("xel_pea_with_cybernetics_core"),
                                false);
                    }
                    if (hasCoil(ship)) {
                        engine.maintainStatusForPlayerShip(STATUSKEY4, "graphics/icons/hullsys/fortress_shield.png",
                                xel_Misc.getHullmodName(HullModUtil.XEL_PROTOSS_ENERGY_ARRAY),
                                i18n_hullmod.get(time > 0f ? "xel_pea_with_resonance_coil_effect" : "xel_pea_with_resonance_coil_ready"),
                                false);
                    }
                }
            }
        }


        @Override
        public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0f;
        }

        @Override
        public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1f;
        }

        @Override
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            if (ship == null || !hasCore(ship)) return 0f;
            if (weapon.getSpec() == null) return 0f;
            if (weapon.getSpec().getMountType() != WeaponAPI.WeaponType.BALLISTIC
                    && weapon.getSpec().getMountType() != WeaponAPI.WeaponType.ENERGY
                    && !weapon.getSpec().isBeam())
                return 0f;
            return WEAPON_RANGE_WITH_CORE * (ship.getShield().isOn() ? 1f : 0.25f);
        }
    }
}
