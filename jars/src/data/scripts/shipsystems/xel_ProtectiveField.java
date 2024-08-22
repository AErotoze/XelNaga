package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.sun.prism.image.ViewPort;
import data.utils.xel.xel_Misc;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import javax.swing.*;
import javax.swing.plaf.ViewportUI;
import java.awt.*;
import java.util.EnumSet;

import static data.utils.xel.Constants.i18n_shipSystem;

public class xel_ProtectiveField extends xel_BaseShipSystemScript {
    /*
    防护场（守护者之盾）
    在1000su内展开防护场，任何进入场内的地方射弹、导弹都会损失部分动能，从而导致杀伤力下降（设定罢了）
    场内的敌对射弹弹速降低20%，导弹最大速度降低20%，两者伤害降低15%，不影响光束类武器（但好像能影响dem，alex你都在写些什么啊）
     */
    private static final String DATA_KEY = "xel_ProtectiveField_data_key";
    private static final float RANGE = 1000f;
    private static final float SPEED_DECREASE = 20f;
    private static final float DAMAGE_DECREASE = 15f;
    private static final IntervalUtil interval = new IntervalUtil(0.033f, 0.033f);
    private boolean isStared = true;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (getPlayerShip(stats) != null && !engine.isPaused()) {
            interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
            if (interval.intervalElapsed() || isStared) {
                ShipAPI ship = getPlayerShip(stats);

                for (DamagingProjectileAPI projectile : engine.getProjectiles()) {
                    if (Misc.getDistance(projectile.getLocation(), ship.getLocation()) > RANGE) continue;
                    if (projectile.getSource() != null && projectile.getSource().getOwner() == 0) continue;

                    Boolean done = (Boolean) projectile.getCustomData().get(DATA_KEY + "_done");
                    if (done == null) {
                        done = false;
                    }

                    if (!done) {
                        projectile.setCustomData(DATA_KEY + "_done", true);
                        Vector2f speed = projectile.getVelocity();
                        speed.scale(1f - SPEED_DECREASE * 0.01f);//降速
                        projectile.getDamage().getModifier().modifyMult(DATA_KEY, 1f - DAMAGE_DECREASE * 0.01f);//减低伤害
                    }
                }
                isStared = false;
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        isStared = true;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return index == 0f ? new StatusData(i18n_shipSystem.get("xel_PF_active"), false) : null;
    }
}
