package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class xel_SolariteReaperBeamEffect implements BeamEffectPlugin {
    private boolean done = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (done) return;

        ShipAPI ship = beam.getSource();
        CombatEntityAPI target = beam.getDamageTarget();
        if (target != null && beam.getBrightness() >= 1f) {
            if (ship.getSystem() != null && ship.getSystem().isActive()) {
                float radius = Math.min(250f, target.getCollisionRadius());
                Vector2f point = Misc.getPointWithinRadiusUniform(target.getLocation(), radius / 5f, radius, new Random());
                DamagingProjectileAPI e = engine.spawnDamagingExplosion(spawnExplosion(ship), ship, point, false);
                done = true;
            }
        }

    }

    private DamagingExplosionSpec spawnExplosion(ShipAPI source) {
        float level = source.getFluxLevel();
        float baseDamage = 50f + 300f * Math.min(1f, level / 0.5f);

        DamagingExplosionSpec explosionSpec = new DamagingExplosionSpec(
                0.5f,
                50f,
                25f,
                baseDamage,
                baseDamage / 2f,
                CollisionClass.PROJECTILE_NO_FF,
                CollisionClass.PROJECTILE_NO_FF,
                6f,
                10f,
                0.5f,
                75,
                new Color(255, 255, 255, 255),
                new Color(60, 255, 255, 175));

        explosionSpec.setDamageType(DamageType.ENERGY);
        explosionSpec.setUseDetailedExplosion(false);
        explosionSpec.setSoundSetId("mine_explosion");
        return explosionSpec;
    }


}
