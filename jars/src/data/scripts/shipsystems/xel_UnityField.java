package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.utils.xel.xel_Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static data.utils.xel.Constants.i18n_shipSystem;

public class xel_UnityField extends BaseShipSystemScript {
    /*
     * 统合力场
     * 根据母舰的战机总存活数量，提供一个暂时性的镀层
     * +镀层生命值/战机 拦截机+5 战斗机+8 轰炸机+10
     * 净化者协议： Unity Array Overload
     * 暂时给战机的护盾 +15% 抵抗力
     * 没有护盾的战机对装甲和结构 +25% 抵抗力
     */
    private static final String FIELD_KEY = "xel_UF_Field_Manager";
    private static final Map<Integer, Float> mag = new HashMap<>();
    private static final float SHIELD_DAMAGE_TAKEN_DECEASED = 0.85f;
    private static final float DAMAGE_TAKEN_DECEASED = 0.75f;

    static {
        mag.put(0, 0f); // 不好好写tag你就等着吧
        mag.put(1, 15f); // 原定数值太低，这个比较合适
        mag.put(2, 20f);
        mag.put(3, 30f);
    }

    private boolean isCounted = false;

    private static int getWingClass(FighterWingAPI wing) {
        if (wing.getSpec().getTags().contains("interceptor")) {
            return 1;
        } else if (wing.getSpec().getTags().contains("fighter")) {
            return 2;
        } else if (wing.getSpec().getTags().contains("bomber")) {
            return 3;
        } else return 0;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        if (effectLevel > 0f) {
            float totalField = 0f;

            for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                if (bay.getWing() == null) continue;
                if (!isCounted) {
                    for (FighterLaunchBayAPI launchBay : ship.getLaunchBaysCopy()) {
                        if (launchBay.getWing() == null) continue;
                        totalField += mag.get(getWingClass(launchBay.getWing())) * (float) launchBay.getWing().getSpec().getNumFighters();
                    }
                    isCounted = true;
                }
            }
            for (ShipAPI fighter : getFighters(ship)) {
                if (fighter.isHulk()) continue;
                if (fighter.hasListenerOfClass(FieldManager.class)) continue;
                if (xel_Misc.hasPurifiersProtocol(ship)) {
                    MutableShipStatsAPI fstats = fighter.getMutableStats();
                    if (fighter.getShield() != null) {
                        fstats.getShieldDamageTakenMult().modifyMult(id, SHIELD_DAMAGE_TAKEN_DECEASED);
                    } else {
                        fstats.getArmorDamageTakenMult().modifyMult(id, DAMAGE_TAKEN_DECEASED);
                        fstats.getHullDamageTakenMult().modifyMult(id, DAMAGE_TAKEN_DECEASED);
                    }
                    totalField *= 1.5f;
                }
                fighter.addListener(new FieldManager(totalField, fighter));
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        isCounted = false;
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        for (ShipAPI fighter : getFighters(ship)) {
            if (fighter.isHulk()) continue;
            if (fighter.hasListenerOfClass(FieldManager.class)) {
                fighter.removeListenerOfClass(FieldManager.class);
            }
            if (xel_Misc.hasPurifiersProtocol(ship)) {
                MutableShipStatsAPI fstats = fighter.getMutableStats();
                if (fighter.getShield() != null) {
                    fstats.getShieldDamageTakenMult().unmodify(id);
                } else {
                    fstats.getArmorDamageTakenMult().unmodify(id);
                    fstats.getHullDamageTakenMult().unmodify(id);
                }
            }
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) return new StatusData(i18n_shipSystem.get("xel_UF_active"), false);
        return super.getStatusData(index, state, effectLevel);
    }

    private java.util.List<ShipAPI> getFighters(ShipAPI carrier) {
        List<ShipAPI> result = new ArrayList<ShipAPI>();

//		this didn't catch fighters returning for refit
//		for (FighterLaunchBayAPI bay : carrier.getLaunchBaysCopy()) {
//			if (bay.getWing() == null) continue;
//			result.addAll(bay.getWing().getWingMembers());
//		}

        for (ShipAPI ship : Global.getCombatEngine().getShips()) {
            if (!ship.isFighter()) continue;
            if (ship.getWing() == null) continue;
            if (ship.getWing().getSourceShip() == carrier) {
                result.add(ship);
            }
        }

        return result;
    }

    private static class FieldManager implements DamageTakenModifier, AdvanceableListener {
        private final ShipAPI fighter;
        private float field;
        private float effectLevel = 0f;

        private Color jitterColor = new Color(0, 255, 100, 64);
        private Color jitterUnderColor = new Color(0, 255, 100, 128);


        public FieldManager(float field, ShipAPI fighter) {
            this.field = field;
            this.fighter = fighter;
            if (xel_Misc.hasPurifiersProtocol(this.fighter.getWing().getSourceShip())) {
                this.jitterColor = new Color(254, 138, 14, 64);
                this.jitterUnderColor = new Color(254, 138, 14, 128);
            }
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!shieldHit && field > 0f) {
                float finalDamage = damage.getDamage();
                fighter.setJitterShields(false);
                if (field >= finalDamage) {
                    field -= finalDamage;
                    damage.getModifier().modifyMult(FIELD_KEY, 0f);
                    fighter.setJitter(fighter, jitterColor, effectLevel, 4, 10f);
                } else {
                    float damageDecease = 1f - (finalDamage - field) / finalDamage;
                    field = 0;
                    damage.getModifier().modifyMult(FIELD_KEY, damageDecease);
                    fighter.setJitter(fighter, jitterColor, effectLevel, 8, 20f);
                }
                return FIELD_KEY;
            }
            return null;
        }

        @Override
        public void advance(float amount) {
            if (field <= 0f || !fighter.isAlive()) return;
            effectLevel = fighter.getWing().getSourceShip().getSystem().getEffectLevel();
            fighter.setJitterShields(false);
            fighter.setJitter(fighter, jitterColor, effectLevel, 2, 5f);
            fighter.setJitterUnder(fighter, jitterUnderColor, effectLevel, 5, 5f);
        }
    }
}
