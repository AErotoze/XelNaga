package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.HullModUtil;
import data.utils.xel.xel_Misc;

import java.awt.*;
import java.text.DecimalFormat;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_ProtossEnergyArray extends BaseHullMod {
	/*
	 * 星灵势力改造：星灵能量矩阵/灵能水晶矩阵
	 *
	 * $星灵能量矩阵
	 * 星灵舰船进入过载状态后立即恢复
	 * 过载发生会导致护盾下线8秒，下线时过载不重置时间
	 * 所有星灵舰船护盾[零维持]，护盾[永远开启]
	 * 星灵等离子护盾有效抵御电弧，穿盾概率-50%
	 *
	 * 升级分支联动[不兼容]：这下只能写进advance里了
	 * 矩阵充能器
	 * 星灵能量矩阵——护盾下线时间减少
	 * 灵能水晶矩阵——异构辐能返还比例 x 0.9
	 * 控制芯核
	 * 星灵能量矩阵——增加全体武器50的基础射程
	 * 灵能水晶矩阵——
	 * 谐振盘
	 * 星灵能量矩阵——
	 * 灵能水晶矩阵——幅能异构阈值提高
	 *
	 * 特殊内置：
	 * 刚毅护盾：护盾受到的伤害不超过最大辐容的0.5%
	 * 复仇协议：根据友方已死亡舰船的部署点，提供永久or暂时的增益
	 * 永恒屏障：每次护盾击溃后获得一个镀层，镀层持续时间内获得高额免伤并持续恢复结构
	 */

	private static final float PIERCE_MULT = 50f;
	private static final float SHIELD_DOWN_TIME = 8f;
	private static final float HARDFLUX_VENT_BONUS = 30f;
	private static final float ARRAY_BATTERY_GOOD_BONUS = 20f;
	private static final float ARRAY_BATTERY_BAD_BONUS = 50f;
	private static final float CYBERNETICS_CORE_GOOD_BONUS = 50f;
	private static final float CYBERNETICS_CORE_BAD_BONUS = 25f;
	private static final float RESONANCE_COIL_GOOD_BONUS = 0f;
	private static final float RESONANCE_COIL_BAD_BONUS = 90f;


	private static final String DATA_KEY = "EP_Protoss_Energy_Array";
	private static final DecimalFormat dc = new DecimalFormat("0.0");

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		// 无护盾维持，+30%硬幅能开盾排散能力
		stats.getShieldUpkeepMult().modifyMult(id, 0f);
		stats.getHardFluxDissipationFraction().modifyFlat(id, HARDFLUX_VENT_BONUS * 0.01f);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		// 特殊机制
		ship.addListener(new psaManager(ship, CYBERNETICS_CORE_GOOD_BONUS));
		MutableShipStatsAPI stats = ship.getMutableStats();
		float bonus = PIERCE_MULT + (hasCyberneticsCore(ship) ? CYBERNETICS_CORE_BAD_BONUS : 0f);
		if (hasShieldBattery(ship)) {
			stats.getShieldDamageTakenMult().modifyMult(id, 1f - ARRAY_BATTERY_GOOD_BONUS * 0.01f);
		}
		if (ship.getShield() != null && hasResonanceCoil(ship)) {
			ship.getShield().setType(ShieldAPI.ShieldType.OMNI);
			stats.getShieldArcBonus().modifyMult(id, RESONANCE_COIL_BAD_BONUS * 0.005f);
		}
	}


	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return (int) PIERCE_MULT + "%";
		if (index == 1) return (int) HARDFLUX_VENT_BONUS + "%";
		if (index == 2) return (int) SHIELD_DOWN_TIME + "";
		return super.getDescriptionParam(index, hullSize);
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 5f;
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color bad = Misc.getNegativeHighlightColor();
		Color good = Misc.getPositiveHighlightColor();

		boolean flag = hasShieldBattery(ship);
		tooltip.addSectionHeading(i18n_hullmod.get("xel_pea_title"), Alignment.TMID, pad * 2f);
		TooltipMakerAPI myText = tooltip.beginImageWithText("graphics/hullmods/xel_ArrayBattery.png", 32f);
		myText.addPara("%s [%s]", pad * 2f,
				new Color[]{new Color(155, 155, 255, 255), flag ? h : g},
				xel_Misc.getHullmodName(HullModUtil.XEL_ARRAY_BATTERY),
				flag ? i18n_hullmod.get("install") : i18n_hullmod.get("uninstall"));
		myText.addPara(i18n_hullmod.get("xel_pea_upgrade1"), pad, flag ? good : g, (int) ARRAY_BATTERY_GOOD_BONUS + "%");
		myText.addPara(i18n_hullmod.get("xel_pea_upgrade2"), pad, flag ? bad : g, (int) ARRAY_BATTERY_BAD_BONUS + "%");
		tooltip.addImageWithText(pad);

		flag = hasCyberneticsCore(ship);
		myText = tooltip.beginImageWithText("graphics/hullmods/xel_CyberneticsCore.png", 32f);
		myText.addPara("%s [%s]", pad * 2f,
				new Color[]{new Color(155, 155, 255, 255), flag ? h : g},
				xel_Misc.getHullmodName(HullModUtil.XEL_CYBERNETICS_CORE),
				flag ? i18n_hullmod.get("install") : i18n_hullmod.get("uninstall"));
		myText.addPara(i18n_hullmod.get("xel_pea_upgrade3"), pad, flag ? good : g, "" + (int) CYBERNETICS_CORE_GOOD_BONUS);
		myText.addPara(i18n_hullmod.get("xel_pea_upgrade4"), pad, flag ? bad : g, (int) CYBERNETICS_CORE_BAD_BONUS + "%");
		tooltip.addImageWithText(pad);

		flag = hasResonanceCoil(ship);
		myText = tooltip.beginImageWithText("graphics/hullmods/xel_ResonanceCoil.png", 32f);
		myText.addPara("%s [%s]", pad * 2f,
				new Color[]{new Color(155, 155, 255, 255), flag ? h : g},
				xel_Misc.getHullmodName(HullModUtil.XEL_RESONANCE_COIL),
				flag ? i18n_hullmod.get("install") : i18n_hullmod.get("uninstall"));
		myText.addPara(i18n_hullmod.get("xel_pea_upgrade5"), pad, flag ? good : g, (int) RESONANCE_COIL_GOOD_BONUS + "%");
		myText.addPara(i18n_hullmod.get("xel_pea_upgrade6"), pad, flag ? bad : g, "" + (int) RESONANCE_COIL_BAD_BONUS);
		tooltip.addImageWithText(pad);

	}


	private boolean hasShieldBattery(ShipAPI ship) {
		return ship.getVariant().hasHullMod(HullModUtil.XEL_ARRAY_BATTERY);
	}

	private boolean hasCyberneticsCore(ShipAPI ship) {
		return ship.getVariant().hasHullMod(HullModUtil.XEL_CYBERNETICS_CORE);
	}

	private boolean hasResonanceCoil(ShipAPI ship) {
		return ship.getVariant().hasHullMod(HullModUtil.XEL_RESONANCE_COIL);
	}


	private class psaManager implements AdvanceableListener, WeaponBaseRangeModifier {
		private final Object STATUSKEY1 = new Object();
		private final ShipAPI SHIP;
		private final float RANGE;
		private float time = 0f;

		public psaManager(ShipAPI SHIP, float range) {
			this.SHIP = SHIP;
			this.RANGE = range;
		}

		@Override
		public void advance(float amount) {
			if (SHIP.isAlive() && SHIP.getShield() != null) {
				CombatEngineAPI engine = Global.getCombatEngine();
				FluxTrackerAPI fluxTracker = SHIP.getFluxTracker();

				SHIP.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
//                ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);

				if (fluxTracker.isOverloaded()) {
					SHIP.getShield().toggleOff();
					fluxTracker.stopOverload();
					if (time <= 0f) {
						time = SHIELD_DOWN_TIME * (hasShieldBattery(SHIP) ? 1f + ARRAY_BATTERY_BAD_BONUS * 0.01f : 1f);
					}
				}

				if (time <= 0f) {
					if (SHIP.getShield().isOff()) {
						SHIP.getShield().toggleOn();
					}
				} else {
					time -= amount;
				}

				if (engine.getPlayerShip() == SHIP) {
					engine.maintainStatusForPlayerShip(STATUSKEY1, "graphics/icons/hullsys/fortress_shield.png",
							xel_Misc.getHullmodName(HullModUtil.XEL_PROTOSS_ENERGY_ARRAY),
							time > 0 ? i18n_hullmod.format("xel_pea_shield_down", dc.format(time)) : i18n_hullmod.get("xel_pea_shield_up"),
							time > 0);
				}
			}
		}

		@Override
		public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
			return 0f;
		}

		@Override
		public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
			return 1f;
		}

		@Override
		public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
			if (!hasCyberneticsCore(ship)) return 0f;
			if (weapon.getSpec() == null) return 0f;
			if (weapon.getSpec().getMountType() != WeaponAPI.WeaponType.BALLISTIC
					&& weapon.getSpec().getMountType() != WeaponAPI.WeaponType.ENERGY)
				return 0f;
			return RANGE;
		}
	}
}
