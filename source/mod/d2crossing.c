
















#define CROSS_DEPTH 10
#define CROSS_SIDE  3
#define CROSS_CELLS 4096

typedef struct {
    const unsigned short* flags;
    long stride;
    long x, y, w, h;
    unsigned short blocked;
} CrossRoom;

static int crossOpen(const CrossRoom* r, int vertical, long edge, int dir, long u, long v) {
    long x = vertical ? edge + dir * v : u, y = vertical ? u : edge + dir * v;
    if (x < r->x || y < r->y || x >= r->x + r->w || y >= r->y + r->h) return 0;
    return !(r->flags[(y - r->y) * r->stride + (x - r->x)] & r->blocked);
}





static int crossingThrough(const CrossRoom* r, int vertical, long edge, int dir, long from, long to) {
    static unsigned char seen[CROSS_CELLS];
    static short queueU[CROSS_CELLS], queueV[CROSS_CELLS];

    long side0 = vertical ? r->y : r->x, side1 = (vertical ? r->y + r->h : r->x + r->w) - 1;
    long deep = vertical ? (dir > 0 ? r->x + r->w - edge : edge - r->x + 1) : (dir > 0 ? r->y + r->h - edge : edge - r->y + 1);
    if (deep <= 0 || to < from) return 0;
    long depth = deep < CROSS_DEPTH + 1 ? deep : CROSS_DEPTH + 1;
    long span = CROSS_CELLS / depth;
    long u0 = from - (span - (to - from + 1)) / 2;
    if (u0 < side0) u0 = side0;
    long u1 = u0 + span - 1;
    if (u1 > side1) u1 = side1;
    if (u1 - u0 + 1 < to - from + 1) return 1;
    memset(seen, 0, (size_t) ((u1 - u0 + 1) * depth));
    int head = 0, tail = 0;
    for (long u = from; u <= to; u++) {
        if (u < u0 || u > u1 || !crossOpen(r, vertical, edge, dir, u, 0)) continue;
        seen[(u - u0) * depth] = 1;
        queueU[tail] = (short) (u - u0);
        queueV[tail++] = 0;
    }
    while (head < tail) {
        long u = queueU[head] + u0, v = queueV[head++];
        if (v >= CROSS_DEPTH || v == deep - 1) return 1;
        if (v >= CROSS_SIDE && (u == side0 || u == side1)) return 1;
        if ((u == u0 && u0 > side0) || (u == u1 && u1 < side1)) return 1;
        static const int stepU[4] = {1, -1, 0, 0}, stepV[4] = {0, 0, 1, -1};
        for (int i = 0; i < 4; i++) {
            long nu = u + stepU[i], nv = v + stepV[i];
            if (nu < u0 || nu > u1 || nv < 0 || nv >= depth) continue;
            if (seen[(nu - u0) * depth + nv] || !crossOpen(r, vertical, edge, dir, nu, nv)) continue;
            seen[(nu - u0) * depth + nv] = 1;
            queueU[tail] = (short) (nu - u0);
            queueV[tail++] = (short) nv;
        }
    }
    return 0;
}






static int crossingBlocked(const CrossRoom* here, const CrossRoom* there, int vertical, long inside, long outside, long from,
                           long to) {
    int dir = outside > inside ? 1 : -1;
    if (!crossingThrough(there, vertical, outside, dir, from, to)) return 1;
    if (!crossingThrough(here, vertical, inside, -dir, from, to)) return 2;
    return 0;
}
