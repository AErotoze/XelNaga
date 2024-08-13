package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.combat.CombatEngine;
import com.sun.prism.image.ViewPort;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class xel_ProtectiveField extends xel_BaseShipSystemScript {
    /*
    防护场（守护者之盾）
    在1200su内展开防护场，任何进入场内的地方射弹、导弹都会损失部分动能，从而导致杀伤力下降（设定罢了）
    场内的敌对射弹弹速降低20%，导弹最大速度降低20%，两者伤害降低15%，不影响光束类武器（但好像能影响dem，alex你都在写些什么啊）
     */
    private static final String DATA_KEY = "xel_ProtectiveField_data_key";
    private static final float SPEED_DECREASE = 20f;
    private static final float DAMAGE_DECREASE = 15f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (getPlayerShip(stats) != null) {
            ShipAPI ship = getPlayerShip(stats);


//            xel_PFRender ren = (xel_PFRender) ship.getCustomData().get(DATA_KEY + "_render");
//            if (ren == null) {
//                ren = new xel_PFRender();
//            }
//            ren.render(CombatEngineLayers.ABOVE_SHIPS_LAYER, Global.getCombatEngine().getViewport());
//            ship.setCustomData(DATA_KEY + "_render", ren);

            render(CombatEngineLayers.ABOVE_SHIPS_LAYER, Global.getCombatEngine().getViewport());
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {

    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return super.getStatusData(index, state, effectLevel);
    }


    public static class xel_PFRender extends BaseCombatLayeredRenderingPlugin {

        xel_PFRender() {
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
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
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
		float x = 0;
		float y = 0f;
		float w = 100;
		float h = 100;
		Color color = Color.cyan;
		float a = 0.25f;
		if (layer == CombatEngineLayers.BELOW_INDICATORS_LAYER) {
			x = 50;
			y = 50;
			w = 100;
			h = 100;
			color = Color.cyan;
		} else if (layer == CombatEngineLayers.BELOW_PHASED_SHIPS_LAYER) {
			x = -50;
			y = 120;
			w = 50;
			h = 50;
			color = new Color(150, 150, 0, 255);
			a = 1f;
		} else if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER) {
			x = -100;
			y = -100;
			w = 200;
			h = 200;
			color = Color.ORANGE;
		}

		float alphaMult = viewport.getAlphaMult();

		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);


		GL11.glColor4ub((byte)color.getRed(),
						(byte)color.getGreen(),
						(byte)color.getBlue(),
						(byte)(color.getAlpha() * alphaMult * a));

		GL11.glBegin(GL11.GL_QUADS);
		{
			GL11.glVertex2f(x, y);
			GL11.glVertex2f(x, y + h);
			GL11.glVertex2f(x + w, y + h);
			GL11.glVertex2f(x + w, y);
		}
		GL11.glEnd();
    }

}
