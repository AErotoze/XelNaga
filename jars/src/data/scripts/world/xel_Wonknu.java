package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.CoronalTapParticleScript;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static data.scripts.world.xel_WorldGen.addPlanetCondition;

/*
 * 翁克努 [Wonknu]星系
 * 恒星：蓝巨星[Wonknu]
 * 基于人灰、地改环境下，绕[Wnoknu]行星及卫星设定：
 * 行星：北落师门{类地} 宜居、温和气候、恒星镜阵列、食物+2 矿物+1 有机物+2 建造——农业、轻工业、贸易中心、度假中心 开启星冕分流器后+破碎采矿业
 *
 * 行星：天大将军{火山} 极端炎热、稀薄大气层、极端构造活动性、矿物+3、稀矿+3、巨大废墟 建造——t4分选矿业、最高指挥部、t3军用重工业、遗迹挖掘/人之领遗迹工厂
 *
 * 行星：天狼星座{气态巨行星} 高重力、极端气候、挥发物+2 拍一个挥发物空间站——t2挥发物采矿专精、燃料制造、t3民用重工业、贸易中心 开启星冕分流器后+t3矿物冶炼业
 *
 * 卫星：天狼星座-I 天鹰座{荒芜-轰击} 无大气层、矿物-1、稀矿-1、低重力 建造——t4稀矿冶炼业、燃料制造、t4臭鼬工厂、贸易中心
 *
 * 卫星：天狼星座-II 天琴座{荒芜} 无大气层、矿物+0、低重力 建造——t4矿物冶炼业、燃料制造、t4在轨舰队中心、贸易中心
 *
 * 行星：船帆座{荒芜-沙漠} 稀薄大气层、寒冷、矿物+0、稀矿-1、有机物+2 建造——t4升华矿业、t4在轨舰队中心、智能加工中心
 *
 * 行星：船底座{冰火山} 极端寒冷、构造活动性、矿物+2、稀矿+2、挥发物+2 建造——t4升华矿业、t4臭鼬工厂、智能加工中心
 *
 * 行星：船尾座{冰冻} 极端寒冷、昏暗、矿物+0、稀矿+0、挥发物-1、巨大废墟 建造——遗迹挖掘/人之领遗迹工厂、彼岸工程、超级计算机、星穹工厂
 *
 * 行星：紫微垣{冰巨星} 高重力、极端寒冷、黑暗、极端气候、挥发物+0
 *
 * 卫星：紫微垣-I 太微垣{辐射} 辐射、黑暗、极端寒冷、矿物+0、稀矿-1
 *
 * 卫星：紫微垣-II 天市垣{剧毒} 剧毒大气层、黑暗、极端寒冷、矿物+1、稀矿+1、有机物+1、挥发物+1
 *
 * 小行星带：开阳带 武曲带
 * 行星星环：天狼星座[天狼星座带]
 * 实体：人之领通讯、传感、信标，休眠星门
 * 尝试：加入星冕分流器、人之领低温休眠舱
 * 空间站：天大将军[玉门站]
 *
 * 人之领灰烬规划：破碎保证有油气；保证有升华挖油气；保证稳定产出金属和稀有金属；保证有1星球上冷冻舱爽爽赚钱；
 * 农业宜居规划：农业，轻工业，贸易中心，度假中心（净水处理厂？）
 * 军事工业规划：重工、高指、矿物（人灰1分3——破碎、分选、升华，通常来说，火山+3双矿，沙漠类、宜居类有绿油，冰火山、气态有气）
 * 人灰升华2选1矿球：油气2选一，造升华用，
 * 人灰分选矿球：需要无大气层或空间站上催化核心，双矿应该有要求，最好+3
 * 气球：气态巨行星上发电机，不升级破碎
 * 额外产业：彼岸工程，超级计算机，物流港口，寰宇学院，星穹工厂
 *
 *
 *
 */
