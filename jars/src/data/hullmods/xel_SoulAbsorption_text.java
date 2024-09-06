package data.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;

public class xel_SoulAbsorption_text extends xel_BaseHullmod {
    //描述船插，还会阻止你上一些奇奇怪怪的船插


    @Override
    public void init(HullModSpecAPI spec) {
        super.init(spec);
        this.setNotCompatible(HullMods.MAGAZINES, HullMods.MISSLERACKS);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        removeBlockedMod(ship);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "1600su";
        else if (index == 1) return "1/2/4/6/10";
        else if (index == 2) return "1";
        else if (index == 3) return "0.15%";
        else if (index == 4) return "1";
        else if (index == 5) return "100";
        else return index == 6 ? getNotCompatibleReason() : null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return !hasNotCompatibleMod(ship);
    }

}
