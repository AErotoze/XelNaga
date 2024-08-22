package data.scripts.weapons.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.shipsystems.xel_SoulAbsorption;
import data.scripts.shipsystems.xel_VoidShiftStats;
import data.utils.xel.HullModUtil;
import data.utils.xel.ShipSystemUtil;
import data.utils.xel.xel_Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.AnchoredEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import static data.scripts.shipsystems.xel_SoulAbsorption.DATA_KEY;

public class xel_SoulAI implements MissileAIPlugin, GuidedMissileAI {

    private MissileAPI missile;
    private ShipAPI launchingShip;
    private CombatEngineAPI engine;
    private boolean done;
    private boolean aspectLocked = true;
    private ShipAPI target;
    private AnchoredEntity targetAnchorEntity;
    private final IntervalUtil checker = new IntervalUtil(0.1f, 0.25f);

    private static final float DAMAGE_TAKEN_DECREASE_PER_SOUL = 0.15f;
    private static final float WEAPON_DAMAGE_BONUS_PER_SOUL = 0.1f;
    private static final float MAX_SOUL = 100f;

//    private float closestDis = Float.MAX_VALUE;


    public xel_SoulAI(MissileAPI missile, ShipAPI launchingShip) {
        this.missile = missile;
        this.launchingShip = launchingShip;
        this.engine = Global.getCombatEngine();
        this.done = false;

        setTarget(findBestTarget());
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        if (target instanceof ShipAPI) {
            this.target = (ShipAPI) target;
        } else {
            this.target = null;
        }
    }

