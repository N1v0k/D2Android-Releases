
















#define SORT_MAX_W 10
#define SORT_MAX_H 8
#define SORT_MAX 80


static int sortW = 10, sortH = 4;
#define SORT_W sortW
#define SORT_H sortH


enum { SG_BIG = 0, SG_TOMES, SG_SCROLLS, SG_KEYS, SG_POTIONS, SG_RUNES, SG_GEMS, SG_JEWELS, SG_RINGS, SG_AMULETS,
       SG_CHARMS, SG_SMALL };

typedef struct {
    DWORD id;
    DWORD order;
    BYTE x, y, w, h;
    BYTE group;
    BYTE fixed;
    BYTE tx, ty;
} SortItem;

static int sortFits(const BYTE* grid, int x, int y, int w, int h) {
    if (x < 0 || y < 0 || x + w > SORT_W || y + h > SORT_H) return 0;
    for (int dy = 0; dy < h; dy++)
        for (int dx = 0; dx < w; dx++)
            if (grid[(y + dy) * SORT_W + x + dx]) return 0;
    return 1;
}

static void sortMark(BYTE* grid, int x, int y, int w, int h, BYTE value) {
    for (int dy = 0; dy < h; dy++)
        for (int dx = 0; dx < w; dx++)
            if (x + dx < SORT_W && y + dy < SORT_H) grid[(y + dy) * SORT_W + x + dx] = value;
}


static int sortBigBefore(const SortItem* a, const SortItem* b) {
    int areaA = a->w * a->h, areaB = b->w * b->h;
    if (areaA != areaB) return areaA > areaB;
    if (a->h != b->h) return a->h > b->h;
    if (a->order != b->order) return a->order < b->order;
    return a->id < b->id;
}

static int sortSmallBefore(const SortItem* a, const SortItem* b) {
    if (a->group != b->group) return a->group < b->group;
    if (a->order != b->order) return a->order < b->order;
    return a->id < b->id;
}


static int sortOrder(const SortItem* it, int n, int* out, int (*want)(const SortItem*),
                     int (*before)(const SortItem*, const SortItem*)) {
    int m = 0;
    for (int i = 0; i < n; i++) {
        if (!want(&it[i])) continue;
        int j = m++;
        while (j > 0 && before(&it[i], &it[out[j - 1]])) {
            out[j] = out[j - 1];
            j--;
        }
        out[j] = i;
    }
    return m;
}

static int sortIsBig(const SortItem* s) { return !s->fixed && s->group == SG_BIG; }
static int sortIsSmall(const SortItem* s) { return !s->fixed && s->group != SG_BIG; }
static int sortIsLoose(const SortItem* s) { return !s->fixed; }


static int sortPlace(SortItem* it, const int* idx, int m, BYTE* grid, int fromRight) {
    for (int k = 0; k < m; k++) {
        SortItem* s = &it[idx[k]];
        int placed = 0;
        for (int c = 0; c <= SORT_W - s->w && !placed; c++) {
            int x = fromRight ? SORT_W - s->w - c : c;
            for (int y = 0; y + s->h <= SORT_H && !placed; y++) {
                if (!sortFits(grid, x, y, s->w, s->h)) continue;
                sortMark(grid, x, y, s->w, s->h, 1);
                s->tx = (BYTE) x;
                s->ty = (BYTE) y;
                placed = 1;
            }
        }
        if (!placed) return 0;
    }
    return 1;
}









#define SORT_PERMS 720
#define SORT_KEEP 6

static int sortAtTarget(const SortItem* s);
static int sortNextMove(const SortItem* it, int n, int* k, BYTE* x, BYTE* y);

typedef struct {
    int free24, free23, rect, areas, filled, moved, rank;
    BYTE tx[SORT_MAX], ty[SORT_MAX];
} SortCand;



static int sortFreeColumns(const BYTE* grid, int h) {
    int count = 0;
    for (int x = 0; x + 2 <= SORT_W;) {
        int found = 0;
        for (int y = 0; y + h <= SORT_H;) {
            if (sortFits(grid, x, y, 2, h)) {
                found++;
                y += h;
            }
            else y++;
        }
        if (found) {
            count += found;
            x += 2;
        }
        else x++;
    }
    return count;
}



static int sortLargestRect(const BYTE* grid) {
    int best = 0, up[SORT_MAX_W] = {0};
    for (int y = 0; y < SORT_H; y++) {
        for (int x = 0; x < SORT_W; x++) up[x] = grid[y * SORT_W + x] ? 0 : up[x] + 1;
        for (int x0 = 0; x0 < SORT_W; x0++) {
            int h = up[x0];
            for (int x1 = x0; x1 < SORT_W && h > 0; x1++) {
                if (up[x1] < h) h = up[x1];
                if (h * (x1 - x0 + 1) > best) best = h * (x1 - x0 + 1);
            }
        }
    }
    return best;
}


