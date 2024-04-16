package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.HullModUtil;
import data.utils.xel.xel_Misc;

import java.util.*;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_BaseHullmod extends BaseHullMod {
    private static final int MAX_MODS = 2;
    /*
    难以想象姨丈的按F1显示更多信息，用的船插做的图……
     */
    private final Set<String> BLOCKED_HULLMODS = new LinkedHashSet<>();

    /**
     * 写入不兼容的船插ID
     *
     * @param blockedModIDs 一串ID，用 , 隔开
     */
    protected void setNotCompatible(String... blockedModIDs) {
        this.BLOCKED_HULLMODS.addAll(Arrays.asList(blockedModIDs));
    }

    /**
     * 用于applyEffectsAfterShipCreation
     * 当本船插已安装时，拒绝安装不兼容船插，强行点击发出报警
     *
     * @param ship 进行计算的舰船
     */
    public void removeBlockedMod(ShipAPI ship) {
        for (String tmp : this.BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                ship.getVariant().removeMod(tmp);
                ship.getVariant().removePermaMod(tmp);
                Global.getSoundPlayer().playUISound("cr_allied_warning", 1.0F, 1.0F);
            }
        }
    }

    /**
     * 判断是否有不兼容船插
     *
     * @param ship 进行计算的舰船
     * @return 存在不兼容船插返回ture，反之返回false
     */
    public boolean hasNotCompatibleMod(ShipAPI ship) {
        if (ship != null && ship.getVariant() != null) {
            for (String tmp : this.BLOCKED_HULLMODS) {
                if (ship.getVariant().hasHullMod(tmp))
                    return true;
            }
        }
        return false;
    }

    /**
     * @return 返回 "不兼容于 a，b，c…… 船体插件"
     */
    public String getNotCompatibleReason() {
        List<String> names = new ArrayList<>();

        for (String hullMod : this.BLOCKED_HULLMODS) {
            HullModSpecAPI modSpec = Global.getSettings().getHullModSpec(hullMod);
            if (modSpec != null) {
                names.add(modSpec.getDisplayName());
            }
        }
        return i18n_hullmod.format("notCompatibleWith", new Object[]{Misc.getAndJoined(names)});
    }

    /**
     * 判断响应模组是否超出限制
     *
     * @param ship 进行计算的舰船
     * @return 未超出限制返回false，反之返回true
     */
    public boolean hasTooMuchResponseMod(ShipAPI ship) {
        int num = getNumXelResponseMods(ship);
        boolean has = spec != null && ship.getVariant().hasHullMod(spec.getId());// 判断该 自身 是否安装
        if (has) num--;// 若已安装则不计数 自身
        return num >= MAX_MODS;
    }

    /**
     * @return 返回 "至多能安装 2 个星灵响应模组"
     */
    public String getTooMuchResponseModReason() {
        return i18n_hullmod.get("tooMuchResponseMod");
    }

    /**
     * @param ship 进行计算的舰船
     * @return 已安装的响应模组数量
     */
    protected int getNumXelResponseMods(ShipAPI ship) {
        int num = 0;
        for (String id : ship.getVariant().getHullMods()) {
//			if (ship.getHullSpec().isBuiltInMod(id)) continue;
//			if (ship.getVariant().getPermaMods().contains(id)) continue;

            HullModSpecAPI mod = Global.getSettings().getHullModSpec(id);
            if (mod.hasUITag(HullModUtil.XEL_RESPONSE_MOD_TAG)) {
                num++;
            }
        }
        return num;
    }

    /**
     * 判断是否有 星灵能量矩阵 船体插件
     *
     * @param ship 进行计算的舰船
     * @return 存在插件返回ture，反之返回false
     */
    public boolean hasEnergyArrayMod(ShipAPI ship) {
        return ship.getVariant().hasHullMod(HullModUtil.XEL_PROTOSS_ENERGY_ARRAY);
    }


    /**
     * @return 返回 "需要 星灵能量矩阵 船体插件"
     */
    public String getNoEnergyArrayReason() {
        return i18n_hullmod.format("needSupportWith", xel_Misc.getHullmodName(HullModUtil.XEL_PROTOSS_ENERGY_ARRAY));
    }
}
