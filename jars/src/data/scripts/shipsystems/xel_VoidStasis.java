package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.xel_Misc;

import java.awt.*;

import static data.utils.xel.Constants.i18n_shipSystem;

public class xel_VoidStasis extends xel_BaseShipSystemScript {
	/*
	虚空静滞
	对敌对目标释放，使其在 8秒 内被转进虚空
	期间 不受任何伤害，无法开火，无法移动
	凋零虹吸 Withering Siphon
	持续时间减少至 2秒(-75%)
	但是目标将每秒增长 10% 最大幅能值的硬幅能

	 */
	private static final float MAX_ACTIVE_TIME = 8f;
	private static final float PP_MAX_ACTIVE_TIME = 2f;
	private static final Object SOURCE_KEY = new Object();
	private static final Object TARGET_KEY = new Object();
	private static final float RANGE = 1500f;
	private static final Color STASIS_COLOR = new Color(171, 241, 255, 191);
	private static final Color PP_STASIS_COLOR = new Color(241, 72, 253, 191);


	private static class TargetData {
		public ShipAPI source;
		public ShipAPI target;
		public AdvanceableListener targetEffectListener;
		public float activeTime;
		public float maxTime;
		public boolean done = false;

		public TargetData(ShipAPI source, ShipAPI target, float maxTime) {
			this.source = source;
			this.target = target;
			activeTime = maxTime;
			this.maxTime = maxTime;
		}
	}

	@Override
	public void apply(MutableShipStatsAPI stats, final String id, State state, final float effectLevel) {
		final ShipAPI ship = getPlayerShip(stats);
		if (ship == null) return;
		final CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused()) return;
		final String targetDataKey = id + ship.getId() + "_stasis_target_data";

		TargetData targetDataObj = (TargetData) engine.getCustomData().get(targetDataKey);
		if (state == State.IN && targetDataObj == null) {
			ShipAPI target = xel_Misc.findTarget(engine, ship, getMaxRange(ship));
			engine.getCustomData().put(targetDataKey, new TargetData(ship, target, hasPP(ship) ? PP_MAX_ACTIVE_TIME : MAX_ACTIVE_TIME));
			if (target != null) {
				if (target.getFluxTracker().showFloaty() ||
						ship == Global.getCombatEngine().getPlayerShip() ||
						target == Global.getCombatEngine().getPlayerShip()) {
					String text = i18n_shipSystem.get(hasPP(ship) ? "xel_VS_active_floaty_PP" : "xel_VS_active_floaty");
					target.getFluxTracker().showOverloadFloatyIfNeeded(text, Misc.getNegativeHighlightColor(), 4f, true);
				}
			}
		} else if (state == State.IDLE && targetDataObj != null) {
			engine.getCustomData().remove(targetDataKey);
			targetDataObj = null;
		}

		if (targetDataObj == null || targetDataObj.target == null) return;
//		if (targetDataObj.target.hasListenerOfClass(targetDataObj.targetEffectListener.getClass())) return;
		if (effectLevel >= 1f) {
			final TargetData targetData = targetDataObj;
			if (targetData.targetEffectListener == null) {
				targetData.targetEffectListener = new AdvanceableListener() {
					@Override
					public void advance(float amount) {
						ShipAPI source = targetData.source;
						ShipAPI target = targetData.target;
						float activeTime = targetData.activeTime;
						float maxTime = targetData.maxTime;
						if (isPlayerShip(target)) {
							engine.maintainStatusForPlayerShip(
									SOURCE_KEY,
									source.getSystem().getSpecAPI().getIconSpriteName(),
									source.getSystem().getDisplayName(),
									i18n_shipSystem.get("xel_VS_status"),
									true);
						}
						if (activeTime > 0f) {
							float level = (activeTime > maxTime * 0.75f) ? 1f - (maxTime - activeTime) / (maxTime * 0.25f) : (maxTime * 0.75f - activeTime) / (maxTime * 0.75f);
							level = Math.max(0.2f, level);
							activeTime -= amount;
							target.setPhased(true);
							target.setAlphaMult(level);

							if (!targetData.done) {
								targetData.done = true;
								if (target.getShield() != null && target.getShield().isOn())
									target.getShield().toggleOff();
								if (target.getSystem() != null && target.getSystem().isActive())
									target.getSystem().forceState(ShipSystemAPI.SystemState.OUT, 0f);
								target.getMutableStats().getMaxTurnRate().modifyMult(id, 0f);
								target.getMutableStats().getFluxDissipation().modifyMult(id,0f);
							}
							for (WeaponAPI weapon : target.getAllWeapons()) {
								if (weapon.isDisabled()) continue;
								weapon.setForceNoFireOneFrame(true);
							}
							target.getVelocity().scale(0f);
							target.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
							target.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
							target.blockCommandForOneFrame(ShipCommand.VENT_FLUX);

							target.setJitterShields(false);
							if (hasPP(source))
								target.setJitter(TARGET_KEY, PP_STASIS_COLOR, level, 3, ship.getCollisionRadius() / 2f * level);
							else
								target.setJitter(TARGET_KEY, STASIS_COLOR, level, 3, ship.getCollisionRadius() / 2f * level);

							if (hasPP(source)) {
								target.getFluxTracker().increaseFlux(target.getMaxFlux() * 0.1f * amount, true);
							}
						} else {
							target.getMutableStats().getMaxTurnRate().unmodify(id);
							target.getMutableStats().getFluxDissipation().unmodify(id);
							target.setPhased(false);
							target.removeListener(targetData.targetEffectListener);
						}
						targetData.activeTime = activeTime;
					}
				};
				targetData.target.addListener(targetData.targetEffectListener);
			}
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		super.unapply(stats, id);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return super.getStatusData(index, state, effectLevel);
	}

	public static float getMaxRange(ShipAPI ship) {
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != ShipSystemAPI.SystemState.IDLE) return null;
		ShipAPI target = xel_Misc.findTarget(Global.getCombatEngine(), ship, getMaxRange(ship));
		if (target != null) {
			if (target.isPhased()) {
				return "UNAVAILABLE TARGET";
			}
			return "READY";
		}
		if (ship.getShipTarget() != null) {
			return "OUT OF RANGE";
		}
		return "NO TARGET";
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		ShipAPI target = xel_Misc.findTarget(Global.getCombatEngine(), ship, RANGE);
		return target != null && !target.isPhased();
	}
}
