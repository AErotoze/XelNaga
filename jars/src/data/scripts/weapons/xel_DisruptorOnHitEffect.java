package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static data.utils.xel.Constants.i18n_weapon;

public class xel_DisruptorOnHitEffect implements OnHitEffectPlugin {
	/*
		命中目标护盾后，根据护盾效率，在2s内使目标产生的软幅能的1%~10%转化成硬幅能
		护盾效率越高则转化率越高，最高转化率在0.4盾效提供，最低转化率在1盾效提供
		盾效与转化率不成反比会导致这里的代码bug，不要乱动噢
	 */
	private static final String DATA_KEY = "xel_DisruptorOnHitEffect_data_key";
	private static final float DURATION = 2f;
	private static final float MAX_FLUX_TURNED_RATE = 0.1f;
	private static final float MIN_FLUX_TURNED_RATE = 0.01f;
	private static final float SHIELD_EFFICIENCY_IN_MIN_TURNED_RATE = 1f;
	private static final float SHIELD_EFFICIENCY_IN_MAX_TURNED_RATE = 0.4f;

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

			if (time >= 0f) {
				time -= amount;

				String softFluxKey = DATA_KEY + ship.getId() + "_soft_flux";
				FluxTrackerAPI fluxTracker = ship.getFluxTracker();
				float currSoftFlux = fluxTracker.getCurrFlux() - fluxTracker.getHardFlux();
				Float lastSoftFlux = (Float) ship.getCustomData().get(softFluxKey);
				if (lastSoftFlux == null) {
					lastSoftFlux = currSoftFlux;
				}

				//效果对护盾幅能比成反比的专用数学式，若不成反比则会产生错误
				float fraction = 1f - (Math.min(SHIELD_EFFICIENCY_IN_MIN_TURNED_RATE, Math.max(SHIELD_EFFICIENCY_IN_MAX_TURNED_RATE, ship.getShield().getFluxPerPointOfDamage())) - SHIELD_EFFICIENCY_IN_MAX_TURNED_RATE) / (SHIELD_EFFICIENCY_IN_MIN_TURNED_RATE - SHIELD_EFFICIENCY_IN_MAX_TURNED_RATE);
				float turnedEffect = (MAX_FLUX_TURNED_RATE - MIN_FLUX_TURNED_RATE) * fraction + MIN_FLUX_TURNED_RATE;

				if ((currSoftFlux - lastSoftFlux) > 0f) {
					float increasedSoftFlux = currSoftFlux - lastSoftFlux;
					fluxTracker.decreaseFlux(increasedSoftFlux * turnedEffect);
					fluxTracker.increaseFlux(increasedSoftFlux * turnedEffect, true);
				}

				ship.setCustomData(softFluxKey, currSoftFlux);

				ship.getShield().setInnerColor(new Color(255, 66, 66));
				ship.getShield().setInnerColor(new Color(255, 66, 66));

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
	}
}
