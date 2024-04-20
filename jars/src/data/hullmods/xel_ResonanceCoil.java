package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.HullModUtil;
import data.utils.xel.xel_Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_ResonanceCoil extends xel_BaseHullmod {
	//    根据舰船等级，降低25/20/15/10幅能容量
	//    在峰值时间的前30%内恢复
	//    改善护盾舰护盾抗性，相位舰装甲抗性
	//    内置后 结构抗性 +5%，再改善护盾舰护盾抗性，相位舰装甲抗性
	//    不兼容于重型装甲，护盾分流，硬化护盾

	/*
	 * 舰舰护盾
	 * 伤害类型		调整水平		s-mod调整水平
	 * 动能			-25%		-50%
	 * 能量/光束		-10%		-20%
	 * 高爆			+50%		+25%
	 * 破片			+20%		+10%
	 *
	 * 相位舰装甲/舰体
	 * 伤害类型		调整水平		s-mod调整水平
	 * 动能			+50%		+25%
	 * 能量/光束		-10%		-20%
	 * 高爆			-25%		-50%
	 * 破片			+20%		+10%
	 */
	private static final float KH_DAMAGE_TAKEN_ADJUST = 50f;
	private static final float EF_DAMAGE_TAKEN_ADJUST = 10f;
	private static final float SMOD_DAMAGE_TAKEN_ADJUST = 2f;
	private static final Map<ShipAPI.HullSize, Float> FLUX_DECREASE = new HashMap<>();
	private static final float RECOVER_DURATION_PERCENT = 30f;
	private static final float HULL_DAMAGE_TAKEN_BONUS = 5f;
	private static final String RC_TIME_KEY = "rc_time_key";
	private static final Map<String, damageData> data = new HashMap<>();

	static {
		FLUX_DECREASE.put(ShipAPI.HullSize.FRIGATE, 25f);
		FLUX_DECREASE.put(ShipAPI.HullSize.DESTROYER, 20f);
		FLUX_DECREASE.put(ShipAPI.HullSize.CRUISER, 15f);
		FLUX_DECREASE.put(ShipAPI.HullSize.CAPITAL_SHIP, 10f);

		data.put("kh_increase", new damageData(50f, 25f));
		data.put("kh_decrease", new damageData(-25f, -50f));
		data.put("ef_increase", new damageData(20f, 10f));
		data.put("ef_decrease", new damageData(-10f, -20f));
	}

	private static class damageData {
		private final float bonus;
		private final float sModBonus;

		public damageData(float bonus, float sModBonus) {
			this.bonus = bonus;
			this.sModBonus = sModBonus;
		}
	}

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);
		this.setNotCompatible(HullMods.HEAVYARMOR, HullMods.SHIELD_SHUNT, HullMods.HARDENED_SHIELDS);
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		if (isSMod(stats)) stats.getHullDamageTakenMult().modifyMult(id, 1f - 0.01f * HULL_DAMAGE_TAKEN_BONUS);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship.isAlive()) {
			float maxRecoveryTime = ship.getMutableStats().getPeakCRDuration().computeEffective(ship.getHullSpec().getNoCRLossTime()) * RECOVER_DURATION_PERCENT * 0.01f;
			Float time = (Float) ship.getCustomData().get(RC_TIME_KEY);
			if (time == null) {
				time = maxRecoveryTime;
			}

			if (time >= 0f) {
				float level = 1f - time / maxRecoveryTime;
				float bonus = FLUX_DECREASE.get(ship.getHullSize()) * level;
				ship.getMutableStats().getFluxCapacity().modifyMult(ship.getId(), 1f - 0.01f * bonus);
				time -= amount;
				ship.setCustomData(RC_TIME_KEY, time);
			}
			if (Global.getCombatEngine().getPlayerShip() == ship) {
				Global.getCombatEngine().maintainStatusForPlayerShip(
						RC_TIME_KEY,
						"graphics/icons/hullsys/high_energy_focus.png",
						xel_Misc.getHullmodName(HullModUtil.XEL_RESONANCE_COIL),
						time > 0f ? i18n_hullmod.format("xel_rc_flux_recovering", time.intValue()) : i18n_hullmod.get("xel_rc_flux_recovered"),
						time > 0f);
			}
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		removeBlockedMod(ship);
		ship.addListener(new damageTakenManager(ship, isSMod(ship)));
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return xel_Misc.getHullSizePercentString(FLUX_DECREASE);
		else return index == 1 ? (int) RECOVER_DURATION_PERCENT + "%" : super.getDescriptionParam(index, hullSize);
	}

	@Override
	public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		return index == 0 ? (int) HULL_DAMAGE_TAKEN_BONUS + "%" : super.getSModDescriptionParam(index, hullSize);
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 5f;
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color bad = Misc.getNegativeHighlightColor();
		Color good = Misc.getPositiveHighlightColor();

		float tableWidth = width - 5f;
		boolean isPhase = ship.getHullSpec().isPhase();
		tooltip.addSectionHeading(i18n_hullmod.get(ship.getHullSpec().isPhase() ? "xel_rc_phase_title" : "xel_rc_shield_title"), Alignment.TMID, pad);
		tooltip.beginTable(
				Misc.getBasePlayerColor(),
				Misc.getDarkPlayerColor(),
				Misc.getBrightPlayerColor(),
				20f,
				i18n_hullmod.get("xel_rc_damageSource"), tableWidth * 0.35f,
				i18n_hullmod.get("xel_rc_damageFactor"), tableWidth * 0.25f,
				i18n_hullmod.get("xel_rc_sMod_damageFactor"), tableWidth * 0.4f);
		tooltip.addRow(new Color(255, 211, 154), DamageType.KINETIC.getDisplayName(),
				isPhase ? bad : good, (isPhase ? "+" : "") + (int) data.get(isPhase ? "kh_increase" : "kh_decrease").bonus + "%",
				isPhase ? bad : good, (isPhase ? "+" : "") + (int) data.get(isPhase ? "kh_increase" : "kh_decrease").sModBonus + "%");
		tooltip.addRow(new Color(0, 187, 255), DamageType.ENERGY.getDisplayName(),
				good, (int) data.get("ef_decrease").bonus + "%",
				good, (int) data.get("ef_decrease").sModBonus + "%");
		tooltip.addRow(h, DamageType.HIGH_EXPLOSIVE.getDisplayName(),
				!isPhase ? bad : good, (!isPhase ? "+" : "") + (int) data.get(!isPhase ? "kh_increase" : "kh_decrease").bonus + "%",
				!isPhase ? bad : good, (!isPhase ? "+" : "") + (int) data.get(!isPhase ? "kh_increase" : "kh_decrease").sModBonus + "%");
		tooltip.addRow(new Color(255, 66, 66), DamageType.FRAGMENTATION.getDisplayName(),
				bad, "+" + (int) data.get("ef_increase").bonus + "%",
				bad, "+" + (int) data.get("ef_increase").sModBonus + "%");

		tooltip.addTable("N/A", 0, pad);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return !hasNotCompatibleMod(ship) && !hasTooMuchHarmonyMod(ship) && hasArrayMod(ship);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (hasNotCompatibleMod(ship)) return getNotCompatibleReason();
		else if (hasTooMuchHarmonyMod(ship)) return getTooMuchHarmonyModReason();
		else return !hasArrayMod(ship) ? getNoArrayReason() : super.getUnapplicableReason(ship);
	}

	private static class damageTakenManager implements DamageTakenModifier {
		private static ShipAPI SHIP;
		private static Boolean SMOD;

		public damageTakenManager(ShipAPI ship, boolean sMod) {
			SHIP = ship;
			SMOD = sMod;
		}

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			boolean isPhaseShip = SHIP.getHullSpec().isPhase();
			DamageType damageType = damage.getType();
			String ID = "xel_rc_damage_taken_manager";
			if (isPhaseShip && !shieldHit) {
				if (damageAdjust(damage, damageType, ID, 1f + 0.01f * KH_DAMAGE_TAKEN_ADJUST / (SMOD ? SMOD_DAMAGE_TAKEN_ADJUST : 1f), 1f - 0.01f * KH_DAMAGE_TAKEN_ADJUST / (!SMOD ? SMOD_DAMAGE_TAKEN_ADJUST : 1f)))
					return ID;
			} else if (!isPhaseShip && shieldHit) {
				if (damageAdjust(damage, damageType, ID, 1f - 0.01f * KH_DAMAGE_TAKEN_ADJUST / (!SMOD ? SMOD_DAMAGE_TAKEN_ADJUST : 1f), 1f + 0.01f * KH_DAMAGE_TAKEN_ADJUST / (SMOD ? SMOD_DAMAGE_TAKEN_ADJUST : 1f)))
					return ID;
			}
			return null;
		}

		private boolean damageAdjust(DamageAPI damage, DamageType damageType, String ID, float v, float v2) {
			if (damageType == DamageType.KINETIC) {
				damage.getModifier().modifyMult(ID, v);
				return true;
			} else if (damageType == DamageType.HIGH_EXPLOSIVE) {
				damage.getModifier().modifyMult(ID, v2);
				return true;
			} else if (damageType == DamageType.ENERGY) {
				damage.getModifier().modifyMult(ID, 1f - 0.01f * EF_DAMAGE_TAKEN_ADJUST * (SMOD ? SMOD_DAMAGE_TAKEN_ADJUST : 1f));
				return true;
			} else if (damageType == DamageType.FRAGMENTATION) {
				damage.getModifier().modifyMult(ID, 1f + 0.01f * EF_DAMAGE_TAKEN_ADJUST * (!SMOD ? SMOD_DAMAGE_TAKEN_ADJUST : 1f));
				return true;
			}
			return false;
		}
	}
}