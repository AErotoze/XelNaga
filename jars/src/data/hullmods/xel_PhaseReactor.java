package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.utils.xel.HullModUtil;
import data.utils.xel.xel_Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_PhaseReactor extends xel_BaseHullmod {
	private static final String DATA_KEY = "xel_PhaseReactor_data_key";
	private static final float MAX_ARMOR_EFFECT_REDUCTION = 0.1f;
	private static final float HULL_REPAIRED_PERCENT = 0.01f;
	private static final float HULL_REPAIRED_VALUE = 50f;
	private static final float EFFECTIVE_ARMOR_BONUS = 0.3f;
	private static final IntervalUtil PARTICLE_SPAWN_INTERVAL = new IntervalUtil(0.05f, 0.05f);
	private static final Object STATUS1 = new Object();

	private static final Map<Integer, effectLevel> effectData = new HashMap<>();

	static {
		effectData.put(0, new effectLevel(0f, 1f, 1f));
		effectData.put(1, new effectLevel(0.25f, 0.8f, 0.66f));
		effectData.put(2, new effectLevel(0.5f, 0.6f, 0.33f));
		effectData.put(3, new effectLevel(0.75f, 0.4f, 0f));
	}

	private static class effectLevel {
		private final float hullLevel;
		private final float repairedFraction;
		private final float effectiveArmorFraction;

		public effectLevel(float hullLevel, float repairedFraction, float effectiveArmorFraction) {
			this.hullLevel = hullLevel;
			this.repairedFraction = repairedFraction;
			this.effectiveArmorFraction = effectiveArmorFraction;
		}

	}

	@Override
	public void init(HullModSpecAPI spec) {
		this.setNotCompatible(HullMods.HEAVYARMOR);
		super.init(spec);
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getMaxArmorDamageReduction().modifyFlat(id, -MAX_ARMOR_EFFECT_REDUCTION);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		removeBlockedMod(ship);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!ship.isAlive() && ship.isHulk()) return;

		CombatEngineAPI engine = Global.getCombatEngine();
		MutableShipStatsAPI stats = ship.getMutableStats();
		String id = DATA_KEY + ship.getId();
		String repKey = DATA_KEY + ship.getId() + "_repaired";
		float maxHull = ship.getMaxHitpoints();
		float currHull = ship.getHitpoints();

		Float repaired = (Float) ship.getCustomData().get(repKey);
		if (repaired == null) {
			repaired = 0f;
		}

		// 判断已修复结构值是否达到要求
		int level = 0;
		// 减少无伤时的计算(玩家一般无伤不是常识吗？)
		if (repaired != 0f) {
			if (repaired > maxHull * effectData.get(3).hullLevel) level = 3;
			else if (repaired > maxHull * effectData.get(2).hullLevel) level = 2;
			else if (repaired > maxHull * effectData.get(1).hullLevel) level = 1;
		}

		// 装甲计算值增益
		float armorBonus = ship.getHullSpec().getArmorRating() * effectData.get(level).effectiveArmorFraction * EFFECTIVE_ARMOR_BONUS;
		stats.getEffectiveArmorBonus().modifyFlat(id, armorBonus);

		// 结构修复
		float repairAmount = Math.min(HULL_REPAIRED_VALUE, HULL_REPAIRED_PERCENT * maxHull) * amount;
		repaired *= effectData.get(level).repairedFraction;
		if (repairAmount > maxHull - currHull) repairAmount = maxHull - currHull;
		ship.setHitpoints(currHull + repairAmount);
		repaired += repairAmount;
		ship.setCustomData(repKey, repaired);

		// 粒子特效
		PARTICLE_SPAWN_INTERVAL.advance(amount);
		if (PARTICLE_SPAWN_INTERVAL.intervalElapsed()) {
			Vector2f vel = ship.getVelocity().length() == 0f ? MathUtils.getRandomPointInCircle(xel_Misc.V2ZERO, Math.max(50f, ship.getCollisionRadius() * 0.1f)) : ship.getVelocity();
			xel_Misc.spawnSeveralParticles(
					engine,
					MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 0.7f),
					vel,
					4,
					3f,
					ship.getCollisionRadius() * 2f,
					2.5f,
					new Color(0, 255, 166, 255));
		}

		// 战斗文本
		if (ship == engine.getPlayerShip()) {
			String levelStr = "";
			switch (level) {
				case 0:
					levelStr = i18n_hullmod.get("xel_pr_level_high");
					break;
				case 1:
					levelStr = i18n_hullmod.get("xel_pr_level_mid");
					break;
				case 2:
					levelStr = i18n_hullmod.get("xel_pr_level_low");
					break;
				case 3:
					levelStr = i18n_hullmod.get("xel_pr_level_very_low");
					break;
				default:
					break;
			}
			engine.maintainStatusForPlayerShip(
					STATUS1,
					"graphics/icons/hullsys/damper_field.png",
					xel_Misc.getHullmodName(HullModUtil.XEL_PHASE_REACTOR),
					i18n_hullmod.get("xel_pr_level") + levelStr + "--[" + String.format("%.1f", 100f * repaired / ship.getMaxHitpoints()) + "]",
					false);
		}
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
		if (index == 0) return "30%";
		else if (index == 1) return (int) (MAX_ARMOR_EFFECT_REDUCTION * 100f) + "%";
		else if (index == 2) return (int) (HULL_REPAIRED_PERCENT * 100f) + "%";
		else if (index == 3) return (int) HULL_REPAIRED_VALUE + "";
		else if (index == 4) return "25%/50%/75%";
		else if (index == 5) return "20%/40%/60%";
		else
			return index == 6 ? "33%/66%/100%" : super.getDescriptionParam(index, hullSize, ship);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return hasArrayMod(ship) && !hasNotCompatibleMod(ship) && !hasTooMuchResponseMod(ship);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (!hasArrayMod(ship)) return getNoArrayReason();
		else if (hasTooMuchResponseMod(ship)) return getTooMuchResponseModReason();
		else return hasNotCompatibleMod(ship) ? getNotCompatibleReason() : super.getUnapplicableReason(ship);
	}

