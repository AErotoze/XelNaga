package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

public class xel_SoulAbsorption extends xel_BaseShipSystemScript {
    /*
        灵魂吸食

        切换类：未激活时无任何效果，激活后受到buff（1f/s，武器射速降低50%，机动性提高）

        激活时，一定距离内的敌方ship（包括lpc）死亡后都会掉落灵魂，向本体飞去
        每收集10点灵魂，就播放塔达林高阶领主——阿拉纳克的语音（不是）
        战机/护卫舰/驱逐舰/巡洋舰/主力舰 分别掉落1/2/4/6/10点灵魂
        单次战斗最多收集100点灵魂
        别忘记了这是相位主力舰！！！小心超模，也要自洽

        每点灵魂+0.15%伤害低抗/+1虚空能量

        升级后[灵魂之匣]
        每点灵魂额外+0.1%武器伤害

        实现思路：
        用船插描述，系统执行（对的）
        ai直接用混沌的（摸摸我的议长复制人JR-114514
        apply给范围内的船addlistener，开启时直接加，之后每0.5秒执行一次（为了性能着想）
        unapply直接全部removelistener（又是遍历，alex你不能自己写个遍历方法吗？非要全图）
     */

    public static final String DATA_KEY = "xel_SoulAbsorption_data_key";
    private static final float RANGE = 2000f;

    private static final float WEAPON_ROF_DECREASE = 0.5f;

    private static final Map<ShipAPI.HullSize, Float> soulMap = new HashMap<>();
    private static final IntervalUtil checker = new IntervalUtil(0.2f, 0.2f);

    static {
        soulMap.put(ShipAPI.HullSize.FIGHTER, 1f);
        soulMap.put(ShipAPI.HullSize.FRIGATE, 2f);
        soulMap.put(ShipAPI.HullSize.DESTROYER, 4f);
        soulMap.put(ShipAPI.HullSize.CRUISER, 6f);
        soulMap.put(ShipAPI.HullSize.CAPITAL_SHIP, 10f);
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        stats.getBallisticRoFMult().modifyMult(DATA_KEY + id, WEAPON_ROF_DECREASE * effectLevel);
        stats.getEnergyRoFMult().modifyMult(DATA_KEY + id, WEAPON_ROF_DECREASE * effectLevel);
        stats.getMissileRoFMult().modifyMult(DATA_KEY + id, WEAPON_ROF_DECREASE * effectLevel);

        stats.getAcceleration().modifyPercent(DATA_KEY + id, 200f * effectLevel);
        stats.getDeceleration().modifyPercent(DATA_KEY + id, 200f * effectLevel);
        stats.getTurnAcceleration().modifyFlat(DATA_KEY + id, 30f * effectLevel);
        stats.getTurnAcceleration().modifyPercent(DATA_KEY + id, 200f * effectLevel);
        stats.getMaxTurnRate().modifyFlat(DATA_KEY + id, 15f);
        stats.getMaxTurnRate().modifyPercent(DATA_KEY + id, 100f);

        ShipAPI ship = getPlayerShip(stats);
        if (ship == null) return;



        checker.advance(Global.getCombatEngine().getElapsedInLastFrame());
        if (checker.intervalElapsed()) {
            for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, RANGE)) {
                if (!enemy.hasListenerOfClass(spawnSoul.class)) {
                    enemy.addListener(new spawnSoul());
                }
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getBallisticRoFMult().unmodify(DATA_KEY + id);
        stats.getEnergyRoFMult().unmodify(DATA_KEY + id);
        stats.getMissileRoFMult().unmodify(DATA_KEY + id);

        stats.getMaxTurnRate().unmodify(DATA_KEY + id);
        stats.getTurnAcceleration().unmodify(DATA_KEY + id);
        stats.getAcceleration().unmodify(DATA_KEY + id);
        stats.getDeceleration().unmodify(DATA_KEY + id);

        ShipAPI ship = getPlayerShip(stats);
        if (ship == null) return;
        for (ShipAPI enemy : Global.getCombatEngine().getShips()) {
            if (!enemy.isAlive())continue;
            if (enemy.getOwner()==ship.getOwner())continue;

            if (enemy.hasListenerOfClass(spawnSoul.class)) {
                enemy.removeListenerOfClass(spawnSoul.class);
            }
        }
    }


    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return super.getStatusData(index, state, effectLevel);
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        int num = 0;
        for (ShipAPI target : Global.getCombatEngine().getShips()) {
            if (!target.isAlive()) continue;
            if (target == ship) continue;
            if (Misc.getDistance(target.getLocation(), ship.getLocation()) > RANGE) continue;
            if (hasPP(ship) && target.getOwner() == 1) {
                num += (int) (soulMap.get(target.getHullSize()) * 2);
            } else {
                num += soulMap.get(target.getHullSize());
            }
        }
        if (num > 40) return "存在极多灵魂";
        else if (num > 20) return "存在大量灵魂";
        else if (num > 10) return "存在中量灵魂";
        else if (num > 0) return "存在少量灵魂";
        else return "不存在灵魂";
    }

    private static class spawnSoul implements HullDamageAboutToBeTakenListener {

        public spawnSoul() {
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if (damageAmount >= ship.getHitpoints()) {
                Global.getCombatEngine().spawnProjectile(ship, (WeaponAPI) null, "xel_Soul", ship.getLocation(), (float) (Math.random() * 360f), ship.getVelocity());
                ship.removeListener(this);
            }
            return false;
        }
    }
}
