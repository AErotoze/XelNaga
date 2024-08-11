package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.TimeoutTracker;

import java.util.List;

public class xel_WeaknessAnalysisBeamEffect implements BeamEffectPlugin {
    /*
    被单束光束照射的目标受到的伤害增加6%，两束及两束以上增加10%
     */
    private static final String DATA_KEY = "xel_WeaknessAnalysisBeamEffect_data_key";
    private static final float EFFECT_DUR = 1f;
    private static final float DAMAGE_INCREASE_ONE = 6f;
    private static final float DAMAGE_INCREASE_TWO = 10f;
    private boolean wasZero = true;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();
        if (target instanceof ShipAPI && beam.getBrightness() >= 1f && beam.getWeapon() != null) {
            float dur = beam.getDamage().getDpsDuration();
            if (!wasZero) dur = 0f;
            wasZero = beam.getDamage().getDpsDuration() <= 0f;

            if (dur > 0f) {
                ShipAPI ship = (ShipAPI) target;
                if (!ship.hasListenerOfClass(WeaknessAnalysisBeamDamageTakenMod.class)) {
                    ship.addListener(new WeaknessAnalysisBeamDamageTakenMod(ship));
                }
                List<WeaknessAnalysisBeamDamageTakenMod> listeners = ship.getListeners(WeaknessAnalysisBeamDamageTakenMod.class);
                if (listeners.isEmpty()) return;

                WeaknessAnalysisBeamDamageTakenMod listener = listeners.get(0);
                listener.notifyHit(beam.getWeapon());
            }
        }
    }

    private static class WeaknessAnalysisBeamDamageTakenMod implements AdvanceableListener {
        private final ShipAPI ship;
        private final TimeoutTracker<WeaponAPI> recentHits = new TimeoutTracker<>();

        public WeaknessAnalysisBeamDamageTakenMod(ShipAPI ship) {
            this.ship = ship;
        }

        public void notifyHit(WeaponAPI w) {
            recentHits.add(w, EFFECT_DUR, EFFECT_DUR);
        }

        @Override
        public void advance(float amount) {
            recentHits.advance(amount);

            int beams = recentHits.getItems().size();
            MutableShipStatsAPI stats = ship.getMutableStats();

            float bonus = 0f;
            if (beams == 1) bonus = DAMAGE_INCREASE_ONE;
            else if (beams >= 2) bonus = DAMAGE_INCREASE_TWO;

            if (bonus > 0f) {
                stats.getShieldDamageTakenMult().modifyMult(DATA_KEY + ship.getId(), 1f + bonus * 0.01f);
                stats.getArmorDamageTakenMult().modifyMult(DATA_KEY + ship.getId(), 1f + bonus * 0.01f);
                stats.getHullDamageTakenMult().modifyMult(DATA_KEY + ship.getId(), 1f + bonus * 0.01f);
            } else {
                ship.removeListener(this);
                stats.getShieldDamageTakenMult().unmodify(DATA_KEY + ship.getId());
                stats.getArmorDamageTakenMult().unmodify(DATA_KEY + ship.getId());
                stats.getHullDamageTakenMult().unmodify(DATA_KEY + ship.getId());
            }
        }
    }
}
