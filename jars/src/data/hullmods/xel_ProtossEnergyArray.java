package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.HullModUtil;
import data.utils.xel.xel_Misc;

import java.awt.*;
import java.text.DecimalFormat;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_ProtossEnergyArray extends BaseHullMod {
    /*
     * $星灵势力改造：星灵能量矩阵
     * 所有星灵舰船护盾[零维持]，护盾[永远开启]
     * 星灵舰船进入过载状态后立即恢复
     * 星灵等离子护盾有效抵御电弧，穿盾概率-50%
     * 过载发生会导致护盾下线8秒，下线时过载不重置时间
     *
     * 升级分支联动[不兼容]：
     * $护盾充能器——护盾下线时长增加，降低20%护盾受伤
     * $控制芯核——降低受到的EMP伤害，增加战斗中武器和引擎的修复时间
     * $谐振盘——护甲增加15%，被穿盾概率提高至75%
     *
     * 升级分支S插联动[待商榷]
     * 护盾充能器——任意形式上涨硬幅能的10%转化为软幅能
     * 控制芯核——提高PD武器150射程，非PD武器提高300射程
     * 谐振盘——受击后，根据硬辐能水平，有概率产生电弧，每0.5s至多触发一次
     *
     * 其他升级：
     * $萨尔纳加虚空阵列——航母插，抑制战备下降，根据辐能点数提供相应的额外加成，完全防止舰载机成员丧生，但远离母舰后会有减益
     * $净化者协议——解除所有安全协议限制，大幅增加作战能力（你以为是安超？其实是把系统换成更强力的版本）
     * 血水晶共鸣——扩大战斗视野，提高武器射程，对目标有额外伤害
     * 重力加速器——最高速度、机动性增强，降低护盾受伤
     * 相位反应堆——每8s（？）按照百分比排散辐能，排散持续时间2s [整个船插都待商榷]
     *
     * 特殊内置：
     * 刚毅护盾：护盾受到的伤害不超过最大辐容的0.5%
     * 复仇协议：根据友方已死亡舰船的部署点，提供永久or暂时的增益
     * 永恒屏障：每次护盾击溃后获得一个镀层，镀层持续时间内获得高额免伤并持续恢复结构
     */

    private static final float PIERCE_MULT = 50f;
    private static final float SHIELD_DOWN_TIME = 8f;
    private static final float HARDFLUX_VENT_BONUS = 30f;
    private static final float SHIELD_BATTERY_GOOD_BONUS = 20f;
    private static final float SHIELD_BATTERY_BAD_BONUS = 50f;
    private static final float CYBERNETICS_CORE_GOOD_BONUS = 50f;
    private static final float CYBERNETICS_CORE_BAD_BONUS = 33f;
    private static final float RESONANCE_COIL_GOOD_BONUS = 15f;
    private static final float RESONANCE_COIL_BAD_BONUS = 30f;


    private static final String DATA_KEY = "EP_Protoss_Energy_Array";
    private static final DecimalFormat dc = new DecimalFormat("0.0");

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getShieldUpkeepMult().modifyMult(id, 0f);
        stats.getHardFluxDissipationFraction().modifyFlat(id, HARDFLUX_VENT_BONUS * 0.01f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new noOverload(ship));
        MutableShipStatsAPI stats = ship.getMutableStats();
        float pierceBonus = PIERCE_MULT + (hasResonananceCoil(ship) ? RESONANCE_COIL_BAD_BONUS : 0f);
        if (hasShieldBattery(ship)) {
            stats.getShieldDamageTakenMult().modifyMult(id, 1f - SHIELD_BATTERY_GOOD_BONUS * 0.01f);
        }
        if (hasCyberneticsCore(ship)) {
            stats.getCombatEngineRepairTimeMult().modifyMult(id, 1f + CYBERNETICS_CORE_BAD_BONUS * 0.01f);
            stats.getCombatWeaponRepairTimeMult().modifyMult(id, 1f + CYBERNETICS_CORE_BAD_BONUS * 0.01f);
            stats.getEmpDamageTakenMult().modifyMult(id, 1f - CYBERNETICS_CORE_GOOD_BONUS * 0.01f);
        }
        stats.getDynamic().getStat(Stats.SHIELD_PIERCED_MULT).modifyMult(id, pierceBonus * 0.01f);
    }


    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return (int) PIERCE_MULT + "%";
        if (index == 1) return (int) HARDFLUX_VENT_BONUS + "%";
        if (index == 2) return (int) SHIELD_DOWN_TIME + "";
        return super.getDescriptionParam(index, hullSize);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 5f;
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color bad = Misc.getNegativeHighlightColor();
        Color good = Misc.getPositiveHighlightColor();

        boolean flag = hasShieldBattery(ship);
        tooltip.addSectionHeading(i18n_hullmod.get("xel_pea_title"), Alignment.TMID, pad * 2f);
        TooltipMakerAPI myText = tooltip.beginImageWithText("graphics/hullmods/xel_ShieldBattery.png", 32f);
        myText.addPara("%s [%s]", pad * 2f,
                new Color[]{new Color(155, 155, 255, 255), flag ? h : g},
                xel_Misc.getHullmodName(HullModUtil.XEL_SHIELD_BATTERY),
                flag ? i18n_hullmod.get("install") : i18n_hullmod.get("uninstall"));
        myText.addPara(i18n_hullmod.get("xel_pea_upgrade1"), pad, flag ? good : g, "" + (int) SHIELD_BATTERY_GOOD_BONUS + "%");
        myText.addPara(i18n_hullmod.get("xel_pea_upgrade2"), pad, flag ? bad : g, "" + (int) SHIELD_BATTERY_BAD_BONUS + "%");
        tooltip.addImageWithText(pad);

        flag = hasCyberneticsCore(ship);
        myText = tooltip.beginImageWithText("graphics/hullmods/xel_CyberneticsCore.png", 32f);
        myText.addPara("%s [%s]", pad * 2f,
                new Color[]{new Color(155, 155, 255, 255), flag ? h : g},
                xel_Misc.getHullmodName(HullModUtil.XEL_CYBERNETICS_CORE),
                flag ? i18n_hullmod.get("install") : i18n_hullmod.get("uninstall"));
        myText.addPara(i18n_hullmod.get("xel_pea_upgrade3"), pad, flag ? good : g, "" + (int) CYBERNETICS_CORE_GOOD_BONUS + "%");
        myText.addPara(i18n_hullmod.get("xel_pea_upgrade4"), pad, flag ? bad : g, "" + (int) CYBERNETICS_CORE_BAD_BONUS + "%");
        tooltip.addImageWithText(pad);

        flag = hasResonananceCoil(ship);
        myText = tooltip.beginImageWithText("graphics/hullmods/xel_ResonanceCoil.png", 32f);
        myText.addPara("%s [%s]", pad * 2f,
                new Color[]{new Color(155, 155, 255, 255), flag ? h : g},
                xel_Misc.getHullmodName(HullModUtil.XEL_RESONANANCE_COIL),
                flag ? i18n_hullmod.get("install") : i18n_hullmod.get("uninstall"));
        myText.addPara(i18n_hullmod.get("xel_pea_upgrade5"), pad, flag ? good : g, "" + (int) RESONANCE_COIL_GOOD_BONUS + "%");
        myText.addPara(i18n_hullmod.get("xel_pea_upgrade6"), pad, flag ? bad : g, "" + (int) RESONANCE_COIL_BAD_BONUS + "%");
        tooltip.addImageWithText(pad);

    }


    private boolean hasShieldBattery(ShipAPI ship) {
        return ship.getVariant().hasHullMod(HullModUtil.XEL_SHIELD_BATTERY);
    }

    private boolean hasCyberneticsCore(ShipAPI ship) {
        return ship.getVariant().hasHullMod(HullModUtil.XEL_CYBERNETICS_CORE);
    }

    private boolean hasResonananceCoil(ShipAPI ship) {
        return ship.getVariant().hasHullMod(HullModUtil.XEL_RESONANANCE_COIL);
    }


    private class noOverload implements AdvanceableListener {
        private final String NO_OVERLOAD_KEY = "no_overload";
        private final ShipAPI ship;
        private float time = 0f;

        public noOverload(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (ship.isAlive() && ship.getShield() != null) {
                CombatEngineAPI engine = Global.getCombatEngine();
                FluxTrackerAPI fluxTracker = ship.getFluxTracker();

                ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
//                ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);

                if (fluxTracker.isOverloaded()) {
                    ship.getShield().toggleOff();
                    fluxTracker.stopOverload();
                    if (time <= 0f) {
                        time = SHIELD_DOWN_TIME * (hasShieldBattery(ship) ? 1f + SHIELD_BATTERY_BAD_BONUS * 0.01f : 1f);
                    }
                }

                if (time <= 0f) {
                    if (ship.getShield().isOff()) {
                        ship.getShield().toggleOn();
                    }
                } else {
                    time -= amount;
                }

                if (engine.getPlayerShip() == ship) {
                    engine.maintainStatusForPlayerShip(NO_OVERLOAD_KEY, "graphics/icons/hullsys/fortress_shield.png",
                            xel_Misc.getHullmodName(HullModUtil.XEL_PROTOSS_ENERGY_ARRAY),
                            time > 0 ? i18n_hullmod.format("xel_pea_shield_down", dc.format(time)) : i18n_hullmod.get("xel_pea_shield_up"),
                            time > 0);
                }
            }
        }

    }
}
