package com.patatos.sac.anoca.cards.data.csv

interface Csvable {

    fun csv(s: String, q: String): String

    companion object {

        /*fun dataToCsv(s: String, q: String, vararg data: Any): String {
            return List(data.count()) {
                val w = data[it].toString()
                if (w.contains(s)) "$q$w$q" else w
            } .joinToString(s)
        }

        fun csvToData(s: String, q: String, raw: String): List<String> {
            var saved: String? = null
            return raw.split(s).fold(mutableListOf()) { acc, cur ->
                when {
                    cur.endsWith(q) -> {
                        val nul = saved == null
                        acc.add((if (nul) "" else saved + s) + cur.substring(if (nul) 1 else 0, cur.length - 1))
                        saved = null
                    }
                    cur.startsWith(q) -> saved = cur.substring(1)
                    else -> acc.add(cur)
                }
                acc
            }
        }*/

        fun dataToCsv(s: String, q: String, vararg data: Any): String {
            return List(data.count()) { q + data[it].toString().replace("$q$s$q", s) + q }.joinToString(s)
        }

        fun csvToData(s: String, q: String, raw: String): List<String> {
            return raw.substring(q.length, raw.length - q.length).split("$q$s$q")
        }

        fun csvAll(s: String, q: String, o: List<Csvable>, names: String = ""): String {
            return o.fold(StringBuilder(if (names != "") "$names\n" else "")) { acc, it ->
                acc.append(
                    it.csv(
                        s,
                        q
                    ) + "\n"
                )
            }.toString()
        }

    }

}
