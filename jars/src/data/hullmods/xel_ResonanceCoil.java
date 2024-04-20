package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
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


/*
 * 谐振盘
 * 星灵能量矩阵
 * 根据舰船等级降低25%/20%/15%/10%的辐能容量
 * s插后数据变为15%/10%/5%/5%
 * 根据战斗时间恢复幅能容量，需要峰值时间的30%
 *
 * 舰船护盾受伤重新调整
 * 伤害类型/来源		最终伤害/面板伤害		s插
 * 动能x2	    	1.33(-33.5%)		1.2(-40%)
 * 能量x1			0.9(-10%)			0.8(-20%)
 * 高爆x0.5			0.77(+54%)			0.65(+30%)
 * 破片x0.25		0.4(+60%) 			0.3(+20%)
 * 战机x1			1.1(+10%)			1.05(+5%)
 *
 * 原版减伤期望       95%     (2+1+0.5+0.25+1)/5
 * 谐振减伤期望       90%     (1.33+0.9+0.77+0.4+1.1)/5 = 0.9
 * s-mod谐振减伤期望  80%     (1.2+0.8+0.65+0.3+1.05)/5 = 0.8
 *
 * 灵能水晶矩阵
 * 装甲承伤 -15%
 * s-mod提高最低装甲计算值
 *
 */
public class xel_ResonanceCoil extends xel_BaseHullmod {
	private static final Map<ShipAPI.HullSize, Float> fluxCapMap = new HashMap<>();
	private static final Map<DamageType, Float> damageTakenMap = new HashMap<>();
	private static final Map<ShipAPI.HullSize, Float> sModfluxCapMap = new HashMap<>();
	private static final Map<DamageType, Float> sModdamageTakenMap = new HashMap<>();
	private static final float FIGHTER_DAMAGE_TAKEN = 10f;
	private static final float SMOD_FIGHTER_DAMAGE_TAKEN = 5f;
	private static final float RECOVER_TIME_PERCENT = 30f;
	private static final String DATA_KEY = "xel_Resonance_Coil_key";

