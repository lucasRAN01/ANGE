package com.ange.mobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract.CommonDataKinds.Phone

data class ContactNumber(val name: String, val number: String)

internal object ContactDirectory {
    fun normalizedName(name: String) = JarvisVoice.withoutAccents(name)
        .replace(Regex("[^\\p{L}\\p{N} ]"), " ").trim().replace(Regex("\\s+"), " ")

    fun match(name: String, numbers: List<ContactNumber>): List<ContactNumber> {
        val query = normalizedName(name)
        if (query.isBlank()) return emptyList()
        val exact = numbers.filter { normalizedName(it.name) == query }
        return (if (exact.isNotEmpty()) exact else numbers.filter { normalizedName(it.name).split(' ').contains(query) })
            .filter { Regex("^\\+?[0-9 () .-]{3,30}$").matches(it.number) }
            .map { it.copy(number=it.number.filter { char -> char.isDigit() || char=='+' }) }
            .filter { Regex("^\\+?[0-9]{3,18}$").matches(it.number) }
            .distinctBy { normalizedName(it.name) to it.number }.take(30)
    }

    fun find(context: Context, name: String): List<ContactNumber> {
        require(context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) { "Autorisation Contacts nécessaire." }
        val found = mutableListOf<ContactNumber>()
        context.contentResolver.query(Phone.CONTENT_URI, arrayOf(Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val label=cursor.getString(0) ?: continue
                val number=cursor.getString(1) ?: continue
                found.add(ContactNumber(label,number))
            }
        }
        return match(name,found)
    }
}
