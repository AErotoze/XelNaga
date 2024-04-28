package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.xel_Misc;

import java.awt.*;

public class xel_VoidStasis extends xel_BaseShipSystemScript {
	/*
	虚空静滞
	对敌对目标释放，使其在 8秒 内被转进虚空
	期间 不受任何伤害，无法开火，无法移动
	凋零虹吸 Withering Siphon
	持续时间减少至 2秒(-75%)
	但是目标将每秒增长 10% 最大幅能值的硬幅能

	处于静滞的敌人无法再被系统选中
	 */
	private static final float MAX_ACTIVE_TIME = 8f;
	private static final float PP_MAX_ACTIVE_TIME = 2f;
	private static final Object SOURCE_KEY = new Object();
	private static final Object TARGET_KEY = new Object();
	private static final float RANGE = 1500f;
	private static final Color STASIS_COLOR = new Color(171, 241, 255, 191);

	private static class TargetData {
		public ShipAPI source;
		public ShipAPI target;
		public AdvanceableListener targetEffectListener;
		public float activeTime;

		public TargetData(ShipAPI source, ShipAPI target, float maxTime) {
			this.source = source;
			this.target = target;
			activeTime = maxTime;
		}
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, final float effectLevel) {
		final ShipAPI ship = getPlayerShip(stats);
		if (ship == null) return;
		final CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused()) return;
		final String targetDataKey = id + ship.getId() + "_stasis_target_data";

		TargetData targetDataObj = (TargetData) engine.getCustomData().get(targetDataKey);
		if (state == State.IN && targetDataObj == null) {
			ShipAPI target = xel_Misc.findTarget(engine, ship, RANGE);
			engine.getCustomData().put(targetDataKey, new TargetData(ship, target, MAX_ACTIVE_TIME));
			if (target != null) {
				if (target.getFluxTracker().showFloaty() ||
						ship == Global.getCombatEngine().getPlayerShip() ||
						target == Global.getCombatEngine().getPlayerShip()) {
					target.getFluxTracker().showOverloadFloatyIfNeeded("发生什么事了？", Misc.getNegativeHighlightColor(), 4f, true);
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
						if (isPlayerShip(targetData.target)) {
							engine.maintainStatusForPlayerShip(
									TARGET_KEY,
									targetData.source.getSystem().getSpecAPI().getIconSpriteName(),
									targetData.source.getSystem().getDisplayName(),
									"没发什么什么事捏^_^",
									true);
						}
						if (targetData.activeTime > 0f) {
							targetData.activeTime -= amount;
//						if (hasPP(targetData.source)){}
							targetData.target.setPhased(true);
//							targetData.target.blockCommandForOneFrame(ShipCommand.FIRE);
							for (WeaponAPI weapon : targetData.target.getAllWeapons()) {
								if (weapon.isDisabled()) continue;
								weapon.setForceNoFireOneFrame(true);
							}
							targetData.target.setJitter(TARGET_KEY, Misc.getHighlightColor(), 1f, 3, 50f);
						} else {
							targetData.target.setPhased(false);
							targetData.target.removeListener(targetData.targetEffectListener);
						}
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

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != ShipSystemAPI.SystemState.IDLE) return null;
		ShipAPI target = xel_Misc.findTarget(Global.getCombatEngine(), ship, RANGE);
		if (target != null && target != ship) {
			return "READY";
		}
		if (target == null && ship.getShipTarget() != null) {
			return "OUT OF RANGE";
		}
		return "NO TARGET";

	}
}
