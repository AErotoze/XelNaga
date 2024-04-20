package data.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;

public class xel_PsionicCrystalArray extends xel_BaseHullmod{

    /*
     * 灵能水晶矩阵
     * 幅能异构化！！！呀哈哈
     * 降低 %25 相位线圈冷却时间
     *
     * 升级分支联动[不兼容]：还是写进applybefore更好
     * 矩阵充能器
     * 星灵能量矩阵——护盾下线时长增加，降低20%护盾受伤
     * 灵能水晶矩阵——异构阈值提高，不再降低相位线圈的冷却时间
     * 控制芯核
     * 星灵能量矩阵——增加全体武器50的基础射程，emp电弧穿盾概率+25%
     * 灵能水晶矩阵——降低受到的EMP伤害，增加战斗中武器和引擎的修复时间
     * 谐振盘
     * 星灵能量矩阵——提高100%护盾灵敏度，降低120度盾角
     * 灵能水晶矩阵——提高15%护甲，增加相位激活幅能
     *
     * 特殊内置：
     * 刚毅护盾：护盾受到的伤害不超过最大辐容的0.5%
     * 复仇协议：根据友方已死亡舰船的部署点，提供永久or暂时的增益
     * 永恒屏障：每次护盾击溃后获得一个镀层，镀层持续时间内获得高额免伤并持续恢复结构
     */
    @Override
    public void init(HullModSpecAPI spec) {
        super.init(spec);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return super.isApplicableToShip(ship);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return super.getUnapplicableReason(ship);
    }


}
