










static int isActBoss(unsigned long cls) {
    switch (cls) {
        case 156:
        case 211:
        case 242:
        case 243:
        case 256:
        case 333:
        case 526:
        case 540:
        case 541:
        case 542:
        case 544:
        case 570:
        case 704:
        case 705:
        case 706:
        case 707:
        case 708:
        case 709:
            return 1;
        default:
            return 0;
    }
}