public class xel_Wonknu {
    public void generate(SectorAPI sector) {
        // 新建一个恒星系
        StarSystemAPI system = sector.createStarSystem("翁克努 [Wonknu]");
        system.getLocation().set(-8000f, 2500f); // 设定星系坐标，可重复
        system.setBackgroundTextureFilename("graphics/backgrounds/background5.jpg");// 选择进入星系后加载的背景图像

        // 设置处于中心的恒星
        PlanetAPI W_star = system.initStar("wonknu", StarTypes.BLUE_GIANT, 1500f, 500f, 2f, 1f, 4f);
        // 设置星系渲染光颜色
        system.setLightColor(new Color(185, 185, 255));

        // 设置星系中的行星
        PlanetAPI wonknu_i = system.addPlanet("wonknu_i", W_star, "天大将军", Planets.PLANET_LAVA, 180f, 180f,
                4200f,
                224f);
        PlanetAPI wonknu_ii = system.addPlanet("wonknu_ii", W_star, "北落师门", "terran-eccentric", 90f, 200f,
                6000f,
                365f);
        PlanetAPI wonknu_iii = system.addPlanet("Wonknu_iii", W_star, "天狼星座", "gas_giant", 270f, 420f, 9000f,
                378f);
        PlanetAPI wonknu_iii_a = system.addPlanet("wonknu_iii_a", wonknu_iii, "天鹰座", "barren", 0f,
                50f,
                wonknu_iii.getRadius() + 120f + 350f,
                190f);
        PlanetAPI wonknu_iii_b = system.addPlanet("wonknu_iii_b", wonknu_iii, "天琴座", "barren-bombarded", 45f,
                80f,
                wonknu_iii.getRadius() + 80f + 800f,
                140f);
        PlanetAPI wonknu_iv = system.addPlanet("wonknu_iv", W_star, "船帆座", "barren-desert", 135f, 170f, 12000f,
                599f);
        PlanetAPI wonknu_v = system.addPlanet("wonknun_v", W_star, "船尾座", "frozen", 85f, 80f, 12490f,
                300f);
        PlanetAPI wonknu_vi = system.addPlanet("wonknu_vi", W_star, "船底座", "cryovolcanic", 90f, 65f, 13490f,
                300f);
        PlanetAPI wonknu_vii = system.addPlanet("wonknu_vii", W_star, "紫微垣", "ice_giant", 90f, 500f, 18000f,
                1460f);
        PlanetAPI wonknu_vii_a = system.addPlanet("wonknu_vii_a", wonknu_vii, "太微垣", "irradiated", 90f, 70f,
                wonknu_vii.getRadius() + 70f + 444f, 144f);
        PlanetAPI wonknu_vii_b = system.addPlanet("wonknu_vii_b", wonknu_vii, "天市垣", "toxic_cold", 90f, 110f,
                wonknu_vii.getRadius() + 110f + 1444f, 88f);

        // 小行星带：开阳带[北落师门-天狼星座 6000f-9000f] 武曲带[天狼星座-船帆座 9000f-12000f]
        // addAsteroidBelt(SectorEntityToken focus, int numAsteroids, float orbitRadius,
        // float width, float minOrbitDays, float maxOrbitDays, String terrainId, String
        // optionalName)
        system.addAsteroidBelt(W_star, 40, 7300f, 200f, 300f, 600f, Terrain.ASTEROID_BELT, null);
        system.addAsteroidBelt(W_star, 50, 7500f, 200f, 300f, 600f, Terrain.ASTEROID_BELT, null);
        system.addAsteroidBelt(W_star, 60, 7700f, 200f, 300f, 600f, Terrain.ASTEROID_BELT, null);
        // addRingBand(SectorEntityToken focus, String category, String key,
        // float bandWidthInTexture, int bandIndex, Color color,
        // float bandWidthInEngine, float middleRadius, float orbitDays);
        system.addRingBand(W_star, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 7400f, 420f);
        system.addRingBand(W_star, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 7500f, 450f);
        system.addRingBand(W_star, "misc", "rings_dust0", 256f, 2, Color.white, 256f, 7600f, 480f);
        system.addRingBand(W_star, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, 7500, 400f);

        SectorEntityToken ring = system.addTerrain(Terrain.RING,
                new RingParams(400f + 256f, 7500, null, "开阳-小行星带"));
        ring.setCircularOrbit(W_star, 0, 0, 100f);

        system.addAsteroidBelt(W_star, 80, 10400f, 300f, 400f, 800f, Terrain.ASTEROID_BELT, null);
        system.addAsteroidBelt(W_star, 100, 10700f, 300f, 400f, 800f, Terrain.ASTEROID_BELT, null);
        system.addAsteroidBelt(W_star, 120, 11000f, 300f, 400f, 800f, Terrain.ASTEROID_BELT, null);
        system.addRingBand(W_star, "misc", "rings_ice0", 256f, 0, Color.white, 256f, 10550f, 600f);
        system.addRingBand(W_star, "misc", "rings_ice0", 256f, 2, Color.white, 256f, 10650f, 630f);
        system.addRingBand(W_star, "misc", "rings_ice0", 256f, 1, Color.white, 256f, 10750f, 660f);
        system.addRingBand(W_star, "misc", "rings_ice0", 256f, 3, Color.white, 256f, 10850f, 690f);

        ring = system.addTerrain(Terrain.RING, new RingParams(300f + 256f, 10700f, null, "武曲-小行星带"));
        ring.setCircularOrbit(W_star, 0, 0, 100f);

        // 行星星环：紫微垣[紫微垣-行星环 350-800]
        system.addAsteroidBelt(wonknu_vii, 20, wonknu_vii.getRadius() + 100f + 888f, 100f, 100f, 400f);
        system.addRingBand(wonknu_vii, "misc", "rings_special0", 256f, 0, Color.white, 256f, 1400f, 120f,
                null,
                "天狼星座-行星环");

        // 实体：人之领通讯、传感、信标，休眠星门，星门周围的残骸
        SectorEntityToken w_loc1 = system.addCustomEntity(null, null, Entities.SENSOR_ARRAY, Factions.NEUTRAL);
        SectorEntityToken w_loc2 = system.addCustomEntity(null, null, Entities.COMM_RELAY, Factions.NEUTRAL);
        SectorEntityToken w_loc3 = system.addCustomEntity(null, null, Entities.NAV_BUOY, Factions.NEUTRAL);
        w_loc1.setCircularOrbitPointingDown(W_star, 0f, 2800f, 200f);
        w_loc2.setCircularOrbitPointingDown(W_star, 90f, 2800f, 200f);
        w_loc3.setCircularOrbitPointingDown(W_star, 180f, 2800f, 200f);
        SectorEntityToken gate = system.addCustomEntity("ROU_system_gate", "却邪之道", Entities.INACTIVE_GATE,
                Factions.NEUTRAL);
        gate.setCircularOrbit(wonknu_ii, 180f, 2000f, 365f);
        DebrisFieldParams params = new DebrisFieldParams( // 碎片(Debris)
                500f, // 碎片场半径，不应超过1000，影响性能
                1f, // 密度-影响碎片数量，基础为1
                1000000f, // 持续时间
                0f);// 几天后开始渲染
        params.source = DebrisFieldSource.MIXED;
        params.baseSalvageXP = 2000;// 打捞残骸获得的经验
        SectorEntityToken debires = Misc.addDebrisField(system, params, StarSystemGenerator.random);
        SalvageSpecialAssigner.assignSpecialForDebrisField(debires);
        debires.setDiscoverable(true);// 能够被玩家发现
        debires.setDiscoveryXP(2000f);// 发现后给予经验值
        debires.setSensorProfile(1f);// 可以检测到的范围，1单位=2000大地图su
        debires.getDetectedRangeMod().modifyFlat("gen", 2000f);
        debires.setCircularOrbitWithSpin(gate, 0f, 1f, 100f, 1f, 10f);

        // 尝试：加入星冕分流器、人之领低温休眠舱
        SectorEntityToken cryosleeper = system.addCustomEntity(null, null, Entities.DERELICT_CRYOSLEEPER,
                Factions.DERELICT);
        cryosleeper.setCircularOrbitWithSpin(W_star, 90f,
                W_star.getRadius() + cryosleeper.getRadius() + 500f, W_star.getRadius() / 20f, 1f,
                11f);
        // cryosleeper.getMemoryWithoutUpdate().set(null, null);
        cryosleeper.setSensorProfile(1f);
        cryosleeper.setDiscoverable(true);
        cryosleeper.setDiscoveryXP(10000f);
        cryosleeper.getDetectedRangeMod().modifyFlat("gen", 3500f);

        SectorEntityToken hypershunt = system.addCustomEntity(null, null, Entities.CORONAL_TAP, null);
        float hypershuntOrbitRadius = W_star.getRadius() + hypershunt.getRadius() + 100f;
        hypershunt.setCircularOrbitPointingDown(W_star, 270f, hypershuntOrbitRadius,
                hypershuntOrbitRadius / 20f);
        hypershunt.setSensorProfile(1f);
        hypershunt.setDiscoverable(true);
        hypershunt.setDiscoveryXP(10000f);
        hypershunt.getDetectedRangeMod().modifyFlat("gen", 3500f);
        system.addScript(new CoronalTapParticleScript(hypershunt));
        system.addTag("theme_derelict_cryosleeper");
        system.addTag("has_coronal_tap");
        system.addTag("theme_interesting");

        // 空间站: 玉门-空间站 带一个欧米伽核心（彩蛋）
        SectorEntityToken station = system.addCustomEntity("ROU_system_station", "玉门-空间站", "station_side07",
                Factions.NEUTRAL);
        station.setCircularOrbitPointingDown(W_star, 270f, 2800f, 200f);
        station.setInteractionImage("illustrations", "cargo_loading");
        Misc.setAbandonedStationMarket("ROU_system_station_market", station);
        station.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addItems(
                CargoItemType.RESOURCES,
                Commodities.OMEGA_CORE, 1);

        // 给设置好的planet精装修
        // 北落师门（6恒星镜,1个跳跃点）[类地]
        MarketAPI wonknu_ii_market = addPlanetCondition(wonknu_ii, new ArrayList<String>(
                Arrays.asList(
                        // 北落师门{类地} 宜居、温和气候、恒星镜阵列、食物+2 矿物+1 有机物+2 建造——农业、轻工业、贸易中心、度假中心
                        // 开启星冕分流器后+破碎采矿业
                        Conditions.HABITABLE,
                        Conditions.MILD_CLIMATE,
                        Conditions.FARMLAND_BOUNTIFUL,
                        Conditions.ORE_ABUNDANT,
                        Conditions.ORGANICS_ABUNDANT,
                        Conditions.SOLAR_ARRAY)));
        SectorEntityToken wonknu_ii_mirror1 = system.addCustomEntity("ROU_blsm_mirror", "北落师门-Alpha 恒星镜",
                Entities.STELLAR_MIRROR, Factions.NEUTRAL);
        SectorEntityToken wonknu_ii_mirror2 = system.addCustomEntity("ROU_blsm_mirror", "北落师门-Beta 恒星镜",
                Entities.STELLAR_SHADE, Factions.NEUTRAL);
        SectorEntityToken wonknu_ii_mirror3 = system.addCustomEntity("ROU_blsm_mirror", "北落师门-Gamma 恒星镜",
                Entities.STELLAR_MIRROR, Factions.NEUTRAL);
        SectorEntityToken wonknu_ii_mirror4 = system.addCustomEntity("ROU_blsm_mirror", "北落师门-Delta 恒星镜",
                Entities.STELLAR_SHADE, Factions.NEUTRAL);
        SectorEntityToken wonknu_ii_mirror5 = system.addCustomEntity("ROU_blsm_mirror", "北落师门-Epsilon 恒星镜",
                Entities.STELLAR_MIRROR, Factions.NEUTRAL);
        SectorEntityToken wonknu_ii_mirror6 = system.addCustomEntity("ROU_blsm_mirror", "北落师门-Zeta 恒星镜",
                Entities.STELLAR_SHADE, Factions.NEUTRAL);
        float mirroOrbitRadius = wonknu_ii.getRadius() + wonknu_ii_mirror1.getRadius() + 100f;
        wonknu_ii_mirror1.setCircularOrbitPointingDown(wonknu_ii, 0f, mirroOrbitRadius, 30f);
        wonknu_ii_mirror2.setCircularOrbitPointingDown(wonknu_ii, 60f, mirroOrbitRadius, 30f);
        wonknu_ii_mirror3.setCircularOrbitPointingDown(wonknu_ii, 120f, mirroOrbitRadius, 30f);
        wonknu_ii_mirror4.setCircularOrbitPointingDown(wonknu_ii, 180f, mirroOrbitRadius, 30f);
        wonknu_ii_mirror5.setCircularOrbitPointingDown(wonknu_ii, 240f, mirroOrbitRadius, 30f);
        wonknu_ii_mirror6.setCircularOrbitPointingDown(wonknu_ii, 300f, mirroOrbitRadius, 30f);

        JumpPointAPI wonknu_ii_jumpPoint = Global.getFactory().createJumpPoint("blsm_jump_point", "未明之路");
        wonknu_ii_jumpPoint.setOrbit(Global.getFactory().createCircularOrbit(wonknu_ii, 270f, 800f, 360f));
        wonknu_ii_jumpPoint.setRelatedPlanet(wonknu_ii);
        wonknu_ii_jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(wonknu_ii_jumpPoint);

        // 天大将军{火山} 极端炎热、稀薄大气层、极端构造活动性、矿物+3、稀矿+3、巨大废墟
        // 建造——t4分选矿业、最高指挥部、t3军用重工业、遗迹挖掘/人之领遗迹工厂
        MarketAPI wonknu_i_market = addPlanetCondition(wonknu_i, new ArrayList<String>(
                Arrays.asList(
                        // 天大将军{火山} 极端炎热、稀薄大气层、极端构造活动性、矿物+3、稀矿+3、巨大废墟
                        // 建造——t4分选矿业、最高指挥部、t3军用重工业、遗迹挖掘/人之领遗迹工厂
                        Conditions.VERY_HOT,
                        Conditions.THIN_ATMOSPHERE,
                        Conditions.EXTREME_TECTONIC_ACTIVITY,
                        Conditions.ORE_ULTRARICH,
                        Conditions.RARE_ORE_ULTRARICH,
                        Conditions.RUINS_VAST)));

        // 天狼星座{气态巨行星} 高重力、极端气候、挥发物+2 拍一个挥发物空间站——t2挥发物采矿专精、燃料制造、t3民用重工业、贸易中心
        // 开启星冕分流器后+t3矿物冶炼业
        MarketAPI wonknu_iii_market = addPlanetCondition(wonknu_iii, new ArrayList<String>(
                Arrays.asList(
                        Conditions.HIGH_GRAVITY,
                        Conditions.EXTREME_WEATHER,
                        Conditions.VOLATILES_PLENTIFUL)));

        // 天鹰座{荒芜-轰击} 无大气层、矿物-1、稀矿-1、低重力 建造——t4稀矿冶炼业、燃料制造、t4臭鼬工厂、贸易中心
        MarketAPI wonknu_iii_a_market = addPlanetCondition(wonknu_iii_a, new ArrayList<String>(
                Arrays.asList(
                        Conditions.NO_ATMOSPHERE,
                        Conditions.ORE_SPARSE,
                        Conditions.RARE_ORE_SPARSE,
                        Conditions.LOW_GRAVITY)));

        // 天琴座{荒芜} 无大气层、矿物+0、低重力 建造——t4矿物冶炼业、燃料制造、t4在轨舰队中心、贸易中心
        MarketAPI wonknu_iii_b_market = addPlanetCondition(wonknu_iii_b, new ArrayList<String>(
                Arrays.asList(
                        Conditions.NO_ATMOSPHERE,
                        Conditions.ORE_MODERATE,
                        Conditions.LOW_GRAVITY)));

        // 船帆座{荒芜-沙漠} 稀薄大气层、寒冷、矿物+0、稀矿-1、有机物+2 建造——t4升华矿业、t4在轨舰队中心、智能加工中心
        MarketAPI wonknu_iv_market = addPlanetCondition(wonknu_iv, new ArrayList<String>(
                Arrays.asList(
                        Conditions.THIN_ATMOSPHERE,
                        Conditions.COLD,
                        Conditions.ORE_MODERATE,
                        Conditions.RARE_ORE_SPARSE,
                        Conditions.ORGANICS_PLENTIFUL)));

        // 船尾座{冰冻} 极端寒冷、昏暗、矿物+0、稀矿+0、挥发物-1、巨大废墟 建造——遗迹挖掘/人之领遗迹工厂、彼岸工程、超级计算机、星穹工厂

        MarketAPI wnoknu_v_market = addPlanetCondition(wonknu_v, new ArrayList<String>(
                Arrays.asList(
                        Conditions.VERY_COLD,
                        Conditions.POOR_LIGHT,
                        Conditions.ORE_MODERATE,
                        Conditions.RARE_ORE_MODERATE,
                        Conditions.VOLATILES_TRACE,
                        Conditions.RUINS_VAST)));

        // 船底座{冰火山} 极端寒冷、构造活动性、矿物+2、稀矿+2、挥发物+2 建造——t4升华矿业、t4臭鼬工厂、智能加工中心
        MarketAPI wonknu_vi_market = addPlanetCondition(wonknu_vi, new ArrayList<String>(
                Arrays.asList(
                        Conditions.VERY_COLD,
                        Conditions.TECTONIC_ACTIVITY,
                        Conditions.ORE_RICH,
                        Conditions.RARE_ORE_RICH,
                        Conditions.VOLATILES_PLENTIFUL)));

        // 紫微垣{冰巨星} 高重力、极端寒冷、黑暗、极端气候、挥发物+0
        MarketAPI wonknu_vii_market = addPlanetCondition(wonknu_vii, new ArrayList<String>(
                Arrays.asList(
                        Conditions.HIGH_GRAVITY,
                        Conditions.VERY_COLD,
                        Conditions.DARK,
                        Conditions.EXTREME_WEATHER,
                        Conditions.VOLATILES_DIFFUSE)));

        SectorEntityToken wonknu_vii_field = system.addTerrain(Terrain.MAGNETIC_FIELD,
                new MagneticFieldTerrainPlugin.MagneticFieldParams(wonknu_vii.getRadius() + 400f,
                        (wonknu_vii.getRadius() + 400f) / 2f,
                        wonknu_vii,
                        wonknu_vii.getRadius() + 10f,
                        wonknu_vii.getRadius() + 550f,
                        new Color(50, 20, 100, 120),
                        0.8f,
                        new Color(90, 40, 220),
                        new Color(130, 80, 160),
                        new Color(150, 55, 140),
                        new Color(180, 80, 110),
                        new Color(90, 200, 170),
                        new Color(65, 230, 160),
                        new Color(20, 220, 70)));
        wonknu_vii_field.setCircularOrbit(wonknu_vii, 0f, 0f, 100f);

        // 太微垣{辐射} 辐射、黑暗、极端寒冷、矿物+0、稀矿-1
        MarketAPI wonknu_vii_a_market = addPlanetCondition(wonknu_vii_a, new ArrayList<String>(
                Arrays.asList(
                        Conditions.IRRADIATED,
                        Conditions.DARK,
                        Conditions.VERY_COLD,
                        Conditions.ORE_MODERATE,
                        Conditions.RARE_ORE_SPARSE)));

        SectorEntityToken wonknu_vii_a_field = system.addTerrain(Terrain.MAGNETIC_FIELD,
                new MagneticFieldTerrainPlugin.MagneticFieldParams(
                        wonknu_vii_a.getRadius() + 100f,
                        (wonknu_vii_a.getRadius() + 100f) / 2f,
                        wonknu_vii_a,
                        wonknu_vii_a.getRadius() + 10f,
                        wonknu_vii_a.getRadius() + 190f,
                        new Color(50, 20, 100, 120), 0f));
        wonknu_vii_a_field.setCircularOrbit(wonknu_vii_a, 0f, 0f, 100f);

        // 天市垣{剧毒} 剧毒大气层、黑暗、极端寒冷、矿物+1、稀矿+1、有机物+1、挥发物+1
        MarketAPI wonknu_vii_b_market = addPlanetCondition(wonknu_vii_b, new ArrayList<String>(
                Arrays.asList(
                        Conditions.TOXIC_ATMOSPHERE,
                        Conditions.DARK,
                        Conditions.VERY_COLD,
                        Conditions.ORE_ABUNDANT,
                        Conditions.RARE_ORE_ABUNDANT,
                        Conditions.ORGANICS_ABUNDANT,
                        Conditions.VOLATILES_ABUNDANT)));

        // 创造跳跃点(b0:气态巨行星是否创造跳跃点；b1：是否创造星系边缘跳跃点)
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("ROU_jump_point", "内部跳跃点");
        jumpPoint.setOrbit(Global.getFactory().createCircularOrbit(W_star, 300f, 8100f, 421f));
        // jumpPoint.setRelatedPlanet(null);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint);

        system.autogenerateHyperspaceJumpPoints(true, true);
        cleanup(system);
    }

    private void cleanup(StarSystemAPI system) {
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
    }
}
