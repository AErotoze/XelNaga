package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.HullModUtil;
import data.utils.xel.xel_Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static data.utils.xel.Constants.i18n_hullmod;

public class xel_PhaseReactor extends xel_BaseHullmod {
    /*
    相位反应堆
    反应堆受到的伤害
    反应堆受到超过伤害50的攻击触发超限
    每秒恢复 6.67% 额度
    反应堆需要 25秒 重启
    超限：
    [守护之壳]免疫所有伤害 2秒 该效果冷却时间 5秒
    [天界太阳能]排散12%当前软幅能和6%当前硬幅能，在 2秒 内排空 冷却时间 8秒
    [圣堂表象]在 2秒 内提供300%时流，冷却时间12秒
     */

	private static final float DAMAGE_THRESHOLD = 20f;
	private static final float RECOVER_PERCENT_PER_SEC = 100f / 15f;
	private static final float MAX_RESTART_TIME = 25f;
	private static final float TIME_MULT = 100f;
	private static final float SHIELD_EFFECT = 25f;

	private static final Map<ShipAPI.HullSize, Float> HEALTH_MAP = new HashMap<>();
	private static final Map<String, effectData> EFFECT_DATA = new HashMap<>();

	static {
		HEALTH_MAP.put(ShipAPI.HullSize.FIGHTER, 0f);
		HEALTH_MAP.put(ShipAPI.HullSize.FRIGATE, 500f);
		HEALTH_MAP.put(ShipAPI.HullSize.DESTROYER, 600f);
		HEALTH_MAP.put(ShipAPI.HullSize.CRUISER, 800f);
		HEALTH_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, 1000f);

