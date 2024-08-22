package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class xel_SoulAbsorptionAI implements ShipSystemAIScript {
    private static final IntervalUtil interval = new IntervalUtil(0.5f, 0.5f);
    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;

    /*
        系统开启后会降低武器射速，提高机动性，同时吸食敌我灵魂
        使用情况应如下：
        幅能高压开
        周边舰船多且处于交火状态开
        追击时关（如有条件）

        参考ai：趋光扩展 混沌-级 日月交食
     */

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.flags = flags;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine != null && !engine.isPaused()) {
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                boolean isActive = system.isActive();
                float fluxLevel = ship.getFluxLevel();

                //周边出现敌人
                if (ship.areAnyEnemiesInRange()) {
                    //是否需要友军护航
                    if (!flags.hasFlag(AIFlags.NEEDS_HELP)) {
                        //是否在追击敌人
                        if (flags.hasFlag(AIFlags.PURSUING)) {
                            if (target != null) {
                                //目标在可吸取范围且结构值低
                                if (MathUtils.getDistance(ship, target) <= 1200f && target.getHullLevel() <= 0.2f) {
                                    turnOnActive(isActive);
                                } else {
                                    turnOnIdle(isActive);
                                }
                            }
                        } else {
                            turnOnIdle(isActive);
                        }
                        //自身幅能压力高
                    } else if (fluxLevel >= 0.75f) {
                        if (target != null) {
                            //就该开
                            turnOnActive(isActive);
                        }
                        //既不需护航，幅能压力也可以接受
                        //周边有过载中或结构值低的友方舰船
                    } else {
                        List<ShipAPI> allies = AIUtils.getNearbyAllies(ship, 1200f);
                        for (ShipAPI ally : allies) {
                            if (ally.getHullLevel() <= 0.2f && ally.getFluxTracker().isOverloaded()) {
                                turnOnActive(isActive);
                            } else {
                                turnOnIdle(isActive);
                            }
                        }
                    }
                } else {
                    turnOnIdle(isActive);
                }
            }
        }
    }

    private void turnOnActive(boolean isActive) {
        if (!isActive) {
            ship.useSystem();
        }
    }

    private void turnOnIdle(boolean isActive) {
        if (isActive) {
            ship.useSystem();
        }
    }
}
