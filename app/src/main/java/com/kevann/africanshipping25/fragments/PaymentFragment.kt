package com.kevann.africanshipping25.fragments

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.kevann.africanshipping25.R  // Add this import


class PaymentFragment : Fragment() {

    // Declare EditText fields at class level so they can be accessed after the click
    private lateinit var etPhoneNumber: EditText
    private lateinit var etAmount: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views using findViewById
        etPhoneNumber = view.findViewById(R.id.et_phone_number)
        etAmount = view.findViewById(R.id.et_amount) // Find the new amount EditText
        val btnPrompt = view.findViewById<Button>(R.id.btn_track)

        // Set up the click listener for the Prompt button
        btnPrompt.setOnClickListener {
            val phoneNumber = etPhoneNumber.text.toString().trim()
            val amountString = etAmount.text.toString().trim()

            if (phoneNumber.isEmpty() || phoneNumber.length != 10 || !phoneNumber.startsWith("07")) {
                Toast.makeText(requireContext(), "Please enter a valid Safaricom phone number (e.g., 07XXXXXXXX)", Toast.LENGTH_LONG).show()
                return@setOnClickListener // Stop execution if phone number is invalid
            }

            if (amountString.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter the amount to pay.", Toast.LENGTH_LONG).show()
                return@setOnClickListener // Stop execution if amount is empty
            }

            val amount: Int
            try {
                amount = amountString.toInt()
                if (amount <= 0) {
                    Toast.makeText(requireContext(), "Amount must be a positive number.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "Please enter a valid numeric amount.", Toast.LENGTH_LONG).show()
                return@setOnClickListener // Stop execution if amount is not a valid number
            }

            // Prepend 254 to the phone number for M-Pesa API
            val formattedPhoneNumber = "254" + phoneNumber.substring(1)

            // Initiate STK Push with the dynamic amount
            initiateMpesaSTKPush(formattedPhoneNumber, amount.toString()) // Pass amount as String
        }
    }

    /**
     * Initiates the M-Pesa STK Push (Lipa Na M-Pesa Online Checkout)
     *
     * @param phoneNumber The Safaricom phone number to initiate the STK Push on (format: 2547XXXXXXXX).
     * @param amount The amount to be paid as a String.
     */
    private fun initiateMpesaSTKPush(phoneNumber: String, amount: String) {
        val client = OkHttpClient()

        // --- M-Pesa Daraja API Configuration ---
        // IMPORTANT: Replace with your actual values from Daraja API portal.
        // Ensure these match EXACTLY what is configured for your application on Daraja.
        val consumerKey = "IyT7syN6aGg5usbEwzcrLcthUVwb3mfNtjo7RBiWFrCvl6c2" // Get from Daraja App
        val consumerSecret = "4A29MkSvT5xLbwJ3eCuloA4tDVjnFGuPAa9HPTQqY5f5sQLFJHgK4MdaSCxmWJPd" // Get from Daraja App

        // VERY IMPORTANT: Double-check these two values in your Daraja portal.
        // 'Wrong credentials' error often means these are incorrect or not properly linked to STK Push.
        val shortCode = "174379" // Your Pay Bill or Till Number (Business Short Code)
        val passkey = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919" // Your Lipa Na M-Pesa Online Passkey

        val transactionType = "CustomerPayBillOnline" // Or "CustomerBuyGoodsOnline"
        val partyA = phoneNumber // Phone number initiating the payment
        val partyB = shortCode // Pay Bill or Till Number

        // IMPORTANT: This MUST be a publicly accessible URL where M-Pesa will send transaction results.
        // For local testing, ensure your Ngrok tunnel is active and a backend server is listening.
        val callBackUrl = "https://d803-41-209-60-100.ngrok-free.app" // Your current Ngrok HTTPS URL

        val accountReference = "AfricanShipping" // A unique identifier for the transaction
        val transactionDesc = "Payment for Shipping" // Description of the transaction

        // Validate CallBackURL before proceeding
        if (callBackUrl.isBlank() || !callBackUrl.startsWith("http")) {
            Log.e("MpesaAPI", "Invalid CallBackURL: '$callBackUrl'. It must be a valid public URL starting with http or https.")
            Toast.makeText(requireContext(), "Payment failed: Invalid Callback URL. Please configure it correctly.", Toast.LENGTH_LONG).show()
            return // Stop execution if URL is invalid
        }


        // Generate Timestamp
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())

        // Generate Password (Base64 encoded string of Shortcode + Passkey + Timestamp)
        val password = try {
            val dataToEncode = shortCode + passkey + timestamp
            Base64.encodeToString(dataToEncode.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("MpesaAPI", "Error encoding password: ${e.message}")
            Toast.makeText(requireContext(), "Error preparing payment. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- First, get Access Token ---
        val credentials = "$consumerKey:$consumerSecret"
        val encodedCredentials = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        val tokenRequest = Request.Builder()
            .url("https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials") // Use sandbox for testing
            .header("Authorization", "Basic $encodedCredentials")
            .build()

        client.newCall(tokenRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MpesaAPI", "Failed to get access token (network error): ${e.message}")
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                    clearInputFields() // Clear fields on network error
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("MpesaAPI", "Access Token Raw Response: ${response.code} - $responseBody")

                if (response.isSuccessful) {
                    val jsonResponse = responseBody?.let { JSONObject(it) }
                    val accessToken = jsonResponse?.optString("access_token") // Using optString for safer access

                    if (!accessToken.isNullOrBlank()) {
                        Log.d("MpesaAPI", "Access Token obtained successfully.")
                        // --- Now, initiate STK Push ---
                        initiateSTKPushWithToken(accessToken, client, shortCode, password, timestamp,
                            transactionType, amount, partyA, partyB, callBackUrl, accountReference, transactionDesc)
                    } else {
                        Log.e("MpesaAPI", "Access token not found or is blank in response: $responseBody")
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Failed to get M-Pesa access token. Check credentials.", Toast.LENGTH_SHORT).show()
                            clearInputFields() // Clear fields on access token failure
                        }
                    }
                } else {
                    Log.e("MpesaAPI", "Failed to get access token. Code: ${response.code}, Message: ${response.message}. Body: $responseBody")
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Failed to get M-Pesa access token. Check credentials or network.", Toast.LENGTH_LONG).show()
                        clearInputFields() // Clear fields on access token failure
                    }
                }
            }
        })
    }

    /**
     * Initiates the STK Push using the obtained access token.
     */
    private fun initiateSTKPushWithToken(
        accessToken: String,
        client: OkHttpClient,
        shortCode: String,
        password: String,
        timestamp: String,
        transactionType: String,
        amount: String, // Now accepts dynamic amount
        partyA: String,
        partyB: String,
        callBackUrl: String,
        accountReference: String,
        transactionDesc: String
    ) {
        val jsonBody = JSONObject().apply {
            put("BusinessShortCode", shortCode)
            put("Password", password)
            put("Timestamp", timestamp)
            put("TransactionType", transactionType)
            put("Amount", amount) // Use the dynamic amount here
            put("PartyA", partyA)
            put("PartyB", partyB)
            put("PhoneNumber", partyA)
            put("CallBackURL", callBackUrl)
            put("AccountReference", accountReference)
            put("TransactionDesc", transactionDesc)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)

        val stkPushRequest = Request.Builder()
            .url("https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest") // Use sandbox for testing
            .header("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        client.newCall(stkPushRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MpesaAPI", "STK Push request failed (network error): ${e.message}")
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "STK Push failed. Check network connection.", Toast.LENGTH_LONG).show()
                    clearInputFields() // Clear fields on network error
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("MpesaAPI", "STK Push Raw Response: ${response.code} - $responseBody")

                activity?.runOnUiThread {
                    if (response.isSuccessful) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val responseCode = jsonResponse.optString("ResponseCode", "N/A")
                            val customerMessage = jsonResponse.optString("CustomerMessage", "Unknown error.")
                            val errorMessage = jsonResponse.optString("errorMessage", "No specific error message.") // For error responses

                            if (responseCode == "0") {
                                Toast.makeText(requireContext(), "STK Push initiated successfully! Check your phone for a prompt.", Toast.LENGTH_LONG).show()
                                Log.d("MpesaAPI", "STK Push successful: $customerMessage")
                            } else {
                                // Log the full error message from M-Pesa
                                Log.e("MpesaAPI", "STK Push failed. ResponseCode: $responseCode, CustomerMessage: $customerMessage, ErrorMessage: $errorMessage, Raw: $responseBody")
                                Toast.makeText(requireContext(), "STK Push failed: $customerMessage. Check logs for details.", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.e("MpesaAPI", "Error parsing STK Push response JSON: ${e.message}. Raw Response: $responseBody", e)
                            Toast.makeText(requireContext(), "Error processing M-Pesa response. Check logs.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Log the full error response from the server
                        Log.e("MpesaAPI", "STK Push request failed. Server returned non-2xx code: ${response.code}. Message: ${response.message}. Body: $responseBody")
                        Toast.makeText(requireContext(), "STK Push request failed. Server error. Check logs.", Toast.LENGTH_LONG).show()
                    }
                    clearInputFields() // Clear fields after any STK Push response (success or failure)
                }
            }
        })
    }

    /**
     * Clears the text from the phone number and amount input fields.
     */
    private fun clearInputFields() {
        etPhoneNumber.text.clear()
        etAmount.text.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}