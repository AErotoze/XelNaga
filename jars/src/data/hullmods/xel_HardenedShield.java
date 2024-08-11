package data.hullmods;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.lwjgl.util.vector.Vector2f;

public class xel_HardenedShield extends xel_BaseHullmod {
	private static final String DATA_KEY = "xel_HardenedShield_data_key";
	private static final float MAX_DAMAGE_TAKEN = 1f;

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);
		this.setNotCompatible(HullMods.HARDENED_SHIELDS, HullMods.STABILIZEDSHIELDEMITTER);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		removeBlockedMod(ship);
		if (!ship.hasListenerOfClass(damageTakenBuff.class)) {
			ship.addListener(new damageTakenBuff(ship));
		}
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return String.format("%.1f", MAX_DAMAGE_TAKEN) + "%";
		return index == 1 ? getNotCompatibleReason() : super.getDescriptionParam(index, hullSize);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return !hasNotCompatibleMod(ship);
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		return getNotCompatibleReason();
	}

	private static class damageTakenBuff implements DamageTakenModifier {
		private final ShipAPI ship;

		public damageTakenBuff(ShipAPI ship) {
			this.ship = ship;
		}

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			if (shieldHit) {
				float damageValue = damage.getDamage();
				float damageThreshold = ship.getMaxFlux() * MAX_DAMAGE_TAKEN * 0.01f;
				if (damage.isDps()) {
					damageValue *= damage.getDpsDuration();
				}

				if (damageValue > damageThreshold) {
					if (damage.getType() == DamageType.HIGH_EXPLOSIVE) damageThreshold *= 2f;
					if (damage.getType() == DamageType.KINETIC) damageThreshold *= 0.5f;
					if (damage.getType() == DamageType.FRAGMENTATION) damageThreshold *= 4f;
					damage.setDamage(damageThreshold);

/*
					damage.getModifier().modifyMult(DATA_KEY + "_damageTakenBuff_key" + ship.getId(), 0f);
					Global.getCombatEngine().addFloatingDamageText(point, damageThreshold, Misc.getNegativeHighlightColor(), ship, damage.getStats().getEntity());
					ship.getFluxTracker().increaseFlux(damageThreshold, true);
*/
                    return DATA_KEY + "_damageTakenBuff_key" + ship.getId();
				}
			}
			return null;
		}
	}
}
