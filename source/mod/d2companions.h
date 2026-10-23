








#ifndef D2COMPANIONS_H
#define D2COMPANIONS_H


static int isMercenary(unsigned long txt) {
    return txt == 271 || txt == 338 || txt == 359 || txt == 560 || txt == 561;
}






static int isSummon(unsigned long txt) {
    static const unsigned short summons[] = {289, 290, 291, 292, 351, 352, 353, 356, 357, 363, 364, 410, 411, 412, 413,
        415, 416, 417, 418, 419, 420, 421, 422, 423, 424, 425, 426, 427, 428};
    for (unsigned i = 0; i < sizeof(summons) / sizeof(summons[0]); i++) if (txt == summons[i]) return 1;
    return 0;
}

#endif
