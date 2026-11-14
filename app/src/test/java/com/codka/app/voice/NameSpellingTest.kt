package com.codka.app.voice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class NameSpellingTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "Axmed Cali, Ahmed Ali",
        "Maxamed Cabdi, Mahamed Abdi",
        "Cumar Faarax, Umar Faarah",
        "Xaliimo Dhuxul, Haliimo Duhul",
        "Qaasim Cismaan, Kaasim Ismaan",
        "Sacaad, Saad",
        "Hodan Jaamac, Hodan Jaama",
        "Khadar Shire, Khadar Shire",
        "Ugaas Nuur, Ugaas Nuur",
        "Faadumo Ca'ase, Faadumo Aase"
    )
    fun `somali names are respelled for a swahili voice`(somali: String, swahili: String) {
        assertEquals(swahili, NameSpelling.forSwahili(somali))
    }

    @Test
    fun `extra spaces are cleaned up`() {
        assertEquals("Ahmed Ali", NameSpelling.forSwahili("  Axmed   Cali "))
    }

    @Test
    fun `a name that is only the letter c becomes nothing`() {
        assertNull(NameSpelling.forSwahili("C"))
        assertEquals("Ali", NameSpelling.forSwahili("C Cali"))
        assertNull(NameSpelling.forSwahili("   "))
    }
}
