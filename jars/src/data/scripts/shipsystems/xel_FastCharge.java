package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.util.IntervalUtil;
import data.utils.xel.ShipSystemUtil;
import data.utils.xel.xel_Misc;

import java.awt.*;

import static data.utils.xel.Constants.i18n_hullmod;
import static data.utils.xel.Constants.i18n_shipSystem;

public class xel_FastCharge extends xel_BaseShipSystemScript {
	/*
	$快速充能
	武器射速+
	武器幅能-
	$PP 奇点超载
	武器射程+(不对PD武器生效)
	武器射速++
	武器幅能--

	持续10s，冷却10s
	 */

	private static final float WEAPON_ROF_BONUS = 50f;
	private static final float PP_WEAPON_ROF_BONUS = 100f;
	private static final float FLUX_REDUCTION = 1f / 3f;
	private static final float PP_FLUX_REDUCTION = 2f / 3f;
	private static final float PP_WEAPON_RANGE_BONUS = 20f;
	private static final float PP_MIN_WEAPON_RANGE_BONUS = 100f;
	private static final IntervalUtil interval = new IntervalUtil(0.25f, 0.25f);
	private static final Object STATUS1 = new Object();
	private static final Object STATUS2 = new Object();
	private static final Object STATUS3 = new Object();

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = getPlayerShip(stats);
		if (ship == null) return;
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused()) return;

		String spriteName = Global.getSettings().getShipSystemSpec(ShipSystemUtil.XEL_FAST_CHARGE).getIconSpriteName();
		String title = hasPP(ship) ? i18n_hullmod.get("xel_pp_FC_change_name") : xel_Misc.getShipSystemSpecName(ShipSystemUtil.XEL_FAST_CHARGE);

		float rofBonus = 1f + 0.01f * (hasPP(ship) ? PP_WEAPON_ROF_BONUS : WEAPON_ROF_BONUS);
		float fluxReduction = 1f - (hasPP(ship) ? PP_FLUX_REDUCTION : FLUX_REDUCTION);
		stats.getBallisticRoFMult().modifyMult(id, rofBonus * effectLevel);
		stats.getEnergyRoFMult().modifyMult(id, rofBonus * effectLevel);
		stats.getBallisticWeaponFluxCostMod().modifyMult(id, fluxReduction * effectLevel);
		stats.getEnergyWeaponFluxCostMod().modifyMult(id, fluxReduction * effectLevel);
		if (hasPP(ship)) {
			if (!ship.hasListenerOfClass(WeaponRangeManager.class)) ship.addListener(new WeaponRangeManager());
			interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
			if (interval.intervalElapsed()) {
				ship.addAfterimage(
						new Color(0x40FE8A0E, true),
						0f,
						0f,
						-1f * ship.getVelocity().getX(),
						-1f * ship.getVelocity().getY(),
						0f,
						0f,
						0.25f,
						0.25f,
						false,
						false,
						true
				);
			}
		}
		if (isPlayerShip(ship)) {
			engine.maintainStatusForPlayerShip(STATUS1, spriteName, title, i18n_shipSystem.format("xel_FC_active1", (int) ((rofBonus - 1f) * 100f) + "%"), false);
			engine.maintainStatusForPlayerShip(STATUS2, spriteName, title, i18n_shipSystem.format("xel_FC_active2", String.format("%.1f", (1f - fluxReduction) * 100f)) + "%", false);
			if (hasPP(ship)) {
				engine.maintainStatusForPlayerShip(STATUS3, spriteName, title, i18n_shipSystem.get("xel_FC_active3"), false);
			}
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = getPlayerShip(stats);
		if (ship == null) return;

		stats.getEnergyRoFMult().unmodify(id);
		stats.getBallisticRoFMult().unmodify(id);
		stats.getEnergyWeaponFluxCostMod().unmodify(id);
		stats.getBallisticWeaponFluxCostMod().unmodify(id);
		if (ship.hasListenerOfClass(WeaponRangeManager.class)) ship.removeListenerOfClass(WeaponRangeManager.class);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return super.getStatusData(index, state, effectLevel);
	}

	private static class WeaponRangeManager implements WeaponRangeModifier {

		@Override
		public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
			if (weapon.hasAIHint(WeaponAPI.AIHints.PD) || weapon.hasAIHint(WeaponAPI.AIHints.PD_ONLY)) return 0f;
			if (weapon.getSpec().getMaxRange() > 500f) return PP_WEAPON_RANGE_BONUS;
			else return 0f;
		}

		@Override
		public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
			return 1f;
		}

		@Override
		public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
			if (weapon.hasAIHint(WeaponAPI.AIHints.PD) || weapon.hasAIHint(WeaponAPI.AIHints.PD_ONLY)) return 0f;
			if (weapon.getSpec().getMaxRange() <= 500f) return PP_MIN_WEAPON_RANGE_BONUS;
			else return 0f;
		}
	}
}
