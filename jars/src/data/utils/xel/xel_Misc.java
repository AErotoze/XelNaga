package data.utils.xel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.fs.starfarer.api.util.Misc.getJoined;

public class xel_Misc {
    public static Vector2f V2ZERO = new Vector2f();
    public static Random RND = new Random();

    public static float nextFloat() {
        return RND.nextFloat();
    }

    public static String getHullSizeFlatString(Map<HullSize, ? extends Number> map) {
        return map.get(HullSize.FRIGATE).intValue() + "/" + map.get(HullSize.DESTROYER).intValue() + "/"
                + map.get(HullSize.CRUISER).intValue() + "/" + map.get(HullSize.CAPITAL_SHIP).intValue();
    }

    public static String getHullSizePercentString(Map<ShipAPI.HullSize, Float> map) {
        return map.get(HullSize.FRIGATE).intValue() + "%/" + map.get(HullSize.DESTROYER).intValue() + "%/"
                + map.get(HullSize.CRUISER).intValue() + "%/" + map.get(HullSize.CAPITAL_SHIP).intValue() + "%";
    }

    public static String getHullmodName(String hullmodId) {
        return Global.getSettings().getHullModSpec(hullmodId).getDisplayName();
    }

    public static String getShipSystemSpecName(String systemId) {
        return Global.getSettings().getShipSystemSpec(systemId).getName();
    }

    public static Boolean hasPurifiersProtocol(ShipAPI ship) {
        return ship.getVariant().hasHullMod(HullModUtil.XEL_PURIFIERS_PROTOCOL);
    }

    /**
     * 返回range内敌舰目标
     *
     * @param engine 战斗引擎
     * @param ship   自己所在的舰船
     * @param range  测量范围
     * @return 被锁定目标或距离鼠标最近的敌舰目标
     */
    public static ShipAPI findTarget(CombatEngineAPI engine, ShipAPI ship, float range) {
        if (ship.getShipTarget() != null && ship.getShipTarget().isAlive() && ship.getShipTarget().getOwner() != ship.getOwner()) {
            if (MathUtils.isWithinRange(ship.getLocation(), ship.getShipTarget().getLocation(), range)) {
                return ship.getShipTarget(); // 直接返回距离内R键锁定目标
            }
        }
        Vector2f mouseLocation = ship.getMouseTarget();
        float closestDest = Float.MAX_VALUE;
        ShipAPI finalTarget = null;
        // 遍历场上舰船
        for (ShipAPI target : engine.getShips()) {
            if (!target.isAlive() || target == ship || ship.getOwner() == target.getOwner()) continue;
            if (!MathUtils.isWithinRange(ship.getLocation(), target.getLocation(), range)) {
                continue;
            }
            float distance = MathUtils.getDistance(mouseLocation, target.getLocation());
            // 防止溢出
            if (distance <= closestDest) {
                closestDest = distance;
                finalTarget = target;
            }
        }
        return finalTarget;// 返回范围内距离鼠标最近的目标
    }

    public static String getOrJoined(List<String> strings) {
        return getOrJoined(strings.toArray(new String[0]));
    }

    public static String getOrJoined(String... strings) {
        return getJoined("or", strings);
    }

    // 闪电风暴来咯
    public static void spawnFakeEmpInShipRange(ShipAPI target, float range, float thickness, Color fringe, Color core) {
        Vector2f loc = MathUtils.getRandomPointInCircle(target.getLocation(), range);
        Global.getCombatEngine().spawnEmpArcVisual(
                loc,
                null,
                MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius()),
                null,
                thickness, fringe, core);
    }

    /**
     * 生成复数粒子特效
     *
     * @param engine   战斗引擎
     * @param loc      特效生成位置
     * @param vel      特效的速度向量
     * @param count    特效生成次数
     * @param size     粒子特效的尺寸
     * @param spread   扩散范围
     * @param duration 持续时间
     * @param color    颜色
     */
    public static void spawnSeveralParticles(CombatEngineAPI engine, Vector2f loc, Vector2f vel, int count,
                                             float size, float spread, float duration, Color color) {
        float origin = VectorUtils.getFacing(vel);
        float dis = vel.length();

        for (int i = 0; i < count; ++i) {
            float angle = origin + MathUtils.getRandomNumberInRange(-spread * 0.5F, spread * 0.5F);
            float radius = dis * MathUtils.getRandomNumberInRange(0.5F, 1.0F);
            angle = MathUtils.clampAngle(angle);
            size *= MathUtils.getRandomNumberInRange(0.75F, 1.25F);
            Vector2f vec = MathUtils.getPointOnCircumference(null, radius, angle);
            engine.addSmoothParticle(loc, vec, size, 1.0F, duration, color);
        }
    }
}
