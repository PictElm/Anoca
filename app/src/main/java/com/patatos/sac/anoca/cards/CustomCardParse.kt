package com.patatos.sac.anoca.cards

import com.patatos.sac.anoca.cards.data.MyRoomDatabase

import kotlin.random.Random

class CustomCardParse(private var c: String, private var delayed: Boolean = false) {

    init {
        if (!this.delayed)
            this.doRegex(REGEX_MAKE)
    }

    private fun doRegex(withThat: List<Pair<Regex, String>>) {
        withThat.forEach {
            this.c = this.c.replace(it.first, it.second)
        }
    }

    fun toMarkup(): String {
        if (this.delayed) {
            this.delayed = false
            this.doRegex(REGEX_MAKE)
        }

        return this.c
    }

    override fun toString(): String {
        if (this.delayed)
            this.doRegex(REGEX_CLEAN)

        return this.c
    }

    companion object {

        // TODO: "<ruby>$1<rt>$2</rt></ruby>" --> needs an alternative to RubySpan
        private val REGEX_MAKE: List<Pair<Regex, String>> = listOf(
            Pair(Regex("\\^\\{(.*?)\\}"), "<sup><small>$1</small></sup>"),
            Pair(Regex("_\\{(.*?)\\}"), "<sub><small>$1</small></sub>"),
            Pair(Regex("\\*(.*?)\\*"), "<b>$1</b>"),
            Pair(Regex("(^|\\s)\\/(.*?)\\/(\\s|$)"), "$1<i>$2</i>$3"),
            Pair(Regex("_(.*?)_"), "<u>$1</u>"),
            Pair(Regex("`(.*?)`"), "<code>$1</code>"),
            Pair(Regex("~(.*?)~"), "<strike>$1</strike>"),
            Pair(Regex("\\{(.+?):(.+?)\\}"), "$1" +
                    "<sup>".repeat(5) + "<small>".repeat(5) + "<u>$2</u>" +
                    "</small>".repeat(5) + "</sup>".repeat(5)
            )
        )
        private val REGEX_CLEAN: List<Pair<Regex, String>> = listOf(
            Pair(Regex("\\^\\{(.*?)\\}"), "$1"),
            Pair(Regex("_\\{(.*?)\\}"), "$1"),
            Pair(Regex("\\*(.*?)\\*"), "$1"),
            Pair(Regex("(^|\\s)\\/(.*?)\\/(\\s|\$)"), "$1"),
            Pair(Regex("_(.*?)_"), "$1"),
            Pair(Regex("`(.*?)`"), "$1"),
            Pair(Regex("~(.*?)~"), "$1"),
            Pair(Regex("\\{(.+?):(.+?)\\}"), "$1")
        )

        /**
         * Changes `("This is a $category.f1;!", "And its $category.b1;?")`
         * into `("This is a front!", "And its back?")`.
         *
         * Uses special characters: `"$.?!;"`.
         * A valid inclusion matches: `\$(.+?)([.?!])([fb])(\w)(?:;|:(.*?)(?:;|$))`.
         *
         * Details for the groups:
         *  1. name of the category to include from
         *  0. special character for inclusion behaviour
         *  0. `f` or `b` to use respectively the front or the back of the card
         *  0. identifier of the inclusion
         *  0. a potential replacement string (default to empty)
         *
         * Details for each specials characters:
         *  1. `$` - marks the beginning of a inclusion
         *  0. `.` - marks a normal inclusion
         *  0. `?` - marks a probabilistic inclusion (one in two chances)
         *  0. `!` - marks a one-depth inclusion (included won't include in its turn)
         *  0. `:` - marks an alternative string to keep if inclusion is prevented
         *  0. `;` - marks the end of an inclusion
         */
        fun processNested(
            db: MyRoomDatabase,
            raw: Pair<String, String>,
            depth: Int = 3,
            inclusion: Boolean = true,
            regex: Regex = Regex("\\$(.+?)([.?!])([fb])(\\w)(?:;|:(.*?)(?:;|$))")
        ): Pair<String, String> {
            if (depth < 0)
                return raw

            val pickedCards = mutableMapOf<String, Pair<String, String>>()

            return Pair(
                raw.first.replace(regex) {
                    if (!inclusion) it.groupValues[5]
                    else {
                        var pair = pickedCards[it.groupValues[1] + it.groupValues[4]]
                        if (pair != null) when (it.groupValues[3]) {
                            "f" -> pair.first
                            "b" -> pair.second
                            else -> "`'${it.groupValues[3]}' is not a valid side`"
                        } else when (it.groupValues[2]) {
                            ".", "!", "?" -> {
                                if (it.groupValues[2] == "?" && Random.nextBoolean())
                                    it.groupValues[5]
                                else {
                                    val categories = db.getDao().findCategories(it.groupValues[1])
                                    if (categories.count() == 0)
                                        "`category '${it.groupValues[1]}' not found`"
                                    else {
                                        val cards = db.getDao().includeCard(categories[0].id)
                                        if (cards.count() == 0) it.groupValues[5]
                                        else {
                                            val card = cards[0]
                                            pair = processNested(
                                                db,
                                                Pair(card.dataFRaw, card.dataBRaw),
                                                depth - 1,
                                                it.groupValues[2] != "!"
                                            )
                                            pickedCards[it.groupValues[1] + it.groupValues[4]] = pair
                                            when (it.groupValues[3]) {
                                                "f" -> pair.first
                                                "b" -> pair.second
                                                else -> "`'${it.groupValues[3]}' is not a valid side`"
                                            }
                                        }
                                    }
                                }
                            } else -> "`'${it.groupValues[2]}' is not a valid punctuation`"
                        }
                    }
                },
                raw.second.replace(regex) {
                    val pair = pickedCards[it.groupValues[1] + it.groupValues[4]]
                    if (pair == null) it.groupValues[5]
                    else when (it.groupValues[3]) {
                        "f" -> pair.first
                        "b" -> pair.second
                        else -> "`'${it.groupValues[3]}' is not a valid side`"
                    }
                }
            ) // ^processNested
        }

    }

}
