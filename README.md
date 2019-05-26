# Anoca

Anoca is a card studies application which present you with a new card each time you unlock your phone. These cards are Android alerts and can therefore be easily dismissed. This application was made as a try at [Kotlin](https://developer.android.com/kotlin) for Android.

> This application's aim is not to be a good study app (but rather satisfies my own needs)! This README.md doesn't go into too much technical details.
> 
> Also note that this application was build in English and include a French translation. Some interface elements may not be labelled the same.

---

## Categories

All the cards you create must be in a category. One card can only be in 1 category at a time. When you create a cards, you are asked in which category it should be sorted (by default, is automatically selects the one you were looking at).

A category can be renamed or disabled from it's viewing menu by pressing the "Edit" button. A disabled category will no longer be used when Anoca is picking cards to display.

---

## Cards

A card is the Android alert that pops up when unlocking the device with Anoca activated. There are 4 kind of study cards available for now. Each kind can be deactivated individually from the Setting activity.

### Two-sided

Uses 1 note.

A simple tow-sided card. When you tap on the front, the card will flip to reveal its back. And when you tap its back, you get its front back. You should dismiss this card by clicking on "Ok" or away from the alert.

### Writing task

Uses 1 note.

A writing task present a side a note and asks for you to type its back. The text entered should not contain any formatting the note's side may contain (see [Content]()).

### Multiple choices

Uses 2 notes.

A multiple choices card pick 2 notes, shuffles backs and fronts before organising them in a grid of 2 by 2. You must tap the two side of a same card consecutively (basically matching them to continue).

### Associate

Uses 3 notes (or rather 2.5).

An associate type of card will effectively pick 3 notes from the database, but only uses 5 sides form the 3. It first shows one of the side like a two-sided. When pressed, the card will flip to reveal a grid similar to the multiple choices type of card. You must then select the note side from the grid that correspond to the first side of the card.

---

## Notes

A card, as displayed when unlocking the device, is made of 1 to 3 notes picked from a database. Each note present a 'front' and a 'back' (like _actual cards_).

When "creating a card", you are actually creating one of theses two-sided notes to be added to the database. The "Weight" setting of a note allows you to force the picking algorithm to consider this note multiple times (by default 1), hence skewing the probabilities of this note being picked. A card with 0 weight won't be picked (but may be included).

### Content

A note may refer to an only document (using a full URL). If the targeted document is an image (`.png`, `.jpg`, `.jpeg`, `.gif`) it will be loaded into the card as such :3.

A text note can use some basic [not-]markdown:
* sup: `a^{2}`
* sub: `a_{2}`
* bold: `*main*`
* italic: `/print/`
* underline: `_hello_`
* code: ``` `world` ```
* strikethrough: `~not~`
* ruby notation (not properly functional tho): `{漢:ㄏㄢˋ}`

### Inclusion

A note may dynamically include another note using this syntax:

```
front = "Including the back of a card from the category 'cat': $cat.f1;."
back = "And including its back: $cat.b1;, and its front again: $cat.f1;!"
```

The key element is the next-to-last character (here: the '`1`'). This character will be a unique identifier for the included note for the whole including process (only at this level; the included note may, in its turn, include a note and identify-it by '`1`', this won't impact the previous layer and previous layer's included cards can't be accessed either). The identifying character may be any of the numbers of any of the letter (upper or lower case &mdash; case sensitive).

In the previous simple example, we only use the standard inclusion. There exist:
- `.` standard inclusion; Include a note from the specified category.
- `?` probable inclusion; Include a notes only every other time. Note that when a note is included when doing the front, it will also be for the back.
- `!` limited inclusion; When included, every limited inclusion it contains will fail.

> Note: the maximal depth of inclusion is.. like.. maybe 3?

A replacement behaviour can be added for any of the inclusion type:

```
front = "This may not include: $cat?f1:it didn't..;."
back = "$cat!b2:Hay!;"
```

If the card is not included, the text after the '`:`' will be used as a replacement.