static int sortAreas(const BYTE* grid) {
    BYTE seen[SORT_W * SORT_H];
    int stack[SORT_W * SORT_H], areas = 0;
    memcpy(seen, grid, sizeof(seen));
    for (int c = 0; c < SORT_W * SORT_H; c++) {
        if (seen[c]) continue;
        areas++;
        int top = 0;
        stack[top++] = c;
        seen[c] = 1;
        while (top) {
            int d = stack[--top], x = d % SORT_W, y = d / SORT_W;
            int next[4] = {x > 0 ? d - 1 : -1, x + 1 < SORT_W ? d + 1 : -1, y > 0 ? d - SORT_W : -1,
                           y + 1 < SORT_H ? d + SORT_W : -1};
            for (int k = 0; k < 4; k++)
                if (next[k] >= 0 && !seen[next[k]]) {
                    seen[next[k]] = 1;
                    stack[top++] = next[k];
                }
        }
    }
    return areas;
}


static int sortCandBefore(const SortCand* a, const SortCand* b) {
    if (a->free24 != b->free24) return a->free24 > b->free24;
    if (a->free23 != b->free23) return a->free23 > b->free23;

    if ((a->filled > 0) != (b->filled > 0)) return a->filled == 0;
    if (a->rect != b->rect) return a->rect > b->rect;
    if (a->areas != b->areas) return a->areas < b->areas;
    if (a->filled != b->filled) return a->filled < b->filled;
    if (a->moved != b->moved) return a->moved < b->moved;
    return a->rank < b->rank;
}


static int sortPlaceSmall(SortItem* it, const int* idx, int m, BYTE* grid, int edge, int fill, int* filled) {
    for (int k = 0; k < m; k++) {
        SortItem* s = &it[idx[k]];
        int placed = 0;
        for (int c = 0; fill && c + s->w <= edge && !placed; c++)
            for (int y = 0; y + s->h <= SORT_H && !placed; y++)
                if (sortFits(grid, c, y, s->w, s->h)) {
                    sortMark(grid, c, y, s->w, s->h, 1);
                    s->tx = (BYTE) c;
                    s->ty = (BYTE) y;
                    placed = 1;
                    (*filled)++;
                }
        if (placed) continue;
        if (!sortPlace(it, &idx[k], 1, grid, 1)) return 0;
        sortMark(grid, s->tx, s->ty, s->w, s->h, 1);
    }
    return 1;
}


static int sortPlayable(const SortItem* it, int n) {
    SortItem copy[SORT_MAX];
    memcpy(copy, it, (size_t) n * sizeof(SortItem));
    for (int moves = 0; moves <= 3 * n; moves++) {
        int k;
        BYTE x, y;
        int r = sortNextMove(copy, n, &k, &x, &y);
        if (r == 0) return 1;
        if (r < 0) return 0;
        copy[k].x = x;
        copy[k].y = y;
    }
    return 0;
}


static int sortNextPerm(int* a, int m) {
    int i = m - 2;
    while (i >= 0 && a[i] >= a[i + 1]) i--;
    if (i < 0) return 0;
    int j = m - 1;
    while (a[j] <= a[i]) j--;
    int t = a[i];
    a[i] = a[j];
    a[j] = t;
    for (int l = i + 1, r = m - 1; l < r; l++, r--) {
        t = a[l];
        a[l] = a[r];
        a[r] = t;
    }
    return 1;
}



static int sortSearch(SortItem* it, int n, int twoSided, SortCand* best, int kept, SortCand* plain) {
    int big[SORT_MAX], small[SORT_MAX], cls[SORT_MAX], seq[SORT_MAX], order[SORT_MAX];
    int nb = sortOrder(it, n, big, twoSided ? sortIsBig : sortIsLoose, sortBigBefore);
    int ns = twoSided ? sortOrder(it, n, small, sortIsSmall, sortSmallBefore) : 0;

    for (int k = 0; k < nb; k++) {
        cls[k] = k;
        for (int j = 0; j < k; j++)
            if (it[big[j]].w == it[big[k]].w && it[big[j]].h == it[big[k]].h) {
                cls[k] = cls[j];
                break;
            }
        seq[k] = cls[k];
    }
    int rank = 0;
    plain->rank = -1;
    do {
        for (int fill = 0; fill <= (twoSided && ns > 0); fill++) {
            BYTE grid[SORT_W * SORT_H];
            memset(grid, 0, sizeof(grid));
            for (int i = 0; i < n; i++) {
                it[i].tx = it[i].x;
                it[i].ty = it[i].y;
                if (it[i].fixed) sortMark(grid, it[i].x, it[i].y, it[i].w, it[i].h, 1);
            }

            BYTE used[SORT_MAX] = {0};
            for (int k = 0; k < nb; k++)
                for (int j = 0; j < nb; j++)
                    if (!used[j] && cls[j] == seq[k]) {
                        used[j] = 1;
                        order[k] = big[j];
                        break;
                    }
            if (!sortPlace(it, order, nb, grid, 0)) continue;
            int edge = 0, filled = 0;
            for (int k = 0; k < nb; k++)
                if (it[order[k]].tx + it[order[k]].w > edge) edge = it[order[k]].tx + it[order[k]].w;
            if (ns && !sortPlaceSmall(it, small, ns, grid, edge, fill, &filled)) continue;
            SortCand c;
            c.free24 = sortFreeColumns(grid, 4);
            c.free23 = sortFreeColumns(grid, 3);
            c.rect = sortLargestRect(grid);
            c.areas = sortAreas(grid);
            c.filled = filled;
            c.moved = 0;
            for (int i = 0; i < n; i++) {
                c.tx[i] = it[i].tx;
                c.ty[i] = it[i].ty;
                c.moved += !sortAtTarget(&it[i]);
            }
            c.rank = rank * 2 + fill;
            if (c.rank == 0) *plain = c;

            int at = kept;
            while (at > 0 && sortCandBefore(&c, &best[at - 1])) at--;
            if (at < SORT_KEEP) {
                for (int j = (kept < SORT_KEEP ? kept : SORT_KEEP - 1); j > at; j--) best[j] = best[j - 1];
                best[at] = c;
                if (kept < SORT_KEEP) kept++;
            }
        }
        rank++;
    } while (rank < SORT_PERMS && sortNextPerm(seq, nb));
    return kept;
}


