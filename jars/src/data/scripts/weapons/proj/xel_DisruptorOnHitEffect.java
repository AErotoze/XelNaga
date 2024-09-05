package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import static data.utils.xel.Constants.i18n_weapon;

public class xel_DisruptorOnHitEffect implements OnHitEffectPlugin {
	/*
		命中目标护盾后，根据护盾效率，在2s内使目标以任何形式上涨的软幅能的5%~15%转化成硬幅能
		护盾效率越高则转化率越高，呈线性关系，最高转化率在0.5盾效提供，最低转化率在1盾效提供
		盾效与转化率不成反比会导致这里的代码bug，不要乱动噢
	 */
	private static final String DATA_KEY = "xel_DisruptorOnHitEffect_data_key";
	private static final float DURATION = 2f;
	private static final float MAX_FLUX_TURNED_RATE = 0.15f;
	private static final float MIN_FLUX_TURNED_RATE = 0.05f;
	private static final float SHIELD_EFFICIENCY_IN_MIN_TURNED_RATE = 1f;
	private static final float SHIELD_EFFICIENCY_IN_MAX_TURNED_RATE = 0.5f;
//	private static final Color TARGET_COLOR = new Color(140, 38, 145);

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (target instanceof ShipAPI && shieldHit) {
			ShipAPI enemy = (ShipAPI) target;
			if (enemy.isFighter()) return;
			if (enemy.isHulk()) return;

			String fluxTurnedKey = DATA_KEY + enemy.getId() + "_flux_turned";
			fluxTurnedBuff manager = (fluxTurnedBuff) enemy.getCustomData().get(fluxTurnedKey);
			if (manager == null) {
				manager = new fluxTurnedBuff(enemy, projectile.getWeapon());
			}
			manager.time = DURATION;

			if (!enemy.hasListenerOfClass(fluxTurnedBuff.class)) {
				enemy.addListener(manager);
			}
			enemy.setCustomData(fluxTurnedKey, manager);
		}
	}

	private static class fluxTurnedBuff implements AdvanceableListener {
		private final ShipAPI ship;
		private final WeaponAPI weapon;
		private float time = 0f;

		public fluxTurnedBuff(ShipAPI ship, WeaponAPI weapon) {
			this.ship = ship;
			this.weapon = weapon;
		}

		@Override
		public void advance(float amount) {
			if (!ship.isAlive()) return;

			if (time > 0f) {
				time -= amount;

				String softFluxKey = DATA_KEY + ship.getId() + "_soft_flux";
				FluxTrackerAPI fluxTracker = ship.getFluxTracker();
				float currSoftFlux = fluxTracker.getCurrFlux() - fluxTracker.getHardFlux();
				Float lastSoftFlux = (Float) ship.getCustomData().get(softFluxKey);
				if (lastSoftFlux == null) {
					lastSoftFlux = currSoftFlux;
				}

				//效果对护盾幅能比成反比的专用数学式，若不成反比则会产生错误
				float shieldEfficiency = Math.min(SHIELD_EFFICIENCY_IN_MIN_TURNED_RATE, Math.max(SHIELD_EFFICIENCY_IN_MAX_TURNED_RATE, ship.getShield().getFluxPerPointOfDamage()));
				float fraction = (shieldEfficiency - SHIELD_EFFICIENCY_IN_MAX_TURNED_RATE) / (SHIELD_EFFICIENCY_IN_MIN_TURNED_RATE - SHIELD_EFFICIENCY_IN_MAX_TURNED_RATE);
				float turnedRate = fraction * (MAX_FLUX_TURNED_RATE - MIN_FLUX_TURNED_RATE) + MIN_FLUX_TURNED_RATE;

				if (currSoftFlux - lastSoftFlux > 0f) {
					float increasedSoftFlux = currSoftFlux - lastSoftFlux;
					fluxTracker.decreaseFlux(increasedSoftFlux * turnedRate);
					fluxTracker.increaseFlux(increasedSoftFlux * turnedRate, true);
				}
				ship.setCustomData(softFluxKey, currSoftFlux);

				if (ship == Global.getCombatEngine().getPlayerShip()) {
					Global.getCombatEngine().maintainStatusForPlayerShip(
							DATA_KEY + "_1",
							"graphics/icons/hullsys/entropy_amplifier.png",
							weapon.getDisplayName(),
							i18n_weapon.format("xel_disruptor", String.format("%.1f", time)),
							true);
				}
			} else {
				ship.removeListener(this);
			}
		}

//		private void easyRender(float amount) {
//			ShieldAPI shield = ship.getShield();
//			Color innerColor = ship.getHullSpec().getShieldSpec().getInnerColor();
//			int r = innerColor.getRed();
//			int g = innerColor.getGreen();
//			int b = innerColor.getBlue();
//			float fraction;
//			if (time >= (DURATION / 2f)) {
//				fraction = 1f - ((time - DURATION / 2f) / DURATION * 2f);
//			} else {
//				fraction = time / DURATION * 2f;
//			}
//			shield.setInnerColor(new Color(
//					MathUtils.clamp(Math.round((TARGET_COLOR.getRed() - r) * fraction) + r, 0, 255),
//					MathUtils.clamp(Math.round((TARGET_COLOR.getGreen() - g) * fraction) + g, 0, 255),
//					MathUtils.clamp(Math.round((TARGET_COLOR.getBlue() - b) * fraction) + b, 0, 255),
//					innerColor.getAlpha()
//			));
//		}
	}
}
