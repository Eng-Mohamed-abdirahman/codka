package com.codka.app.voice

/** Which recorded clip in res/raw says each Somali word that [SomaliNumbers] produces. */
object VoiceClips {
    private val WORDS =
        mapOf(
            "eber" to "n_0_eber",
            "hal" to "n_1_hal",
            "kow" to "n_1_kow",
            "laba" to "n_2_laba",
            "saddex" to "n_3_saddex",
            "afar" to "n_4_afar",
            "shan" to "n_5_shan",
            "lix" to "n_6_lix",
            "toddoba" to "n_7_toddoba",
            "siddeed" to "n_8_siddeed",
            "sagaal" to "n_9_sagaal",
            "toban" to "n_10_toban",
            "labaatan" to "n_20_labaatan",
            "soddon" to "n_30_soddon",
            "afartan" to "n_40_afartan",
            "konton" to "n_50_konton",
            "lixdan" to "n_60_lixdan",
            "toddobaatan" to "n_70_toddobaatan",
            "siddeetan" to "n_80_siddeetan",
            "sagaashan" to "n_90_sagaashan",
            "boqol" to "n_100_boqol",
            "kun" to "n_1000_kun",
            "malyuun" to "n_1000000_malyuun",
            "iyo" to "w_iyo",
            "doolar" to "c_doolar",
            "shilin" to "c_shilin",
            "senti" to "c_senti"
        )

    /** Every clip this map can return (used by the tests to check the files exist). */
    val all: Collection<String> get() = WORDS.values

    fun forWord(word: String): String = requireNotNull(WORDS[word]) {
        "No clip for the word '$word'"
    }
}
