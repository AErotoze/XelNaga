package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class xel_VoidStasis extends BaseShipSystemScript {
    /*
    虚空静滞
    对敌对目标释放，使其在 8秒 内被转进虚空
    期间 不受任何伤害，无法开火，无法移动
    凋零虹吸 Withering Siphon
    持续时间减少至 2秒(-75%)
    但是目标将每秒增长 10% 最大幅能值的硬幅能
     */


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        super.apply(stats, id, state, effectLevel);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        super.unapply(stats, id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return super.getStatusData(index, state, effectLevel);
    }
}
