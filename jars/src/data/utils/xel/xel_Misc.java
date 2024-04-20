package data.utils.xel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

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

}
