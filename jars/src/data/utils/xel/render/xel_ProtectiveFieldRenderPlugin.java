package data.utils.xel.render;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class xel_ProtectiveFieldRenderPlugin implements CombatLayeredRenderingPlugin {
    protected CombatEngineLayers layer = CombatEngineLayers.BELOW_INDICATORS_LAYER;
    protected CombatEntityAPI entity;

    public xel_ProtectiveFieldRenderPlugin() {
        super();
    }

    public xel_ProtectiveFieldRenderPlugin(CombatEngineLayers layer) {
        this.layer = layer;
    }

    @Override
    public void init(CombatEntityAPI entity) {

    }

    @Override
    public void cleanup() {

    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(layer);
    }

    @Override
    public float getRenderRadius() {
        return 100f;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
            if (Global.getCombatEngine().getPlayerShip() == null) return;
            ShipAPI ship = Global.getCombatEngine().getPlayerShip();
            SpriteAPI sprite = ship.getSpriteAPI();

            Vector2f location = ship.getLocation();
            Vector2f size = new Vector2f(sprite.getHeight() * 0.5f, sprite.getWidth() * 0.5f);
            size = new Vector2f(sprite.getHeight() * 0.5f, sprite.getWidth() * 0.5f);

            // 考虑到只能使用OpenGL1.5的远古设备不支持NPOT纹理，原版的纹理载入是设定为留空至2的n次幂尺寸的，所以通常不能直接取1
            // 至于这些设备能不能启动Windows XP/7/10/11，这里不做讨论
            // 这里不考虑这点，直接将uv坐标取全
            Vector2f uv = new Vector2f(1.0f, 1.0f);
            float facing = ship.getFacing() - 90f;

            // 开始进行OpenGL绘制，由于改变了矩阵状态，需要对当前矩阵进行保存
            GL11.glPushMatrix();
            // 依次应用变换矩阵
            GL11.glTranslatef(location.x, location.y, 0.0f);
            GL11.glRotatef(facing, 0.0f, 0.0f, 1.0f);

            // 由于游戏问题，此处可以不保存属性，但不要养成这种坏习惯
            // 此处需打开 GL_TEXTURE_2D 以使用2D纹理
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            // 纹理单元0是默认打开的，此时可以不使用该方法
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
            // 可选其一
            //sprite.bindTexture();

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // 对应为 (255, 0, 0, 127)
            GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.5f);

            // 指示开始绘制矩形
            GL11.glBegin(GL11.GL_QUADS);
            // 依次设定顶点，以及UV坐标
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex2f(-size.x, -size.y);
            GL11.glTexCoord2f(0.0f, uv.y);
            GL11.glVertex2f(-size.x, size.y);
            GL11.glTexCoord2f(uv.x, uv.y);
            GL11.glVertex2f(size.x, size.y);
            GL11.glTexCoord2f(uv.x, 0.0f);
            GL11.glVertex2f(size.x, -size.y);
            // 指示绘制结束
            GL11.glEnd();
            // 释放当前GL上下文绑定的 GL_TEXTURE_2D 对象
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            // 释放保存的矩阵，执行其他渲染
            GL11.glPopMatrix();
        }
    }

    public void render_yuzi1(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
            if (Global.getCombatEngine().getPlayerShip() == null) return;
            ShipAPI ship = Global.getCombatEngine().getPlayerShip();
            SpriteAPI sprite = ship.getSpriteAPI();

            Vector2f location = ship.getLocation();
            Vector2f size = new Vector2f(sprite.getHeight() * 0.5f, sprite.getWidth() * 0.5f);
            float facing = ship.getFacing();

            // 开始进行OpenGL绘制，由于改变了矩阵状态，需要对当前矩阵进行保存
            GL11.glPushMatrix();
            // 依次应用变换矩阵
            GL11.glTranslatef(location.x, location.y, 0.0f);
            GL11.glRotatef(facing, 0.0f, 0.0f, 1.0f);

            // 由于游戏问题，此处可以不保存属性，但不要养成这种坏习惯
            // 此处由于防止受先前其他原版或Mod渲染的影响，需关闭 GL_TEXTURE_2D
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // 对应为 (255, 0, 0, 127)
            GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.5f);

            // 指示开始绘制矩形
            GL11.glBegin(GL11.GL_QUADS);
            // 依次设定顶点坐标
            GL11.glVertex2f(-size.x, -size.y);
            GL11.glVertex2f(-size.x, size.y);
            GL11.glVertex2f(size.x, size.y);
            GL11.glVertex2f(size.x, -size.y);
            // 指示绘制结束
            GL11.glEnd();
            // 释放保存的矩阵，执行其他渲染
            GL11.glPopMatrix();
        }
    }

    public void render_yuzi2(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
            if (Global.getCombatEngine().getPlayerShip() == null) return;
            ShipAPI ship = Global.getCombatEngine().getPlayerShip();
            SpriteAPI sprite = ship.getSpriteAPI();

            Vector2f location = ship.getLocation();
            Vector2f size = new Vector2f(sprite.getHeight() * 0.5f, sprite.getWidth() * 0.5f);
            size = new Vector2f(-0.25f, 0.25f);

            // 考虑到只能使用OpenGL1.5的远古设备不支持NPOT纹理，原版的纹理载入是设定为留空至2的n次幂尺寸的，所以通常不能直接取1
            // 至于这些设备能不能启动Windows XP/7/10/11，这里不做讨论
            // 这里不考虑这点，直接将uv坐标取全
            Vector2f uv = new Vector2f(1.0f, 1.0f);
            float facing = ship.getFacing();

            // 开始进行OpenGL绘制，由于改变了矩阵状态，需要对当前矩阵进行保存
            GL11.glPushMatrix();
            // 依次应用变换矩阵
//            GL11.glTranslatef(location.x, location.y, 0.0f);
            GL11.glRotatef(facing, 0.0f, 0.0f, 1.0f);

            // 由于游戏问题，此处可以不保存属性，但不要养成这种坏习惯
            // 此处需打开 GL_TEXTURE_2D 以使用2D纹理
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            // 纹理单元0是默认打开的，此时可以不使用该方法
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
            // 可选其一
            //sprite.bindTexture();

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // 对应为 (255, 0, 0, 127)
            GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.5f);

            // 指示开始绘制矩形
            GL11.glBegin(GL11.GL_QUADS);
            // 依次设定顶点，以及UV坐标
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex2f(-size.x, -size.y);
            GL11.glTexCoord2f(0.0f, uv.y);
            GL11.glVertex2f(-size.x, size.y);
            GL11.glTexCoord2f(uv.x, uv.y);
            GL11.glVertex2f(size.x, size.y);
            GL11.glTexCoord2f(uv.x, 0.0f);
            GL11.glVertex2f(size.x, -size.y);
            // 指示绘制结束
            GL11.glEnd();
            // 释放当前GL上下文绑定的 GL_TEXTURE_2D 对象
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            // 释放保存的矩阵，执行其他渲染
            GL11.glPopMatrix();
        }
    }

    public void render1(CombatEngineLayers layer, ViewportAPI viewport) {
        if (Global.getCombatEngine().getPlayerShip() == null) return;

        ShipAPI ship = Global.getCombatEngine().getPlayerShip();
        SpriteAPI sprite = ship.getSpriteAPI();
        Vector2f size = new Vector2f(sprite.getHeight() * 0.5f, sprite.getWidth() * 0.5f);
        float facing = ship.getFacing();

        Color color = Color.red;
        float alphaMult = viewport.getAlphaMult();
        float a = 0.25f;
        GL11.glPushMatrix();

        GL11.glScalef(1f / viewport.getVisibleHeight(), 1f / viewport.getVisibleWidth(), 0f);
        GL11.glRotatef(facing - 90f, 0f, 0f, 1f);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) (color.getAlpha() * alphaMult * a));
        GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glVertex2f(-size.x, -size.y);
            GL11.glVertex2f(-size.x, size.y);
            GL11.glVertex2f(size.x, size.y);
            GL11.glVertex2f(size.x, -size.y);
        }
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    public void render2(CombatEngineLayers layers, ViewportAPI viewportAPI) {
        //我也要披上一块红色小披风
        if (Global.getCombatEngine().getPlayerShip() != null) {
            ShipAPI ship = Global.getCombatEngine().getPlayerShip();
            Vector2f size = new Vector2f(ship.getSpriteAPI().getHeight() * 0.5f, ship.getSpriteAPI().getWidth() * 0.5f);
            Color color = Color.red;
            float alphaMult = viewportAPI.getAlphaMult(), a = 0.25f;

            GL11.glPushMatrix();
            {
                GL11.glTranslatef(1f, 1f, 0f);
                GL11.glScalef(1f, 1f, 0f);
                GL11.glRotatef(1f, 0f, 0f, 1f);

                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) (color.getAlpha() * alphaMult * a));
                GL11.glBegin(GL11.GL_QUADS);
                {
                    GL11.glVertex2f(-size.x, -size.y);
                    GL11.glVertex2f(-size.x, size.y);
                    GL11.glVertex2f(size.x, size.y);
                    GL11.glVertex2f(size.x, -size.y);
                }
                GL11.glEnd();
            }
            GL11.glPopMatrix();

        }
    }
}
