package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class xel_AntiMatterMissileOnHitEffect implements OnHitEffectPlugin {
    /*
        500x2能量伤害，命中装甲或船体后，产生一次500高爆伤害的爆炸，该伤害根据目标的质量调整
        质量大于500时，提高50%爆炸伤害，此后质量每多出100，伤害就额外提高10%
     */

    private static final String DATA_KEY = "xel_AntiMatterMissile_effect_key";
    private static final float MASS_THRESHOLD = 500f;
    private static final float MASS_DEGREE_FLOOR = 100f;
    private static final float DAMAGE_INCREASE_BASE = 50f;
    private static final float DAMAGE_INCREASE_PER_FLOOR = 10f;
    private static final float MAX_DAMAGE_INCREASE = 300f;
    private static final float EXPLOSION_BASE_DAMAGE = 500f;
    private static final Color EXPLOSION_COLOR = new Color(74, 31, 143, 255);

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        //目标是舰船（包括战机）且质量大于等于500时生效
        if (target instanceof ShipAPI && target.getMass() >= MASS_THRESHOLD) {
            ShipAPI enemy = (ShipAPI) target;
            if (enemy.isHulk()) return;

            if (shieldHit) return;
            if (projectile.getSource() == null) return;

            float floor = (float) Math.floor((target.getMass() - MASS_THRESHOLD) / MASS_DEGREE_FLOOR);// 向下取整，质量超过500的100间隔有多少
            float damageFraction = 1f + Math.max(DAMAGE_INCREASE_BASE + floor * DAMAGE_INCREASE_PER_FLOOR, MAX_DAMAGE_INCREASE) * 0.01f;// 每个100间隔提高伤害

            DamagingExplosionSpec explosionSpec = getDamagingExplosionSpec(damageFraction);

            Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, projectile.getSource(), point);
        }
    }

    private static @NotNull DamagingExplosionSpec getDamagingExplosionSpec(float damageFraction) {
        float rad = 25f * damageFraction;
        DamagingExplosionSpec explosionSpec = new DamagingExplosionSpec(
                0.5f,
                rad,
                rad / 2f,
                EXPLOSION_BASE_DAMAGE * damageFraction,
                EXPLOSION_BASE_DAMAGE * damageFraction / 2f,
                CollisionClass.PROJECTILE_NO_FF,
                CollisionClass.PROJECTILE_NO_FF,
                5f,
                15f,
                1f,
                30,
                Color.WHITE,
                EXPLOSION_COLOR
        );
        explosionSpec.setDamageType(DamageType.HIGH_EXPLOSIVE);
        explosionSpec.setUseDetailedExplosion(false);
        explosionSpec.setSoundSetId("mine_explosion");
        return explosionSpec;
    }
}
