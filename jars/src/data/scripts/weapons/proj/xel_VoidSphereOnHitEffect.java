package data.scripts.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class xel_VoidSphereOnHitEffect implements OnHitEffectPlugin {
    /*
        自身仅有50能量伤害，发射一次需要200幅能
        射速5s，备弹1，备弹恢复时间30s，一次恢复量1，射程1500su，追踪能力perfect，导弹结构值：300
        命中护盾则上涨5%总幅能值/1000幅能值的硬幅能，取最小值；命中装甲或船体则额外产生一次700能量伤害的爆炸
     */
    private static final String DATA_KEY = "xel_VoidSphereOnHitEffect_data_key";
    private static final float FLUX_INCREASE_VALUE = 1000f;
    private static final float FLUX_INCREASE_PERCENT = 5f;
    private static final float EXPLOSION_DAMAGE = 700f;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            if (shieldHit) {
                float flux = Math.min(ship.getMaxFlux() * FLUX_INCREASE_PERCENT * 0.01f, FLUX_INCREASE_VALUE);
                ship.getFluxTracker().increaseFlux(flux, true);
            } else {
                MissileAPI mine = (MissileAPI) engine.spawnProjectile(projectile.getSource(), null,
                        "xel_VoidSphere_minelayer",
                        point,
                        (float) (Math.random() * 360f), null);
                if (projectile.getSource()!=null){
                    engine.applyDamageModifiersToSpawnedProjectileWithNullWeapon(
                            projectile.getSource(), WeaponAPI.WeaponType.MISSILE,false,mine.getDamage()
                    );
                }
                float fadeInTime = 0.05f;
                mine.getVelocity().scale(0f);
                mine.fadeOutThenIn(fadeInTime);

                float liveTime = 0f;
                mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
                mine.addDamagedAlready(projectile.getSource());
                mine.setNoMineFFConcerns(true);
            }

        }
    }
}
