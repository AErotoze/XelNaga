package data.scripts.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class xel_VoidFuryOnHitEffect implements OnHitEffectPlugin {
    /*
    500能量伤害，500EMP伤害，发射一次500幅能
        射速3s，射程700su，精度perfect
        命中船体或装甲时大概率产生打击武器与引擎的电弧，造成额外 EMP 与能量伤害。当命中护盾时，将根据目标硬幅能水平随机产生穿透护盾的电弧。
     */
    private static final String DATA_KEY = "xel_VoidFuryOnHitEffect_data_key";
    private static final Color EMP_COLOR = new Color(0, 167, 98, 255);

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            float pierceChance = ship.getHardFluxLevel() - 0.1f;
            pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);
            boolean piercedShield = shieldHit && Math.random() < pierceChance;
            if (!shieldHit || piercedShield) {
                float emp = projectile.getDamage().getFluxComponent() * 0.5f;
                float dam = projectile.getDamage().getDamage() * 0.25f;
                for (int i = 0; i < (int) (Math.random() * 4) + 1; i++) {
                    engine.spawnEmpArcPierceShields(
                            projectile.getSource(), point, target, target,
                            DamageType.ENERGY,
                            dam,
                            emp,
                            100000f,
                            "tachyon_lance_emp_impact",
                            10f,
                            EMP_COLOR,
                            EMP_COLOR);
                }
            }
        }
    }
}
