package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.ShipSystemUtil;
import data.utils.xel.xel_Misc;

import java.awt.*;
import java.util.Objects;

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
		return hasArrayMod(ship) && !hasNotCompatibleMod(ship) && !hasTooMuchResponseMod(ship);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (!hasArrayMod(ship)) return getNoArrayReason();
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
		TooltipMakerAPI text = null;
		if (Objects.equals(ship.getSystem().getSpecAPI().getId(), ShipSystemUtil.XEL_RUSH)) {
			text = tooltip.beginImageWithText("graphics/icons/hullsys/maneuvering_jets.png", 64f);
			text.addPara(i18n_hullmod.get("xel_pp_change"), pad * 2f, h, xel_Misc.getShipSystemSpecName(ShipSystemUtil.XEL_RUSH));
			text.addPara("[%s]", pad, h, i18n_hullmod.get("xel_pp_rush_change_name"));
			text.setBulletedListMode("--");
			text.addPara(i18n_hullmod.get("xel_pp_rush_change1"), pad, good, "50", "75");
			text.addPara(i18n_hullmod.get("xel_pp_rush_change2"), pad, good, "25");
//            myText.addPara("%s", pad, h, "新增炫酷特效")；
			text.setBulletedListMode(null);
			tooltip.addImageWithText(pad);
		} else if (Objects.equals(ship.getSystem().getSpecAPI().getId(), ShipSystemUtil.XEL_UNITY_FIELD)) {
			text = tooltip.beginImageWithText("graphics/icons/hullsys/xel_DefensiveMatrix.png", 64f);
			text.addPara(i18n_hullmod.get("xel_pp_change"), pad * 2f, h, xel_Misc.getShipSystemSpecName(ShipSystemUtil.XEL_UNITY_FIELD));
			text.addPara("[%s]", pad, h, i18n_hullmod.get("xel_pp_UF_change_name"));
			text.setBulletedListMode("--");
			text.addPara(i18n_hullmod.get("xel_pp_UF_change1"), pad, good, "50%");
			text.addPara(i18n_hullmod.get("xel_pp_UF_change2"), pad, good, "15%", "25%");
			text.setBulletedListMode(null);
			tooltip.addImageWithText(pad);
		} else if (Objects.equals(ship.getSystem().getSpecAPI().getId(), ShipSystemUtil.XEL_FAST_CHARGE)) {
			text = tooltip.beginImageWithText(Global.getSettings().getShipSystemSpec(ShipSystemUtil.XEL_FAST_CHARGE).getIconSpriteName(), 64f);
			text.addPara(i18n_hullmod.get("xel_pp_change"), pad * 2f, h, xel_Misc.getShipSystemSpecName(ShipSystemUtil.XEL_FAST_CHARGE));
			text.addPara("[%s]", pad, h, i18n_hullmod.get("xel_pp_FC_change_name"));
			text.setBulletedListMode("--");
			text.addPara(i18n_hullmod.get("xel_pp_FC_change1"), pad, good, "100%");
			text.addPara(i18n_hullmod.get("xel_pp_FC_change2"), pad, good, "66%");
			text.addPara(i18n_hullmod.get("xel_pp_FC_change3"), pad, good, "20%", "100su");
			text.setBulletedListMode(null);
			tooltip.addImageWithText(pad);
		} else {
			tooltip.addPara(i18n_hullmod.get("xel_pp_no_change"), pad * 2f);
		}
	}

}
