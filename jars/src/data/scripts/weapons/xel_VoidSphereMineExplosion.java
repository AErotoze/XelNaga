package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ProximityExplosionEffect;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.utils.xel.xel_VoidSphereExplosionVisual;
import data.utils.xel.xel_VoidSphereExplosionVisual.xel_NEParams;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class xel_VoidSphereMineExplosion implements ProximityExplosionEffect {
    private static String SIZE_MULT_KEY = "core_sizeMultKey";

    @Override
    public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
        Float sizeMult = null;
        if (originalProjectile.getCustomData() != null) {
            sizeMult = (Float) originalProjectile.getCustomData().get(SIZE_MULT_KEY);
        }
        if (sizeMult == null) sizeMult = 1f;

        xel_NEParams p =createStandardRiftParams("xel_VoidSphere_minelayer",25f* sizeMult);
        p.fadeOut = 1f;

//        NegativeExplosionVisual.NEParams param = RiftCascadeMineExplosion.createStandardRiftParams("",25f*sizeMult);
//        param.fadeOut = 1f;

        try {
            spawnStandardRift(explosion, p);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

    }

    private static void spawnStandardRift(DamagingProjectileAPI explosion, xel_NEParams params) throws CloneNotSupportedException {
        CombatEngineAPI engine = Global.getCombatEngine();
        explosion.addDamagedAlready(explosion.getSource());

        CombatEntityAPI prev = null;
        for (int i = 0; i < 2; i++) {
            xel_NEParams p = params.clone();
            p.radius *= 0.75f + 0.5f * (float) Math.random();

            p.withHitGlow = prev == null;

            Vector2f loc = new Vector2f(explosion.getLocation());
            //loc = Misc.getPointWithinRadius(loc, p.radius * 1f);
            loc = Misc.getPointAtRadius(loc, p.radius * 0.4f);

            CombatEntityAPI e = engine.addLayeredRenderingPlugin(new xel_VoidSphereExplosionVisual(p));
            e.getLocation().set(loc);

            if (prev != null) {
                float dist = Misc.getDistance(prev.getLocation(), loc);
                Vector2f vel = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(loc, prev.getLocation()));
                vel.scale(dist / (p.fadeIn + p.fadeOut) * 0.7f);
                e.getVelocity().set(vel);
            }

            prev = e;
        }
    }

    private static xel_NEParams createStandardRiftParams(String minelayerId, float baseRadius){
        Color color = new Color(100,100,255,255);
        Object o = Global.getSettings().getWeaponSpec(minelayerId).getProjectileSpec();
        if (o instanceof MissileSpecAPI) {
            MissileSpecAPI spec = (MissileSpecAPI) o;
            color = spec.getGlowColor();
        }
        xel_NEParams p = createStandardRiftParams(color,baseRadius);
        return p;
    }

    private static xel_NEParams createStandardRiftParams(Color borderColor, float radius) {
        xel_NEParams p = new xel_NEParams();
        //p.radius = 50f;
        p.hitGlowSizeMult = .75f;
        //p.hitGlowSizeMult = .67f;
        p.spawnHitGlowAt = 0f;
        p.noiseMag = 1f;
        //p.fadeIn = 0f;
        //p.fadeOut = 0.25f;

//        p.color = new Color(55, 255, 167);//爆炸烟雾颜色

        //p.hitGlowSizeMult = .75f;
        p.fadeIn = 0.1f;
        //p.noisePeriod = 0.05f;
        p.underglow = RiftCascadeEffect.EXPLOSION_UNDERCOLOR;
        //p.withHitGlow = i == 0;
        p.withHitGlow = true;

        //p.radius = 20f;
        p.radius = radius;
        //p.radius *= 0.75f + 0.5f * (float) Math.random();

        p.color = borderColor;
        return p;
    }
}
