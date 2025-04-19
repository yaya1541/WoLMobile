package com.example.minecraft

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.minecraft.ui.theme.MinecraftTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : ComponentActivity() {
    private lateinit var etMacAddress: EditText
    private lateinit var etBroadcastAddress: EditText
    private lateinit var etPort: EditText
    private lateinit var btnWakeUp: Button

    private val sharedPrefName = "WakeOnLanPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main)


        etMacAddress = findViewById(R.id.MacAdress)
        etBroadcastAddress = findViewById(R.id.Adress)
        etPort = findViewById(R.id.Port)
        btnWakeUp = findViewById(R.id.WakeUp)

        btnWakeUp.setOnClickListener {
            val macAddress = etMacAddress.text.toString()
            val broadcastAddress = etBroadcastAddress.text.toString()
            val port = etPort.text.toString().toIntOrNull() ?: 9
            if (isValidMacAddress(macAddress) && broadcastAddress.isNotEmpty()) {
                savePreferences(macAddress, broadcastAddress, port)
                sendWakeOnLan(macAddress, broadcastAddress, port)
            } else {
                Toast.makeText(this, "Veuillez entrer une adresse MAC valide et une adresse de diffusion", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadSavedPreferences() {
        val prefs = getSharedPreferences(sharedPrefName, MODE_PRIVATE)
        etMacAddress.setText(prefs.getString("macAddress", ""))
        etBroadcastAddress.setText(prefs.getString("broadcastAddress", "192.168.1.255"))
        etPort.setText(prefs.getInt("port", 9).toString())
    }

    private fun savePreferences(macAddress: String, broadcastAddress: String, port: Int) {
        val editor = getSharedPreferences(sharedPrefName, MODE_PRIVATE).edit()
        editor.putString("macAddress", macAddress)
        editor.putString("broadcastAddress", broadcastAddress)
        editor.putInt("port", port)
        editor.apply()
    }

    private fun isValidMacAddress(macAddress: String): Boolean {
        // Format MAC accepté: XX:XX:XX:XX:XX:XX ou XX-XX-XX-XX-XX-XX
        return macAddress.matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"))
    }

    private fun sendWakeOnLan(macAddress: String, broadcastAddress: String, port: Int) {
        // Utiliser des coroutines pour les opérations réseau
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Formatage de l'adresse MAC
                val macBytes = getMacBytes(macAddress)

                // Création du paquet magique (6 bytes FF suivis de 16 répétitions de l'adresse MAC)
                val bytes = ByteArray(6 + 16 * macBytes.size)

                // Remplissage des 6 premiers bytes avec 0xFF
                for (i in 0 until 6) {
                    bytes[i] = 0xFF.toByte()
                }

                // Répétition de l'adresse MAC 16 fois
                for (i in 6 until bytes.size step macBytes.size) {
                    System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
                }

                // Envoi du paquet UDP
                val address = InetAddress.getByName(broadcastAddress)
                val packet = DatagramPacket(bytes, bytes.size, address, port)
                DatagramSocket().use { socket ->
                    socket.send(packet)
                    socket.close()
                }

                // Notification de succès sur le thread principal
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Signal Wake-on-LAN envoyé avec succès!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Erreur: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun getMacBytes(macAddress: String): ByteArray {
        // Conversion de l'adresse MAC en tableau de bytes
        val macParts = macAddress.split(":|\\-".toRegex())
        val bytes = ByteArray(6)

        for (i in 0 until 6) {
            bytes[i] = Integer.parseInt(macParts[i], 16).toByte()
        }

        return bytes
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MinecraftTheme {
        Greeting("Android")
    }
}