package org.atlasapi.feeds.xmltv;

import java.util.Map;

import org.atlasapi.media.entity.Channel;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

public class XmlTvChannelLookup extends ForwardingMap<Integer, Channel> {

    public static final Channel IGNORED = new Channel("Ignored","http://ignored","ignored");
    
    private static final ImmutableMap<Integer, Channel> delegate = ImmutableMap
            .<Integer, Channel> builder()
            .put(22, Channel.NAT_GEO_WILD)
            .put(24, Channel.ITV1_ANGLIA)
            .put(25, Channel.ITV1_BORDER_SOUTH)
            .put(26, Channel.ITV1_LONDON)
            .put(27, Channel.ITV1_CARLTON_CENTRAL)
            .put(28, Channel.ITV1_CHANNEL)
            .put(29, Channel.ITV1_GRANADA)
            .put(30, Channel.ITV1_MERIDIAN)
            .put(31, Channel.ITV1_TYNE_TEES)
            .put(32, Channel.YTV)
            .put(33, Channel.ITV1_CARLTON_WESTCOUNTRY)
            .put(35, Channel.ITV1_WALES)
            .put(36, Channel.ITV1_WEST)
            .put(37, Channel.STV_CENTRAL)
            .put(38, Channel.ULSTER)
            .put(39, Channel.ANIMAL_PLANET)
            .put(40, Channel.SKY_ARTS_1)
            .put(43, Channel.B4U_MOVIES)
            .put(45, Channel.BBC_THREE)
            .put(47, Channel.BBC_FOUR)
            .put(48, Channel.BBC_NEWS)
            .put(49, Channel.BBC_PARLIMENT)
            .put(50, Channel.BBC_ENTERTAINMENT)
            .put(92, Channel.BBC_ONE)
            .put(93, Channel.BBC_ONE_EAST)
            .put(94, Channel.BBC_ONE)
            .put(95, Channel.BBC_ONE_EAST_MIDLANDS)
            .put(96, Channel.BBC_ONE_NORTH_EAST)
            .put(97, Channel.BBC_ONE_NORTH_EAST)
            .put(98, Channel.BBC_ONE_NORTH_WEST)
            .put(99, Channel.BBC_ONE_NORTHERN_IRELAND)
            .put(100, Channel.BBC_ONE_WALES)
            .put(101, Channel.BBC_ONE_SCOTLAND)
            .put(102, Channel.BBC_ONE_SOUTH)
            .put(103, Channel.BBC_ONE_SOUTH_WEST)
            .put(104, Channel.BBC_ONE_WEST)
            .put(105, Channel.BBC_TWO)
            .put(106, Channel.BBC_TWO)
            .put(107, Channel.BBC_TWO)
            .put(108, Channel.BBC_TWO)
            .put(109, Channel.BBC_TWO)
            .put(110, Channel.BBC_TWO)
            .put(111, Channel.BBC_TWO)
            .put(112, Channel.BBC_TWO_NORTHERN_IRELAND)
            .put(113, Channel.BBC_TWO_SCOTLAND)
            .put(114, Channel.BBC_TWO_WALES)
            .put(115, Channel.BBC_TWO)
            .put(116, Channel.BBC_TWO)
            .put(117, Channel.BBC_TWO)
            .put(119, Channel.BIO)
            .put(120, Channel.BLOOMBERG_TV)
            .put(122, Channel.BRAVO)
            .put(123, Channel.EUROSPORT)
            .put(125, Channel.CNBC)
            .put(126, Channel.CNN)
            .put(128, Channel.CARTOON_NETWORK)
            .put(129, Channel.BOOMERANG_PLUS1)
            .put(131, Channel.CHALLENGE)
            .put(132, Channel.CHANNEL_FOUR)
            .put(134, Channel.FIVE)
            .put(137, Channel.GOD_CHANNEL)
            .put(147, Channel.DISCOVERY)
            .put(148, Channel.DISCOVERY_KNOWLEDGE)
            .put(149, Channel.DISCOVERY_HOME_AND_HEALTH)
            .put(150, Channel.DISCOVERY_REAL_TIME)
            .put(152, Channel.DISCOVERY_PLUS1)
            .put(153, Channel.DISCOVERY_SCIENCE)
            .put(154, Channel.DISCOVERY_TRAVEL_AND_LIVING)
            .put(155, Channel.DISCOVERY_TURBO)
            .put(156, Channel.THE_DISNEY_CHANNEL)
            .put(157, Channel.THE_DISNEY_CHANNEL_PLUS1)
            .put(158, Channel.E_FOUR)
            .put(159, Channel.EURONEWS)
            .put(160, Channel.FILM_4)
            .put(165, Channel.DISNEY_XD)
            .put(166, Channel.DISNEY_XD_PLUS1)
            .put(180, Channel.UNIVERSAL)
            .put(182, Channel.HISTORY)
            .put(183, Channel.HISTORY_PLUS1)
            .put(185, Channel.ITV2)
            .put(191, Channel.KERRANG)
            .put(197, Channel.SKY_LIVING)
            .put(202, Channel.MTV)
            .put(203, Channel.MTV_BASE)
            .put(204, Channel.MTV_HITS)
            .put(205, Channel.MTV_ROCKS)
            .put(206, Channel.MUTV)
            .put(213, Channel.NATIONAL_GEOGRAPHIC)
            .put(214, Channel.NATIONAL_GEOGRAPHIC_PLUS1)
            .put(215, Channel.NICK_JR)
            .put(216, Channel.NICKELODEON_REPLAY)
            .put(217, Channel.NICKELODEON)
            .put(228, Channel.QVC)
            .put(231, Channel.RTE1)
            .put(246, IGNORED)
            .put(248, Channel.SKY1)
            .put(249, Channel.SKY_MOVIES_PREMIERE)
            .put(250, Channel.SKY_MOVIES_PREMIERE_PLUS1)
            .put(253, Channel.SKY_MOVIES_INDIE)
            .put(256, Channel.SKY_NEWS)
            .put(257, Channel.SKY_MOVIES_COMEDY)
            .put(258, Channel.SKY_MOVIES_FAMILY)
            .put(259, Channel.SKY_MOVIES_CLASSICS)
            .put(260, Channel.SKY_MOVIES_MODERN_GREATS)
            .put(262, Channel.SKY_SPORTS_1)
            .put(264, Channel.SKY_SPORTS_2)
            .put(265, Channel.SKY_SPORTS_3)
            .put(266, IGNORED)
            .put(267, Channel.SONY_ENTERTAINMENT_TV_ASIA)
            .put(271, Channel.TCM)
            .put(273, Channel.TG4)
            .put(276, Channel.TV5)
            .put(281, Channel.THE_BOX)
            .put(287, Channel.LIVING_PLUS2)
            .put(288, Channel.GOLD)
            .put(292, Channel.ALIBI)
            .put(293, Channel.VH1)
            .put(294, Channel.MTV_CLASSIC)
            .put(300, Channel.SKY_SPORTS_NEWS)
            //.put(421, Channel.DISNEY_JUNIOR)
            .put(461, Channel.ITV1_BORDER_NORTH)
            .put(482, Channel.CBBC)
            .put(483, Channel.CBEEBIES)
            .put(581, Channel.ZEE_TV)
            .put(582, Channel.EXTREME_SPORTS)
            .put(588, Channel.MAGIC)
            .put(590, Channel.STAR_NEWS)
            .put(591, Channel.STAR_PLUS)
            .put(592, Channel.SMASH_HITS)
            .put(594, Channel.BID_TV)
            .put(609, Channel.KISS)
            .put(610, Channel.MTV_DANCE)
            .put(625, Channel.EDEN_PLUS1)
            .put(661, Channel.ATTHERACES)
            .put(664, Channel.NICKTOONS_TV)
            .put(665, Channel.GOLD_PLUS1)
            .put(721, Channel.S4C)
            .put(742, Channel.CN_TOO)
            .put(801, Channel.YESTERDAY)
            .put(841, Channel.LIVING_PLUS1)
            .put(922, Channel.SKY2)
            .put(941, Channel.TV3_SPANISH)
            .put(1061, Channel.COMEDY_CENTRAL)
            .put(1143, Channel.SCUZZ)
            .put(1144, Channel.BLISS)
            .put(1161, Channel.E4_PLUS1)
            .put(1201, Channel.COMEDY_CENTRAL_EXTRA)
            .put(1221, Channel.BRAVO_PLUS1)
            //.put(1261, Channel.SKY_MOVIES_BOX_OFFICE)
            //.put(1421, Channel.E_ENTERTAINMENT)
            .put(1461, Channel.FX)
            .put(1462, Channel.TRAVELCHANNEL)
            .put(1521, Channel.YESTERDAY_PLUS1)
            .put(1542, Channel.THE_COMMUNITY_CHANNEL)
            .put(1543, Channel.ESPN_AMERICA)
            .put(1544, Channel.FOUR_MUSIC)
            .put(1601, Channel.EDEN)
            .put(1602, Channel.BLIGHTY)
            .put(1661, Channel.BOOMERANG)
            .put(1662, IGNORED)
            .put(1741, Channel.CHELSEA_TV)
            .put(1761, Channel.ANIMAL_PLANET_PLUS1)
            .put(1764, Channel.DISCOVERY_REAL_TIME_PLUS1)
            .put(1802, IGNORED)
            .put(1804, Channel.TELEG)
            .put(1855, Channel.TRAVELCHANNEL_PLUS1)
            .put(1859, Channel.ITV3)
            //.put(1862, Channel.BBC_TWO_SOUTH_EAST)
            .put(1865, Channel.TVE_INTERNACIONAL)
            .put(1869, Channel.BBC_ONE_SOUTH_EAST)
            .put(1870, Channel.RTE2)
            .put(1872, Channel.CHALLENGE_PLUS1)
            .put(1876, Channel.EUROSPORT_2)
            .put(1882, Channel.REALLY)
            .put(1944, IGNORED)
            .put(1949, Channel.RACING_UK)
            .put(1953, Channel.DISCOVERY_HOME_AND_HEALTH_PLUS1)
            .put(1956, IGNORED)
            .put(1958, Channel.MOTORS_TV)
            .put(1959, Channel.MORE_FOUR)
            .put(1961, Channel.ITV4)
            //.put(1963, Channel.PICK_TV)
            .put(1969, IGNORED)
            .put(1971, Channel.FX_PLUS)
            .put(1972, Channel.MORE4_PLUS1)
            .put(1981, Channel.CITV)
            .put(1983, Channel.CARTOONITO)
            .put(1984, Channel.DISNEY_CINEMAGIC)
            .put(1985, Channel.DISNEY_CINEMAGIC_PLUS1)
            .put(1990, Channel.ITV2_PLUS1)
            .put(1993, Channel.ALIBI_PLUS1)
            .put(1994, Channel.BBC_HD)
            .put(2008, Channel.FIVE_USA)
            .put(2010, Channel.BRAVO_2)
            .put(2011, Channel.ESPN_CLASSIC)
            .put(2013, Channel.COMEDY_CENTRAL_PLUS1)
            .put(2014, Channel.PROPELLER_TV)
            .put(2016, Channel.TCM2)
            .put(2021, Channel.FILM4_PLUS1)
            .put(2040, Channel.BBC_SPORT_INTERACTIVE_FREEVIEW)
            .put(2047, Channel.Channel_4_PLUS1)
            .put(2049, Channel.Channel_ONE)
            .put(2050, Channel.DAVE)
            .put(2052, Channel.DAVE_JA_VU)
            .put(2055, Channel.AL_JAZEERA_ENGLISH)
            .put(2056, Channel.Channel_4_HD)
            .put(2057, Channel.DISCOVERY_SHED)
            .put(2058, Channel.MOVIES4MEN)
            .put(2059, Channel.TRUE_MOVIES)
            //.put(2062, Channel.FIVE_STAR)
            .put(2098, Channel.BBC_ALBA)
            .put(2115, Channel.WATCH)
            .put(2116, Channel.WATCH_PLUS1)
            .put(2118, Channel.ITV1_HD)
            .put(2122, Channel.SKY_ARTS_2)
            .put(2134, Channel.HOME)
            .put(2135, Channel.HOME_PLUS1)
            .put(2136, Channel.GOOD_FOOD)
            .put(2137, Channel.GOOD_FOOD_PLUS1)
            .put(2139, Channel.SKY1_HD)
            .put(2142, Channel.ESPN)
            .put(2143, Channel.BIO_HD)
            .put(2144, Channel.CRIME_AND_INVESTIGATION_HD)
            .put(2145, Channel.DISCOVERY_HD)
            .put(2146, Channel.DISNEY_CINEMAGIC_HD)
            .put(2147, Channel.ESPN_HD)
            .put(2148, Channel.EUROSPORT_HD)
            .put(2149, Channel.FX_HD)
            .put(2150, Channel.MTVN_HD)
            .put(2151, Channel.NATIONAL_GEOGRAPHIC_HD)
            .put(2152, Channel.NAT_GEO_WILD_HD)
            .put(2154, Channel.SKY_ARTS_1_HD)
            .put(2155, Channel.SKY_ARTS_2_HD)
            .put(2157, Channel.SKY_MOVIES_COMEDY_HD)
            .put(2159, Channel.SKY_MOVIES_FAMILY_HD)
            .put(2160, Channel.SKY_MOVIES_MODERN_GREATS_HD)
            .put(2161, Channel.SKY_MOVIES_SCIFI_HORROR_HD)
            .put(2162, IGNORED)
            .put(2164, Channel.BBC_WORLD_NEWS)
            .put(2165, Channel.CRIME_AND_INVESTIGATION)
            .put(2168, Channel.SKY_MOVIES_SCIFI_HORROR)
            .put(2169, Channel.Channel_ONE_PLUS1)
            .put(2173, Channel.RUSH_HD)
            .put(2174, Channel.SKY_SPORTS_1_HD)
            .put(2175, Channel.SKY_SPORTS_2_HD)
            .put(2176, Channel.SKY_SPORTS_3_HD)
            .put(2177, Channel.CBS_DRAMA)
            .put(2178, Channel.CBS_ACTION)
            .put(2179, Channel.QUEST)
            .put(2181, Channel.CINEMOI)
            .put(2184, Channel.VIVA)
            .put(2185, Channel.FOOD_NETWORK)
            .put(2186, Channel.FOOD_NETWORK_PLUS1)
            .put(2189, Channel.SKY_LIVINGIT)
            .put(2190, Channel.LIVINGIT_PLUS1)
            .put(2192, Channel.TRUE_MOVIES_2)
            .put(2195, Channel.E4_HD)
            .put(2196, Channel.MGM)
            .put(2197, Channel.CHRISTMAS24)
            .put(2200, Channel.SKY_SPORTS_4)
            .put(2203, Channel.STV_NORTH)
            .put(2204, Channel.SKY_MOVIES_ACTION_AND_ADVENTURE)
            .put(2205, Channel.SKY_MOVIES_ACTION_AND_ADVENTURE_HD)
            .put(2206, Channel.SKY_MOVIES_CRIME_AND_THRILLER)
            .put(2207, Channel.SKY_MOVIES_CRIME_AND_THRILLER_HD)
            .put(2208, Channel.SKY_MOVIES_DRAMA_AND_ROMANCE)
            .put(2209, Channel.SKY_MOVIES_DRAMA_AND_ROMANCE_HD)
            //.put(2210, Channel.SKY_MOVIES_CHRISTMAS)
            //.put(2211, Channel.SKY_MOVIES_CHRISTMAS_HD)
            .put(2212, Channel.SYFY)
            .put(2213, Channel.SYFY_PLUS1)
            .put(2214, Channel.SYFY_HD)
            .put(2217, Channel.SKY_NEWS_HD)
            .put(2219, Channel.STV_HD)
            .put(2240, Channel.BBC_ONE_EAST_MIDLANDS)
            //.put(2241, Channel.BBC_TWO_EAST_MIDLANDS)
            .put(2244, Channel.BET_INTERNATIONAL)
            .put(2246, Channel.PHOENIX_CNE)
            //.put(2249, Channel.RAI_UNO)
            //.put(2250, Channel.HEALTH)
            .put(2251, Channel.SIMPLY_SHOPPING)
            .put(2252, IGNORED)
            .put(2253, IGNORED)
            .put(2254, IGNORED)
            .put(2255, IGNORED)
            .put(2256, IGNORED)
            .put(2257, IGNORED)
            .put(2258, IGNORED)
            .put(2259, IGNORED)
            .put(2260, IGNORED)
            .put(2261, IGNORED)
            .put(2264, IGNORED)
            .put(2331, Channel.GMTV_DIGITAL)
            .put(2421, IGNORED)
            .put(2422, IGNORED)
            .put(2427, Channel.S4C2)
            .put(2429, Channel.CHANNEL_9)
            .put(2439, Channel.CHART_SHOW_TV)
            .put(2457, Channel.FLAUNT)
            .put(2476, Channel.THE_HORROR_CHANNEL)
            .put(2479, Channel.SETANTA_IRELAND)
            .put(2487, Channel.M95_TV_MARBELLA)
            .put(2492, IGNORED)
            .put(2502, Channel.THREE_E)
            .put(2505, Channel.FILMFLEX)
            //.put(2506, Channel.SKY_MOVIES_BOX_OFFICE_HD2)
            //.put(2507, Channel.SKY_MOVIES_BOX_OFFICE_HD1)
            .put(2511, Channel.ITV3_PLUS1)
            .put(2514, Channel.ITV1_THAMES_VALLEY_NORTH)
            .put(2515, Channel.ITV1_THAMES_VALLEY_SOUTH)
            .put(2518, Channel.E_EUROPE)
            .put(2526, Channel.HISTORY_HD)
            .put(2527, Channel.BEST_DIRECT)
            .put(2528, Channel.GEMS_TV)
            .put(2529, Channel.GEM_COLLECTOR)
            .put(2530, Channel.DEUTSCHE_WELLE)
            //.put(2531, Channel.Channel_SS)
            .put(2533, Channel.SETANTA_SPORTS_1_IRELAND)
            .put(2537, Channel.DIVA)
            .put(2542, Channel.PLAYHOUSE_DISNEY_PLUS)
            .put(2543, Channel.TINY_POP)
            .put(2544, Channel.POP)
            .put(2545, Channel.DMAX)
            .put(2547, Channel.MTV_PLUS1)
            .put(2549, Channel.HORSE_AND_COUNTRY)
            .put(2550, Channel.Channel_7)
            .put(2551, Channel.FLAVA)
            .put(2552, Channel.SKY_MOVIES_PREMIERE_HD)
            .put(2553, IGNORED)
            .put(2557, Channel.NATIONAL_GEOGRAPHIC_HD_PAN_EUROPEAN)
            .put(2558, Channel.MOVIES4MEN2)
            .put(2559, Channel.MILITARY_HISTORY)
            .put(2561, Channel.THE_HORROR_CHANNEL_PLUS1)
            .put(2563, Channel.THE_STYLE_NETWORK)
            .put(2566, Channel.WEDDING_TV)
            .put(2568, Channel.ITV4_PLUS1)
            .put(2569, Channel.SUPER_CASINO)
            .put(2570, Channel.INVESTIGATION_DISCOVERY)
            //.put(2572, Channel.ROCKS_AND_CO)
            .put(2574, Channel.DISCOVERY_TRAVEL_AND_LIVING_PLUS1)
            //.put(2575, Channel.SUMO_TV)
            .put(2577, Channel.FIVE_USA_PLUS1)
            //.put(2578, Channel.FIVE_START_PLUS1)
            .put(2583, Channel.CRIME_AND_INVESTIGATION_PLUS1)
            //.put(2585, Channel.SKY_LIVING_HD)
            .put(2586, Channel.CBS_REALITY)
            .put(2587, Channel.SKY_MOVIES_INDIE_HD)
            .put(2588, Channel.DISCOVERY_QUEST_PLUS1)
            .put(2589, Channel.DIVA_PLUS1)
            .put(2590, Channel.CHRISTMAS24_PLUS)
            .put(2591, Channel.UNIVERSAL_PLUS1)
            .put(2592, Channel.MTV_SHOWS)
            .put(2593, Channel.NICK_JR_2)
            .put(2594, Channel.NHK_WORLD)
            .put(2595, Channel.DMAX_PLUS1)
            .put(2596, Channel.COMEDY_CENTRAL_EXTRA_PLUS1)
            .put(2597, Channel.BET_PLUS1)
            .put(2598, Channel.MOVIES4MEN_PLUS1)
            .put(2599, Channel.MOVIES4MEN2_PLUS1)
            .put(2600, Channel.DISCOVERY_SCIENCE_PLUS1)
            //.put(2601, Channel.DISCOVERY_KNOWLEDGE_PLUS1)
            .put(2602, Channel.NICKTOONS_REPLAY)
            .put(2603, Channel.TRUEENT)
            .put(2604, Channel.BODY_IN_BALANCE)
            .put(2605, Channel.THE_ACTIVE_CHANNEL)
            .put(2606, Channel.SKY_3D)
            .put(2607, Channel.SKY_SPORTS_4_HD)
            .put(2610, Channel.BBC_SPORT_INTERACTIVE_BBC_TWO)
            .put(2612, Channel.BBC_SPORT_INTERACTIVE_BBC_ONE)
            .put(2613, Channel.BBC_SPORT_INTERACTIVE_BBC_THREE)
            .put(2615, Channel.FIVE_HD)
            .put(2617, Channel.FILM4_HD)
            //.put(2618, Channel.BLIGHTY_PLUS1)
            .put(2619, Channel.DISCOVERY_PLUS1_POINT5)
            //.put(2622, Channel.SHOWCASE)
            .put(2624, Channel.S4C_CLIRLUN)
            //.put(2625, Channel.ASIANET)
            //.put(2636, Channel.MULTI_CHANNEL)
            //.put(2637, Channel.MUSIC_CHOICE_EUROPE)
            .put(2638, Channel.FITNESS_TV)
            .put(2639, Channel.Q)
            .put(2640, Channel.PRICE_DROP_TV)
            .put(2642, Channel.TV3)
            .put(2645, Channel.MGM_HD)
            .put(2646, Channel.COMEDY_CENTRAL_HD)
//            .put(2647, Channel.PICK_TV_PLUS_1)
            .put(2661, Channel.ITV2_HD)
            .put(2662, Channel.EDEN_HD)
            .put(2663, Channel.QUEST_FREEVIEW)
            .put(2667, Channel.BBC_ONE_HD)
            .put(2668, Channel.DISCOVERY_HISTORY)
            .put(2672, Channel.DISCOVERY_HISTORY_PLUS_1)
            .put(2676, Channel.ITV1_GRANADA_PLUS1)
            .put(2677, Channel.ITV1_UTV_PLUS1)
            .put(2678, Channel.ITV1_CENTRAL_PLUS1)
            .put(2679, Channel.ITV1_WEST_PLUS1)
            .put(2680, Channel.ITV1_SOUTH_EAST_PLUS1)
            //.put(2681, Channel.ITV1_STV_PLUS1)
            .put(2682, Channel.ITV1_LONDON_PLUS1)
            .put(2683, Channel.ITV1_YORKSHIRE_TYNE_TEES_PLUS1)
            .put(2684, Channel.SKY_ATLANTIC_HD)
            .put(2685, Channel.SKY_ATLANTIC)
            .put(2686, Channel.SKY_LIVING_LOVES)
            //.put(2696, Channel.SONY_ENTERTAINMENT_TV_(PLUS_1))
            //.put(2697, Channel.SONY_ENTERTAINMENT_TV_ASIA_ENTERTAINMENT_TELEVISION)
        .build();

    @Override
    protected Map<Integer, Channel> delegate() {
        return delegate;
    }

}
