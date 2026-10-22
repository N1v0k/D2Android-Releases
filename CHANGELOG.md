# What's new

<!-- One section per public release, newest first: "## <version> (<YYYY-MM-DD>)", then bullet points.
     The top section is the version being released; its text becomes the release notes and the app's "What's new" page. -->

## 1.3.0 (2026-10-22)

- New start page in the game's look: your characters in their equipment, with the area of their last save. A tap starts the character straight into the game, without the game's menus and with its loading animation; a long press picks the difficulty. "New character" leads straight to the class choice, "Classic start" shows the game's menus as before, and saving and leaving the game in its Esc menu returns to the start page.
- Diablo II's title music plays on the app pages, and taps there click like in the game; "Music" at the top right switches both off.
- Gamepad support in the style of D2R: the left stick walks, the buttons attack, take and cast with a second row on LT, the D-pad drinks potions. A pointer works in the inventory, stash, vendors and menus; Start opens the menu, View the inventory or (held) a wheel of windows. The touch buttons hide while the pad is in use and come back with the first tap.
- Settings in the look of the game's options menu: groups with a search, shorter texts, the PlugY options, and reachable in the game from the ≡ menu.
- Bottom bar: an info grid (FPS, battery, players, level, DPS or gold) in place of the stamina, an experience bar, and the clock on top.
- Frame rate limit, with 90, 120 and 144 FPS on displays that support them.
- Effect column: every status effect with its remaining time, Bone Armor's remaining protection, shrines with their picture, and auras shown as active as long as they are on.
- Damage numbers over time: poison, burning and open wounds show a summed number about every ¾ s, fire wall and inferno sums instead of a flood of ones, and DPS counts them. Hits of revived monsters are grey and not counted.
- Builds for every class (27 in all), "All points back" and "Spend all points following the build".
- "Take": the ring of targets sits in the middle and is chosen with a short swipe; holding "Take" on a townsperson shows their options as a ring.
- A tap on the mercenary's portrait opens his items; with a healing potion on the cursor it heals him.
- A tap on the death screen brings the character back.
- Stash: the gold amount to withdraw is filled in; the materials window fits into the stash; rune and gem names without "[fs]".
- Item comparison shows the weapon damage before and after, and the equipped item's tooltip moves above or below when there is no room beside it.
- Sorting the inventory leaves the largest free block; the loot filter is kept per character.
- Menu row: "Assign Keys", "Show Items" and walk/run next to them.
- Aim assist: a "Blind spot behind the character" (60° by default) – without the joystick, monsters right behind your back are left alone until you turn towards them.
- The buttons in the settings window ("Move buttons", help and troubleshooting) are drawn in the game's style.
- Installing: in portrait with only the steps shown; when something goes wrong (no network, full storage, missing permission), the page says what to do and offers a way on; a stopped patch continues on the next start; choosing a folder says what it did.
- A fresh install starts with the picture size "Large (540 lines)"; the app pages run in full screen.

- Holding attack or a spell no longer stalls after pressing again; a tapped skill casts while the joystick is held; "Attack" tapped while walking strikes once, then walks on; a tap into the bare world no longer moves the character.
- Teleport onto a spot with a monster above it lands on the marked spot.
- The game keeps running with picture and sound after a long time in the background with the screen off.
- The pentagram in the main menu no longer quits Diablo II; the game's Esc menu stays open after the app pause instead of crashing.
- Game buttons appear with the game world instead of over "Loading…".
- Auto gold picks gold up reliably, also while attacking, and leaves it lying when the purse is full.
- Quest scrolls (Inifuss, Horadric) can be used by tap; the quest button shows in the folded menu row when there is quest news.
- Opening a window while running stops the character at once; with the compact bottom bar, taps beside the orbs open nothing.
- The ≡ menu has no black band, "Quit" is visible; the status bar stays hidden after the screen goes off and on.
- Sharp text on the stone buttons.

## 1.2.1 (2026-10-18)

- Materials: taking a gem back out after "Deposit all" no longer crashes the game.

## 1.2.0 (2026-10-17)

- Shoulder buttons: besides a skill, each one can now attack, drink a healing potion, take, show names, open the map, inventory, a town portal and more.
- Teleport shows a ring on the ground where you will land; it jumps in the joystick's direction, avoids walls and obstacles, and shows a red cross instead of casting when there is no free spot.
- Inventory: hold the inventory button to sort it, or use the new "Sort" button next to the gold. A double tap on a potion puts it into the belt.
- "Refill potions" at vendors buys healing and mana potions for the belt and a small supply.
- "Materials": runes, gems and skulls get their own storage at the stash (separate for hardcore), with "store all" and taking one, three or all back.
- Item tooltips: the equipped item in the same place is shown next to the one you look at, below it the changes in life, mana, resistances and cast, hit recovery and block rates ("Show changes"), and a "Rating 7/10" line.
- Damage numbers rise over monsters, with DPS in the top left; plain or coloured by element.
- Loot filter: hides the names of junk on the ground (Loose, Strict or Own). Tapping an item still shows its name; runes, gems, uniques and sets always stay visible. With "Own", the ticks "Normal", "Inferior" and "Superior" each work on their own.
- "Build" page in the menu with a skill and stat plan for some classes; a golden frame highlights the recommended skill or stat when you have points to spend.
- Stats page: a tap on "+" adds one point, a double tap five, a long press all free points.
- "Tour": a short introduction to the buttons in the first game, which can be restarted from the menu.
- "Report a bug" in the game helpers opens the bug form on GitHub with only phone, Android version and app version filled in.
- "Skip the intro videos" (on by default) goes straight to the main menu.
- Larger stacks (tomes 80, keys 50) as an optional game helper, single player only.
- "Resume where I left off" (test, off by default): after loading, the character returns to the place of the last save, in areas with their own waypoint; if that place cannot be reached safely, to the area's waypoint.

