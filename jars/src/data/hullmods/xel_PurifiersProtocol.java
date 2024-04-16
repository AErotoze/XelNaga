package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.ShipSystemUtil;
import data.utils.xel.xel_Misc;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_PurifiersProtocol extends xel_BaseHullmod {
    /**
     * 净化者协议
     * 检测到对应的系统后，显示一个加强版的系统信息
     * <p>
     * 若系统无可改造的版本[待商榷]
     * 提供一些增益吧
     * 提高5%CR
     * 奖励一部内置LPC
     */

    private static final Map<String, Integer> mag = new HashMap<>();

    static {
        mag.put(ShipSystemUtil.XEL_RUSH, 1);// 冲锋 xel_Rush
        mag.put(ShipSystemUtil.XEL_UNITY_FIELD, 2);// 统合屏障 xel_UnityField
        mag.put("", 3);
        mag.put("", 4);
        mag.put("", 5);
        mag.put("", 6);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMinCrewMod().modifyMult(id, 0f);
        stats.getMaxCrewMod().modifyMult(id, 0.5f);
        stats.getCRLossPerSecondPercent().modifyPercent(id, 50f);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "50%";
        else if (index == 1) return "50%";
        else return index == 2 ? i18n_hullmod.get("xel_pp_system") : super.getDescriptionParam(index, hullSize);


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

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 5f;
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color good = Misc.getPositiveHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();

        if (ship == null || ship.getSystem() == null) {
            return;
        }

        tooltip.addSectionHeading(i18n_hullmod.get("xel_pp_title"), new Color(255, 140, 20, 255), new Color(0, 255, 255, 128), Alignment.TMID, pad);
        if (mag.get(ship.getSystem().getId()) == null) {
            tooltip.addPara(i18n_hullmod.get("xel_pp_no_change"), pad * 2f);
        } else if (mag.get(ship.getSystem().getId()) == 1) {
            TooltipMakerAPI myText = tooltip.beginImageWithText("graphics/icons/hullsys/maneuvering_jets.png", 64f);
            myText.addPara(i18n_hullmod.get("xel_pp_change"), pad * 2f, h, xel_Misc.getShipSystemSpecName(ShipSystemUtil.XEL_RUSH));
            myText.addPara("[%s]", pad, h, i18n_hullmod.get("xel_pp_rush_change_name"));
            myText.setBulletedListMode("--");
            myText.addPara(i18n_hullmod.get("xel_pp_rush_change1"), pad, good, "50", "75");
            myText.addPara(i18n_hullmod.get("xel_pp_rush_change2"), pad, good, "25");
//            myText.addPara("%s", pad, h, "新增炫酷特效")；
            myText.setBulletedListMode(null);
            tooltip.addImageWithText(pad);
        } else if (mag.get(ship.getSystem().getId()) == 2) {
            TooltipMakerAPI myText = tooltip.beginImageWithText("graphics/icons/hullsys/xel_DefensiveMatrix.png", 64f);
            myText.addPara(i18n_hullmod.get("xel_pp_change"), pad * 2f, h, xel_Misc.getShipSystemSpecName(ShipSystemUtil.XEL_UNITY_FIELD));
            myText.addPara("[%s]", pad, h, i18n_hullmod.get("xel_pp_UF_change_name"));
            myText.setBulletedListMode("--");
            myText.addPara(i18n_hullmod.get("xel_pp_UF_change1"), pad, good, "50%");
            myText.addPara(i18n_hullmod.get("xel_pp_UF_change2"), pad, good, "15%", "25%");
            myText.setBulletedListMode(null);
            tooltip.addImageWithText(pad);
        }
    }

}