//	private static class phaseReactorRegen implements AdvanceableListener {
//		private final ShipAPI ship;
//		private boolean inited = false;
//		private String repKey;
//		private float repaired;
//		private final String id;
//		private final IntervalUtil interval = new IntervalUtil(0.05f, 0.05f);
//		private final Object status1 = new Object();
//
//		private void init() {
//			inited = true;
//			repKey = DATA_KEY + ship.getId() + "_repaired";
//			repaired = getRepaired(repKey);
//		}
//
//		protected float getRepaired(String key) {
//			Float r = (Float) ship.getCustomData().get(key);
//			if (r == null) r = 0f;
//			return r;
//		}
//
//		private phaseReactorRegen(ShipAPI ship, String id) {
//			this.ship = ship;
//			this.id = id;
//		}
//
//		@Override
//		public void advance(float amount) {
//			if (!ship.isAlive() || ship.isHulk()) return;
//
//			CombatEngineAPI engine = Global.getCombatEngine();
//			MutableShipStatsAPI stats = ship.getMutableStats();
//
//			float maxHull = ship.getMaxHitpoints();
//			float currHull = ship.getHitpoints();
//
//			int level = 0;
//			if (repaired > maxHull * effectData.get(3).hullLevel) level = 3;
//			else if (repaired > maxHull * effectData.get(2).hullLevel) level = 2;
//			else if (repaired > maxHull * effectData.get(1).hullLevel) level = 1;
//
//			float armorBonus = ship.getHullSpec().getArmorRating() * effectData.get(level).effectiveArmorFraction * EFFECTIVE_ARMOR_BONUS;
//			stats.getEffectiveArmorBonus().modifyFlat(id, armorBonus);
//
//			if (!inited) init();
//
//			float repairAmount = Math.min(HULL_REPAIRED_VALUE, HULL_REPAIRED_PERCENT * maxHull) * amount;
//
//			repaired *= effectData.get(level).repairedFraction;
//			if (repairAmount > maxHull - currHull) repairAmount = maxHull - currHull;
//
//			ship.setHitpoints(currHull + repairAmount);
//			repaired += repairAmount;
//			ship.setCustomData(repKey, repaired);
//
//			interval.advance(amount);
//			if (interval.intervalElapsed()) {
//				Vector2f vel = ship.getVelocity().length() == 0f ? MathUtils.getRandomPointInCircle(xel_Misc.V2ZERO, Math.max(50f, ship.getCollisionRadius() * 0.1f)) : ship.getVelocity();
//				xel_Misc.spawnSeveralParticles(
//						engine,
//						MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 0.7f),
//						vel,
//						4,
//						3f,
//						ship.getCollisionRadius() * 2f,
//						2.5f,
//						new Color(0, 255, 166, 255));
//			}
//
//			if (ship == engine.getPlayerShip()) {
//				String levelStr = "";
//				switch (level) {
//					case 0:
//						levelStr = i18n_hullmod.get("xel_pr_level_high");
//						break;
//					case 1:
//						levelStr = i18n_hullmod.get("xel_pr_level_mid");
//						break;
//					case 2:
//						levelStr = i18n_hullmod.get("xel_pr_level_low");
//						break;
//					case 3:
//						levelStr = i18n_hullmod.get("xel_pr_level_very_low");
//						break;
//					default:
//						break;
//				}
//				engine.maintainStatusForPlayerShip(
//						status1,
//						"graphics/icons/hullsys/damper_field.png",
//						xel_Misc.getHullmodName(HullModUtil.XEL_PHASE_REACTOR),
//						i18n_hullmod.get("xel_pr_level") + levelStr + "--[" + String.format("%.1f", 100f * repaired / ship.getMaxHitpoints()) + "]",
//						false);
//			}
//		}
//	}
}
