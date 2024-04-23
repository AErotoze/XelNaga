package data.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;

public class xel_PsionicCrystalArray extends xel_BaseHullmod {

    /*
     * 灵能水晶矩阵
     *
     * 特殊内置：
     * 刚毅护盾：护盾受到的伤害不超过最大辐容的0.5%
     * 复仇协议：根据友方已死亡舰船的部署点，提供永久or暂时的增益
     * 永恒屏障：每次护盾击溃后获得一个镀层，镀层持续时间内获得高额免伤并持续恢复结构
     */

    //    相位无需维持，1秒冷却时间，进入相位需要25%硬幅能
    //    相位不会导致最高航速下降
    //    相位有专属能量条可维持10s
    //    10x3 秒恢复完毕
    //    不兼容所有原版相位插：自适应线圈，相位锚点
    //
    //    矩阵充能器——能量恢复速度增加至 10x2 秒恢复完毕
    //    控制芯核——进入相位需要的硬幅能降低10%，相位期间武器冷却速度、装填速度增加
    //    谐振盘——退出相位1s内（实际上需要 +0.5s 以抵消chargeDown），装甲减伤效果
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
