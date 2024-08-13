package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.utils.xel.xel_Misc;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class xel_SythosOxide extends xel_BaseHullmod {
	private static final String DATA_KEY = "xel_SythosOxide_data_key";
	private static final float ZERO_FLUX_BOOST_LEVEL = 50f;
	private static final float DAMAGE_REDUCTION = 25f;
	private static final IntervalUtil interval = new IntervalUtil(1.5f, 1.5f);

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);
		this.setNotCompatible(HullMods.SAFETYOVERRIDES);
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, ZERO_FLUX_BOOST_LEVEL * 0.01f);
		stats.getHullDamageTakenMult().modifyMult(id, 1f - DAMAGE_REDUCTION * 0.01f);
		stats.getShieldDamageTakenMult().modifyMult(id, 1f - DAMAGE_REDUCTION * 0.01f);
		stats.getArmorDamageTakenMult().modifyMult(id, 1f - DAMAGE_REDUCTION * 0.01f);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		removeBlockedMod(ship);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!ship.isAlive()) return;

		interval.advance(amount / ship.getMutableStats().getTimeMult().modified);
		if (interval.intervalElapsed()) {
			SpriteAPI sprite = ship.getSpriteAPI();
			float offsetX = sprite.getWidth() / 2f - sprite.getCenterX();
			float offsetY = sprite.getHeight() / 2f - sprite.getCenterY();

			float trueOffsetX = (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetX - (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetY;
			float trueOffsetY = (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetX + (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetY;

			MagicRender.battlespace(
					Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
					new Vector2f(ship.getLocation().getX() + trueOffsetX, ship.getLocation().getY() + trueOffsetY),
					xel_Misc.V2ZERO,
					new Vector2f(sprite.getWidth(), sprite.getHeight()),
					xel_Misc.V2ZERO,
					ship.getFacing() - 90f,
					0f,
					new Color(255, 255, 255, 204),
					true,
					0f, 0f, 0f, 0f, 0f, 0f, 0f, 4f,
					CombatEngineLayers.BELOW_SHIPS_LAYER
			);
		}
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return (int) ZERO_FLUX_BOOST_LEVEL + "%";
		else if (index == 1) return (int) DAMAGE_REDUCTION + "%";
		else return index == 2 ? getNotCompatibleReason() : super.getDescriptionParam(index, hullSize);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return !hasNotCompatibleMod(ship);
	}
}
