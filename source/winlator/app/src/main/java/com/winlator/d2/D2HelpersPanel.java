package com.winlator.d2;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;











public final class D2HelpersPanel {
    private static final int OPTION_ID = 100;

    static final String KEY_TAB = "helpersTab";

    private D2HelpersPanel() {}


    public static LinearLayout create(Context context, Runnable onChange, java.util.function.Consumer<String> onAimChange) {
        return create(context, onChange, onAimChange, false);
    }


    public static LinearLayout create(Context context, Runnable onChange, java.util.function.Consumer<String> onAimChange, boolean scroll) {
        return create(context, onChange, onAimChange, scroll, null);
    }





    public static LinearLayout create(Context context, Runnable onChange, java.util.function.Consumer<String> onAimChange, boolean scroll,
                                      String lootScope) {
        return create(context, onChange, onAimChange, scroll, lootScope, null);
    }

    public static LinearLayout create(Context context, Runnable onChange, java.util.function.Consumer<String> onAimChange, boolean scroll,
                                      String lootScope, Runnable onEditButtons) {
        return create(context, onChange, onAimChange, scroll, lootScope, onEditButtons, null);
    }


    public interface Host {
        View view(D2HelperEntries.Entry e);
    }









    public static LinearLayout create(Context context, Runnable onChange, java.util.function.Consumer<String> onAimChange, boolean scroll,
                                      String lootScope, Runnable onEditButtons, Host host) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        D2PadFocus.all(root);
        TextView backLink = new TextView(context);
        backLink.setText(D2Text.get(context, com.winlator.R.string.settings_overview));
        backLink.setTextColor(D2Style.GOLD);
        backLink.setTextSize(14);
        backLink.setPadding(0, dp(context, 8), dp(context, 8), dp(context, 6));
        android.widget.EditText search = new android.widget.EditText(context);
        search.setHint(D2Text.get(context, com.winlator.R.string.settings_search_hint));
        search.setSingleLine(true);
        search.setTextColor(D2Style.TEXT);
        search.setHintTextColor(D2Style.HINT);
        search.setTextSize(14);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout groups = new LinearLayout(context);
        groups.setOrientation(LinearLayout.VERTICAL);
        android.widget.ScrollView holder = scroll ? new android.widget.ScrollView(context) : null;
        if (holder != null) holder.addView(content);
        android.widget.ScrollView groupHolder = new android.widget.ScrollView(context);
        groupHolder.addView(groups);

        boolean inGame = host == null && scroll;

        android.widget.FrameLayout searchBox = new android.widget.FrameLayout(context);
        searchBox.addView(search, new android.widget.FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        D2GameText[] searchHint = new D2GameText[1];
        View[] back = {backLink};
        boolean[] twoPanes = {host != null && twoPanes(context)};
        String[] page = {twoPanes[0] ? D2HelperEntries.SETTINGS_GROUPS.get(0) : null};
        Runnable[] show = new Runnable[1];


        Runnable refresh = () -> D2PadFocus.keep(content, show[0]);
        Runnable build = () -> {
            root.removeAllViews();
            D2SettingsLook look = D2SettingsLook.get();
            if (look != null && back[0] == backLink) {

                back[0] = inGame ? framedText(context, look, D2Text.get(context, com.winlator.R.string.settings_overview_stone),
                    () -> backLink.performClick())
                    : D2ButtonTiers.make(context, look, D2ButtonTiers.TEXT,

                    "< " + D2Text.get(context, com.winlator.R.string.settings_overview_stone), () -> backLink.performClick());
            }
            if (look != null && searchHint[0] == null) {

                searchHint[0] = new D2GameText(context, look, look.formal10, GREY, D2Text.get(context, com.winlator.R.string.settings_search_hint));
                searchHint[0].setPadding(dp(context, 4), dp(context, 14), 0, 0);
                searchHint[0].setVisibility(search.length() == 0 ? View.VISIBLE : View.GONE);
                search.setHint("");
                search.setTextColor(0xFFC8B074);
                searchBox.addView(searchHint[0], new android.widget.FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
                search.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence t, int a, int b, int c) {}
                    @Override public void onTextChanged(CharSequence t, int a, int b, int c) {}
                    @Override public void afterTextChanged(android.text.Editable e) {
                        if (searchHint[0] != null) searchHint[0].setVisibility(e.length() == 0 ? View.VISIBLE : View.GONE);
                    }
                });
            }
            detach(back[0]);
            detach(backLink);
            detach(searchBox);
            detach(groupHolder);
            View body = holder != null ? holder : content;
            detach(body);

