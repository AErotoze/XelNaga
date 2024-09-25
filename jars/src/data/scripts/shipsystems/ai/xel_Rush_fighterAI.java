package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class xel_Rush_fighterAI implements ShipSystemAIScript {

    private static final IntervalUtil interval = new IntervalUtil(0.1f, 0.25f);
    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.flags = flags;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null || engine.isPaused()) return;
//        if (!ship.isFighter()) return;
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            boolean isActive = system.isActive();

            if (flags.hasFlag(ShipwideAIFlags.AIFlags.IN_ATTACK_RUN)
                    || flags.hasFlag(ShipwideAIFlags.AIFlags.POST_ATTACK_RUN)
                    || flags.hasFlag(ShipwideAIFlags.AIFlags.WING_NEAR_ENEMY)) {
                useSystem(isActive);
            }

            if (flags.hasFlag(ShipwideAIFlags.AIFlags.PURSUING)) useSystem(isActive);
            if (target != null) useSystem(isActive);
        }
    }

    private void useSystem(boolean isActive) {
        if (!isActive) {
            ship.useSystem();
        }
    }
}
