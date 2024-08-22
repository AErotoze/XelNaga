package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.HullModUtil;
import data.utils.xel.ShipSystemUtil;
import data.utils.xel.xel_Misc;

import java.awt.*;
import java.util.*;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_PurifiersProtocol extends xel_BaseHullmod {
    /**
     * 净化者协议
     * 检测到对应的系统后，显示一个加强版的系统信息
     * <p>
     * 若系统无可改造的版本[待商榷]
     * 提供一些增益吧
     * 提高5%CR（实现不了）
     * 奖励一部内置LPC（这个可以有……吧；更正：有不了一点）
     * 甲板+1，此甲板上的lpc获得buff，+船体结构，-装配点，整备速率不下降或下降满，整备恢复速率大幅提高？（alex你在写什么啊，这是我可以抄的吗）
     * 强制召回系统：战斗中收到致命攻击后执行强制召回，舰船扣除一次部署的战备值，损失舰船自身10%~100%船员（如果可以，100%恢复舰船结构和装甲吧）（gg）
     * 战术数据网：根据已安装数量提供增益（这个好）:
     * 根据舰船级别提供点数 2/3/4/6 ,点数经由运算换成增益
     * 增益效果：非导弹武器射速最高提高15%，非导弹武器后坐力最高降低25%，非导弹武器射程最高提高100su
     * 加载有实验系统改装的舰船提供的增幅点数x0.5
     * 点数最多20点
     */

    private static final String DATA_KEY = "xel_PurifiersProtocol_data_key";
    private static final float MIN_CREW_MULT = 0f;
    private static final float MAX_CREW_MULT = 0.5f;
    private static final float CR_LOSS_INCREASE = 50f;
    private static final float MAX_DATA_POINT = 20f;
    private static final float DATA_POINT_MULT = 0.5f;
    private static final float DATA_WEB_EFFECT_MULT = 0.2f;
    private static final float ROF_BUFF_PER_POINT = 0.75f;
    private static final float RECOIL_BUFF_PER_POINT = 1.25f;
    private static final float WEAPON_RANGE_BUFF_PER_POINT = 5f;
    private static final Map<ShipAPI.HullSize, Float> dataPointMap = new HashMap<>();

    static {
        dataPointMap.put(ShipAPI.HullSize.FRIGATE, 2f);
        dataPointMap.put(ShipAPI.HullSize.DESTROYER, 3f);
        dataPointMap.put(ShipAPI.HullSize.CRUISER, 4f);
        dataPointMap.put(ShipAPI.HullSize.CAPITAL_SHIP, 6f);
    }

    @Override
    public void init(HullModSpecAPI spec) {
        this.setNotCompatible();
        super.init(spec);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMinCrewMod().modifyMult(id, MIN_CREW_MULT);
        stats.getMaxCrewMod().modifyMult(id, MAX_CREW_MULT);
        stats.getCRLossPerSecondPercent().modifyPercent(id, CR_LOSS_INCREASE);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        float fraction = getAllDataPoint(ship) * (isUpgradableSystem(ship) ? DATA_WEB_EFFECT_MULT : 1f);
        float RoFBonus = fraction * ROF_BUFF_PER_POINT;
        float recoilBonus = 1f - fraction * RECOIL_BUFF_PER_POINT * 0.01f;
        float rangeBonus = fraction * WEAPON_RANGE_BUFF_PER_POINT;
        MutableShipStatsAPI stats = ship.getMutableStats();

        stats.getBallisticRoFMult().modifyPercent(id, RoFBonus);
        stats.getEnergyRoFMult().modifyPercent(id, RoFBonus);
        stats.getMaxRecoilMult().modifyMult(id, recoilBonus);
        stats.getRecoilPerShotMult().modifyMult(id, recoilBonus);
        stats.getRecoilDecayMult().modifyMult(id, recoilBonus);
        stats.getBallisticWeaponRangeBonus().modifyFlat(id, rangeBonus);
        stats.getEnergyWeaponRangeBonus().modifyFlat(id, rangeBonus);

        removeBlockedMod(ship);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "50%";
        else return index == 1 ? "50%" : super.getDescriptionParam(index, hullSize);
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

        tooltip.addSectionHeading(i18n_hullmod.get("xel_pp_title1"), bad, Misc.getDarkPlayerColor(), Alignment.TMID, pad);
        TooltipMakerAPI text;
        // 冲锋
        if (Objects.equals(ship.getSystem().getSpecAPI().getId(), ShipSystemUtil.XEL_RUSH)) {
            text = tooltip.beginImageWithText("graphics/icons/hullsys/maneuvering_jets.png", 64f);
            text.addPara(i18n_hullmod.get("xel_pp_change"), pad * 2f, h, i18n_hullmod.get("xel_pp_rush_change_name"));
            text.setBulletedListMode("--");
            text.addPara(i18n_hullmod.get("xel_pp_rush_change1"), pad, good, "50", "75");
            text.addPara(i18n_hullmod.get("xel_pp_rush_change2"), pad, good, "25");
//            myText.addPara("%s", pad, h, "新增炫酷特效")；
            text.setBulletedListMode(null);
            tooltip.addImageWithText(pad);
        }
        // 统合力场
        else if (Objects.equals(ship.getSystem().getSpecAPI().getId(), ShipSystemUtil.XEL_UNITY_FIELD)) {
            text = tooltip.beginImageWithText("graphics/icons/hullsys/xel_DefensiveMatrix.png", 64f);
            text.addPara(i18n_hullmod.get("xel_pp_change"), pad * 2f, h, i18n_hullmod.get("xel_pp_UF_change_name"));
            text.setBulletedListMode("--");
            text.addPara(i18n_hullmod.get("xel_pp_UF_change1"), pad, good, "50%");
            text.addPara(i18n_hullmod.get("xel_pp_UF_change2"), pad, good, "15%", "25%");
            text.setBulletedListMode(null);
            tooltip.addImageWithText(pad);
        }
        // 快速充能
        else if (Objects.equals(ship.getSystem().getSpecAPI().getId(), ShipSystemUtil.XEL_FAST_CHARGE)) {
            text = tooltip.beginImageWithText(Global.getSettings().getShipSystemSpec(ShipSystemUtil.XEL_FAST_CHARGE).getIconSpriteName(), 64f);
            text.addPara(i18n_hullmod.get("xel_pp_change"), pad * 2f, h, i18n_hullmod.get("xel_pp_FC_change_name"));
            text.setBulletedListMode("--");
            text.addPara(i18n_hullmod.get("xel_pp_FC_change1"), pad, good, "100%");
            text.addPara(i18n_hullmod.get("xel_pp_FC_change2"), pad, good, "66%");
            text.setBulletedListMode(null);
            tooltip.addImageWithText(pad);
        }
        // 虚空静滞
        else if (Objects.equals(ship.getSystem().getSpecAPI().getId(), ShipSystemUtil.XEL_VOID_STASIS)) {
            text = tooltip.beginImageWithText(Global.getSettings().getShipSystemSpec(ShipSystemUtil.XEL_VOID_STASIS).getIconSpriteName(), 64f);
            text.addPara(i18n_hullmod.get("xel_pp_change"), pad * 2f, h, i18n_hullmod.get("xel_pp_VS_change_name"));
            text.setBulletedListMode("--");
            text.addPara(i18n_hullmod.get("xel_pp_VS_change1"), pad, good, "10%");
            text.addPara(i18n_hullmod.get("xel_pp_VS_change2"), pad, bad, "2sec");
            text.setBulletedListMode(null);
            tooltip.addImageWithText(pad);
        } else if (Objects.equals(ship.getSystem().getSpecAPI().getId(), ShipSystemUtil.XEL_SOUL_ABSORPTION)) {
            text = tooltip.beginImageWithText(Global.getSettings().getShipSystemSpec(ShipSystemUtil.XEL_SOUL_ABSORPTION).getIconSpriteName(), 64f);
            text.addPara(i18n_hullmod.get("xel_pp_change"), pad * 2f, h, i18n_hullmod.get("xel_pp_SA_change_name"));
            text.setBulletedListMode("--");
            text.addPara(i18n_hullmod.get("xel_pp_SA_change1"), pad, good, "0.1%");
            text.addPara(i18n_hullmod.get("xel_pp_SA_change2"), pad, bad, "x2");
            text.setBulletedListMode(null);
            tooltip.addImageWithText(pad);
        } else {
            tooltip.addPara(i18n_hullmod.get("xel_pp_unchanged"), pad * 2f);
        }

        tooltip.addSectionHeading(i18n_hullmod.get("xel_pp_title2"), h, Misc.getDarkPlayerColor(), Alignment.TMID, pad);
        tooltip.setBulletedListMode("--");
        tooltip.addPara(i18n_hullmod.get("xel_pp_TDW_effect1"), pad * 2f, h, xel_Misc.getHullSizeFlatString(dataPointMap), (int) MAX_DATA_POINT + "");
        tooltip.addPara(i18n_hullmod.get("xel_pp_TDW_effect2"), pad, new Color[]{h, bad, bad}, i18n_hullmod.get("xel_pp_title1"), String.format("x%.1f", DATA_POINT_MULT), (int) (DATA_WEB_EFFECT_MULT * 100f) + "%");
        tooltip.addPara(i18n_hullmod.get("xel_pp_TDW_effect3"), pad, good, String.format("%.2f%%", ROF_BUFF_PER_POINT), String.format("%.2f%%", RECOIL_BUFF_PER_POINT), (int) WEAPON_RANGE_BUFF_PER_POINT + "su");
        tooltip.addPara(i18n_hullmod.get("xel_pp_TDW_effect4"), pad * 4f, h, String.format("%.1f", getAllDataPoint(ship)));
        tooltip.setBulletedListMode(null);
    }

    /**
     * 计算舰队中安装pp获得全部数据点
     *
     * @param ship 安装pp的本舰船
     * @return 总数据点
     */
    private float getAllDataPoint(ShipAPI ship) {
        if (ship == null) return 10f;
        if (Global.getSettings().getCurrentState() == GameState.TITLE) return 10f;
        if (ship.getFleetMember() == null) return 10f;
        if (ship.getFleetMember().getFleetData() == null) return 10f;


//        Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy().iterator();

        float result = 0f;
        for (FleetMemberAPI member : ship.getFleetMember().getFleetData().getMembersListCopy()) {
            if (member.getStats().getVariant().hasHullMod(HullModUtil.XEL_PURIFIERS_PROTOCOL)
                    && !member.isMothballed()) {
                result += dataPointMap.get(member.getHullSpec().getHullSize()) * (isUpgradableSystem(member) ? DATA_POINT_MULT : 1f);
            }
        }
        return Math.min(MAX_DATA_POINT, result);
    }

    /**
     * 判断战术系统是否可改造，下同
     *
     * @param member 舰队成员
     * @return 可改造返回真，否则假
     */
    private boolean isUpgradableSystem(FleetMemberAPI member) {
        if (member.getHullSpec() != null) {
            for (String system : ShipSystemUtil.XEL_UPGRADABLE_SYSTEMS) {
                if (Objects.equals(member.getHullSpec().getShipSystemId(), system)) return true;
            }
        }
        return false;
    }

    private boolean isUpgradableSystem(ShipAPI ship) {
        if (ship.getSystem() != null) {
            for (String system : ShipSystemUtil.XEL_UPGRADABLE_SYSTEMS) {
                if (Objects.equals(ship.getSystem().getId(), system)) return true;
            }
        }
        return false;
    }
}