	static {
		fluxCapMap.put(ShipAPI.HullSize.FRIGATE, 25f);
		fluxCapMap.put(ShipAPI.HullSize.DESTROYER, 20f);
		fluxCapMap.put(ShipAPI.HullSize.CRUISER, 15f);
		fluxCapMap.put(ShipAPI.HullSize.CAPITAL_SHIP, 10f);
		sModfluxCapMap.put(ShipAPI.HullSize.FRIGATE, 15f);
		sModfluxCapMap.put(ShipAPI.HullSize.DESTROYER, 10f);
		sModfluxCapMap.put(ShipAPI.HullSize.CRUISER, 5f);
		sModfluxCapMap.put(ShipAPI.HullSize.CAPITAL_SHIP, 5f);

		damageTakenMap.put(DamageType.KINETIC, -33.5f);
		damageTakenMap.put(DamageType.ENERGY, -10f);
		damageTakenMap.put(DamageType.HIGH_EXPLOSIVE, 54f);
		damageTakenMap.put(DamageType.FRAGMENTATION, 60f);
		sModdamageTakenMap.put(DamageType.KINETIC, -40f);
		sModdamageTakenMap.put(DamageType.ENERGY, -20f);
		sModdamageTakenMap.put(DamageType.HIGH_EXPLOSIVE, 30f);
		sModdamageTakenMap.put(DamageType.FRAGMENTATION, 20f);
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		float fluxBonus;
		float damageBonus;
		boolean sMod = isSMod(stats);
		fluxBonus = 1f - 0.01f * (isSMod(stats) ? sModfluxCapMap.get(hullSize) : fluxCapMap.get(hullSize));
		stats.getFluxCapacity().modifyMult(id, fluxBonus);

		damageBonus = sMod ? sModdamageTakenMap.get(DamageType.KINETIC) : damageTakenMap.get(DamageType.KINETIC);
		stats.getKineticShieldDamageTakenMult().modifyMult(id, 1f + 0.01f * damageBonus);
		damageBonus = sMod ? sModdamageTakenMap.get(DamageType.ENERGY) : damageTakenMap.get(DamageType.ENERGY);
		stats.getEnergyShieldDamageTakenMult().modifyMult(id, 1f + 0.01f * damageBonus);
		damageBonus = sMod ? sModdamageTakenMap.get(DamageType.HIGH_EXPLOSIVE) : damageTakenMap.get(DamageType.HIGH_EXPLOSIVE);
		stats.getHighExplosiveShieldDamageTakenMult().modifyMult(id, 1f + 0.01f * damageBonus);
		damageBonus = sMod ? sModdamageTakenMap.get(DamageType.FRAGMENTATION) : damageTakenMap.get(DamageType.FRAGMENTATION);
		stats.getFragmentationShieldDamageTakenMult().modifyMult(id, 1f + 0.01f * damageBonus);

		stats.getArmorBonus().modifyPercent(id, 15f);//难以置信它不能写在 applyEffectsAfterShipCreation 里头

	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (ship.getShield() == null) return;
		float bonus = isSMod(ship) ? SMOD_FIGHTER_DAMAGE_TAKEN : FIGHTER_DAMAGE_TAKEN;
		ship.getShield().setInnerColor(new Color(104, 99, 255, 64));
		ship.getShield().setRingColor(new Color(255, 255, 255, 255));
		ship.addListener(new fighterDamageTakenModifer(bonus));
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship.isAlive()) {
			Float time = (Float) ship.getCustomData().get(DATA_KEY);
			float peakTime = ship.getMutableStats().getPeakCRDuration().computeEffective(ship.getHullSpec().getNoCRLossTime());
			float recoverTime = RECOVER_TIME_PERCENT * 0.01f * peakTime;
			if (time == null) {
				time = recoverTime;
			}
			if (time > 0f) {
				time = Math.max(0f, time - amount);
				float effectLevel = 1f - time / recoverTime;
				float reductionFlux = ship.getMaxFlux() * (1f - 0.01f * (isSMod(ship) ? sModfluxCapMap.get(ship.getHullSize()) : fluxCapMap.get(ship.getHullSize())));
				ship.getMutableStats().getFluxCapacity().modifyFlat(DATA_KEY, effectLevel * reductionFlux);

				ship.setCustomData(DATA_KEY, time);
			}
			if (Global.getCombatEngine().getPlayerShip() == ship) {
				Global.getCombatEngine().maintainStatusForPlayerShip(DATA_KEY,
						"graphics/icons/hullsys/high_energy_focus.png",
						xel_Misc.getHullmodName(HullModUtil.XEL_RESONANCE_COIL),
						time > 0f ? i18n_hullmod.format("xel_rc_flux_recovering", time.intValue()) : i18n_hullmod.get("xel_rc_flux_recovered"),
						time > 0f);
			}
		}
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return xel_Misc.getHullSizePercentString(fluxCapMap);
		else return index == 1 ? (int) RECOVER_TIME_PERCENT + "%" : super.getDescriptionParam(index, hullSize);
	}

	@Override
	public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return xel_Misc.getHullSizePercentString(sModfluxCapMap);
		else return super.getSModDescriptionParam(index, hullSize);
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 5f;
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color bad = Misc.getNegativeHighlightColor();
		Color good = Misc.getPositiveHighlightColor();

		float tableWidth = width - 5f;
		tooltip.addSectionHeading(i18n_hullmod.get("xel_rc_title"), Alignment.TMID, pad);
		tooltip.beginTable(
				Misc.getBasePlayerColor(),
				Misc.getDarkPlayerColor(),
				Misc.getBrightPlayerColor(),
				20f,
				i18n_hullmod.get("xel_rc_damageSource"), tableWidth * 0.35f,
				i18n_hullmod.get("xel_rc_damageFactor"), tableWidth * 0.25f,
				i18n_hullmod.get("xel_rc_sMod_damageFactor"), tableWidth * 0.4f
		);
		tooltip.addRow(new Color(255, 211, 154), DamageType.KINETIC.getDisplayName(),
				good, damageTakenMap.get(DamageType.KINETIC).intValue() + "%",
				good, sModdamageTakenMap.get(DamageType.KINETIC).intValue() + "%");
		tooltip.addRow(new Color(0, 187, 255), DamageType.ENERGY.getDisplayName(),
				good, damageTakenMap.get(DamageType.ENERGY).intValue() + "%",
				good, sModdamageTakenMap.get(DamageType.ENERGY).intValue() + "%");
		tooltip.addRow(h, DamageType.HIGH_EXPLOSIVE.getDisplayName(),
				bad, "+" + damageTakenMap.get(DamageType.HIGH_EXPLOSIVE).intValue() + "%",
				bad, "+" + sModdamageTakenMap.get(DamageType.HIGH_EXPLOSIVE).intValue() + "%");
		tooltip.addRow(new Color(255, 66, 66), DamageType.FRAGMENTATION.getDisplayName(),
				bad, "+" + damageTakenMap.get(DamageType.FRAGMENTATION).intValue() + "%",
				bad, "+" + sModdamageTakenMap.get(DamageType.FRAGMENTATION).intValue() + "%");
		tooltip.addRow(new Color(148, 253, 136), i18n_hullmod.get("xel_rc_damageType_fighter"),
				bad, "+" + (int) FIGHTER_DAMAGE_TAKEN + "%",
				bad, "+" + (int) SMOD_FIGHTER_DAMAGE_TAKEN + "%");

		tooltip.addTable("N/A", 0, pad);
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (!hasEnergyArrayMod(ship)) return getNoEnergyArrayReason();
		else return hasTooMuchHarmonyMod(ship) ? getTooMuchHarmonyModReason() : super.getUnapplicableReason(ship);
	}


	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return hasEnergyArrayMod(ship) && !hasTooMuchHarmonyMod(ship);
	}

	private static class fighterDamageTakenModifer implements DamageTakenModifier {
		private static final String ID = "xel_RC_fighterDamageTaken";
		private static float damageTakenBonus;

		public fighterDamageTakenModifer(float bonus) {
			damageTakenBonus = bonus;
		}

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			if (shieldHit) {
				if (param instanceof ShipAPI) {
					if (((ShipAPI) param).isFighter()) {
						damage.getModifier().modifyMult(ID, 1f + damageTakenBonus * 0.01f);
						return ID;
					}
				} else if (param instanceof DamagingProjectileAPI) {
					ShipAPI source = ((DamagingProjectileAPI) param).getSource();
					if (source != null && source.isFighter()) {
						damage.getModifier().modifyMult(ID, 1f + damageTakenBonus * 0.01f);
						return ID;
					}
				} else if (param instanceof BeamAPI) {
					ShipAPI source = ((BeamAPI) param).getSource();
					if (source != null && source.isFighter()) {
						damage.getModifier().modifyMult(ID, 1f + damageTakenBonus * 0.01f);
						return ID;
					}
				}
			}
			return null;
		}
	}
}
