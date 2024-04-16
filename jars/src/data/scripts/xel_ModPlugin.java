package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import data.scripts.world.xel_WorldGen;

public class xel_ModPlugin extends BaseModPlugin {
    @Override
    public void onNewGame() {
        new xel_WorldGen().generate(Global.getSector());
    }

    @Override
    public void onApplicationLoad() throws Exception {
    }
}
