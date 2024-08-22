package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.utils.xel.xel_Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class xel_PowerOverwhelming extends xel_BaseShipSystemScript {
    /*
        势不可挡[无升级]
        生成1道2000能量伤害2000emp伤害的根据目标硬幅能的概率穿盾电弧，击中目标前会分成3道电弧（总伤 / 3）
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

            float thickness = Math.max(target.getCollisionRadius() * 0.2f, 40f);
            float pierceChance = target.getHardFluxLevel() - 0.1f;
            pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

            float shieldAngle = target.getShield() != null ? 45f : Math.max(180f, target.getShield().getActiveArc()) / 2f;
            float angle = VectorUtils.getAngle(target.getLocation(), ship.getLocation());
            float minAngle = angle - shieldAngle;
            float maxAngle = angle + shieldAngle;

            //point由地图原点出发指向中心为target的规定圆锥内的某处
            Vector2f point = MathUtils.getRandomPointInCone(
                    target.getLocation(),
                    target.getCollisionRadius(),
                    minAngle, maxAngle);
            //将point规定在距离taget一定距离内
            Vector2f.add(
                    target.getLocation(),
                    VectorUtils.clampLength(
                            Vector2f.sub(point, target.getLocation(), null),
                            target.getCollisionRadius() * 1.25f, target.getCollisionRadius() * 1.35f),
                    point);

            engine.spawnEmpArcVisual(
                    MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 1.1f),
                    ship,
                    point,
                    null,
                    thickness, empColor, empColor);

            for (int i = 0; i < 3; i++) {
                if (pierceChance > Math.random()) {
                    engine.spawnEmpArcPierceShields(
                            ship,
                            point,
                            null,
                            target,
                            DamageType.ENERGY,
                            DAMAGE / 3f,
                            EMP / 3f,
                            100000f,
                            "tachyon_lance_emp_impact",
                            thickness / 3f,
                            empColor, empColor);
                } else {
                    engine.spawnEmpArc(
                            ship,
                            point,
                            null,
                            target,
                            DamageType.ENERGY,
                            DAMAGE / 3f,
                            EMP / 3f,
                            100000f,
                            "tachyon_lance_emp_impact",
                            thickness / 3f,
                            empColor, empColor
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
