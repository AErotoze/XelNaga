package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.HullModUtil;
import data.utils.xel.xel_Misc;

import java.awt.*;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_ProtossEnergyArray extends xel_BaseHullmod {

    private static final String DATA_KEY = "xel_ProtossEnergyArray_data_key";
    private static final float SHIELD_UPKEEP = 0f;
    private static final float SHIELD_COOLDOWN = 10f;
    private static final float SOFT_FLUX_CONVERSION = 60f;
    private static final float FLUX_VENT_RATE = 20f;
    private static final float SHIELD_UNFOLD_RATE = 1000f;

    private static final IntervalUtil empInterval = new IntervalUtil(0.25f, 0.5f);
    private static final Color empFringe = new Color(65, 119, 255);
    private static final Color empCore = new Color(213, 250, 248);

    //矩阵充能器
    private static final float BATTERY_FLUX_BONUS = 10f;

    //控制芯核
    private static final float CORE_WEAPON_RANGE = 100f;
    private static final float CORE_PROJ_SPEED = 20f;

    //谐振盘
    private static final float COIL_COOLDOWN_REDUCE = 4f;
    private static final float COIL_FLUX_CONVERSION = 90f;

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
        stats.getShieldUnfoldRateMult().modifyPercent(id, SHIELD_UNFOLD_RATE);
        stats.getHardFluxDissipationFraction().modifyFlat(id, FLUX_VENT_RATE * 0.01f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        MutableShipStatsAPI stats = ship.getMutableStats();

        stats.getShieldSoftFluxConversion().modifyFlat(id, 0.01f * (hasCoil(ship) ? COIL_FLUX_CONVERSION : SOFT_FLUX_CONVERSION));
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;
        if (ship.getShield() == null) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        FluxTrackerAPI tracker = ship.getFluxTracker();
        String cdKey = DATA_KEY + "_cooldown";

        Float cooldown = (Float) ship.getCustomData().get(cdKey);
        if (cooldown == null) {
            cooldown = 0f;
        }

        //锁定护盾开关
        ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);

        //舰船过载抑制，护盾关闭一段时间
        if (tracker.isOverloaded()) {
            tracker.stopOverload();
            ship.getShield().toggleOff();
            //谐振盘升级
            //在冷却时间内再次过载（某些特殊战术系统）不会重置冷却
            if (cooldown <= 0f)
                cooldown = SHIELD_COOLDOWN - (hasCoil(ship) ? COIL_COOLDOWN_REDUCE : 0f);
        }

        //冷却结束后开启护盾，冷却中有特效
        if (cooldown <= 0f) {
            if (ship.getShield().isOff() && !ship.getFluxTracker().isVenting()) {
                ship.getShield().toggleOn();
            }
        } else {
            cooldown -= amount;
            empInterval.advance(amount);
            if (empInterval.intervalElapsed())
                xel_Misc.spawnFakeEmpInShipRange(ship, ship.getCollisionRadius() * 0.8f, 10f, empFringe, empCore);
        }
        ship.setCustomData(cdKey, cooldown);

        if (engine.getPlayerShip() == ship) {
            engine.maintainStatusForPlayerShip(
                    DATA_KEY + "_status",
                    "graphics/icons/hullsys/fortress_shield.png",
                    xel_Misc.getHullmodName(HullModUtil.XEL_PROTOSS_ENERGY_ARRAY),
                    cooldown > 0f ? i18n_hullmod.format("xel_pea_shield_down", String.format("%.1f", cooldown)) : i18n_hullmod.get("xel_pea_shield_up"),
                    cooldown > 0f);
            if (hasCoil(ship)) {
                engine.maintainStatusForPlayerShip(
                        DATA_KEY + "_coil_status",
                        "graphics/icons/hullsys/fortress_shield.png",
                        xel_Misc.getHullmodName(HullModUtil.XEL_ARRAY_BATTERY),
                        i18n_hullmod.get("xel_pea_with_resonance_coil"),
                        false
                );
            }
        }

        MutableShipStatsAPI stats = ship.getMutableStats();
        String id = DATA_KEY + ship.getId();

        /*
         * 控制芯核升级
         * 提高射弹类武器的射程和射弹速度
         */
        if (hasCore(ship)) {
            if (engine.getPlayerShip() == ship) {
                engine.maintainStatusForPlayerShip(
                        DATA_KEY + "_core_status",
                        "graphics/icons/hullsys/fortress_shield.png",
                        xel_Misc.getHullmodName(HullModUtil.XEL_ARRAY_BATTERY),
                        i18n_hullmod.get("xel_pea_with_cybernetics_core"),
                        false
                );
            }

            if (ship.getShield().isOn()) {
                stats.getBeamWeaponRangeBonus().modifyFlat(id, -CORE_WEAPON_RANGE);
                stats.getEnergyWeaponRangeBonus().modifyFlat(id, CORE_WEAPON_RANGE);
                stats.getBallisticWeaponRangeBonus().modifyFlat(id, CORE_WEAPON_RANGE);
                stats.getProjectileSpeedMult().modifyPercent(id, CORE_PROJ_SPEED);
            } else {
                stats.getBeamWeaponRangeBonus().unmodify(id);
                stats.getEnergyWeaponRangeBonus().unmodify(id);
                stats.getBallisticWeaponRangeBonus().unmodify(id);
                stats.getProjectileSpeedMult().unmodify(id);
            }
        }

        /*
         * 矩阵充能器升级
         * 根据硬幅能水平改变幅能容量和排散
         */
        if (hasBattery(ship)) {

            if (engine.getPlayerShip() == ship) {
                engine.maintainStatusForPlayerShip(
                        DATA_KEY + "_battery_status",
                        "graphics/icons/hullsys/fortress_shield.png",
                        xel_Misc.getHullmodName(HullModUtil.XEL_ARRAY_BATTERY),
                        i18n_hullmod.get("xel_pea_with_array_battery"),
                        false
                );
            }

            float level = ship.getHardFluxLevel();
            if (level <= 0.3f) {
                float fraction = 1f - level / 0.3f;
                stats.getFluxCapacity().unmodify(id);
                stats.getFluxDissipation().modifyMult(id, 1f + fraction * BATTERY_FLUX_BONUS * 0.01f);
            } else {
                float fraction = Math.min(1f, (level - 0.3f) / 0.3f);
                stats.getFluxDissipation().unmodify(id);
                stats.getFluxCapacity().modifyMult(id, 1f + fraction * BATTERY_FLUX_BONUS * 0.01f);
            }
        }

        runVentAssistance(ship, cdKey);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return (int) SHIELD_COOLDOWN + "";
        else if (index == 1) return (int) SOFT_FLUX_CONVERSION + "%";
        else return index == 2 ? (int) FLUX_VENT_RATE + "%" : null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 5f;
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color good = Misc.getPositiveHighlightColor();

        boolean flag = hasCore(ship);
        tooltip.addSectionHeading(i18n_hullmod.get("xel_array_upgrade_title"), Alignment.TMID, pad * 2f);
        TooltipMakerAPI text = tooltip.beginImageWithText(Global.getSettings().getHullModSpec(HullModUtil.XEL_CYBERNETICS_CORE).getSpriteName(), 32f);
        text.addPara("%s [%s]", pad * 2f,
                new Color[]{new Color(155, 155, 255), flag ? h : g},
                xel_Misc.getHullmodName(HullModUtil.XEL_CYBERNETICS_CORE),
                i18n_hullmod.get(flag ? "install" : "uninstall"));
        text.setBulletedListMode("# ");
        text.addPara(i18n_hullmod.get("xel_pea_core_upgrade1"), pad, flag ? good : g, (int) CORE_WEAPON_RANGE + "su");
        text.addPara(i18n_hullmod.get("xel_pea_core_upgrade2"), pad, flag ? good : g, (int) CORE_PROJ_SPEED + "%");
        text.addPara(i18n_hullmod.get("xel_pea_core_upgrade3"), pad);
        text.setBulletedListMode(null);
        tooltip.addImageWithText(pad);

        flag = hasBattery(ship);
        text = tooltip.beginImageWithText(Global.getSettings().getHullModSpec(HullModUtil.XEL_ARRAY_BATTERY).getSpriteName(), 32f);
        text.addPara("%s [%s]", pad * 2f,
                new Color[]{new Color(155, 155, 255), flag ? h : g},
                xel_Misc.getHullmodName(HullModUtil.XEL_ARRAY_BATTERY),
                i18n_hullmod.get(flag ? "install" : "uninstall"));
        text.setBulletedListMode("# ");
        text.addPara(i18n_hullmod.get("xel_pea_battery_upgrade1"), pad, new Color[]{flag ? h : g, flag ? good : g}, "30%", (int) BATTERY_FLUX_BONUS + "%");
        text.addPara(i18n_hullmod.get("xel_pea_battery_upgrade2"), pad, new Color[]{flag ? h : g, flag ? good : g}, "30%", (int) BATTERY_FLUX_BONUS + "%");
        text.addPara(i18n_hullmod.get("xel_pea_battery_upgrade3"), pad);
        text.setBulletedListMode(null);
        tooltip.addImageWithText(pad);

        flag = hasCoil(ship);
        text = tooltip.beginImageWithText(Global.getSettings().getHullModSpec(HullModUtil.XEL_RESONANCE_COIL).getSpriteName(), 32f);
        text.addPara("%s [%s]", pad * 2f,
                new Color[]{new Color(155, 155, 255), flag ? h : g},
                xel_Misc.getHullmodName(HullModUtil.XEL_RESONANCE_COIL),
                i18n_hullmod.get(flag ? "install" : "uninstall"));
        text.setBulletedListMode("# ");
        text.addPara(i18n_hullmod.get("xel_pea_coil_upgrade1"), pad, flag ? good : g, (int) COIL_FLUX_CONVERSION + "%s");
        text.addPara(i18n_hullmod.get("xel_pea_coil_upgrade2"), pad, flag ? h : g, (int) COIL_COOLDOWN_REDUCE + "");
        text.setBulletedListMode(null);
        tooltip.addImageWithText(pad);
    }

    private void runVentAssistance(ShipAPI ship, String cdkey) {
        if (ship.getShipAI() != null && ship.getShield() != null) {
            ShipwideAIFlags flags = ship.getAIFlags();
            boolean forceVenting = false;
//            float fluxLevel = ship.getFluxLevel();
            float hardFluxLevel = ship.getHardFluxLevel();

            //舰船后退，无来袭攻击，则v排
            if (flags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF) && !flags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE)) {
                forceVenting = true;
            }

            //舰船无来袭攻击，脱离危险超过3秒，硬幅能水平大于20%，则v排（非常影响智慧，能从智能人工变成人工智能）
            if (!flags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE) && flags.hasFlag(ShipwideAIFlags.AIFlags.SAFE_FROM_DANGER_TIME)) {
                float safeFromDangerTime = (float) flags.getCustom(ShipwideAIFlags.AIFlags.SAFE_FROM_DANGER_TIME);
                if (safeFromDangerTime > 3f && hardFluxLevel > 0.2f) {
                    forceVenting = true;
                }
            }

            //舰船护盾冷却，无来袭攻击，则v排
            if (ship.getCustomData().get(cdkey) != null && !flags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE)) {
                Float cooldown = (Float) ship.getCustomData().get(cdkey);
                if (cooldown > 0f) {
                    forceVenting = true;
                }
            }

            //舰船硬幅能水平超过75%（装备谐振盘则是50%），则v排
            if (hardFluxLevel > (hasCoil(ship) ? 0.5f : 0.75f)) {
                forceVenting = true;
            }

            if (forceVenting)
                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
        }
    }
}
