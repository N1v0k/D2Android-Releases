package com.winlator.d2;

import android.content.Context;

import com.winlator.R;





final class D2RezeptTexte implements D2RezeptKatalog.Texte {
    private final Context context;

    D2RezeptTexte(Context context) {
        this.context = context;
    }

    @Override
    public String t(int what, Object... args) {
        int id = id(what);
        return id != 0 ? D2Text.get(context, id, args) : "";
    }

    static int id(int what) {
        if (what >= D2RezeptKatalog.T_TYPE_BASE && what < D2RezeptKatalog.T_TYPE_BASE + D2RezeptKatalog.TYPE_CODES.length) {
            return TYPES[what - D2RezeptKatalog.T_TYPE_BASE];
        }
        switch (what) {
            case D2RezeptBuch.T_TITLE: return R.string.rb_title;
            case D2RezeptBuch.T_TAB_CAN: return R.string.rb_tab_can;
            case D2RezeptBuch.T_TAB_CUBE: return R.string.rb_tab_cube;
            case D2RezeptBuch.T_TAB_RUNEWORDS: return R.string.rb_tab_runewords;
            case D2RezeptBuch.T_FILTER_ALL: return R.string.rb_filter_all;
            case D2RezeptBuch.T_FILTER_READY: return R.string.rb_filter_ready;
            case D2RezeptBuch.T_FILTER_ALMOST: return R.string.rb_filter_almost;
            case D2RezeptBuch.T_SEARCH_HINT: return R.string.rb_search_hint;
            case D2RezeptBuch.T_PICK: return R.string.rb_pick;
            case D2RezeptBuch.T_PICK_HINT: return R.string.rb_pick_hint;
            case D2RezeptBuch.T_READY: return R.string.rb_ready;
            case D2RezeptBuch.T_STASH: return R.string.rb_stash;
            case D2RezeptBuch.T_MISSING: return R.string.rb_missing;
            case D2RezeptBuch.T_CHAIN: return R.string.rb_chain;
            case D2RezeptBuch.T_PAGE: return R.string.rb_page;
            case D2RezeptBuch.T_HINT_TOUCH: return R.string.rb_hint_touch;
            case D2RezeptBuch.T_HINT_PAD: return R.string.rb_hint_pad;
            case D2RezeptBuch.T_BACK: return R.string.rb_back;
            case D2RezeptBuch.T_HAVE: return R.string.rb_have;
            case D2RezeptBuch.T_LACKS: return R.string.rb_lacks;
            case D2RezeptBuch.T_INGREDIENTS: return R.string.rb_ingredients;
            case D2RezeptBuch.T_LADDER_OFF: return R.string.rb_ladder_off;
            case D2RezeptBuch.T_WHERE_INV: return R.string.rb_where_inv;
            case D2RezeptBuch.T_WHERE_CUBE: return R.string.rb_where_cube;
            case D2RezeptBuch.T_WHERE_STASH: return R.string.rb_where_stash;
            case D2RezeptBuch.T_WHERE_MAT: return R.string.rb_where_mat;
            case D2RezeptBuch.T_CUBE_HAS: return R.string.rb_cube_has;
            case D2RezeptBuch.T_CUBE_MAKES: return R.string.rb_cube_makes;
            case D2RezeptBuch.T_CUBE_ALMOST: return R.string.rb_cube_almost;
            case D2RezeptBuch.T_MATERIALS_LINK: return R.string.rb_materials_link;
            case D2RezeptBuch.T_EMPTY: return R.string.rb_empty;
            case D2RezeptBuch.T_MISSING_N: return R.string.rb_missing_n;
            case D2RezeptBuch.T_ON_PAGE: return R.string.rb_on_page;
            case D2RezeptBuch.T_ON_PAGE_SHARED: return R.string.rb_on_page_shared;
            case D2RezeptBuch.T_LADDER_MARK: return R.string.rb_ladder_mark;
            case D2RezeptBuch.T_STALE: return R.string.rb_stale;
            case D2RezeptBuch.T_STALE_NONE: return R.string.rb_stale_none;
            case D2RezeptBuch.T_INSERT: return R.string.rb_insert;
            case D2RezeptKatalog.T_CAT_RUNES: return R.string.rb_cat_runes;
            case D2RezeptKatalog.T_CAT_GEMS: return R.string.rb_cat_gems;
            case D2RezeptKatalog.T_CAT_CRAFT: return R.string.rb_cat_craft;
            case D2RezeptKatalog.T_CAT_UPGRADE: return R.string.rb_cat_upgrade;
            case D2RezeptKatalog.T_CAT_QUEST: return R.string.rb_cat_quest;
            case D2RezeptKatalog.T_FAM_BLOOD: return R.string.rb_fam_blood;
            case D2RezeptKatalog.T_FAM_CASTER: return R.string.rb_fam_caster;
            case D2RezeptKatalog.T_FAM_HIT: return R.string.rb_fam_hit;
            case D2RezeptKatalog.T_FAM_SAFETY: return R.string.rb_fam_safety;
            case D2RezeptKatalog.T_CRAFT_NAME: return R.string.rb_craft_name;
            case D2RezeptKatalog.T_CRAFT_KIND: return R.string.rb_craft_kind;
            case D2RezeptKatalog.T_CRAFT_RANDOM: return R.string.rb_craft_random;
            case D2RezeptKatalog.T_CRAFT_LEVEL: return R.string.rb_craft_level;
            case D2RezeptKatalog.T_Q_LOW: return R.string.rb_q_low;
            case D2RezeptKatalog.T_Q_NORMAL: return R.string.rb_q_normal;
            case D2RezeptKatalog.T_Q_MAGIC: return R.string.rb_q_magic;
            case D2RezeptKatalog.T_Q_RARE: return R.string.rb_q_rare;
            case D2RezeptKatalog.T_Q_UNIQUE: return R.string.rb_q_unique;
            case D2RezeptKatalog.T_Q_SET: return R.string.rb_q_set;
            case D2RezeptKatalog.T_F_NOSOCKET: return R.string.rb_f_nosocket;
            case D2RezeptKatalog.T_F_SOCKETED: return R.string.rb_f_socketed;
            case D2RezeptKatalog.T_F_ETH: return R.string.rb_f_eth;
            case D2RezeptKatalog.T_F_NOETH: return R.string.rb_f_noeth;
            case D2RezeptKatalog.T_F_BASIC: return R.string.rb_f_basic;
            case D2RezeptKatalog.T_F_EXC: return R.string.rb_f_exc;
            case D2RezeptKatalog.T_F_ELITE: return R.string.rb_f_elite;
            case D2RezeptKatalog.T_ANY: return R.string.rb_any;
            case D2RezeptKatalog.T_UP_SOCKETS: return R.string.rb_up_sockets;
            case D2RezeptKatalog.T_UP_EXC: return R.string.rb_up_exc;
            case D2RezeptKatalog.T_UP_ELITE: return R.string.rb_up_elite;
            case D2RezeptKatalog.T_UP_MAGIC: return R.string.rb_up_magic;
            case D2RezeptKatalog.T_UP_RARE: return R.string.rb_up_rare;
            case D2RezeptKatalog.T_UP_NORMAL: return R.string.rb_up_normal;
            case D2RezeptKatalog.T_UP_REPAIR: return R.string.rb_up_repair;
            case D2RezeptKatalog.T_UP_RECHARGE: return R.string.rb_up_recharge;
            case D2RezeptKatalog.T_UP_UNSOCKET: return R.string.rb_up_unsocket;
            case D2RezeptKatalog.T_UP_KEEPS: return R.string.rb_up_keeps;
            case D2RezeptKatalog.T_PORTAL_COW: return R.string.rb_portal_cow;
            case D2RezeptKatalog.T_PORTAL_PANDEMONIUM: return R.string.rb_portal_pandemonium;
            case D2RezeptKatalog.T_PORTAL_FINALE: return R.string.rb_portal_finale;
            case D2RezeptKatalog.T_RW_BASE: return R.string.rb_rw_base;
            case D2RezeptKatalog.T_RW_LEVEL: return R.string.rb_rw_level;
            case D2RezeptKatalog.T_LADDER: return R.string.rb_ladder;
            case D2RezeptKatalog.T_SOCKETS: return R.string.rb_sockets;
            case D2RezeptKatalog.T_WEAPON: return R.string.rb_weapon;
            case D2RezeptKatalog.T_ARMOR: return R.string.rb_armor;
            case D2RezeptKatalog.T_SHIELD: return R.string.rb_shield;
            case D2RezeptKatalog.T_UP_LEVELREQ: return R.string.rb_up_levelreq;
            default: return 0;
        }
    }


    private static final int[] TYPES = {
        R.string.rb_type_weap, R.string.rb_type_armo, R.string.rb_type_tors, R.string.rb_type_helm, R.string.rb_type_shld,
        R.string.rb_type_any, R.string.rb_type_gem0, R.string.rb_type_gem1, R.string.rb_type_gem2, R.string.rb_type_gem3,
        R.string.rb_type_gem4, R.string.rb_type_amul, R.string.rb_type_ring, R.string.rb_type_axe, R.string.rb_type_blun,
        R.string.rb_type_knif, R.string.rb_type_rod, R.string.rb_type_spea, R.string.rb_type_staf, R.string.rb_type_swor,
        R.string.rb_type_hpot, R.string.rb_type_mpot, R.string.rb_type_club, R.string.rb_type_h2h, R.string.rb_type_hamm,
        R.string.rb_type_mace, R.string.rb_type_mele, R.string.rb_type_miss, R.string.rb_type_pala, R.string.rb_type_pole,
        R.string.rb_type_scep, R.string.rb_type_wand, R.string.rb_type_glov, R.string.rb_type_boot, R.string.rb_type_belt,
    };
}