    @Override
    public void advance(float amount) {
        if (done) return;
        if (engine.isPaused()) return;
        if (missile.isFizzling() || missile.isFading()) return;

        if (target != null && !target.isAlive()) {
            target = null;
            targetAnchorEntity = null;
        }

        if (target == null) {
            checker.advance(amount);
            if (checker.intervalElapsed()) {
                setTarget(target);
            } else {
                missile.giveCommand(ShipCommand.ACCELERATE);
            }
            return;
        }

        float dis = Misc.getDistance(target.getLocation(), missile.getLocation());
        float acceleration = missile.getAcceleration();
        float maxSpeed = missile.getMaxSpeed();

        Vector2f calculationVelocity = new Vector2f(missile.getVelocity());
        if (calculationVelocity.length() <= maxSpeed * 0.5f) {
            if (calculationVelocity.length() <= maxSpeed * 0.25f) {
                calculationVelocity.set(maxSpeed * 0.5f, 0f);
                VectorUtils.rotate(calculationVelocity, missile.getFacing(), calculationVelocity);
            } else {
                calculationVelocity.scale((maxSpeed * 0.5f) / calculationVelocity.length());
            }
        }

        Vector2f guidedTarget;
        if (targetAnchorEntity != null) {
            guidedTarget = targetAnchorEntity.getLocation();
        } else {
            if (findBestTarget() == null) {
                guidedTarget = null;
                return;
            } else {
                guidedTarget = Objects.requireNonNull(findBestTarget()).getLocation();
            }
        }


        float velocityFacing = VectorUtils.getFacing(calculationVelocity);
        float absoluteDis = MathUtils.getShortestRotation(velocityFacing, VectorUtils.getAngle(missile.getLocation(), guidedTarget));
        float angularDis = MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), guidedTarget));
        float compensationDifference = MathUtils.getShortestRotation(angularDis, absoluteDis);
        if (Math.abs(compensationDifference) <= 75f) {
            angularDis += 0.5f * compensationDifference;
        }

        float absDAng = Math.abs(angularDis);

        if (aspectLocked && absDAng > 60f) {
            aspectLocked = false;
        }

        if (!aspectLocked && absDAng <= 15f) {
            aspectLocked = true;
        }

        float turnRadius = (missile.getMaxSpeed() * (360f / missile.getMaxTurnRate() / (2f * (float) Math.PI)));
        if (aspectLocked || dis > 2.5f * turnRadius) {
            missile.giveCommand(ShipCommand.ACCELERATE);
        }

        if (absDAng > 1) {
            missile.giveCommand(angularDis < 0 ? ShipCommand.TURN_RIGHT : ShipCommand.TURN_LEFT);
        }

        if (absDAng < 5) {
            float MFlightAng = VectorUtils.getAngle(xel_Misc.V2ZERO, calculationVelocity);
            float MFlightCC = MathUtils.getShortestRotation(missile.getFacing(), MFlightAng);
            if (Math.abs(MFlightCC) > 20) {
                missile.giveCommand(MFlightCC < 0 ? ShipCommand.STRAFE_LEFT : ShipCommand.STRAFE_RIGHT);
            }
        }

        if (absDAng < Math.abs(missile.getAngularVelocity() * 0.35f)) {
            missile.setAngularVelocity(angularDis / 0.35f);
        }

        if (reachTarget()) {
            MutableShipStatsAPI stats = target.getMutableStats();
            String id = target.getId();

            Float soul = (Float) target.getCustomData().get(xel_SoulAbsorption.DATA_KEY + "_soul_point");
            if (soul == null) {
                soul = 0f;
            }

            soul += 1f;
            target.setCustomData(xel_SoulAbsorption.DATA_KEY + "_soul_point", soul);

            soul = Math.min(MAX_SOUL, soul);

            stats.getHullDamageTakenMult().modifyMult(id, 1f - soul * DAMAGE_TAKEN_DECREASE_PER_SOUL * 0.01f);
            stats.getArmorDamageTakenMult().modifyMult(id, 1f - soul * DAMAGE_TAKEN_DECREASE_PER_SOUL * 0.01f);
//        stats.getShieldDamageTakenMult().modifyMult( id, 1f - soul * DAMAGE_TAKEN_DECREASE_PER_SOUL * 0.01f);

            stats.getDynamic().getStat(xel_VoidShiftStats.VS_ENERGY_FLAT_BONUS).modifyFlat(id, soul);

            if (target.getVariant().hasHullMod(HullModUtil.XEL_PURIFIERS_PROTOCOL)) {
                stats.getEnergyWeaponDamageMult().modifyMult(id, 1f + soul + WEAPON_DAMAGE_BONUS_PER_SOUL * 0.01f);
                stats.getBallisticWeaponDamageMult().modifyMult(id, 1f + soul + WEAPON_DAMAGE_BONUS_PER_SOUL * 0.01f);
                stats.getMissileWeaponDamageMult().modifyMult(id, 1f + soul + WEAPON_DAMAGE_BONUS_PER_SOUL * 0.01f);
            }

            RippleDistortion ripple = new RippleDistortion(missile.getLocation(), xel_Misc.V2ZERO);
            ripple.flip(false);//波纹翻转
            ripple.setSize(18f);// 波纹范围
            ripple.setFrameRate(180f);
            ripple.setIntensity(10f);// 波纹强度
            ripple.fadeOutSize(0.5f);// 波纹消逝时间
            ripple.fadeInIntensity(0.7f);// 波纹消逝强度？
            DistortionShader.addDistortion(ripple);//生成特效

            engine.addSwirlyNebulaParticle(
                    missile.getLocation(),
                    xel_Misc.V2ZERO,
                    missile.getCollisionRadius() * 4f,
                    0.2f, 1f, 0.3f, 0.25f,
                    new Color(255,75,75,155),
                    false
            );

            end();
        }
    }

    /*
    机制：灵魂吸取（以下简称SA）开启后对可选目标加上唯一的listener，目标被破坏后掉落灵魂，飞向最近的 搭载了SA的敌舰
     */
    private ShipAPI findBestTarget() {
        List<ShipAPI> ships = CombatUtils.getShipsWithinRange(missile.getLocation(), 3000f * (1f - missile.getFlightTime() / missile.getMaxFlightTime()));
        WeightedRandomPicker<ShipAPI> picker = new WeightedRandomPicker<>();
        for (ShipAPI ship : ships) {
            if (!ship.isAlive()) continue;
            if (ship.isFighter()) continue;
            if (ship.getSystem() == null || !Objects.equals(ship.getSystem().getId(), ShipSystemUtil.XEL_SOUL_ABSORPTION))
                continue;

            float weight = Math.max(0f, 1f / Misc.getDistance(missile.getLocation(), ship.getLocation()));
            picker.add(ship, weight);
        }

        if (picker.isEmpty()) return null;
        return picker.pick();
    }

    private boolean reachTarget() {
        if (target == null) return false;
        float curDis = Misc.getDistance(missile.getLocation(), target.getLocation());

        return curDis <= (target.getCollisionRadius() * 0.75f);

    }

    private void end() {
        missile.fadeOutThenIn(0);

        target = null;
        engine.removeEntity(targetAnchorEntity);
        targetAnchorEntity = null;
        done = true;
        engine.removeEntity(missile);
    }
}