- Saving is safer: the app checks saves like the game does, "Save and quit" waits until the character is fully written, and a save cut off by the end of the game is repaired from PlugY's backup before the next start.
- Dark mode, font size or language changes no longer rebuild the game window; when Android rebuilds it anyway, the app saves and quits first.
- Only one of the two app variants can use the game folder at a time; the other one says which app has it.
- A character saved at the moment of death loads with full life.
- Multiplayer entries in the main menu are locked; the app is single player only.
- Holding attack or an aimed spell with no monster in reach attacks the next one that comes close; switching targets mid-swing finishes the swing first.
- The character stops about one step after the joystick is released.
- "Take" on a person walks there around tents and walls and opens the talk without a second tap; in town it no longer targets your own summons.
- "Take" shows the item's name in its quality colour; the counter shows "1/4" without arrows where swiping does nothing.
- Auto gold no longer circles around gold when the gold purse is full.
- Automatic potions no longer add a large potion after you drank one yourself from the game's belt.
- Buttons: sharp icons in the top menu row, a golden ring around the round buttons, buttons stay away from the camera hole, and the game buttons appear only in the game, without flashing over the loading screen or the main menu.
- The top right button is now called "Assign"; the game's mini menu starts collapsed.
- Esc stays reachable while the keyboard is open.
- PlugY's stats page hint names the touch gestures instead of Alt/Shift, and PlugY no longer shows "players set to 1".
- Installing: a failed or cancelled installation leaves no half folder behind; when the installer only shows its start menu (often a wrong key), the app asks "Check keys" or "Keep waiting".

- Game sound is muted right away when the app goes to the background.
- Black game picture after a short trip to the home screen, and several graphics memory and hang problems.
- Special characters in the game font no longer show as "ÿ"; umlauts from the game's tables are shown correctly.
- Loading saved games gives clear messages and is locked while it runs.
- The launcher starts the game only once for several taps, "Back" cancels the start, and after an update it shows the game folder right away.
- The keyboard opens reliably on Android 8 to 10.
- Battery, clock and FPS stay right when the system clock changes; the pause shows the save time of the running character.
- Effect column: a second identical potion or shrine shows the full remaining time.

## 1.0.2 (2026-10-15)

- The in-game menu no longer shows the idle mode.
- First start: the app asks for permission to show notifications before the game starts, no longer over the starting game (which could stay black).
- Choosing the game: the app no longer searches the phone for a game folder by itself. Without a game it offers two ways, "Install the game (without a PC)" and "Choose game folder". After the update, choose your game folder once.
- Belt refill: when the game refuses one kind of potion, only that kind pauses briefly; the others keep refilling.
- Device check: phones whose Adreno GPU is not in the graphics driver's list are shown as "not tested" instead of "supported" (starting is still allowed).
- Automatic potions: when life drops fast or is already low, the largest potion is used, and a potion that is still working is only topped up with a larger one. Rejuvenation potions react faster.
- Item names on the ground no longer carry over from the previous game.
- A tap on a monster that had to wait no longer turns into a stray click after a hang or after returning from the background.
- PlugY stats pages: the page buttons are set up again if that failed once.

## 1.0.1 (2026-10-15)

- Installing the game without a PC: the expansion installer no longer stops and asks for the "Diablo II Game CD" on a fresh installation.
- Installing the game without a PC when the installers are already in Download/D2Android (for example after reinstalling the app): the installation no longer stops with "Path not found".
- Pausing: when the app stays in the background, the pause is handed over to the game's own menu cleanly.
- Sound: more robust audio output.
- Importing saves: a failed import is now rolled back completely.
- Take button: picks up items and uses objects directly through the game instead of simulated clicks.

## 1.0.0 (2026-10-15)

- First public release: Diablo II: Lord of Destruction 1.13d on Android phones with Snapdragon and Adreno graphics.
- Install the game without a PC: the app downloads Blizzard's official installers, you type in your own CD keys, and it installs and patches the game to 1.13d (English or German). An existing 1.13d game folder works too.
- Touch controls: joystick, attack button, skill buttons and shoulder triggers; buttons can be moved and resized.
- Take button and take wheel: pick up items, open chests, use portals and waypoints, talk to people.
- Widescreen picture that fills the display, in three sizes; bottom bar drawn with graphics from your own game files.
- Aim assist towards the joystick direction, with the target highlighted.
- Saves before every pause; pauses in the background or with the screen off.
- Optional helpers: map reveal, picking up gold and potions, refilling the belt, automatic potions, item names, running and stamina, players setting in single player.
- Optional PlugY: shared and larger stash, ladder rune words, taking back skill and attribute points.
- Share all characters and stashes as one zip file and load them on another phone.
- Buff column with the remaining time of shrines and potions.
- New versions are announced in the app, with a "What's new" page (the daily check can be turned off).
- App in English or German.
