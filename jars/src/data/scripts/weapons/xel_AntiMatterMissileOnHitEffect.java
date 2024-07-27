package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class xel_AntiMatterMissileOnHitEffect implements OnHitEffectPlugin {
	private static final String DATA_KEY = "xel_AntiMatterMissile_effect_key";
	private static final float MASS_THRESHOLD = 500f;
	private static final float MASS_DEGREE_INTERVAL = 100f;
	private static final float DAMAGE_INCREASE_BASE = 25f;
	private static final float DAMAGE_INCREASE_INTERVAL = 5f;

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		//目标是舰船（包括战机）且质量大于等于500时生效
		if (target instanceof ShipAPI && target.getMass() >= MASS_THRESHOLD) {
			ShipAPI enemy = (ShipAPI) target;
			if (enemy.isHulk())return;

			float fraction = (float) Math.floor((target.getMass() - MASS_THRESHOLD) / MASS_DEGREE_INTERVAL);// 向下取整，质量超过500的100间隔有多少
			float damageMult = DAMAGE_INCREASE_BASE + fraction * DAMAGE_INCREASE_INTERVAL;// 每个100间隔提高5%伤害

			projectile.getDamage().getModifier().modifyPercent(DATA_KEY + enemy.getId(), damageMult);
		}
	}
}
