package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.PhaseCloakSystemAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;

import static data.utils.xel.Constants.i18n_shipSystem;

public class xel_XelNagaPhaseStats extends BaseShipSystemScript {

    public static Color JITTER_COLOR = new Color(255, 175, 255, 255);
    public static float JITTER_FADE_TIME = 0.5f;

    public static float SHIP_ALPHA_MULT = 0.25f;
    //public static float VULNERABLE_FRACTION = 0.875f;
    public static float VULNERABLE_FRACTION = 0f;
    public static float INCOMING_DAMAGE_MULT = 0.25f;


    public static float MAX_TIME_MULT = 3f;

    public static boolean FLUX_LEVEL_AFFECTS_SPEED = true;
    public static float MIN_SPEED_MULT = 0.33f;
    public static float BASE_FLUX_LEVEL_FOR_MIN_SPEED = 0.5f;

    protected Object STATUSKEY1 = new Object();
    protected Object STATUSKEY2 = new Object();
    protected Object STATUSKEY3 = new Object();
    protected Object STATUSKEY4 = new Object();


    /**
     * 计算时间增益
     *
     * @param stats 进行计算的对象
     * @return 默认返回3f
     */
    public static float getMaxTimeMult(MutableShipStatsAPI stats) {
        return 1f + (MAX_TIME_MULT - 1f) * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
    }

    protected boolean isDisruptable(ShipSystemAPI cloak) {
        return cloak.getSpecAPI().hasTag(Tags.DISRUPTABLE);
    }

    /**
     * 计算硬幅能水平对速度的影响
     *
     * @param ship 进行计算的舰船
     * @return 返回0-1
     */
    protected float getDisruptionLevel(ShipAPI ship) {
        if (FLUX_LEVEL_AFFECTS_SPEED) {
            //取最低速度的幅能水平阈值，默认0.5
            float threshold = ship.getMutableStats().getDynamic().getMod(
                    Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).computeEffective(BASE_FLUX_LEVEL_FOR_MIN_SPEED);
            if (threshold <= 0) return 1f;
            float level = ship.getHardFluxLevel() / threshold;
            if (level > 1f) level = 1f;
            return level;
        }
        return 0f;
    }


    /**
     * 用于在相位时显示信息
     *
     * @param playerShip  玩家舰船
     * @param state       增益？我猜的，因为这里也没用上
     * @param effectLevel 系统进度
     */
    protected void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {
        float f = VULNERABLE_FRACTION;

        ShipSystemAPI cloak = playerShip.getPhaseCloak();
        if (cloak == null) cloak = playerShip.getSystem();
        if (cloak == null) return;

        if (effectLevel > f) {
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
                    cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(),
                    i18n_shipSystem.get("xel_phase_time_flow"), false);
        }

        if (FLUX_LEVEL_AFFECTS_SPEED) {
            if (effectLevel > f) {
                if (getDisruptionLevel(playerShip) <= 0f) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
                            cloak.getSpecAPI().getIconSpriteName(),
                            i18n_shipSystem.get("xel_phase_coil_stable"),
                            i18n_shipSystem.get("xel_phase_stable_speed"), false);
                } else {

                    String speedPercentStr = Math.round(getSpeedMult(playerShip, effectLevel) * 100f) + "%";
                    Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
                            cloak.getSpecAPI().getIconSpriteName(),
                            i18n_shipSystem.get("xel_phase_coil_stress"),
                            i18n_shipSystem.format("xel_phase_stress_speed", speedPercentStr), true);
                }
            }
        }
    }

    /**
     * 计算舰船的速度变化
     *
     * @param ship        进行计算的舰船
     * @param effectLevel 系统运行进程
     * @return 返回0-1
     */
    public float getSpeedMult(ShipAPI ship, float effectLevel) {
        if (getDisruptionLevel(ship) <= 0f) return 1f;
        return MIN_SPEED_MULT + (1f - MIN_SPEED_MULT) * (1f - getDisruptionLevel(ship) * effectLevel);
    }

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship;
        boolean player;
        // 取舰船
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        // 若是玩家舰船 显示信息
        if (player) {
            maintainStatus(ship, state, effectLevel);
        }

        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        // 取"相位"系统
        ShipSystemAPI cloak = ship.getPhaseCloak();
        if (cloak == null) cloak = ship.getSystem();
        if (cloak == null) return;

        // 硬辐能影响速度为真 则运行对应效果
        if (FLUX_LEVEL_AFFECTS_SPEED) {
            if (state == State.ACTIVE || state == State.OUT || state == State.IN) {
                float mult = getSpeedMult(ship, effectLevel);
                if (mult < 1f) {
                    stats.getMaxSpeed().modifyMult(id + "_2", mult);
                } else {
                    stats.getMaxSpeed().unmodifyMult(id + "_2");
                }
                // 相位线圈抖动
                ((PhaseCloakSystemAPI) cloak).setMinCoilJitterLevel(getDisruptionLevel(ship));
            }
        }

        // 系统处于冷却时间/停用时间 手动重置增益效果
        if (state == State.COOLDOWN || state == State.IDLE) {
            unapply(stats, id);
            return;
        }

        // 速度效果 出于平衡还是别动
        float speedPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f);
        float accelPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).computeEffective(0f);
        stats.getMaxSpeed().modifyPercent(id, speedPercentMod * effectLevel);
        stats.getAcceleration().modifyPercent(id, accelPercentMod * effectLevel);
        stats.getDeceleration().modifyPercent(id, accelPercentMod * effectLevel);

        float speedMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).getMult();
        float accelMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).getMult();
        stats.getMaxSpeed().modifyMult(id, speedMultMod * effectLevel);
        stats.getAcceleration().modifyMult(id, accelMultMod * effectLevel);
        stats.getDeceleration().modifyMult(id, accelMultMod * effectLevel);


        float levelForAlpha = effectLevel;


        // 设置舰船进入相位
        if (state == State.IN || state == State.ACTIVE) {
            ship.setPhased(true);
            levelForAlpha = effectLevel;
        } else if (state == State.OUT) {
            ship.setPhased(effectLevel > 0.5f);
            levelForAlpha = effectLevel;
        }

        //进入相位后 调低舰船透明度
        ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
        ship.setApplyExtraAlphaToEngines(true);

        // 时间效果 处于平衡最好别动
        float extra = 0f;
        float shipTimeMult = 1f + (getMaxTimeMult(stats) - 1f) * levelForAlpha * (1f - extra);
        stats.getTimeMult().modifyMult(id, shipTimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }
    }


    public void unapply(MutableShipStatsAPI stats, String id) {

        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        // 取消时间流速改变
        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);

        // 取消速度增益
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxSpeed().unmodifyMult(id + "_2");
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);

        // 停止相位 透明度恢复
        ship.setPhased(false);
        ship.setExtraAlphaMult(1f);

        // 停止相位线圈的抖动特效
        ShipSystemAPI cloak = ship.getPhaseCloak();
        if (cloak == null) cloak = ship.getSystem();
        if (cloak != null) {
            ((PhaseCloakSystemAPI) cloak).setMinCoilJitterLevel(0f);
        }

    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return super.getStatusData(index, state, effectLevel);
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isActive()) return "已启动";
        else return "已就绪";
    }
}
