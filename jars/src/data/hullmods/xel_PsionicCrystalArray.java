package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.HullModUtil;
import data.utils.xel.xel_Misc;

import java.awt.*;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_PsionicCrystalArray extends xel_BaseHullmod {

    /*
     * 灵能水晶矩阵
     *
     * 特殊内置：
     * 刚毅护盾：护盾受到的伤害不超过最大辐容的0.5%
     * 复仇协议：根据友方已死亡舰船的部署点，提供永久or暂时的增益
     * 永恒屏障：每次护盾击溃后获得一个镀层，镀层持续时间内获得高额免伤并持续恢复结构
     */

    //    相位无需维持，1秒冷却时间，进入相位需要25%硬幅能
    //    相位不会导致最高航速下降
    //    相位有专属能量条可维持10s
    //    10x3 秒恢复完毕
    //    不兼容所有原版相位插：自适应线圈，相位锚点
    //
    //    矩阵充能器——能量恢复速度增加至 10x2 秒恢复完毕
    //    控制芯核——进入相位需要的硬幅能降低10%，相位期间武器冷却速度、装填速度增加
    //    谐振盘——退出相位1s内（实际上需要 +0.5s 以抵消chargeDown），装甲减伤效果

    private static final float PHASE_DISSIPATION_MULT = 1.5f;
    private static final String UPGRADE_STATUS = "xel_pca_upgrade_status";

    @Override
    public void init(HullModSpecAPI spec) {
        super.init(spec);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().hasHullMod(HullModUtil.XEL_CYBERNETICS_CORE)) {
            ship.getMutableStats().getPhaseCloakActivationCostBonus().modifyMult(id, 0.6f);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;
        String id = ship.getId();
        CombatEngineAPI engine = Global.getCombatEngine();
        String upgrade = "123";

        if (ship.getVariant().hasHullMod(HullModUtil.XEL_CYBERNETICS_CORE)) {
            upgrade = i18n_hullmod.get("xel_pca_with_cybernetics_core");
            boolean phased = ship.isPhased();
            if (ship.getPhaseCloak() != null && ship.getPhaseCloak().isChargedown()) {
                phased = false;
            }

            MutableShipStatsAPI stats = ship.getMutableStats();
            if (phased) {
                stats.getBallisticRoFMult().modifyMult(id, PHASE_DISSIPATION_MULT);
                stats.getEnergyRoFMult().modifyMult(id, PHASE_DISSIPATION_MULT);
                stats.getMissileRoFMult().modifyMult(id, PHASE_DISSIPATION_MULT);
                stats.getBallisticAmmoRegenMult().modifyMult(id, PHASE_DISSIPATION_MULT);
                stats.getEnergyAmmoRegenMult().modifyMult(id, PHASE_DISSIPATION_MULT);
                stats.getMissileAmmoRegenMult().modifyMult(id, PHASE_DISSIPATION_MULT);

                // doesn't actually work, needs to update the ammo tracker in the system and this isn't handled
                // probably overpowered anyway...
            } else {
                stats.getBallisticRoFMult().unmodifyMult(id);
                stats.getEnergyRoFMult().unmodifyMult(id);
                stats.getMissileRoFMult().unmodifyMult(id);
                stats.getBallisticAmmoRegenMult().unmodifyMult(id);
                stats.getEnergyAmmoRegenMult().unmodifyMult(id);
                stats.getMissileAmmoRegenMult().unmodifyMult(id);
            }
        }

        if (ship.getVariant().hasHullMod(HullModUtil.XEL_RESONANCE_COIL)){
            upgrade = i18n_hullmod.get("xel_pca_with_resonance_coil");
        }
        if (engine.getPlayerShip() == ship) {
            engine.maintainStatusForPlayerShip(
                    UPGRADE_STATUS,
                    Global.getSettings().getHullModSpec(HullModUtil.XEL_PSIONIC_CRYSTAL_ARRAY).getSpriteName(),
                    xel_Misc.getHullmodName(HullModUtil.XEL_PSIONIC_CRYSTAL_ARRAY),
                    upgrade,
                    false
            );
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "100";
        else if (index == 1) return "10";
        else if (index == 2) return (String.format("%.1f", 100f / 30f));
        else return index == 3 ? "25" : null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 5f;
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color bad = Misc.getNegativeHighlightColor();
        Color good = Misc.getPositiveHighlightColor();

        boolean flag = ship.getVariant().hasHullMod(HullModUtil.XEL_CYBERNETICS_CORE);
        tooltip.addSectionHeading(i18n_hullmod.get("xel_array_upgrade_title"), Alignment.TMID, pad * 2f);
        TooltipMakerAPI text = tooltip.beginImageWithText(Global.getSettings().getHullModSpec(HullModUtil.XEL_CYBERNETICS_CORE).getSpriteName(), 32f);
        text.addPara("%s [%s]", pad * 2f,
                new Color[]{new Color(155, 155, 255), flag ? h : g},
                xel_Misc.getHullmodName(HullModUtil.XEL_CYBERNETICS_CORE),
                i18n_hullmod.get(flag ? "install" : "uninstall"));
        text.setBulletedListMode("--");
        text.addPara(i18n_hullmod.get("xel_pca_core_upgrade1"), pad, flag ? good : g, (int) (PHASE_DISSIPATION_MULT * 100f) + "%");
        text.addPara(i18n_hullmod.get("xel_pca_core_upgrade2"), pad, flag ? bad : g, "50%");
        text.setBulletedListMode(null);
        tooltip.addImageWithText(pad);
        flag = ship.getVariant().hasHullMod(HullModUtil.XEL_ARRAY_BATTERY);
        text = tooltip.beginImageWithText(Global.getSettings().getHullModSpec(HullModUtil.XEL_ARRAY_BATTERY).getSpriteName(), 32f);
        text.addPara("%s [%s]", pad * 2f,
                new Color[]{new Color(155, 155, 255), flag ? h : g},
                xel_Misc.getHullmodName(HullModUtil.XEL_ARRAY_BATTERY),
                i18n_hullmod.get(flag ? "install" : "uninstall"));
        text.setBulletedListMode("--");
        text.addPara(i18n_hullmod.get("xel_pca_battery_upgrade1"), pad, flag ? good : g, "66.7%");
        text.addPara(i18n_hullmod.get("xel_pca_battery_upgrade2"), pad, flag ? good : g, "15%");
        text.setBulletedListMode(null);
        tooltip.addImageWithText(pad);
        flag = ship.getVariant().hasHullMod(HullModUtil.XEL_RESONANCE_COIL);
        text = tooltip.beginImageWithText(Global.getSettings().getHullModSpec(HullModUtil.XEL_RESONANCE_COIL).getSpriteName(), 32f);
        text.addPara("%s [%s]", pad * 2f,
                new Color[]{new Color(155, 155, 255), flag ? h : g},
                xel_Misc.getHullmodName(HullModUtil.XEL_RESONANCE_COIL),
                i18n_hullmod.get(flag ? "install" : "uninstall"));
        text.setBulletedListMode("--");
        text.addPara(i18n_hullmod.get("xel_pca_coil_upgrade1"), pad, flag ? h : g, i18n_hullmod.get("xel_pca_coil_upgrade11"));
        text.setBulletedListMode(null);
        tooltip.addImageWithText(pad);

    }

}
