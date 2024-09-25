package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import data.utils.xel.Constants;
import data.utils.xel.ShipSystemUtil;
import data.utils.xel.xel_Misc;

public class xel_VoidAccelerator extends xel_BaseShipSystemScript {
    /*
    虚空加速器：（虚空寻觅者专属）
    时流，唉，怎么就绕不开你（错误的，我直接绕
    设定上是舰船部分区域时间加速获得增益
    状态切换
    加快幅能排散/加快武器射速和武器弹药恢复速率
    幅能排散x2    射速x2，弹药恢复速率x2
     */
    private static final String DATA_KEY = "xel_VoidAccelerator_data_key";
    private static final float ROF_BONUS = 100f;
    private static final float REGEN_BONUS = 100f;
    private static final float FLUX_MULT = 2f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        stats.getEnergyRoFMult().modifyPercent(DATA_KEY + id, ROF_BONUS * effectLevel);
//        stats.getBallisticRoFMult().modifyPercent(DATA_KEY + id, ROF_BONUS);
//        stats.getMissileRoFMult().modifyPercent(DATA_KEY + id, ROF_BONUS);
        stats.getMissileAmmoRegenMult().modifyPercent(DATA_KEY + id, REGEN_BONUS * effectLevel);
//        stats.getEnergyAmmoRegenMult().modifyPercent(DATA_KEY + id, REGEN_BONUS);
//        stats.getBallisticAmmoRegenMult().modifyPercent(DATA_KEY + id, REGEN_BONUS);

        stats.getFluxDissipation().unmodify(DATA_KEY + id);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getFluxDissipation().modifyMult(DATA_KEY + id, FLUX_MULT);

        stats.getEnergyRoFMult().unmodify(DATA_KEY + id);
        stats.getMissileAmmoRegenMult().unmodify(DATA_KEY + id);

        ShipAPI ship = getPlayerShip(stats);
        if (ship != null && ship == Global.getCombatEngine().getPlayerShip()){
            Global.getCombatEngine().maintainStatusForPlayerShip(
                    DATA_KEY + "_01",
                    "",
                    xel_Misc.getShipSystemSpecName(ShipSystemUtil.XEL_VOID_ACCELERATOR),
                    Constants.i18n_shipSystem.get("xel_VA_idle"),
                    false);
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return index == 0 ? new StatusData(Constants.i18n_shipSystem.get("xel_VA_active"), false) : super.getStatusData(index, state, effectLevel);
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        return ship.getSystem().isActive() ? "黎明使徒" : "黑暗代理";
    }
}
