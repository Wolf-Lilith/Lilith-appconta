package com.joaomartins.lilith

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class CallBlockerService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val sharedPrefs = getSharedPreferences("call_blocker_prefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("enabled", false)

        if (!isEnabled) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val phoneNumber = callDetails.handle?.schemeSpecificPart
        if (phoneNumber == null) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        if (isNumberInContacts(phoneNumber)) {
            // Número está na agenda, permite a chamada
            respondToCall(callDetails, CallResponse.Builder().build())
        } else {
            // Número NÃO está na agenda, bloqueia a chamada
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()
            respondToCall(callDetails, response)
        }
    }

    private fun isNumberInContacts(phoneNumber: String): Boolean {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        
        return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
        } ?: false
    }
}
