package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.weapons.ai.xel_SoulAI;
import data.scripts.world.xel_WorldGen;

public class xel_ModPlugin extends BaseModPlugin {
    @Override
    public void onNewGame() {
        new xel_WorldGen().generate(Global.getSector());
    }

    @Override
    public void onApplicationLoad() throws Exception {
    }

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        if (missile.getProjectileSpecId().startsWith("xel_Soul")) {
            return new PluginPick<MissileAIPlugin>(new xel_SoulAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SET);
        }

        return super.pickMissileAI(missile, launchingShip);
    }
}
