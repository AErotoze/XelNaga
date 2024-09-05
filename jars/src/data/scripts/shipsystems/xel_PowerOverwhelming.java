package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.xel_Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class xel_PowerOverwhelming extends xel_BaseShipSystemScript {
    /*
        势不可挡[无升级]
        3道500能量伤害500emp伤害电弧
        充能次数5 使用间隔1.754s 充能时间 12s
     */

    private static final String DATA_KEY = "xel_PowerOverwhelming_data_key";
    private static final float DAMAGE = 2000f;
    private static final float EMP = 2000f;
    private static final float RANGE = 1200f;
    private static final Color empColor = new Color(109, 212, 255);
    private Boolean done = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (getPlayerShip(stats) != null && !done) {
            done = true;
            ShipAPI ship = getPlayerShip(stats);
            CombatEngineAPI engine = Global.getCombatEngine();
            ShipAPI target = xel_Misc.findTarget(engine, ship, RANGE);

            if (target == null) return;//应该不需要，有isUsable

            float thickness = Math.max(target.getCollisionRadius() * 0.2f, 20f);
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());

//            boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(Vector2f.sub(target.getLocation(), ship.getLocation(), null));
            float pierceChance = target.getHardFluxLevel() - 0.1f;
            pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

            for (int i = 0; i < 3; i++) {
                float angle1 = Misc.getAngleInDegrees(ship.getLocation(), target.getLocation());
                float angle2 = Misc.getAngleInDegrees(target.getLocation(), ship.getLocation());
                float rad1 = ship.getShield() == null ? dist * 0.33f : Math.max(dist * 0.33f, ship.getShield().getRadius());
                float rad2 = target.getShield() == null ? dist * 0.33f : Math.max(dist * 0.33f, target.getShield().getRadius());
                Vector2f point1 = MathUtils.getRandomPointInCone(ship.getLocation(), rad1, angle1 - 10f, angle1 + 10f);
                Vector2f point2 = MathUtils.getRandomPointInCone(target.getLocation(), rad2, angle2 - 5f, angle2 + 5f);

                engine.spawnEmpArcVisual(
                        MathUtils.getRandomPointInCone(ship.getLocation(), ship.getCollisionRadius() * 1.2f, angle1 - 30f, angle1 + 30f),
                        ship,
                        point1,
                        null,
                        thickness,
                        empColor, empColor);
                engine.spawnEmpArcVisual(point1, null, point2, null, thickness, empColor, empColor);

                if (pierceChance > Math.random()) {
                    engine.spawnEmpArcPierceShields(
                            ship,
                            point2,
                            target,
                            target,
                            DamageType.ENERGY,
                            500f,
                            500f,
                            10000f,
                            "tachyon_lance_emp_impact",
                            thickness, empColor, empColor
                    );
                } else {
                    engine.spawnEmpArc(
                            ship,
                            point2,
                            target,
                            target,
                            DamageType.ENERGY,
                            500f,
                            500f,
                            10000f,
                            "tachyon_lance_emp_impact",
                            thickness, empColor, empColor
                    );
                }
            }

        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (done) done = false;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return index == 0 ? new StatusData("Mok'taridan", false) : null;
    }

    public static float getMaxRange(ShipAPI ship) {
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isOutOfAmmo()) return null;
        if (system.getState() != ShipSystemAPI.SystemState.IDLE) return null;
        ShipAPI target = xel_Misc.findTarget(Global.getCombatEngine(), ship, getMaxRange(ship));
        if (target != null) {
            if (target.isPhased()) {
                return "UNAVAILABLE TARGET";
            }
            return "READY";
        }
        if (ship.getShipTarget() != null) {
            return "OUT OF RANGE";
        }
        return "NO TARGET";
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        ShipAPI target = xel_Misc.findTarget(Global.getCombatEngine(), ship, RANGE);
        return target != null && !target.isPhased();

    }
}
