# What's new

<!-- One section per public release, newest first: "## <version> (<YYYY-MM-DD>)", then bullet points.
     The top section is the version being released; its text becomes the release notes and the app's "What's new" page. -->

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
