package com.winlator.d2;





public final class D2RezeptTextNamen {
    private D2RezeptTextNamen() {}

    public static String name(int what) {
        if (what >= D2RezeptKatalog.T_TYPE_BASE && what < D2RezeptKatalog.T_TYPE_BASE + D2RezeptKatalog.TYPE_CODES.length) {
            return "rb_type_" + D2RezeptKatalog.TYPE_CODES[what - D2RezeptKatalog.T_TYPE_BASE];
        }
        switch (what) {
            case D2RezeptBuch.T_TITLE: return "rb_title";
            case D2RezeptBuch.T_TAB_CAN: return "rb_tab_can";
            case D2RezeptBuch.T_TAB_CUBE: return "rb_tab_cube";
            case D2RezeptBuch.T_TAB_RUNEWORDS: return "rb_tab_runewords";
            case D2RezeptBuch.T_FILTER_ALL: return "rb_filter_all";
            case D2RezeptBuch.T_FILTER_READY: return "rb_filter_ready";
            case D2RezeptBuch.T_FILTER_ALMOST: return "rb_filter_almost";
            case D2RezeptBuch.T_SEARCH_HINT: return "rb_search_hint";
            case D2RezeptBuch.T_PICK: return "rb_pick";
            case D2RezeptBuch.T_PICK_HINT: return "rb_pick_hint";
            case D2RezeptBuch.T_READY: return "rb_ready";
            case D2RezeptBuch.T_STASH: return "rb_stash";
            case D2RezeptBuch.T_MISSING: return "rb_missing";
            case D2RezeptBuch.T_CHAIN: return "rb_chain";
            case D2RezeptBuch.T_PAGE: return "rb_page";
            case D2RezeptBuch.T_HINT_TOUCH: return "rb_hint_touch";
            case D2RezeptBuch.T_HINT_PAD: return "rb_hint_pad";
            case D2RezeptBuch.T_BACK: return "rb_back";
            case D2RezeptBuch.T_HAVE: return "rb_have";
            case D2RezeptBuch.T_LACKS: return "rb_lacks";
            case D2RezeptBuch.T_INGREDIENTS: return "rb_ingredients";
            case D2RezeptBuch.T_LADDER_OFF: return "rb_ladder_off";
            case D2RezeptBuch.T_WHERE_INV: return "rb_where_inv";
            case D2RezeptBuch.T_WHERE_CUBE: return "rb_where_cube";
            case D2RezeptBuch.T_WHERE_STASH: return "rb_where_stash";
            case D2RezeptBuch.T_WHERE_MAT: return "rb_where_mat";
            case D2RezeptBuch.T_CUBE_HAS: return "rb_cube_has";
            case D2RezeptBuch.T_CUBE_MAKES: return "rb_cube_makes";
            case D2RezeptBuch.T_CUBE_ALMOST: return "rb_cube_almost";
            case D2RezeptBuch.T_MATERIALS_LINK: return "rb_materials_link";
            case D2RezeptBuch.T_EMPTY: return "rb_empty";
            case D2RezeptBuch.T_MISSING_N: return "rb_missing_n";
            case D2RezeptBuch.T_LADDER_MARK: return "rb_ladder_mark";
            case D2RezeptBuch.T_STALE: return "rb_stale";
            case D2RezeptBuch.T_STALE_NONE: return "rb_stale_none";
            case D2RezeptBuch.T_INSERT: return "rb_insert";
            case D2RezeptKatalog.T_CAT_RUNES: return "rb_cat_runes";
            case D2RezeptKatalog.T_CAT_GEMS: return "rb_cat_gems";
            case D2RezeptKatalog.T_CAT_CRAFT: return "rb_cat_craft";
            case D2RezeptKatalog.T_CAT_UPGRADE: return "rb_cat_upgrade";
            case D2RezeptKatalog.T_CAT_QUEST: return "rb_cat_quest";
            case D2RezeptKatalog.T_FAM_BLOOD: return "rb_fam_blood";
            case D2RezeptKatalog.T_FAM_CASTER: return "rb_fam_caster";
            case D2RezeptKatalog.T_FAM_HIT: return "rb_fam_hit";
            case D2RezeptKatalog.T_FAM_SAFETY: return "rb_fam_safety";
            case D2RezeptKatalog.T_CRAFT_NAME: return "rb_craft_name";
            case D2RezeptKatalog.T_CRAFT_KIND: return "rb_craft_kind";
            case D2RezeptKatalog.T_CRAFT_RANDOM: return "rb_craft_random";
            case D2RezeptKatalog.T_CRAFT_LEVEL: return "rb_craft_level";
            case D2RezeptKatalog.T_Q_LOW: return "rb_q_low";
            case D2RezeptKatalog.T_Q_NORMAL: return "rb_q_normal";
            case D2RezeptKatalog.T_Q_MAGIC: return "rb_q_magic";
            case D2RezeptKatalog.T_Q_RARE: return "rb_q_rare";
            case D2RezeptKatalog.T_Q_UNIQUE: return "rb_q_unique";
            case D2RezeptKatalog.T_Q_SET: return "rb_q_set";
            case D2RezeptKatalog.T_F_NOSOCKET: return "rb_f_nosocket";
            case D2RezeptKatalog.T_F_SOCKETED: return "rb_f_socketed";
            case D2RezeptKatalog.T_F_ETH: return "rb_f_eth";
            case D2RezeptKatalog.T_F_NOETH: return "rb_f_noeth";
            case D2RezeptKatalog.T_F_BASIC: return "rb_f_basic";
            case D2RezeptKatalog.T_F_EXC: return "rb_f_exc";
            case D2RezeptKatalog.T_F_ELITE: return "rb_f_elite";
            case D2RezeptKatalog.T_ANY: return "rb_any";
            case D2RezeptKatalog.T_UP_SOCKETS: return "rb_up_sockets";
            case D2RezeptKatalog.T_UP_EXC: return "rb_up_exc";
            case D2RezeptKatalog.T_UP_ELITE: return "rb_up_elite";
            case D2RezeptKatalog.T_UP_MAGIC: return "rb_up_magic";
            case D2RezeptKatalog.T_UP_RARE: return "rb_up_rare";
            case D2RezeptKatalog.T_UP_NORMAL: return "rb_up_normal";
            case D2RezeptKatalog.T_UP_REPAIR: return "rb_up_repair";
            case D2RezeptKatalog.T_UP_RECHARGE: return "rb_up_recharge";
            case D2RezeptKatalog.T_UP_UNSOCKET: return "rb_up_unsocket";
            case D2RezeptKatalog.T_UP_KEEPS: return "rb_up_keeps";
            case D2RezeptKatalog.T_PORTAL_COW: return "rb_portal_cow";
            case D2RezeptKatalog.T_PORTAL_PANDEMONIUM: return "rb_portal_pandemonium";
            case D2RezeptKatalog.T_PORTAL_FINALE: return "rb_portal_finale";
            case D2RezeptKatalog.T_RW_BASE: return "rb_rw_base";
            case D2RezeptKatalog.T_RW_LEVEL: return "rb_rw_level";
            case D2RezeptKatalog.T_LADDER: return "rb_ladder";
            case D2RezeptKatalog.T_SOCKETS: return "rb_sockets";
            case D2RezeptKatalog.T_WEAPON: return "rb_weapon";
            case D2RezeptKatalog.T_ARMOR: return "rb_armor";
            case D2RezeptKatalog.T_SHIELD: return "rb_shield";
            case D2RezeptKatalog.T_UP_LEVELREQ: return "rb_up_levelreq";
            default: return null;
        }
    }
}