            LinearLayout.LayoutParams fill = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                host != null ? 0 : LinearLayout.LayoutParams.WRAP_CONTENT, host != null ? 1 : 0);
            if (!twoPanes[0]) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = lp.bottomMargin = dp(context, 4);
                root.addView(back[0], lp);
            }
            root.addView(searchBox, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            View line = new View(context);
            line.setBackgroundColor(D2Style.FRAME);
            root.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 2)));
            if (!twoPanes[0]) {
                root.addView(body, fill);
                return;
            }
            LinearLayout panes = new LinearLayout(context);
            panes.setOrientation(LinearLayout.HORIZONTAL);
            panes.setBaselineAligned(false);
            panes.addView(groupHolder, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3));
            View split = new View(context);
            split.setBackgroundColor(D2Style.FRAME);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(context, 1), LinearLayout.LayoutParams.MATCH_PARENT);
            lp.leftMargin = lp.rightMargin = dp(context, 14);
            panes.addView(split, lp);
            panes.addView(body, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 7));
            root.addView(panes, fill);
        };
        java.util.function.Consumer<String> open = (group) -> {
            page[0] = group;
            if (twoPanes[0]) search.setText("");
            show[0].run();
            if (holder != null) holder.scrollTo(0, 0);
            padFocusFirst(content);
        };
        show[0] = () -> {
            String query = search.getText().toString().trim();
            boolean overview = page[0] == null;
            back[0].setVisibility(overview && query.isEmpty() ? View.GONE : View.VISIBLE);

            searchBox.setVisibility(overview || twoPanes[0] ? View.VISIBLE : View.GONE);
            content.removeAllViews();
            java.util.List<D2HelperEntries.Entry> entries = visible(context, inGame, host != null);
            if (twoPanes[0]) {
                groups.removeAllViews();
                overview(context, groups, entries, query.isEmpty() ? page[0] : "", open);
            }
            if (!query.isEmpty() && (overview || twoPanes[0])) {
                hits(context, content, query, entries, onChange, onAimChange, refresh, lootScope, onEditButtons, host);
            }
            else if (overview) overview(context, content, entries, null, open);
            else group(context, content, page[0], entries, onChange, onAimChange, refresh, lootScope, onEditButtons, host);
        };
        Runnable toOverview = () -> {
            if (!twoPanes[0]) page[0] = null;
            search.setText("");
            show[0].run();
            if (holder != null) holder.scrollTo(0, 0);
            padFocusFirst(content);
        };
        backLink.setOnClickListener((v) -> toOverview.run());
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void afterTextChanged(android.text.Editable e) {
                if (page[0] == null || twoPanes[0]) show[0].run();
            }
        });


        float[] scrolled = {0};
        if (host != null && holder != null) holder.setOnScrollChangeListener((sv, x, y, ox, oy) -> {
            if (!sv.isInLayout()) scrolled[0] = D2Orientation.scrollShare(y, scrollRange(holder));
        });
        if (host != null) root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            boolean now = twoPanes(context);
            if (now == twoPanes[0]) return;
            twoPanes[0] = now;
            boolean searching = !search.getText().toString().trim().isEmpty();


            float at = page[0] != null || searching ? scrolled[0] : 0;
            if (now && page[0] == null) page[0] = D2HelperEntries.SETTINGS_GROUPS.get(0);
            if (!now && searching) page[0] = null;
            v.post(() -> {
                build.run();
                show[0].run();
                if (holder != null) scrollToShare(holder, at);
            });
        });
        root.setTag(new Nav(() -> (!twoPanes[0] && page[0] != null) || !search.getText().toString().trim().isEmpty(), toOverview,
            open, () -> page[0], () -> shownGroups(visible(context, inGame, host != null)), content));
        build.run();
        show[0].run();

        if (D2SettingsLook.get() == null) D2SettingsLook.load(context, () -> {
            build.run();
            show[0].run();
        });
        return root;
    }


    private static int scrollRange(android.widget.ScrollView view) {
        return view.getChildCount() == 0 ? 0 : Math.max(0, view.getChildAt(0).getHeight() - view.getHeight());
    }


    private static void scrollToShare(android.widget.ScrollView holder, float share) {
        holder.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                v.removeOnLayoutChangeListener(this);
                v.post(() -> holder.scrollTo(0, Math.round(share * scrollRange(holder))));
            }
        });
        holder.requestLayout();
    }

    private static void detach(View view) {
        if (view.getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup) view.getParent()).removeView(view);
    }


    private static boolean twoPanes(Context context) {
        android.content.res.Configuration c = context.getResources().getConfiguration();
        return c.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE && c.screenWidthDp >= D2HelperEntries.TWO_COLUMNS_DP;
    }


    private static final class Nav {
        final java.util.function.BooleanSupplier away;
        final Runnable toOverview;
        final java.util.function.Consumer<String> open;
        final java.util.function.Supplier<String> current;

        final java.util.function.Supplier<java.util.List<String>> groups;
        final View content;

        Nav(java.util.function.BooleanSupplier away, Runnable toOverview, java.util.function.Consumer<String> open,
            java.util.function.Supplier<String> current, java.util.function.Supplier<java.util.List<String>> groups, View content) {
            this.away = away;
            this.toOverview = toOverview;
            this.open = open;
            this.current = current;
            this.groups = groups;
            this.content = content;
        }
    }


    private static java.util.List<String> shownGroups(java.util.List<D2HelperEntries.Entry> entries) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String group : D2HelperEntries.SETTINGS_GROUPS) {
            for (D2HelperEntries.Entry e : entries) {
                if (group.equals(D2HelperEntries.settingsGroup(D2HelperEntries.id(e)))) {
                    out.add(group);
                    break;
                }
            }
        }
        return out;
    }





    public static boolean step(View panel, int direction) {
        Object tag = panel != null ? panel.getTag() : null;
        if (!(tag instanceof Nav)) return false;
        Nav nav = (Nav) tag;
        String to = D2PadNav.nextGroup(nav.groups.get(), nav.current.get(), direction);
        if (to == null) return false;
        nav.open.accept(to);
        nav.content.post(() -> {
            View first = firstFocus(panel);

            if (first != null) first.requestFocusFromTouch();
        });
        return true;
    }





    public static boolean padBack(View panel) {
        Object tag = panel != null ? panel.getTag() : null;
        if (!(tag instanceof Nav)) return false;
        Nav nav = (Nav) tag;
        String was = nav.current.get();
        if (!back(panel)) return false;
        nav.content.post(() -> {
            java.util.List<String> groups = nav.groups.get();
            int at = was != null ? groups.indexOf(was) : -1;
            View row = nav.content instanceof android.view.ViewGroup && at >= 0 && at < ((android.view.ViewGroup) nav.content).getChildCount()
                ? ((android.view.ViewGroup) nav.content).getChildAt(at) : firstFocus(panel);
            if (row != null && row.isFocusable()) row.requestFocus();
            else if (firstFocus(panel) != null) firstFocus(panel).requestFocus();
        });
        return true;
    }


    public static View firstFocus(View panel) {
        Object tag = panel != null ? panel.getTag() : null;
        if (!(tag instanceof Nav)) return null;
        D2PadFocus.walk(((Nav) tag).content);
        return firstFocusable(((Nav) tag).content);
    }


    private static void padFocusFirst(View content) {
        if (content.isInTouchMode()) return;
        content.post(() -> {
            D2PadFocus.walk(content);
            View first = firstFocusable(content);
            if (first != null) first.requestFocus();
        });
    }


    private static View firstFocusable(View v) {
        if (v.getVisibility() != View.VISIBLE) return null;
        if (v.isFocusable() && v.isEnabled()) return v;
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View f = firstFocusable(g.getChildAt(i));
                if (f != null) return f;
            }
        }
        return null;
    }


    public static boolean back(View panel) {
        Object tag = panel != null ? panel.getTag() : null;
        if (!(tag instanceof Nav) || !((Nav) tag).away.getAsBoolean()) return false;
        ((Nav) tag).toOverview.run();
        return true;
    }


    public static void open(View panel, String group) {
        Object tag = panel != null ? panel.getTag() : null;
        if (tag instanceof Nav && group != null && D2HelperEntries.SETTINGS_GROUPS.contains(group)) ((Nav) tag).open.accept(group);
    }


    public static String group(View panel) {
        Object tag = panel != null ? panel.getTag() : null;
        return tag instanceof Nav ? ((Nav) tag).current.get() : null;
    }





    private static java.util.List<D2HelperEntries.Entry> visible(Context context, boolean inGame, boolean withHost) {
        java.util.List<D2HelperEntries.Entry> out = new java.util.ArrayList<>();
        D2HelperEntries.Text t = name -> text(context, name);
        for (D2HelperEntries.Entry e : inGame || withHost ? D2HelperEntries.settings(t) : D2HelperEntries.all(t)) {
            if (!D2HelperEntries.shownIn(e, com.winlator.BuildConfig.D2_INTERNAL) || hidden(context, e)) continue;
            out.add(e);
        }
        return out;
    }





    private static void overview(Context context, LinearLayout content, java.util.List<D2HelperEntries.Entry> entries,
                                 String selected, java.util.function.Consumer<String> open) {
        for (String group : D2HelperEntries.SETTINGS_GROUPS) {
            boolean any = false;
            for (D2HelperEntries.Entry e : entries) any |= group.equals(D2HelperEntries.settingsGroup(D2HelperEntries.id(e)));
            if (!any) continue;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(context, 10), 0, dp(context, 8));
            boolean look = D2SettingsLook.get() != null;
            row.addView(title(context, text(context, D2HelperEntries.groupTitle(group)) + (selected == null && !look ? "  ›" : ""),
                false, selected == null || group.equals(selected)));
            View desc = note(context, text(context, D2HelperEntries.groupDescription(group)));
            desc.setPadding(0, 0, 0, 0);
            row.addView(desc);
            row.setOnClickListener((v) -> open.accept(group));
            D2PadFocus.frame(row);
            content.addView(row);
        }
    }





    private static void group(Context context, LinearLayout content, String group, java.util.List<D2HelperEntries.Entry> entries,
                              Runnable onChange, java.util.function.Consumer<String> onAimChange, Runnable refresh,
                              String lootScope, Runnable onEditButtons, Host host) {
        content.addView(title(context, text(context, D2HelperEntries.groupTitle(group)), true, true));
        java.util.List<java.util.List<View>> blocks = new java.util.ArrayList<>();
        java.util.List<Integer> heights = new java.util.ArrayList<>();
        for (String section : D2HelperEntries.SECTIONS.get(group)) {
            java.util.List<View> block = new java.util.ArrayList<>();
            int rows = 1;
            for (D2HelperEntries.Entry e : entries) {
                String id = D2HelperEntries.id(e);
                if (!group.equals(D2HelperEntries.settingsGroup(id)) || !section.equals(D2HelperEntries.settingsSection(id))) continue;
                View v = element(context, e, onChange, onAimChange, refresh, lootScope, onEditButtons, host);
                if (v == null) continue;
                block.add(v);
                rows += D2HelperEntries.rows(e);
            }

            if (block.isEmpty()) continue;
            block.add(0, heading(context, text(context, D2HelperEntries.sectionTitle(section))));
            blocks.add(block);
            heights.add(rows);
        }
        columns(context, content, blocks, heights, false);
    }


    private static void hits(Context context, LinearLayout content, String query, java.util.List<D2HelperEntries.Entry> entries,
                             Runnable onChange, java.util.function.Consumer<String> onAimChange, Runnable refresh, String lootScope,
                             Runnable onEditButtons, Host host) {
        D2HelperEntries.Text t = name -> text(context, name);
        String place = null;
        java.util.List<D2HelperEntries.Hit> found = D2HelperEntries.search(query, entries, t);
        boolean any = false;
        for (D2HelperEntries.Hit hit : found) {
            View v = element(context, hit.entry, onChange, onAimChange, refresh, lootScope, onEditButtons, host);
            if (v == null) continue;

            String here = t.get(D2HelperEntries.groupTitle(hit.group)) + (D2SettingsLook.get() != null ? " - " : " › ")
                + t.get(D2HelperEntries.sectionTitle(hit.section));
            if (!here.equals(place)) content.addView(heading(context, here));
            place = here;
            content.addView(v);
            any = true;
        }
        if (!any) content.addView(note(context, D2Text.get(context, com.winlator.R.string.settings_no_hits)));
    }


    private static void columns(Context context, LinearLayout content, java.util.List<java.util.List<View>> blocks,
                                java.util.List<Integer> heights, boolean twoColumns) {
        int split = blocks.size();
        if (twoColumns) {
            int[] h = new int[heights.size()];
            for (int i = 0; i < h.length; i++) h[i] = heights.get(i);
            split = D2HelperEntries.splitAt(h);
        }
        if (split >= blocks.size()) {
            for (java.util.List<View> block : blocks) for (View v : block) content.addView(v);
            return;
        }
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        LinearLayout left = new LinearLayout(context), right = new LinearLayout(context);
        left.setOrientation(LinearLayout.VERTICAL);
        right.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < blocks.size(); i++) for (View v : blocks.get(i)) (i < split ? left : right).addView(v);
        row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        View line = new View(context);
        line.setBackgroundColor(D2Style.FRAME);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(context, 1), LinearLayout.LayoutParams.MATCH_PARENT);
        lp.leftMargin = lp.rightMargin = dp(context, 14);
        lp.topMargin = dp(context, 10);
        row.addView(line, lp);
        row.addView(right, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        content.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }


    private static String text(Context context, String name) {
        int id = context.getResources().getIdentifier(name, "string", context.getPackageName());
        return id == 0 ? name : D2Text.get(context, id);
    }



    private static boolean hidden(Context context, D2HelperEntries.Entry e) {
        if ("compareEquipped".equals(e.key)) return !D2Settings.compareCapable(context);

        if ("lootFilter".equals(e.key) || "lootHide".equals(e.key)) return !D2Settings.lootCapable(context);

        if ("valueLine".equals(e.key)) return !D2Settings.valueCapable(context);

        if ("materials".equals(e.key)) return !D2Settings.materialsCapable(context);

        if ("diffLines".equals(e.key)) return !D2Settings.diffCapable(context);

        if ("damageStyle".equals(e.key)) return !D2Settings.damageCapable(context);
        return false;
    }


    private static View element(Context context, D2HelperEntries.Entry e, Runnable onChange, java.util.function.Consumer<String> onAimChange, Runnable refresh,
                                String lootScope, Runnable onEditButtons, Host host) {
        if (e.kind == D2HelperEntries.Kind.HOST) return host != null ? host.view(e) : host(context, e, onEditButtons);
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        box.addView(row);

        boolean parent = e.key != null && (e.key.equals("aimAssist") || e.key.equals("mapFightHide") || e.key.equals("padEnabled"));
        Runnable changed = parent ? () -> {
            if (onChange != null) onChange.run();
            refresh.run();
        } : onChange;
        View main;
        View below = null;
        switch (e.kind) {
            case CHECK:
                main = toggle(context, text(e.label), bool(context, e.key), e.key, changed);
                break;
            case SLIDER:
                main = slider(context, e, onChange, onAimChange);
                break;
            case CHOICE:
                if (D2SettingsLook.get() != null) {
                    main = cycle(context, e, onChange, lootScope);
                    if ("lootFilter".equals(e.key)) below = note(context, D2Text.get(context, lootScope != null
                        ? com.winlator.R.string.helper_lootScope_character : com.winlator.R.string.helper_lootScope_default));
                    break;
                }
                main = label(context, e.label);
                below = choice(context, e, onChange, lootScope);
                break;
            case PINS:
                main = name(context, e.label);
                below = pins(context, e, onChange, lootScope);
                break;
            case TRIGGERS:
                main = name(context, e.label);
                below = triggers(context, e, onChange);
                break;
            case PAD:
                main = name(context, e.label);
                below = pad(context, e, onChange);
                break;
            default:

                boolean report = D2HelperEntries.ACTION_REPORT.equals(e.action);
                boolean intro = D2HelperEntries.ACTION_INTRO.equals(e.action);
                main = button(context, e.label, () -> {
                    if (intro) {

                        D2Settings.setHintsSeen(context, "");
                        if (onChange != null) onChange.run();
                        android.widget.Toast.makeText(context, D2Text.get(context, com.winlator.R.string.helper_intro_toast), android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!(context instanceof android.app.Activity)) return;
                    if (report) D2Diagnose.reportBug((android.app.Activity) context);
                    else D2Diagnose.share((android.app.Activity) context);
                });
                break;
        }
        row.addView(main, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        String infoText = e.lead == null ? e.info : (e.info == null ? e.lead : e.lead + " " + e.info);
        if (infoText != null && D2SettingsLook.get() != null) {
            box.addView(note(context, text(infoText)));
        }
        else if (infoText != null) {
            TextView info = hint(context, text(infoText));
            info.setVisibility(View.GONE);
            TextView i = new TextView(context);
            i.setText("i");
            i.setTextSize(14);
            i.setGravity(android.view.Gravity.CENTER);
            i.setTextColor(D2Style.HINT);
            android.graphics.drawable.GradientDrawable ring = new android.graphics.drawable.GradientDrawable();
            ring.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            ring.setStroke(dp(context, 1), D2Style.FRAME);
            i.setBackground(ring);
            i.setOnClickListener((v) -> info.setVisibility(info.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
            int size = dp(context, 26);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.leftMargin = dp(context, 8);
            row.addView(i, lp);
            box.addView(info);
        }
        if (below != null) box.addView(below);

        String needs = D2HelperEntries.dependsOn(e.key);
        if (needs != null && !bool(context, needs)) setEnabled(box, false);

        if ("resume".equals(e.key) && D2Settings.resumeBlocked(context)) {
            box.addView(note(context, D2Text.get(context, com.winlator.R.string.resume_off_regen)));
            setEnabled(box, false);
        }
        return box;
    }





    private static View host(Context context, D2HelperEntries.Entry e, Runnable onEditButtons) {
        if ("editButtons".equals(e.key) && onEditButtons != null) {
            return button(context, e.label, onEditButtons);
        }
        if ("sound".equals(e.key)) {
            LinearLayout sound = new LinearLayout(context);
            sound.setOrientation(LinearLayout.VERTICAL);
            sound.addView(toggle(context, e.label, D2Settings.sound(context), D2Settings.KEY_SOUND, () -> D2Music.soundChanged(context)));
            sound.addView(note(context, D2Text.get(context, com.winlator.R.string.settings_sound_hint)));
            return sound;
        }
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(name(context, e.label));


        int scope = D2HelperEntries.scope(e.key);
        int note = scope == D2HelperEntries.BEFORE_START ? com.winlator.R.string.settings_only_before_start
            : scope == D2HelperEntries.NEXT_START ? com.winlator.R.string.settings_next_start_page
            : "about".equals(e.key) ? com.winlator.R.string.about_disclaimer : com.winlator.R.string.settings_on_page;
        box.addView(note(context, D2Text.get(context, note)));
        if (scope != D2HelperEntries.IN_GAME) box.setAlpha(0.45f);
        return box;
    }

    private static void setEnabled(View view, boolean on) {
        view.setEnabled(on);
        view.setAlpha(on ? 1 : 0.45f);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) group.getChildAt(i).setEnabled(on);
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof android.view.ViewGroup) setEnabled(child, on);
            }
        }
    }



    private static final int GOLD = 0xC8B074, WHITE = 0xD2CEC6, GREY = 0x8C8C8C, DARK_GREY = 0x707070;


    private static View name(Context context, String text) {
        D2SettingsLook look = D2SettingsLook.get();
        if (look == null) return label(context, text);
        D2GameText view = new D2GameText(context, look, look.font16, WHITE, text);
        view.setPadding(dp(context, 8), dp(context, 6), dp(context, 4), 0);
        return view;
    }


    private static View note(Context context, String text) {
        D2SettingsLook look = D2SettingsLook.get();
        if (look == null) return hint(context, text);
        D2GameText view = new D2GameText(context, look, look.formal10, D2SettingsLook.HINT_RGB, text);
        view.gain = D2SettingsLook.HINT_GAIN;
        view.setPadding(dp(context, 10), 0, dp(context, 4), dp(context, 4));
        return view;
    }






    private static View button(Context context, String text, Runnable action) {
        D2SettingsLook look = D2SettingsLook.get();
        if (look == null) {
            android.widget.Button button = new android.widget.Button(context);
            button.setText(text);
            D2Style.button(button, context.getResources().getDisplayMetrics().density);
            button.setOnClickListener((v) -> action.run());
            return button;
        }
        LinearLayout box = new LinearLayout(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        boolean page = context instanceof D2SettingsActivity;
        lp.setMargins(page ? 0 : dp(context, 8), page ? 0 : dp(context, 6), 0, page ? 0 : dp(context, 6));
        box.addView(page ? D2ButtonTiers.make(context, look, D2ButtonTiers.TEXT, text, action) : framedText(context, look, text, action), lp);
        return box;
    }





    static View framedText(Context context, D2SettingsLook look, String text, Runnable action) {
        D2GameText view = new D2GameText(context, look, look.font16, GOLD, text);
        view.wrapWidth = true;
        int p = dp(context, 10);
        view.setPadding(p, dp(context, 6), p, dp(context, 4));
        android.graphics.drawable.GradientDrawable frame = new android.graphics.drawable.GradientDrawable();
        frame.setColor(0xC0000000);
        frame.setStroke(Math.max(1, dp(context, 1)), 0xFF8C7846);
        view.setBackground(frame);
        view.setClickable(true);
        D2PadFocus.frame(view);
        view.setOnClickListener((v) -> action.run());
        return view;
    }


    private static View heading(Context context, String text) {
        D2SettingsLook look = D2SettingsLook.get();
        if (look == null) return subheading(context, text);
        D2GameText view = new D2GameText(context, look, look.font16, GOLD, text);
        view.setPadding(0, dp(context, 12), 0, dp(context, 2));
        return view;
    }


    private static View title(Context context, String text, boolean big, boolean gold) {
        D2SettingsLook look = D2SettingsLook.get();
        if (look == null) {
            TextView view = new TextView(context);
            view.setText(text);
            view.setTextSize(big ? 18 : 16);
            D2Style.heading(view);
            if (!gold) view.setTextColor(D2Style.TEXT);
            return view;
        }

        if (big) return new D2GameText(context, look, look.font30, gold ? GOLD : WHITE, text);
        D2GameText view = new D2GameText(context, look, look.font24, gold ? GOLD : WHITE, text);
        view.gain = D2SettingsLook.TITLE_GAIN;
        return view;
    }





    private static View toggle(Context context, String label, boolean checked, String key, Runnable onChange) {
        D2SettingsLook look = D2SettingsLook.get();
        if (look == null) return check(context, label, checked, key, onChange);
        return gameSwitch(context, look, label, checked, (on) -> {
            D2Settings.putBoolean(context, key, on);
            if (onChange != null) onChange.run();
        });
    }





    static View gameSwitch(Context context, D2SettingsLook look, String label, boolean checked, java.util.function.Consumer<Boolean> set) {
        D2GameText view = new D2GameText(context, look, look.font16, WHITE, label);
        view.setPadding(dp(context, 8), dp(context, 8), dp(context, 4), dp(context, 4));
        boolean[] on = {checked};
        Runnable show = () -> view.setValue(D2Text.get(context, on[0] ? com.winlator.R.string.settings_on : com.winlator.R.string.settings_off),
            on[0] ? GOLD : DARK_GREY);
        show.run();
        view.setClickable(true);
        D2PadFocus.frame(view);
        view.setOnClickListener((v) -> {
            if (!v.isEnabled()) return;
            on[0] = !on[0];
            show.run();
            set.accept(on[0]);
        });
        view.step = (dir) -> {
            if (on[0] == dir > 0) return false;
            view.performClick();
            return true;
        };
        return view;
    }


    private static View cycle(Context context, D2HelperEntries.Entry e, Runnable onChange, String lootScope) {
        D2SettingsLook look = D2SettingsLook.get();
        int[] values = choiceValues(e.key);
        int current = choiceValue(context, e.key, lootScope);
        int[] at = {e.key.equals("potions") ? values.length - 1 : 0};
        for (int i = 0; i < values.length; i++) if (values[i] == current) at[0] = i;
        D2GameText view = new D2GameText(context, look, look.font16, WHITE, e.label);
        view.setPadding(dp(context, 8), dp(context, 8), dp(context, 4), dp(context, 4));
        view.setValue(e.options[at[0]], GOLD);
        boolean loot = "lootFilter".equals(e.key);
        view.setClickable(true);
        D2PadFocus.frame(view);
        java.util.function.IntConsumer take = (i) -> {
            at[0] = i;
            if (loot) D2LabelFilter.putPreset(context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE), lootScope, values[at[0]]);
            else D2Settings.putInt(context, e.key, values[at[0]]);
            view.setValue(e.options[at[0]], GOLD);
            if (onChange != null) onChange.run();
        };
        view.setOnClickListener((v) -> {
            if (!v.isEnabled()) return;
            take.accept((at[0] + 1) % values.length);
        });
        view.step = (dir) -> {
            int to = at[0] + dir;
            if (to < 0 || to >= values.length) return false;
            take.accept(to);
            return true;
        };
        return view;
    }

    private static TextView label(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        D2Style.text(view);
        view.setPadding(dp(context, 8), dp(context, 6), 0, 0);
        return view;
    }

    private static TextView subheading(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(13);
        view.setTextColor(D2Style.GOLD);
        view.setPadding(0, dp(context, 10), 0, dp(context, 2));
        return view;
    }


    static String text(String raw) {
        return raw.replace("{SECONDS}", String.valueOf(D2LabelFilter.SECONDS))
            .replace("{LIFE}", String.valueOf(D2Settings.AUTO_LIFE_DEFAULT))
            .replace("{MANA}", String.valueOf(D2Settings.AUTO_MANA_DEFAULT));
    }


    static boolean bool(Context c, String key) {
        switch (key) {
            case "orbNumbers": return D2Settings.orbNumbers(c);
            case "skillNames": return D2Settings.skillNames(c);
            case "infoGrid": return D2Settings.infoGrid(c);
            case "clockTop": return D2Settings.clockTop(c);
            case "xpBar": return D2Settings.xpBar(c);
            case "xpLevel": return D2Settings.xpLevel(c);
            case "hiFps": case "hiBattery": case "hiPlayers": case "hiClock": case "hiLevel": case "hiDps": case "hiGold":

                return D2Settings.highlighted(c, java.util.Arrays.asList(D2InfoGrid.HIGHLIGHT_KEYS).indexOf(key));
            case "hidePointer": return D2Settings.hidePointer(c);
            case "aimAssist": return D2Settings.aimAssist(c);
            case "highlight": return D2Settings.highlight(c);
            case "mapHack": return D2Settings.mapHack(c);
            case "largeStacks": return D2Settings.largeStacks(c);
            case "materials": return D2Settings.materials(c);
            case "resume": return D2Settings.resume(c);
            case "mapFightHide": return D2Settings.mapFightHide(c);
            case "itemLabels": return D2Settings.itemLabels(c);
            case "compareEquipped": return D2Settings.compareEquipped(c);
            case "windowInfo": return D2Settings.windowInfo(c);
            case "valueLine": return D2Settings.valueLine(c);
            case "diffLines": return D2Settings.diffLines(c);
            case "dropLabels": case "labelsNearOnly": return D2LabelFilter.get(c, key);
            case "autoGold": return D2Settings.autoGold(c);
            case "pickPotions": case "pickScrolls": case "pickRunes": case "pickGems": case "pickJewelry": case "beltRefill":
                return D2AutoItems.get(c, key);
            case "itemsNeedLabels": return D2Settings.itemsNeedLabels(c);
            case "potionShop": return D2Settings.potionShop(c);
            case "autoTrade": return D2Settings.autoTrade(c);
            case "autoPause": return D2Settings.autoPause(c);
            case "alwaysRun": return D2Settings.alwaysRun(c);
            case "directWalk": return D2Settings.directWalk(c);
            case "tapMoves": return D2Settings.tapMoves(c);
            case "joystickVisible": return D2Settings.joystickVisible(c);
            case "padEnabled": return D2Settings.padEnabled(c);
            default: throw new IllegalArgumentException("no switch " + key);
        }
    }


    private static int[] choiceValues(String key) {
        switch (key) {
            case "hud": return new int[]{D2Settings.HUD_COMPACT, D2Settings.HUD_ORIGINAL};
            case "potions": return new int[]{D2Settings.POTIONS_KEYS, D2Settings.POTIONS_BELT, D2Settings.POTIONS_ALL};
            case "staminaMode": return new int[]{D2Settings.STAMINA_NORMAL, D2Settings.STAMINA_POTION, D2Settings.STAMINA_UNLIMITED};
            case "ringSort": return new int[]{D2Settings.RING_SORT_RANK, D2Settings.RING_SORT_NEAR};
            case "lootFilter": return new int[]{D2LabelFilter.LOOT_OFF, D2LabelFilter.LOOT_LOOSE, D2LabelFilter.LOOT_STRICT, D2LabelFilter.LOOT_OWN};
            case "damageStyle": return new int[]{D2DamageStyle.OFF, D2DamageStyle.SIMPLE, D2DamageStyle.ELEMENT};
            case "gridSlot1": case "gridSlot2": case "gridSlot3": case "gridSlot4":
                return new int[]{D2InfoGrid.EMPTY, D2InfoGrid.FPS, D2InfoGrid.BATTERY, D2InfoGrid.PLAYERS, D2InfoGrid.CLOCK, D2InfoGrid.LEVEL,
                    D2InfoGrid.DPS, D2InfoGrid.GOLD};
            case "boxSlot1": case "boxSlot2": case "boxSlot3":
                return new int[]{D2InfoGrid.EMPTY, D2InfoGrid.FPS, D2InfoGrid.BATTERY, D2InfoGrid.PLAYERS, D2InfoGrid.CLOCK, D2InfoGrid.LEVEL,
                    D2InfoGrid.DPS, D2InfoGrid.GOLD};
            default: throw new IllegalArgumentException("no choice " + key);
        }
    }

    private static int choiceValue(Context c, String key, String lootScope) {
        switch (key) {
            case "hud": return D2Settings.hud(c) == D2Settings.HUD_ORIGINAL ? D2Settings.HUD_ORIGINAL : D2Settings.HUD_COMPACT;
            case "potions": return D2Settings.potions(c);
            case "staminaMode": return D2Settings.staminaMode(c);
            case "ringSort": return D2Settings.ringSort(c);
            case "lootFilter": return D2LabelFilter.preset(c.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE), lootScope);
            case "damageStyle": return D2Settings.damageStyle(c);
            case "gridSlot1": case "gridSlot2": case "gridSlot3": case "gridSlot4": {
                int place = key.charAt(key.length() - 1) - '1';
                return D2InfoGrid.slot(D2Settings.gridSlot(c, place), place);
            }
            case "boxSlot1": case "boxSlot2": case "boxSlot3": {
                int place = key.charAt(key.length() - 1) - '1';
                return D2InfoGrid.boxSlot(D2Settings.boxSlot(c, place), place);
            }
            default: throw new IllegalArgumentException("no choice " + key);
        }
    }


    private static int choiceBase(String key) {
        switch (key) {
            case "hud": return OPTION_ID;
            case "potions": return OPTION_ID + 10;
            case "staminaMode": return OPTION_ID + 20;
            case "ringSort": return OPTION_ID + 30;
            case "lootFilter": return OPTION_ID + 40;
            case "damageStyle": return OPTION_ID + 50;
            case "gridSlot1": return OPTION_ID + 60;
            case "gridSlot2": return OPTION_ID + 70;
            case "gridSlot3": return OPTION_ID + 80;
            case "gridSlot4": return OPTION_ID + 90;
            case "boxSlot1": return OPTION_ID + 100;
            case "boxSlot2": return OPTION_ID + 110;
            case "boxSlot3": return OPTION_ID + 120;
            default: throw new IllegalArgumentException("no choice " + key);
        }
    }

    private static View choice(Context context, D2HelperEntries.Entry e, Runnable onChange, String lootScope) {
        RadioGroup group = new RadioGroup(context);
        int[] values = choiceValues(e.key);
        int base = choiceBase(e.key);

        int current = choiceValue(context, e.key, lootScope), checked = e.key.equals("potions") ? values.length - 1 : 0;
        for (int i = 0; i < e.options.length; i++) {
            option(context, group, base + i, e.options[i]);
            if (values[i] == current) checked = i;
        }
        group.check(base + checked);
        boolean loot = "lootFilter".equals(e.key);
        group.setOnCheckedChangeListener((g, id) -> {

            if (loot) D2LabelFilter.putPreset(context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE), lootScope, values[id - base]);
            else D2Settings.putInt(context, e.key, values[id - base]);
            if (onChange != null) onChange.run();
        });
        if (!loot) return group;

        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView whose = new TextView(context);
        whose.setText(D2Text.get(context, lootScope != null ? com.winlator.R.string.helper_lootScope_character : com.winlator.R.string.helper_lootScope_default));
        D2Style.hint(whose);
        box.addView(whose);
        box.addView(group);
        return box;
    }

    private static LinearLayout pins(Context context, D2HelperEntries.Entry e, Runnable onChange, String lootScope) {

        boolean loot = "lootHide".equals(e.key);
        android.content.SharedPreferences prefs = context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE);
        java.util.Set<String> pinned = loot ? null : D2Settings.pinnedMenu(context);
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        D2SettingsLook look = D2SettingsLook.get();
        if (look != null) {

            for (int i = 0; i < e.options.length / 2; i++) {
                final String key = e.options[2 * i];
                int bit = loot ? D2LabelFilter.OWN_BITS[java.util.Arrays.asList(D2LabelFilter.OWN_KEYS).indexOf(key)] : 0;
                boolean checked = loot ? (D2LabelFilter.own(prefs, lootScope) & bit) != 0 : pinned.contains(key);
                View line = gameSwitch(context, look, e.options[2 * i + 1], checked, (on) -> {
                    if (loot) {
                        int own = D2LabelFilter.own(prefs, lootScope);
                        D2LabelFilter.putOwn(prefs, lootScope, on ? own | bit : own & ~bit);
                    }
                    else D2Settings.putPinned(context, key, on);
                    if (onChange != null) onChange.run();
                });
                line.setPadding(dp(context, 24), line.getPaddingTop(), line.getPaddingRight(), line.getPaddingBottom());
                grid.addView(line);
            }
            return grid;
        }
        LinearLayout row = null;
        for (int i = 0; i < e.options.length / 2; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row);
            }
            final String key = e.options[2 * i];
            CheckBox box = new CheckBox(context);
            box.setText(e.options[2 * i + 1]);
            D2Style.control(box);
            int bit = loot ? D2LabelFilter.OWN_BITS[java.util.Arrays.asList(D2LabelFilter.OWN_KEYS).indexOf(key)] : 0;
            box.setChecked(loot ? (D2LabelFilter.own(prefs, lootScope) & bit) != 0 : pinned.contains(key));
            box.setOnCheckedChangeListener((view, on) -> {
                if (loot) {

                    int own = D2LabelFilter.own(prefs, lootScope);
                    D2LabelFilter.putOwn(prefs, lootScope, on ? own | bit : own & ~bit);
                }
                else D2Settings.putPinned(context, key, on);
                if (onChange != null) onChange.run();
            });
            row.addView(box, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        }
        return grid;
    }





    private static LinearLayout triggers(Context context, D2HelperEntries.Entry e, Runnable onChange) {
        LinearLayout table = new LinearLayout(context);
        table.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.addView(new TextView(context), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        float density = context.getResources().getDisplayMetrics().density;
        int column = Math.round(56 * density);
        for (int side = 0; side < 2; side++) {
            String sideText = D2Text.get(context, side == 0 ? com.winlator.R.string.helper_triggers_left : com.winlator.R.string.helper_triggers_right);
            View title;
            if (D2SettingsLook.get() != null) {
                D2GameText t = new D2GameText(context, D2SettingsLook.get(), D2SettingsLook.get().formal10, GREY, sideText);
                t.wrapWidth = true;
                LinearLayout cell = new LinearLayout(context);
                cell.setGravity(android.view.Gravity.CENTER);
                cell.addView(t);
                title = cell;
            }
            else {
                TextView plain = hint(context, sideText);
                plain.setGravity(android.view.Gravity.CENTER);
                title = plain;
            }
            head.addView(title, new LinearLayout.LayoutParams(column, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        table.addView(head);
        int rows = e.options.length / 2;
        RadioButton[][] radios = new RadioButton[2][rows];
        int[] current = {D2Settings.triggerLeft(context), D2Settings.triggerRight(context)};
        for (int i = 0; i < rows; i++) {
            final int action = Integer.parseInt(e.options[2 * i]);
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            View name = name(context, e.options[2 * i + 1]);
            row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            for (int side = 0; side < 2; side++) {
                final int col = side, index = i;
                RadioButton radio = new RadioButton(context);
                D2Style.control(radio);
                radio.setChecked(current[side] == action);
                radio.setContentDescription(e.options[2 * i + 1] + " " + (side == 0 ? "L" : "R"));
                radio.setOnClickListener((v) -> {

                    for (int k = 0; k < radios[col].length; k++) radios[col][k].setChecked(k == index);
                    D2Settings.setTrigger(context, col == 0, action);
                    if (onChange != null) onChange.run();
                });
                radios[side][i] = radio;
                LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(column, LinearLayout.LayoutParams.WRAP_CONTENT);
                row.addView(radio, cell);
            }
            table.addView(row);
        }
        return table;
    }





    private static LinearLayout pad(Context context, D2HelperEntries.Entry e, Runnable onChange) {
        LinearLayout table = new LinearLayout(context);
        table.setOrientation(LinearLayout.VERTICAL);
        int[][] map = D2Gamepad.parseMap(D2Settings.padMap(context));
        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        int name = dp(context, 56);
        head.addView(new TextView(context), new LinearLayout.LayoutParams(name, LinearLayout.LayoutParams.WRAP_CONTENT));
        for (int layer = 0; layer < 2; layer++) {
            head.addView(hint(context, D2Text.get(context, layer == 0 ? com.winlator.R.string.helper_pad_layer1 : com.winlator.R.string.helper_pad_layer2)),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        }
        table.addView(head);
        for (int b : D2Gamepad.ASSIGNABLE) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView key = new TextView(context);
            key.setText(D2Gamepad.NAMES[b]);
            D2Style.heading(key);
            key.setPadding(dp(context, 8), 0, 0, 0);
            row.addView(key, new LinearLayout.LayoutParams(name, LinearLayout.LayoutParams.WRAP_CONTENT));
            for (int layer = 0; layer < 2; layer++) {
                final int button = b, l = layer;
                TextView cell = new TextView(context);
                D2Style.text(cell);
                cell.setText(padActionName(context, map[layer][b]) + "  ▾");
                cell.setPadding(dp(context, 8), dp(context, 6), dp(context, 4), dp(context, 6));
                android.graphics.drawable.GradientDrawable frame = new android.graphics.drawable.GradientDrawable();
                frame.setStroke(dp(context, 1), D2Style.FRAME);
                cell.setBackground(frame);
                cell.setOnClickListener((v) -> choosePadAction(context, button, l == 1, cell, onChange));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                lp.setMargins(dp(context, 2), dp(context, 2), dp(context, 2), dp(context, 2));
                row.addView(cell, lp);
            }
            table.addView(row);
        }
        table.addView(hint(context, D2Text.get(context, com.winlator.R.string.helper_pad_fixed)));
        String device = D2GamepadInput.lastDevice, button = D2GamepadInput.lastButton;
        table.addView(hint(context, device == null ? D2Text.get(context, com.winlator.R.string.helper_pad_none)
            : D2Text.get(context, com.winlator.R.string.helper_pad_seen, device, button != null ? button : "–")));
        return table;
    }


    private static void choosePadAction(Context context, int button, boolean layer2, TextView cell, Runnable onChange) {
        String[] names = new String[D2TriggerAction.PAD.length];
        for (int i = 0; i < names.length; i++) names[i] = padActionName(context, D2TriggerAction.PAD[i]);

        int current = D2Gamepad.parseMap(D2Settings.padMap(context))[layer2 ? 1 : 0][button];
        float density = context.getResources().getDisplayMetrics().density;
        TextView title = new TextView(context);
        title.setText(D2Gamepad.slot(button, layer2).replace("+", " + "));
        title.setTextSize(20);
        D2Style.heading(title);
        int pad = Math.round(16 * density);
        title.setPadding(pad, pad, pad, Math.round(4 * density));
        android.widget.ArrayAdapter<String> rows = new android.widget.ArrayAdapter<String>(context,
                android.R.layout.simple_list_item_1, names) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                TextView row = (TextView) super.getView(position, convertView, parent);
                boolean chosen = D2TriggerAction.PAD[position] == current;
                row.setTextColor(chosen ? D2Style.GOLD : D2Style.TEXT);
                return row;
            }
        };
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context)
            .setCustomTitle(title)
            .setAdapter(rows, (d, which) -> {
                int[][] map = D2Gamepad.parseMap(D2Settings.padMap(context));
                map[layer2 ? 1 : 0][button] = D2TriggerAction.PAD[which];
                D2Settings.setPadMap(context, D2Gamepad.formatMap(map));
                cell.setText(names[which] + "  ▾");
                if (onChange != null) onChange.run();
            })
            .create();
        dialog.show();
        D2Sound.attach(dialog);
        android.view.Window window = dialog.getWindow();
        if (window != null) window.setBackgroundDrawable(D2Style.windowBackground(density));
    }


    private static String padActionName(Context context, int action) {
        if (D2TriggerAction.isSkillKey(action)) {
            return D2Text.get(context, com.winlator.R.string.pad_action_skill_key, D2TriggerAction.skillKeyIndex(action) + 1);
        }
        int id;
        switch (action) {
            case D2TriggerAction.ATTACK_OR_TAKE: id = com.winlator.R.string.trigger_action_attack_take; break;
            case D2TriggerAction.ATTACK: id = com.winlator.R.string.trigger_action_attack; break;
            case D2TriggerAction.HEAL: id = com.winlator.R.string.trigger_action_heal; break;
            case D2TriggerAction.MANA: id = com.winlator.R.string.trigger_action_mana; break;
            case D2TriggerAction.REJUV: id = com.winlator.R.string.trigger_action_rejuv; break;
            case D2TriggerAction.TAKE: id = com.winlator.R.string.trigger_action_take; break;
            case D2TriggerAction.PORTAL: id = com.winlator.R.string.trigger_action_portal; break;
            case D2TriggerAction.MAP: id = com.winlator.R.string.trigger_action_map; break;
            case D2TriggerAction.INVENTORY: id = com.winlator.R.string.trigger_action_inventory; break;
            case D2TriggerAction.NAMES: id = com.winlator.R.string.trigger_action_names; break;
            case D2TriggerAction.WEAPON_SWAP: id = com.winlator.R.string.pad_action_weapons; break;
            case D2TriggerAction.RUN_TOGGLE: id = com.winlator.R.string.pad_action_run; break;
            case D2TriggerAction.ESC_MENU: id = com.winlator.R.string.pad_action_esc; break;
            case D2TriggerAction.APP_MENU: id = com.winlator.R.string.pad_action_menu; break;
            default: id = com.winlator.R.string.pad_action_nothing; break;
        }
        return D2Text.get(context, id);
    }

    private static LinearLayout slider(Context context, D2HelperEntries.Entry e, Runnable onChange, java.util.function.Consumer<String> onAimChange) {
        java.util.function.IntFunction<String> percentOrOff = v -> v == 0 ? D2Text.get(context, com.winlator.R.string.helper_value_off)
            : D2Text.get(context, com.winlator.R.string.helper_value_below, v);
        switch (e.key) {
            case "aimRangeMelee":

                return slider(context, e.label, v -> Math.min(v, D2Settings.aimRange(context)) + " %", D2Settings.AIM_RANGE_MELEE_MIN, D2Settings.AIM_RANGE_MELEE_MAX, 5,
                    D2Settings.aimRangeMelee(context), e.key, onChange, onAimChange);
            case "aimRange":
                return slider(context, e.label, v -> v + " %", D2Settings.AIM_RANGE_MIN, D2Settings.AIM_RANGE_MAX, 5,
                    D2Settings.aimRange(context), e.key, onChange, onAimChange);
            case "aimCone":
                return slider(context, e.label, v -> "± " + v + "°", D2Settings.AIM_CONE_MIN, D2Settings.AIM_CONE_MAX, 5,
                    D2Settings.aimCone(context), e.key, onChange, onAimChange);
            case "aimBlind":
                return slider(context, e.label, v -> v == 0 ? D2Text.get(context, com.winlator.R.string.helper_value_off) : v + "°",
                    D2Settings.AIM_BLIND_MIN, D2Settings.AIM_BLIND_MAX, 10, D2Settings.aimBlind(context), e.key, onChange, onAimChange);
            case "autoLife":
                return slider(context, e.label, percentOrOff, 0, D2Settings.AUTO_POTION_MAX, 5, D2Settings.autoLife(context), e.key,
                    onChange, null);
            case "autoMana":
                return slider(context, e.label, percentOrOff, 0, D2Settings.AUTO_POTION_MAX, 5, D2Settings.autoMana(context), e.key,
                    onChange, null);
            case "autoRejuv":
                return slider(context, e.label, percentOrOff, 0, D2Settings.AUTO_POTION_MAX, 5, D2Settings.autoRejuv(context), e.key,
                    onChange, null);
            case "mercPotions":
                return slider(context, e.label, percentOrOff, 0, D2Settings.AUTO_POTION_MAX, 5, D2Settings.mercPotions(context), e.key,
                    onChange, null);
            case "players":
                return slider(context, e.label, v -> v == 1 ? D2Text.get(context, com.winlator.R.string.helper_players_one) : D2Text.get(context, com.winlator.R.string.helper_players_more, v), 1, 8, 1,
                    D2Settings.players(context), e.key, onChange, null);
            case "padCursorSpeed":
                return slider(context, e.label, v -> v + " %", D2Gamepad.CURSOR_SPEED_MIN, D2Gamepad.CURSOR_SPEED_MAX, 10,
                    D2Settings.padCursorSpeed(context), e.key, onChange, null);
            case "padDeadzone":
                return slider(context, e.label, v -> v + " %", D2Gamepad.DEADZONE_MIN, D2Gamepad.DEADZONE_MAX, 5,
                    D2Settings.padDeadzone(context), e.key, onChange, null);
            case "mapFightDelay":
                return slider(context, e.label, v -> v + " s", D2Settings.MAP_FIGHT_DELAY_MIN, D2Settings.MAP_FIGHT_DELAY_MAX, 1,
                    D2Settings.mapFightDelay(context), e.key, onChange, null);
            case "shopReserveHeal":
            case "shopReserveMana":
                return slider(context, e.label, v -> v == 0 ? D2Text.get(context, com.winlator.R.string.helper_shopReserve_none) : String.valueOf(v),
                    0, D2PotionShop.reserveMax(D2Settings.bigInventory(context)), 1, D2Settings.shopReserve(context, e.key.equals("shopReserveMana")),
                    e.key, onChange, null);
            case "minGold":
                return slider(context, e.label, v -> v == 0 ? D2Text.get(context, com.winlator.R.string.helper_value_off)
                        : String.format(java.util.Locale.getDefault(), "%,d", D2PotionShop.minGold(v)),
                    0, D2PotionShop.MIN_GOLD_STEPS.length - 1, 1, D2Settings.minGoldStep(context), e.key, onChange, null);
            default:
                throw new IllegalArgumentException("no slider " + e.key);
        }
    }

    private static CheckBox check(Context context, String label, boolean checked, String key, Runnable onChange) {
        CheckBox box = new CheckBox(context);
        box.setText(label);
        D2Style.control(box);
        box.setChecked(checked);
        box.setOnCheckedChangeListener((view, on) -> {
            D2Settings.putBoolean(context, key, on);
            if (onChange != null) onChange.run();
        });
        return box;
    }

    private static LinearLayout slider(Context context, String name, java.util.function.IntFunction<String> format, int min, int max,
                                       int step, int value, String key, Runnable onChange, java.util.function.Consumer<String> onAimChange) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(context, 8), dp(context, 4), 0, 0);
        D2SettingsLook look = D2SettingsLook.get();

        D2GameText gameLabel = look != null ? new D2GameText(context, look, look.font16, WHITE, name) : null;
        TextView label = look == null ? new TextView(context) : null;
        if (label != null) {
            D2Style.text(label);
            label.setText(name + ": " + format.apply(value));
            box.addView(label);
        }
        else {
            gameLabel.setValue(format.apply(value), GOLD);
            gameLabel.setPadding(0, dp(context, 4), dp(context, 4), 0);
            box.addView(gameLabel);
        }
        SeekBar bar = new SeekBar(context);
        D2Style.slider(bar);
        bar.setMax((max - min) / step);
        bar.setProgress((value - min) / step);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int v = min + progress * step;
                if (label != null) label.setText(name + ": " + format.apply(v));
                else gameLabel.setValue(format.apply(v), GOLD);
                if (!fromUser) return;
                D2Settings.putInt(context, key, v);
                if (onChange != null) onChange.run();
                if (onAimChange != null) onAimChange.accept(key);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (onAimChange != null) onAimChange.accept(key);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        box.addView(bar);
        return box;
    }

    private static void option(Context context, RadioGroup group, int id, String label) {
        RadioButton button = new RadioButton(context);
        button.setId(id);
        button.setText(label);
        D2Style.control(button);
        group.addView(button);
    }

    private static TextView hint(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(12);
        D2Style.hint(view);
        view.setPadding(dp(context, 8), 0, 0, dp(context, 4));
        return view;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
