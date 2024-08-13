package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static data.utils.xel.Constants.i18n_weapon;

public class xel_DisperserOnHitEffect implements OnHitEffectPlugin {
	/*
		命中目标后，目标的装甲计算值降低30%并在2s内逐渐恢复
		优化？：学习烧蚀装甲的先进（？）经验，被打到装甲在3s内变成烧蚀装甲
	 */

	private static final String DATA_KEY = "xel_DisperserOnHitEffect_data_key";
	private static final float DURATION = 2f;
	private static final float EFFECTIVE_ARMOR_DECREASE = 30f;
	private static final Color EFFECT_COLOR = new Color(150, 20, 20, 64);

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (target instanceof ShipAPI && !shieldHit) {
			ShipAPI enemy = (ShipAPI) target;
			if (enemy.isFighter() || enemy.isHulk() || !enemy.isAlive()) return;

			String effectiveArmorKey = DATA_KEY + enemy.getId() + "_effective_armor";
			effectiveArmorDeBuff manager = (effectiveArmorDeBuff) enemy.getCustomData().get(effectiveArmorKey);
			if (manager == null) {
				manager = new effectiveArmorDeBuff(enemy, projectile.getWeapon());
			}
			manager.time = DURATION;

			if (!enemy.hasListenerOfClass(effectiveArmorDeBuff.class)) {
				enemy.addListener(manager);
			}
			enemy.setCustomData(effectiveArmorKey, manager);
		}
	}

	private static class effectiveArmorDeBuff implements AdvanceableListener {
		private float time = 0f;
		private final ShipAPI ship;
		private final WeaponAPI weapon;
		private final IntervalUtil interval = new IntervalUtil(0.05f, 0.05f);

		public effectiveArmorDeBuff(ShipAPI ship, WeaponAPI weapon) {
			this.ship = ship;
			this.weapon = weapon;
		}

		@Override
		public void advance(float amount) {
			if (!ship.isAlive()) return;
			if (time >= 0f) {
				time -= amount;
				interval.advance(amount);

				float multValue = 1f - (EFFECTIVE_ARMOR_DECREASE * 0.01f) * (time / DURATION); // 逐渐恢复装甲计算值
				ship.getMutableStats().getEffectiveArmorBonus().modifyMult(DATA_KEY + ship.getId(), multValue);

				// 意义不明的特效
				if (interval.intervalElapsed()) {
					ship.setJitter(ship, EFFECT_COLOR, time / DURATION, 2, ship.getCollisionRadius() * 0.05f);
				}

				if (ship == Global.getCombatEngine().getPlayerShip()) {
					Global.getCombatEngine().maintainStatusForPlayerShip(
							DATA_KEY + "_1",
							"graphics/icons/hullsys/quantum_disruptor.png",
							weapon.getDisplayName(),
							i18n_weapon.format("xel_disperser", String.format("%.1f", time)),
							true);
				}
			} else {
				ship.getMutableStats().getEffectiveArmorBonus().unmodify(DATA_KEY + ship.getId());
				ship.removeListener(this);
			}
		}
	}
}
