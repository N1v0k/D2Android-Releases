

#ifndef D2MENU_H
#define D2MENU_H







static inline int menuFieldSecret(unsigned long flags) {
    return (flags & (0x01 | 0x20 | 0x08)) != 0;
}

#endif