		EFFECT_DATA.put(HullModUtil.XEL_RESONANCE_COIL, new effectData(2f, 10f, i18n_hullmod.get("xel_pr_effect_gs"), HullModUtil.XEL_RESONANCE_COIL));
		EFFECT_DATA.put(HullModUtil.XEL_ARRAY_BATTERY, new effectData(1f, 24f, i18n_hullmod.get("xel_pr_effect_sc"), HullModUtil.XEL_ARRAY_BATTERY));
		EFFECT_DATA.put(HullModUtil.XEL_CYBERNETICS_CORE, new effectData(5f, 30f, i18n_hullmod.get("xel_pr_effect_ta"), HullModUtil.XEL_CYBERNETICS_CORE));
	}

	@Override
	public void init(HullModSpecAPI spec) {
		this.setNotCompatible(HullMods.SAFETYOVERRIDES);
		super.init(spec);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		removeBlockedMod(ship);
		effectData data = null;
		for (String tmp : ship.getVariant().getHullMods()) {
			HullModSpecAPI mod = Global.getSettings().getHullModSpec(tmp);
			if (mod.hasUITag(i18n_hullmod.get("harmony_mod_tag"))) {
				data = EFFECT_DATA.get(mod.getId());
			}
		}
		ship.addListener(new PRmanager(ship, data));
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
		if (index == 0) return xel_Misc.getHullSizeFlatString(HEALTH_MAP);
		else if (index == 1) return String.format("%.2f", RECOVER_PERCENT_PER_SEC);
		else if (index == 2) return (int) MAX_RESTART_TIME + "sec";
		else if (index == 3) return (int) SHIELD_EFFECT + "%";
		else if (index == 4) return i18n_hullmod.get("xel_pr_title");
		else if (index == 5) return (int) DAMAGE_THRESHOLD + "";
		else return index == 6 ? "1%" : null;
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		return hasArrayMod(ship) && !hasNotCompatibleMod(ship) && !hasTooMuchResponseMod(ship);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (!hasArrayMod(ship)) return getNoArrayReason();
		else if (hasTooMuchResponseMod(ship)) return getTooMuchResponseModReason();
		else return hasNotCompatibleMod(ship) ? getNotCompatibleReason() : super.getUnapplicableReason(ship);
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 5f;
		Color h = Misc.getHighlightColor();
//        Color g = Misc.getGrayColor();
//        Color bad = Misc.getNegativeHighlightColor();
		Color good = Misc.getPositiveHighlightColor();

		effectData data;
		TooltipMakerAPI text = tooltip.beginImageWithText(Global.getSettings().getSpriteName("hullsys", "xel_PhaseReactor_active"), 64f);
		tooltip.addSectionHeading(i18n_hullmod.get("xel_pr_title"), Alignment.TMID, pad * 2f);
		if (ship.getVariant().hasHullMod(HullModUtil.XEL_CYBERNETICS_CORE)) {
			data = EFFECT_DATA.get(HullModUtil.XEL_CYBERNETICS_CORE);
			text.addPara("[%s]", pad * 2f, h, i18n_hullmod.get("xel_pr_effect_ta"));
			text.setBulletedListMode("--");
			text.addPara(i18n_hullmod.get("xel_pr_effect_ta1"), pad, h, (int) data.maxEffectTime + "sec");
			text.addPara(i18n_hullmod.get("xel_pr_cooldown"), pad, h, (int) data.maxCooldown + "sec");
			text.setBulletedListMode(null);
			tooltip.addImageWithText(pad);
		} else if (ship.getVariant().hasHullMod(HullModUtil.XEL_ARRAY_BATTERY)) {
			data = EFFECT_DATA.get(HullModUtil.XEL_ARRAY_BATTERY);
			text.addPara("[%s]", pad * 2f, h, i18n_hullmod.get("xel_pr_effect_sc"));
			text.setBulletedListMode("--");
			text.addPara(i18n_hullmod.get("xel_pr_effect_sc1"), pad, new Color[]{h, good, good}, (int) data.maxEffectTime + "sec", "12%", "6%");
			text.addPara(i18n_hullmod.get("xel_pr_cooldown"), pad, h, (int) data.maxCooldown + "sec");
			text.setBulletedListMode(null);
			tooltip.addImageWithText(pad);
		} else if (ship.getVariant().hasHullMod(HullModUtil.XEL_RESONANCE_COIL)) {
			data = EFFECT_DATA.get(HullModUtil.XEL_RESONANCE_COIL);
			text.addPara("[%s]", pad * 2f, h, i18n_hullmod.get("xel_pr_effect_gs"));
			text.setBulletedListMode("--");
			text.addPara(i18n_hullmod.get("xel_pr_effect_gs1"), pad, h, (int) data.maxEffectTime + "sec");
			text.addPara(i18n_hullmod.get("xel_pr_cooldown"), pad, h, (int) data.maxCooldown + "sec");
			text.setBulletedListMode(null);
			tooltip.addImageWithText(pad);
		} else {
			text = tooltip.beginImageWithText(Global.getSettings().getHullModSpec(HullModUtil.XEL_CYBERNETICS_CORE).getSpriteName(), 32f);
			text.addPara("%s", pad * 2f, new Color(155, 155, 255, 255), xel_Misc.getHullmodName(HullModUtil.XEL_CYBERNETICS_CORE));
			text.addPara("%s--" + i18n_hullmod.get("xel_pr_effect_ta2"), pad, h, i18n_hullmod.get("xel_pr_effect_ta"));
			tooltip.addImageWithText(pad);
			text = tooltip.beginImageWithText(Global.getSettings().getHullModSpec(HullModUtil.XEL_ARRAY_BATTERY).getSpriteName(), 32f);
			text.addPara("%s", pad * 2f, new Color(155, 155, 255, 255), xel_Misc.getHullmodName(HullModUtil.XEL_ARRAY_BATTERY));
			text.addPara("%s--" + i18n_hullmod.get("xel_pr_effect_sc2"), pad, h, i18n_hullmod.get("xel_pr_effect_sc"));
			tooltip.addImageWithText(pad);
			text = tooltip.beginImageWithText(Global.getSettings().getHullModSpec(HullModUtil.XEL_RESONANCE_COIL).getSpriteName(), 32f);
			text.addPara("%s", pad * 2f, new Color(155, 155, 255, 255), xel_Misc.getHullmodName(HullModUtil.XEL_RESONANCE_COIL));
			text.addPara("%s--" + i18n_hullmod.get("xel_pr_effect_gs2"), pad, h, i18n_hullmod.get("xel_pr_effect_gs"));
			tooltip.addImageWithText(pad);

		}
	}

	private static class effectData {
		private final float maxEffectTime;
		private final float maxCooldown;
		private final String effectName;
		private final String hullmods;

		public effectData(float maxEffectTime, float maxCooldown, String effectName, String hullmods) {
			this.maxCooldown = maxCooldown;
			this.maxEffectTime = maxEffectTime;
			this.effectName = effectName;
			this.hullmods = hullmods;
		}
	}

	private static class PRmanager implements DamageTakenModifier, AdvanceableListener {
		private final ShipAPI ship;
		private final float MAX_HEALTH;
		private final String ID = "xel_PR_damage_taken_modifier";
		private float fluxDeceased = 0f;
		private float recoverTime = 0f;
		private float effectTime = 0f;
		private float effectCooldown = 0f;
		private float health;
		private final effectData data;
		private final DecimalFormat dc = new DecimalFormat("0.0");
		private final IntervalUtil interval = new IntervalUtil(0.1f, 0.1f);
		private final IntervalUtil intervalParticle = new IntervalUtil(0.05f, 0.05f);

		// 计时皆用减法计算
		public PRmanager(ShipAPI ship, effectData data) {
			if (data == null)
				this.data = new effectData(0f, 0f, null, null);
			else
				this.data = data;
			this.ship = ship;
			this.MAX_HEALTH = HEALTH_MAP.get(ship.getHullSize());
			this.health = this.MAX_HEALTH;
		}

		@Override
		public void advance(float amount) {
			if (ship.isAlive()) {
				CombatEngineAPI engine = Global.getCombatEngine();
				// 判断是正常运行还是正在重启
				if (recoverTime <= 0f) {
					// 判断额度是否有剩
					if (health > 0f) {
						// 有剩则回复
						health = Math.min(MAX_HEALTH, health + MAX_HEALTH * RECOVER_PERCENT_PER_SEC * amount * 0.01f);
						float level = health / MAX_HEALTH;
						intervalParticle.advance((ship.isPhased() ? amount / 3f : amount) * level);
//                        ship.setJitterShields(false);
//                        ship.setJitter(ship, new Color(0, 255, 166, 64), level, 3, 10f);
//                        ship.setJitterUnder(ship, new Color(0, 255, 166, 128), level, 5, 15f);
						if (intervalParticle.intervalElapsed()) {
							Vector2f vel = ship.getVelocity().length() == 0f ? MathUtils.getRandomPointInCircle(xel_Misc.V2ZERO, Math.max(50f, ship.getCollisionRadius() * 0.1f)) : ship.getVelocity();
							xel_Misc.spawnSeveralParticles(
									engine,
									MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 0.7f),
									vel,
									3,
									5f * level,
									ship.getCollisionRadius() * 2f,
									2.5f,
									new Color(0, 255, 166, 255));
						}

						// 判断效果是否启动
						// 启动后若额度满载则会提前结束效果
						if (effectTime > 0f) {
							// 判断需要执行的效果
							if (Objects.equals(data.hullmods, HullModUtil.XEL_RESONANCE_COIL)) {
								// 守护之壳
								ship.setJitterShields(false);
								ship.setJitter(ship, new Color(161, 0, 255, 64), 0.5f, 3, 10f);
								ship.setJitterUnder(ship, new Color(163, 0, 255, 128), 0.5f, 5, 15f);

							}
							if (Objects.equals(data.hullmods, HullModUtil.XEL_ARRAY_BATTERY)) {
								interval.advance(amount);
								// 天界太阳能
								// 取开始运行时这一帧的幅能
								if (effectTime >= data.maxEffectTime - amount) {
									fluxDeceased = ship.getMaxFlux() * (ship.getHardFluxLevel() * 0.06f + (ship.getFluxLevel() - ship.getHardFluxLevel()) * 0.012f);
								}
								solariteCelestial(fluxDeceased, amount);
							}
							if (Objects.equals(data.hullmods, HullModUtil.XEL_CYBERNETICS_CORE)) {
								interval.advance(amount / 3f);
								// 圣堂表象
								templarApparent(effectTime, amount);
							}
							// 效果启动，开始计时 启动后effectCooldown已经重置
							effectTime = Math.max(0f, effectTime - amount);
						} else {
							// 效果结束后 开始冷却计时
							effectCooldown = Math.max(0f, effectCooldown - amount);
						}
					} else {
						//反之 进入重启状态 重置效果时间和冷却时间
						recoverTime = MAX_RESTART_TIME;
						effectTime = 0f;
						effectCooldown = 0f;
						//强制退出 圣堂表象
						if (Objects.equals(data.hullmods, HullModUtil.XEL_CYBERNETICS_CORE)) {
							ship.getMutableStats().getTimeMult().unmodify(ID);
							Global.getCombatEngine().getTimeMult().unmodify(ID);
						}
					}
				} else {
					// 处于重启
					recoverTime = Math.max(0f, recoverTime - amount);
					// 若重启完成
					if (recoverTime <= 0f) {
						// 重置health
						health = MAX_HEALTH;
					}
				}
				if (engine.getPlayerShip() == ship) {
					String str = health > 0f ? i18n_hullmod.format("xel_pr_show_health", (int) health) : i18n_hullmod.format("xel_pr_restarting", dc.format(recoverTime));
					engine.maintainStatusForPlayerShip(
							"xel_pr_manager1",
							Global.getSettings().getSpriteName("hullsys", "xel_PhaseReactor_active"),
							i18n_hullmod.get("xel_pr_name"),
							str,
							health <= 0f);
					if (health > 0f && data.effectName != null) {
						str = effectCooldown > 0f ? i18n_hullmod.format("xel_pr_effect_cooldown", data.effectName, dc.format(effectCooldown)) : i18n_hullmod.format("xel_pr_effect_ready", data.effectName);
						engine.maintainStatusForPlayerShip(
								"xel_pr_manager2",
								Global.getSettings().getSpriteName("hullsys", "xel_PhaseReactor_active"),
								effectTime > 0f ? this.data.effectName : i18n_hullmod.get("xel_pr_name"),
								effectTime > 0f ? i18n_hullmod.format("xel_pr_effect_active", dc.format(effectTime)) : str,
								effectCooldown > 0f);
					}
				}
			}
		}

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			if (health > 0f) {
				float baseDamage = damage.getBaseDamage();
				if (damage.isDps()) baseDamage *= damage.getDpsDuration();
				if (shieldHit) {
					baseDamage *= 0.25f * damage.getType().getShieldMult();
				} else {
					baseDamage *= damage.getType().getArmorMult();
					baseDamage *= damage.getType().getHullMult();
				}
				float level = Math.max(health / MAX_HEALTH, 0.36f);
				float damageDecreased = shieldHit ? 1 - SHIELD_EFFECT : 0f;
				baseDamage *= 1f - Math.max((0.6f * (float) Math.log10(level - 0.35f) + 1.0123f), 0f);
				baseDamage *= (1f - damageDecreased);

				CombatEntityAPI source = target;
				if (damage.getStats() != null) {
					source = damage.getStats().getEntity();
					if (source == null) {
						source = target;
					}
				}

				//若冷却完毕 若受到大于的threshold伤害 若效果未激活 重置效果时间和冷却时间
				if (effectCooldown == 0f && baseDamage > DAMAGE_THRESHOLD && effectTime == 0f) {
					effectTime = data.maxEffectTime;
					effectCooldown = data.maxCooldown;
					//弹出信息
					Global.getCombatEngine().addFloatingText(ship.getLocation(), i18n_hullmod.format("xel_pr_effet_on", data.effectName), 25f, Misc.getHighlightColor(), ship, 1f, 0f);
					RippleDistortion ripple = new RippleDistortion(point, ship.getVelocity());
					float factor = ship.getCollisionRadius() * 2f;
					ripple.setSize(factor);
					ripple.setIntensity(factor / 5f);
					ripple.setFrameRate(180f);
					ripple.fadeInSize(0.3f);
					ripple.fadeOutIntensity(0.3f);
					DistortionShader.addDistortion(ripple);

					health -= baseDamage * 0.01f;
					damage.getModifier().modifyMult(ID, 0f);
					return ID;
				}
				ship.setJitterShields(false);
				// 若效果持续时间未结束
				if (effectTime > 0f) {
					//若效果是 守护之壳
					if (Objects.equals(data.hullmods, HullModUtil.XEL_RESONANCE_COIL)) {
						// 运行守护之壳
						return guardianShell(damage, point);
					}
				}
				// 额度是否大于等于吸收伤害
				if (health >= baseDamage) {
					health -= baseDamage;
					damage.getModifier().modifyMult(ID, damageDecreased);
					Global.getCombatEngine().addFloatingDamageText(point, baseDamage, new Color(255, 255, 255), target, source);
				} else {
					float reduction = baseDamage - health;
					health = 0f;
					damage.getModifier().modifyMult(ID, shieldHit ? (1f - reduction / baseDamage) * 0.25f + 0.75f : 1f - reduction / baseDamage);
					Global.getCombatEngine().addFloatingText(ship.getLocation(), i18n_hullmod.get("xel_pr_health_out"), 25f, Misc.getNegativeHighlightColor(), ship, 1f, 0f);
				}
				return ID;
			}
			return null;
		}

		// 守护之壳 0伤害
		private String guardianShell(DamageAPI damage, Vector2f point) {
			damage.getModifier().modifyMult(ID, 0f);
			ship.setJitter(ship, new Color(164, 0, 255, 128), 0.6f, 5, 30f, 60f);
			ship.setJitterUnder(ship, new Color(166, 0, 255, 191), 0.6f, 10, 45f);
//            Global.getCombatEngine().addNebulaSmokeParticle(
//                    point,
//                    MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(10f, 50f), MathUtils.getRandomNumberInRange(0f, 360f)),
//                    MathUtils.getRandomNumberInRange(10f, 40f),
//                    MathUtils.getRandomNumberInRange(1.5f, 3f),
//                    0.5f,
//                    0f,
//                    MathUtils.getRandomNumberInRange(0.1f, 1f),
//                    Misc.scaleAlpha(new Color(216, 139, 255, 204), MathUtils.getRandomNumberInRange(0.3f, 0.6f))
//            );
			RippleDistortion ripple = new RippleDistortion(point, ship.getVelocity());
			float factor = ship.getCollisionRadius() / 2f;
			ripple.setSize(factor);
			ripple.setIntensity(factor / 5f);
			ripple.setFrameRate(180f);
			ripple.fadeInSize(0.3f);
			ripple.fadeOutIntensity(0.3f);
			DistortionShader.addDistortion(ripple);

			return "xel_PR_damage_taken_modifier";
		}

		// 天界太阳能 排幅
		private void solariteCelestial(float fluxDecreased, float amount) {
			ship.getFluxTracker().decreaseFlux(fluxDecreased * amount / data.maxEffectTime);
			ship.setJitterShields(false);
			ship.setJitter(ship, new Color(254, 138, 14, 128), 0.5f, 3, 10f);
			ship.setJitterUnder(ship, new Color(254, 138, 14, 191), 0.5f, 5, 15f);
			if (interval.intervalElapsed()) {
				ship.addAfterimage(
						new Color(254, 138, 14, 204),
						0f,
						0f,
						-ship.getVelocity().getX(),
						-ship.getVelocity().getY(),
						0f,
						0.2f,
						0f,
						0.3f,
						false,
						false,
						false);
			}
		}

		// 圣堂表象 时流
		private void templarApparent(float effectTime, float amount) {
			float level = Math.max(amount, 1f - effectTime / data.maxEffectTime);
			float bonus = Math.max(1f, TIME_MULT * level);
			if (effectTime > amount + amount) {
				ship.getMutableStats().getTimeMult().modifyMult(ID, bonus);
				Global.getCombatEngine().getTimeMult().modifyMult(ID, 1f / bonus);
				ship.setJitterShields(false);
				ship.setJitterUnder(ship, new Color(0, 178, 255, 191), 0.5f, 10, 20f);
				if (interval.intervalElapsed()) {
					SpriteAPI sprite = ship.getSpriteAPI();
					float offsetX = sprite.getWidth() / 2f - sprite.getCenterX();
					float offsetY = sprite.getHeight() / 2f - sprite.getCenterY();

					float trueOffsetX = (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetX - (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetY;
					float trueOffsetY = (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetX + (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetY;
					MagicRender.battlespace(
							Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
							new Vector2f(ship.getLocation().getX() + trueOffsetX, ship.getLocation().getY() + trueOffsetY),
							new Vector2f(0f, 0f),
							new Vector2f(sprite.getWidth(), sprite.getHeight()),
							new Vector2f(0f, 0f),
							ship.getFacing() - 90f,
							0f,
							new Color(0, 178, 255, 128),
							true,
							0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f,
							CombatEngineLayers.BELOW_SHIPS_LAYER
					);
				}
			} else {
				ship.getMutableStats().getTimeMult().unmodify(ID);
				Global.getCombatEngine().getTimeMult().unmodify(ID);
			}
		}
	}
}