static int sortLayout(SortItem* it, int n) {
    static SortCand best[SORT_KEEP + 1];
    if (n > SORT_MAX) return 0;
    for (int pass = 1; pass <= 2; pass++) {
        SortCand plain;
        int kept = sortSearch(it, n, pass == 1, best, 0, &plain);
        if (!kept) continue;

        int tries = kept, inBest = 0;
        for (int j = 0; j < kept; j++) inBest |= best[j].rank == 0;
        if (plain.rank == 0 && !inBest) best[tries++] = plain;

        int pick = 0;
        for (int j = 0; j < tries; j++) {
            for (int i = 0; i < n; i++) {
                it[i].tx = best[j].tx[i];
                it[i].ty = best[j].ty[i];
            }
            if (sortPlayable(it, n)) {
                pick = j;
                break;
            }
        }
        for (int i = 0; i < n; i++) {
            it[i].tx = best[pick].tx[i];
            it[i].ty = best[pick].ty[i];
        }
        return pass;
    }
    for (int i = 0; i < n; i++) {
        it[i].tx = it[i].x;
        it[i].ty = it[i].y;
    }
    return 0;
}

static int sortAtTarget(const SortItem* s) {
    return s->x == s->tx && s->y == s->ty;
}

static int sortOverlap(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
    return ax < bx + bw && bx < ax + aw && ay < by + bh && by < ay + ah;
}





static int sortNextMove(const SortItem* it, int n, int* k, BYTE* x, BYTE* y) {
    BYTE grid[SORT_W * SORT_H];
    int open = 0;
    memset(grid, 0, sizeof(grid));
    for (int i = 0; i < n; i++) sortMark(grid, it[i].x, it[i].y, it[i].w, it[i].h, (BYTE) (i + 1));

    int order[SORT_MAX];
    int m = sortOrder(it, n, order, sortIsLoose, sortBigBefore);
    for (int j = 0; j < m; j++) {
        const SortItem* s = &it[order[j]];
        if (sortAtTarget(s)) continue;
        open++;
        int free = 1;
        for (int dy = 0; dy < s->h && free; dy++)
            for (int dx = 0; dx < s->w && free; dx++) {
                BYTE c = grid[(s->ty + dy) * SORT_W + s->tx + dx];
                if (c && c != order[j] + 1) free = 0;
            }
        if (free) {
            *k = order[j];
            *x = s->tx;
            *y = s->ty;
            return 1;
        }
    }
    if (!open) return 0;

    for (int j = 0; j < m; j++) {
        const SortItem* s = &it[order[j]];
        if (sortAtTarget(s)) continue;
        int blocks = 0;
        for (int i = 0; i < n && !blocks; i++)
            blocks = i != order[j] && !sortAtTarget(&it[i])
                && sortOverlap(s->x, s->y, s->w, s->h, it[i].tx, it[i].ty, it[i].w, it[i].h);
        if (!blocks) continue;
        for (int px = 0; px + s->w <= SORT_W; px++) {
            for (int py = 0; py + s->h <= SORT_H; py++) {
                if (px == s->x && py == s->y) continue;
                int ok = 1;
                for (int dy = 0; dy < s->h && ok; dy++)
                    for (int dx = 0; dx < s->w && ok; dx++) {
                        BYTE c = grid[(py + dy) * SORT_W + px + dx];
                        if (c && c != order[j] + 1) ok = 0;
                    }
                for (int i = 0; i < n && ok; i++)
                    if (!sortAtTarget(&it[i]) && sortOverlap(px, py, s->w, s->h, it[i].tx, it[i].ty, it[i].w, it[i].h))
                        ok = 0;
                if (ok) {
                    *k = order[j];
                    *x = (BYTE) px;
                    *y = (BYTE) py;
                    return 1;
                }
            }
        }
    }
    return -1;
}
