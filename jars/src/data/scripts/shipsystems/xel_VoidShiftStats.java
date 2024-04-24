package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.HullModUtil;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.magiclib.util.MagicUI;

import java.awt.*;

import static data.utils.xel.Constants.i18n_shipSystem;

public class xel_VoidShiftStats extends BaseShipSystemScript {
	public static final float VULNERABLE_FRACTION = 0f;
	public static final float MAX_TIME_MULT = 4f;
	public static final float SHIP_ALPHA_MULT = 0.3f;
	public static final float MAX_SPEED_PERCENT = 10f;
	public static final float MAX_TURN_RATE = 50f;
	private static final float MAX_ENERGY = 100f;
	private static final float ENERGY_REDUCED_PER_SEC = 10f;
	private static final float ENERGY_INCREASED_PER_SEC = 100f / 30f;
	private static final float ACTIVE_ENERGY_THRESHOLD = 25f;
	private static final String VOID_SHIFT_DATA_KEY = "xel_void_shift_data_key";
	private static final Object STATUS1 = new Object();
	private static final Object STATUS2 = new Object();

	private boolean isUnapplied = false;
	private boolean isStarted = false;

	private void maintainStatus(CombatEngineAPI engine, ShipAPI playership, float effectLevel, float timeMultiplier) {
		ShipSystemAPI cloak = playership.getPhaseCloak();
		if (cloak == null) cloak = playership.getSystem();
		if (cloak == null) return;

		if (effectLevel > VULNERABLE_FRACTION) {
			engine.maintainStatusForPlayerShip(STATUS1, cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), i18n_shipSystem.get("xel_voidshift_time_flow") + Misc.getRoundedValue(timeMultiplier), false);
		}
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship;
		boolean player;
		CombatEngineAPI engine;
		// 取安装此系统舰船
		if (stats.getEntity() instanceof ShipAPI) {
			engine = Global.getCombatEngine();
			if (engine == null) return;
			ship = (ShipAPI) stats.getEntity();
			player = ship == engine.getPlayerShip();
			id = id + "_" + ship.getId();
		} else {
			return;
		}

		String dataKey = VOID_SHIFT_DATA_KEY + ship.getId();
		Float energy = (Float) ship.getCustomData().get(dataKey);
		if (energy == null) {
			energy = MAX_ENERGY;
		}
		float energyLevel = energy / MAX_ENERGY;
//        MagicUI.drawHUDStatusBar(
//                ship,
//                energy / MAX_ENERGY,
//                new Color(0, 155, 255),
//                new Color(155, 0, 255),
//                0f,
//                "能量值",
//                null,
//                false);
		int r = (int) (255 * (1f - energyLevel));
		int g = (int) (155 * energyLevel * energyLevel);
		int b = (int) (255 * energyLevel);
		MagicUI.drawInterfaceStatusBar(
				ship,
				dataKey,
				energyLevel,
				new Color(r, g, b, 192),
				new Color(255, 255, 0, 255),
				ACTIVE_ENERGY_THRESHOLD * 0.01f,
				"能量值",
				energy.intValue());

		// 战场时间暂停的时候不跑，节省性能
		if (engine.isPaused()) {
			if (energy < ACTIVE_ENERGY_THRESHOLD)
				ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
			return;
		}

		float energyIncrease = ENERGY_INCREASED_PER_SEC;
		float amount = engine.getElapsedInLastFrame();

		// 若有矩阵充能器，恢复加快
		if (ship.getVariant().hasHullMod(HullModUtil.XEL_ARRAY_BATTERY)) {
			energyIncrease *= 1.5f;
		}

		// apply真的会一直跑
		// 系统处于冷却或闲置时，跑unapply 并 return
		if (state == State.COOLDOWN || state == State.IDLE) {
			// 只跑一次  unapply
			if (!isUnapplied) {
				unapply(stats, id);
				isUnapplied = true; // 是否生效置否
			}
			energy = Math.min(MAX_ENERGY, energyIncrease * amount + energy); //energy只要在系统不运作就一直增加
			if (energy < ACTIVE_ENERGY_THRESHOLD)
				ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
			ship.setCustomData(dataKey, energy);
			return;
		}

