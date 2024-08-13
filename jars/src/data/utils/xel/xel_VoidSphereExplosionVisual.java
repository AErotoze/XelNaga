package data.utils.xel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Noise;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Arrays;
import java.util.EnumSet;

public class xel_VoidSphereExplosionVisual extends BaseCombatLayeredRenderingPlugin {

    public static class xel_NEParams implements Cloneable {
        public float fadeIn = 0.1f;
        public float fadeOut = 0.5f;
        public float spawnHitGlowAt = 0f;
        public float hitGlowSizeMult = 0.75f;
        public float radius = 20f;
        public float thickness = 25f;
        public float noiseMag = 1f;
        public final float noisePeriod = 0.1f;
        public boolean withHitGlow = true;
        public Color color = new Color(100, 100, 255);
        public Color underglow = RiftCascadeEffect.EXPLOSION_UNDERCOLOR;
        public final Color invertForDarkening = null;

        public xel_NEParams() {
        }

        public xel_NEParams(float radius, float thickness, Color color) {
            super();
            this.radius = radius;
            this.thickness = thickness;
            this.color = color;
        }

        public xel_NEParams clone() throws CloneNotSupportedException {
            try {
                return (xel_NEParams) super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }

    private FaderUtil fader;
    private SpriteAPI atmosphereTex;
    private float[] noise;
    private float[] noise1;
    private float[] noise2;
    private final xel_NEParams p;

    private int segments;
    private float noiseElapsed = 0f;

    private boolean spawnedHitGlow = false;

    public xel_VoidSphereExplosionVisual(xel_NEParams p) {
        this.p = p;
    }

    public float getRenderRadius() {
        return p.radius + 500f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_PARTICLES_LOWER, CombatEngineLayers.ABOVE_PARTICLES);
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) return;

        fader.advance(amount);

        if (p.noiseMag > 0) {
            noiseElapsed += amount;
            if (noiseElapsed > p.noisePeriod) {
                noiseElapsed = 0;
                noise1 = Arrays.copyOf(noise2, noise2.length);
                noise2 = Noise.genNoise(segments, p.noiseMag);
            }
            float f = noiseElapsed / p.noisePeriod;
            for (int i = 0; i < noise.length; i++) {
                float n1 = noise1[i];
                float n2 = noise2[i];
                noise[i] = n1 + (n2 - n1) * f;
            }
        }

        if (!p.withHitGlow) return;

        float glowSpawnAt = 1f;
        if (!spawnedHitGlow && (!fader.isFadingIn() || fader.getBrightness() >= p.spawnHitGlowAt)) {
            float size = Math.min(p.radius * 7f, p.radius + 150f);
            float coreSize = Math.max(size, p.radius * 4f);
            if (coreSize > size) size = coreSize;

            size *= p.hitGlowSizeMult;
            coreSize *= p.hitGlowSizeMult;

            CombatEngineAPI engine = Global.getCombatEngine();
            Vector2f point = entity.getLocation();
            Vector2f vel = entity.getVelocity();
            float dur = fader.getDurationOut() * glowSpawnAt;
            engine.addHitParticle(point, vel, size * 3f, 1f, dur, p.color);
            engine.addHitParticle(point, vel, coreSize * 1.5f, 1f, dur, Color.white);

            Color invert = p.color;
            if (p.invertForDarkening != null) invert = p.invertForDarkening;
            Color c = new Color(255 - invert.getRed(),
                    255 - invert.getGreen(),
                    255 - invert.getBlue(), 127);
            c = Misc.interpolateColor(c, Color.white, 0.4f);
            float durMult = 1f;
            for (int i = 0; i < 7; i++) {
                dur = 4f + 4f * (float) Math.random();
                //dur = p.fadeIn + p.fadeOut + 3f + (float) Math.random() * 2f;
                dur *= durMult;
                dur *= 0.5f;
                //float nSize = size * (1f + 0.0f * (float) Math.random());
                //float nSize = size * (0.75f + 0.5f * (float) Math.random());
                float nSize = size;
                Vector2f pt = Misc.getPointAtRadius(point, nSize * 0.5f);
                Vector2f v = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
                v.scale(nSize + nSize * (float) Math.random() * 0.5f);
                v.scale(0.15f);
                Vector2f.add(vel, v, v);

                v = new Vector2f(entity.getVelocity());
                engine.addNegativeNebulaParticle(pt, v, nSize, 2f,
                        p.fadeIn / dur, 0f, dur, c);
            }

            dur = p.fadeIn + p.fadeOut + 2f;
            dur *= durMult;
            float rampUp = (p.fadeIn + p.fadeOut) / dur;
            rampUp = 0f;

            c = p.underglow;
            for (int i = 0; i < 15; i++) {
                Vector2f loc = new Vector2f(point);
                loc = Misc.getPointWithinRadius(loc, size);
                float s = size * 3f * (0.25f + (float) Math.random() * 0.25f);
                engine.addNebulaParticle(loc, entity.getVelocity(), s, 1.5f, rampUp, 0f, dur, c);
            }
            spawnedHitGlow = true;
        }
    }

