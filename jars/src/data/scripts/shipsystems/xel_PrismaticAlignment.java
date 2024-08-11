package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;

import static data.utils.xel.Constants.i18n_shipSystem;

public class xel_PrismaticAlignment extends xel_BaseShipSystemScript {
	/*
		棱镜校准[无升级]
		提高能量武器伤害和射程，持续10s，冷却20s
	 */

    private static final String DATA_KEY = "xel_PrismaticAlignment_data_key";
    private static final float DAMAGE_INCREASE = 20f;
    private static final float WEAPON_RANGE_INCREASE = 100f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        stats.getEnergyWeaponDamageMult().modifyMult(DATA_KEY + id, 1f + DAMAGE_INCREASE * 0.01f * effectLevel);
        stats.getEnergyWeaponRangeBonus().modifyFlat(DATA_KEY + id, WEAPON_RANGE_INCREASE * effectLevel);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getEnergyWeaponDamageMult().unmodify(DATA_KEY + id);
        stats.getEnergyWeaponRangeBonus().unmodify(DATA_KEY + id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0)
            return new StatusData(i18n_shipSystem.format("xel_PA_active1", (int) (DAMAGE_INCREASE * effectLevel)), false);
        if (index == 1)
            return new StatusData(i18n_shipSystem.format("xel_PA_active2", (int) (WEAPON_RANGE_INCREASE * effectLevel)), false);
        return null;
    }
}
