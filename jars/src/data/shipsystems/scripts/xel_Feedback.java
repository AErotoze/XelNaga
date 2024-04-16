package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;

public class xel_Feedback extends BaseShipSystemScript {
	/*
	反馈
	选择一名敌方目标，根据目标当前空余的幅能值引发闪电风暴
	闪电风暴以目标为中心，对周围600su（或者根据空余幅能值）的敌我双方进行EMP电弧打击，空余幅能值越高伤害越高
	净化者协议 能量潮涌 energy surge
	对友方不再造成伤害（或是恢复幅能）
	自身周围也会引发闪电风暴
	 */

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else return;

        if (effectLevel > 0f) {
            float range = ship.getCollisionRadius();
            float jitterLevel = effectLevel;
            if (state == State.OUT) jitterLevel *= jitterLevel;
            ship.setJitter(ship, new Color(55, 55, 255, 64), effectLevel, 3, range * jitterLevel, range * jitterLevel * 2f);
            ship.setJitterUnder(ship, new Color(125, 125, 255, 64), jitterLevel, 10, 0f, 30f * jitterLevel);
        }
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
