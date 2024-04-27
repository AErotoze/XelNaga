package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.utils.xel.HullModUtil;

public class xel_BaseShipSystemScript extends BaseShipSystemScript {

	public Boolean hasPP(ShipAPI ship) {
		return ship.getVariant().hasHullMod(HullModUtil.XEL_PURIFIERS_PROTOCOL);
	}

	public ShipAPI getPlayerShip(MutableShipStatsAPI stats) {
		ShipAPI player = null;
		if (stats.getEntity() instanceof ShipAPI) {
			player = (ShipAPI) stats.getEntity();
		}
		return player;
	}

	public boolean isPlayerShip(MutableShipStatsAPI stats) {
		return Global.getCombatEngine().getPlayerShip() == getPlayerShip(stats);
	}

	public boolean isPlayerShip(ShipAPI ship) {
		return Global.getCombatEngine().getPlayerShip() == ship;
	}
}