    @Override
    public void init(CombatEntityAPI entity) {
        super.init(entity);

        fader = new FaderUtil(0f, p.fadeIn, p.fadeOut);
        fader.setBounceDown(true);
        fader.fadeIn();

        atmosphereTex = Global.getSettings().getSprite("combat", "corona_hard");

        float perSegment = 2f;
        segments = (int) ((p.radius * 2f * 3.14f) / perSegment);
        if (segments < 8) segments = 8;

        noise1 = Noise.genNoise(segments, p.noiseMag);
        noise2 = Noise.genNoise(segments, p.noiseMag);
        noise = Arrays.copyOf(noise1, noise1.length);
    }

    @Override
    public boolean isExpired() {
        return fader.isFadedOut();
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        float x = entity.getLocation().x;
        float y = entity.getLocation().y;

        float f = fader.getBrightness();
        float alphaMult = viewport.getAlphaMult();
        if (f < 0.5f) {
            alphaMult *= f * 2f;
        }

        float r = p.radius;
        float tSmall = p.thickness;

        if (fader.isFadingIn()) {
            r *= 0.75f + Math.sqrt(f) * 0.25f;
        } else {
            r *= 0.1f + 0.9f * f;
            tSmall = Math.min(r, p.thickness);
        }

        if (layer == CombatEngineLayers.ABOVE_PARTICLES_LOWER) {
            float a = 1f;
            renderAtmosphere(x, y, r, tSmall, alphaMult * a, segments, atmosphereTex, noise, p.color, true);
            renderAtmosphere(x, y, r - 2f, tSmall, alphaMult * a, segments, atmosphereTex, noise, p.color, true);
        } else if (layer == CombatEngineLayers.ABOVE_PARTICLES) {
            float circleAlpha = 1f;
            if (alphaMult < 0.5f) {
                circleAlpha = alphaMult * 2f;
            }
            float tCircleBorder = 1f;
            renderCircle(x, y, r, circleAlpha, segments, new Color(255,255, 255,155)); //
            renderAtmosphere(x, y, r, tCircleBorder, circleAlpha, segments, atmosphereTex, noise, Color.black, false);//
        }
    }

    private void renderCircle(float x, float y, float radius, float alphaMult, int segments, Color color) {
        if (fader.isFadingIn()) alphaMult = 1f;

        float startRad = (float) Math.toRadians(0);
        float endRad = (float) Math.toRadians(360);
        float spanRad = Misc.normalizeAngle(endRad - startRad);
        float anglePerSegment = spanRad / segments;

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glRotatef(0, 0, 0, 1);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);


        GL11.glColor4ub((byte) color.getRed(),
                (byte) color.getGreen(),
                (byte) color.getBlue(),
                (byte) ((float) color.getAlpha() * alphaMult));

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(0, 0);
        for (float i = 0; i < segments + 1; i++) {
            boolean last = i == segments;
            if (last) i = 0;
            float theta = anglePerSegment * i;
            float cos = (float) Math.cos(theta);
            float sin = (float) Math.sin(theta);

            float m1 = 0.75f + 0.65f * noise[(int) i];
            if (p.noiseMag <= 0) {
                m1 = 1f;
            }

            float x1 = cos * radius * m1;
            float y1 = sin * radius * m1;

            GL11.glVertex2f(x1, y1);

            if (last) break;
        }


        GL11.glEnd();
        GL11.glPopMatrix();

    }


    private void renderAtmosphere(float x, float y, float radius, float thickness, float alphaMult, int segments, SpriteAPI tex, float[] noise, Color color, boolean additive) {

        float startRad = (float) Math.toRadians(0);
        float endRad = (float) Math.toRadians(360);
        float spanRad = Misc.normalizeAngle(endRad - startRad);
        float anglePerSegment = spanRad / segments;

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glRotatef(0, 0, 0, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        tex.bindTexture();

        GL11.glEnable(GL11.GL_BLEND);
        if (additive) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        } else {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        GL11.glColor4ub((byte) color.getRed(),
                (byte) color.getGreen(),
                (byte) color.getBlue(),
                (byte) ((float) color.getAlpha() * alphaMult));
        float texX = 0f;
        float incr = 1f / segments;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (float i = 0; i < segments + 1; i++) {
            boolean last = i == segments;
            if (last) i = 0;
            float theta = anglePerSegment * i;
            float cos = (float) Math.cos(theta);
            float sin = (float) Math.sin(theta);

            float m1 = 0.75f + 0.65f * noise[(int) i];
            float m2 = m1;
            if (p.noiseMag <= 0) {
                m1 = 1f;
                m2 = 1f;
            }

            float x1 = cos * radius * m1;
            float y1 = sin * radius * m1;
            float x2 = cos * (radius + thickness * m2);
            float y2 = sin * (radius + thickness * m2);

            GL11.glTexCoord2f(0.5f, 0.05f);
            GL11.glVertex2f(x1, y1);

            GL11.glTexCoord2f(0.5f, 0.95f);
            GL11.glVertex2f(x2, y2);

            texX += incr;
            if (last) break;
        }

        GL11.glEnd();
        GL11.glPopMatrix();
    }

}