		// 判断是否可以开技能, allow 要在结束相位后赋值
		float levelForAlpha = effectLevel;

		// 系统处于充能 (chargeUp) 或 运行时 特效与相位 减少energy
		if (state == State.IN || state == State.ACTIVE) {
			levelForAlpha = effectLevel;
			energy = Math.max(0f, energy - ENERGY_REDUCED_PER_SEC * amount);
			ship.setCustomData(dataKey, energy);
			// 启动的判断
			if (!isStarted) {
				isStarted = true;
				isUnapplied = false;

				// 第一帧特效
				RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
				ripple.flip(false);//波纹翻转
				ripple.setSize(ship.getCollisionRadius() * 2f);// 波纹范围
				ripple.setFrameRate(60f / 0.4f);
				ripple.setIntensity(10f);// 波纹强度
				ripple.fadeOutSize(1f);// 波纹消逝时间
				ripple.fadeInIntensity(0.6f);// 波纹消逝强度？
				DistortionShader.addDistortion(ripple);//生成特效
			}
			ship.setPhased(true);
		} else if (state == State.OUT) {
			// 若系统处于退出(chargeDown)状态
			ship.setPhased(effectLevel > 0.5f);
			levelForAlpha = effectLevel;
		}
		// 设置透明度
		ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
		ship.setApplyExtraAlphaToEngines(true);


		// 时流效果 我觉得最好不要受到其他增益
		float shipTimeMult = 1f + (MAX_TIME_MULT - 1f) * levelForAlpha;
		stats.getTimeMult().modifyMult(id, shipTimeMult);
		engine.getTimeMult().modifyMult(id, 1f / shipTimeMult);

		if (player) {
			maintainStatus(engine, ship, effectLevel, shipTimeMult);
			// 不知道是什么的音效
			Global.getSoundPlayer().applyLowPassFilter(0.75f, 0.25f);
		}


		//机动性增益
		stats.getMaxTurnRate().modifyPercent(id, MAX_TURN_RATE * effectLevel);
		stats.getTurnAcceleration().modifyPercent(id, MAX_TURN_RATE * effectLevel * 2f);

		stats.getMaxSpeed().modifyPercent(id, MAX_SPEED_PERCENT * effectLevel);
		stats.getAcceleration().modifyPercent(id, MAX_SPEED_PERCENT * effectLevel * 5f);
		stats.getDeceleration().modifyPercent(id, MAX_SPEED_PERCENT * effectLevel * 3f);

		//fob的辅助v排，可以让ai更聪明
		runVentAssistance(ship);
		if (energy <= 0f) {
			ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 1);
			isStarted = false;
			isUnapplied = true;
		}
	}


	/**
	 * 辅助V排
	 *
	 * @param ship 本舰
	 */
	private void runVentAssistance(ShipAPI ship) {
		if (ship.getShipAI() != null && !ship.isPhased()) {
			ShipwideAIFlags flags = ship.getAIFlags();
			boolean forceVenting = false;
//            float fluxLevel = ship.getFluxLevel();
			float hardFluxLevel = ship.getHardFluxLevel();

			if (flags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF) && !flags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE)) {
				forceVenting = true;
			}

			if (hardFluxLevel > 0.75f) {
				forceVenting = true;
			}

			if (!flags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE) && flags.hasFlag(ShipwideAIFlags.AIFlags.SAFE_FROM_DANGER_TIME)) {
				float safeFromDangerTime = (float) flags.getCustom(ShipwideAIFlags.AIFlags.SAFE_FROM_DANGER_TIME);
				if (safeFromDangerTime > 3f && hardFluxLevel > 0.2f) {
					forceVenting = true;
				}
			}

			if (forceVenting)
				ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getTimeMult().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		Global.getCombatEngine().getTimeMult().unmodify(id);
		isStarted = false;
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) return;
		ship.setPhased(false);
		ship.setExtraAlphaMult(1f);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return super.getStatusData(index, state, effectLevel);
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		return (ship.getCustomData().get(VOID_SHIFT_DATA_KEY + ship.getId()) == null ? MAX_SPEED_PERCENT : ship.getCustomData().get(VOID_SHIFT_DATA_KEY + ship.getId())) + "%";

	}
}
