package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.utils.xel.xel_Misc;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

import static data.utils.xel.Constants.i18n_shipSystem;
import static data.utils.xel.xel_Misc.findTarget;

public class xel_Rush extends BaseShipSystemScript {
    /*
    冲锋
    获得8s的机动爆发，向敌人前进时额外获得最大速度增益
    特效：引擎出力UPUPUP
     */

    private static final float MAX_SPEED_BONUS = 50f;
    private static final float PP_MAX_SPEED_BONUS = 25f;
    private static final float ACCELERATION_BOUNS = 100f;
    private static final float MAX_CHASE_RANGE = 2000f;
    private static final String DATA_KEY = "xel_Rush_key";
    private static final String PP_KEY = "xel_system_change";

    private static final IntervalUtil Interval = new IntervalUtil(0.1f, 0.1f);
    private static final IntervalUtil empArcInterval = new IntervalUtil(0.1f, 0.2f);


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
            stats.getMaxSpeed().unmodify(DATA_KEY);
            stats.getMaxTurnRate().unmodify(DATA_KEY);
        } else {
            stats.getMaxSpeed().modifyFlat(id, MAX_SPEED_BONUS);
            stats.getAcceleration().modifyPercent(id, 2f * ACCELERATION_BOUNS);
            stats.getDeceleration().modifyPercent(id, 2f * ACCELERATION_BOUNS);

            stats.getTurnAcceleration().modifyFlat(id, MAX_SPEED_BONUS * 0.5f);
            stats.getTurnAcceleration().modifyPercent(id, ACCELERATION_BOUNS);

            stats.getMaxTurnRate().modifyFlat(id, MAX_SPEED_BONUS * 0.5f);
            stats.getMaxTurnRate().modifyPercent(id, ACCELERATION_BOUNS);

            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine.isPaused()) return;
            ShipAPI ship = null;
            if (stats.getEntity() instanceof ShipAPI) {
                ship = (ShipAPI) stats.getEntity();
            } else return;
            boolean isTakenEffect = false;

            if (xel_Misc.hasPurifiersProtocol(ship)) {
                float amount = engine.getElapsedInLastFrame();

                ship.setJitter(id, new Color(254, 138, 14, 40), effectLevel, 3, 10f);
                ship.setJitterUnder(id, new Color(254, 138, 14, 80), effectLevel, 10, 10f);
                ship.setJitterShields(false);
                ship.getEngineController().extendFlame(id, 1.5f, 1.5f, 1.5f);
                ship.getEngineController().fadeToOtherColor(id, new Color(254, 138, 14, 255), null, effectLevel, 0.7f);
                Interval.advance(amount);
                if (Interval.intervalElapsed()) {
                    SpriteAPI sprite = ship.getSpriteAPI();
                    float offsetX = sprite.getWidth() / 2f - sprite.getCenterX();
                    float offsetY = sprite.getHeight() / 2f - sprite.getCenterY();

                    float trueOffsetX = (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetX - (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetY;
                    float trueOffsetY = (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetX + (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetY;

                    //SpriteAPI sprite: 要渲染的sprite对象，可以通过Global.getSettings().getSprite(settings category, settings id)获得。
                    //Vector2f loc: sprite在世界坐标中的中心位置。
                    //Vector2f vel: sprite的速度向量。
                    //Vector2f size: sprite的初始尺寸（宽度和高度），以像素为单位。
                    //Vector2f growth: sprite尺寸的时间变化率，单位是像素/秒，可以是负值（表示sprite缩小）。
                    //float angle: sprite的初始朝向角度，0度指向上方。
                    //float spin: sprite的旋转速度，单位是度/秒。
                    //Color color: sprite的颜色，用于覆盖原始sprite颜色，也用于淡入淡出效果。
                    //boolean additive: 是否使用加色混合模式。
                    //float jitterRange: 基准位置的最大偏移量。
                    //float jitterTilt: 基准旋转的最大偏移角度。
                    //float flickerRange: 闪烁范围，大于1可以使sprite保持显示或隐藏。
                    //float flickerMedian: 闪烁前的默认透明度，可以大于或小于0。
                    //float maxDelay: 更新频率基准是每秒60次，这个值是随机增加的最大延迟。
                    //float fadein: 淡入时间，单位秒。
                    //float full: 最大透明度维持的时间，单位秒。
                    //float fadeout: 淡出时间，单位秒。
                    //CombatEngineLayers layer: 渲染层级，决定了sprite在视觉上的层次。
                    MagicRender.battlespace(
                            Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                            new Vector2f(ship.getLocation().getX() + trueOffsetX, ship.getLocation().getY() + trueOffsetY),
                            new Vector2f(0f, 0f),
                            new Vector2f(sprite.getWidth(), sprite.getHeight()),
                            new Vector2f(0f, 0f),
                            ship.getFacing() - 90f,
                            0f,
                            new Color(254, 138, 14, 52),
                            true,
                            0f, 0f, 0f, 0f, 0f, 0.1f, 0.1f, 1f,
                            CombatEngineLayers.BELOW_SHIPS_LAYER
                    );
                }

//                empArcInterval.advance(amount);
//                if (empArcInterval.intervalElapsed()) {
//                    ShipEngineControllerAPI.ShipEngineAPI shipEngine = ship.getEngineController().getShipEngines().get(MathUtils.getRandomNumberInRange(0, ship.getEngineController().getShipEngines().size() - 1));
//                    engine.spawnEmpArcVisual(
//                            shipEngine.getLocation(),
//                            ship,
//                            MathUtils.getRandomPointInCircle(shipEngine.getLocation(), ship.getCollisionRadius()),
//                            null,
//                            10f,
//                            new Color(255, 117, 62, 150),
//                            new Color(255, 117, 62, 200));
//                }
            } else {
                ship.getEngineController().fadeToOtherColor(id, new Color(0, 178, 255, 255), null, effectLevel, 0.7f);
            }
            ShipAPI target = findTarget(engine, ship, MAX_CHASE_RANGE);
            if (target != null) {
                float shipVelAngle = VectorUtils.getFacing(ship.getVelocity());
                float rushAngle = VectorUtils.getAngle(ship.getLocation(), target.getLocation());
                float chaseAngle = MathUtils.getShortestRotation(shipVelAngle, rushAngle);
                chaseAngle = Math.abs(chaseAngle);
                float bonus = MAX_SPEED_BONUS + (xel_Misc.hasPurifiersProtocol(ship) ? PP_MAX_SPEED_BONUS : 0f);
                if (chaseAngle < 90f) {
                    float chaseEffect = 1f - chaseAngle / 90f;
                    stats.getMaxSpeed().modifyFlat(DATA_KEY, bonus * chaseEffect);
                    stats.getMaxTurnRate().modifyFlat(DATA_KEY, bonus * 0.5f * chaseEffect);
                    isTakenEffect = true;
                    if (engine.getPlayerShip() == ship) {
                        engine.maintainStatusForPlayerShip(DATA_KEY, "graphics/icons/hullsys/maneuvering_jets.png",
                                i18n_shipSystem.get("xel_rush_title"),
                                i18n_shipSystem.format("xel_rush_effct", (int) (bonus * chaseEffect)),
                                false);
                    }
                }
            }
            if (engine.getPlayerShip() == ship && !isTakenEffect) {
                engine.maintainStatusForPlayerShip(DATA_KEY, "graphics/icons/hullsys/maneuvering_jets.png",
                        i18n_shipSystem.get("xel_rush_title"), i18n_shipSystem.get("xel_rush_idle"), true);
            }
        }
    }


    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);

        stats.getMaxSpeed().unmodify(DATA_KEY);
        stats.getMaxTurnRate().unmodify(DATA_KEY);

        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            if (xel_Misc.hasPurifiersProtocol(ship)) {
                stats.getMaxSpeed().modifyFlat(PP_KEY, PP_MAX_SPEED_BONUS);
            }
        }
    }


    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) return new StatusData(i18n_shipSystem.get("xel_rush1"), false);
        else
            return index == 1 ? new StatusData(i18n_shipSystem.format("xel_rush2", (int) MAX_SPEED_BONUS), false) : super.getStatusData(index, state, effectLevel);
    }
}
