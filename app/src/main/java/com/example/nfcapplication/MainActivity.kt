package com.example.nfcapplication

import android.R.attr.tag
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.forEach
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.nfcapplication.databinding.ActivityMainBinding
import com.example.nfcapplication.util.NfcReadUtility
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var nfcReadUtility: NfcReadUtility? = null
    protected var nfcMessages: List<String?>? = null
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private lateinit var mIntentFilters: Array<IntentFilter>
    private lateinit var mTechLists: Array<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        nfcReadUtility = NfcReadUtility()

        initAdapter()
        initFields()
    }

    private fun initFields() {
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        mIntentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))
        mTechLists = arrayOf(
            arrayOf(Ndef::class.java.name), arrayOf(
                NdefFormatable::class.java.name
            )
        )
    }

    private fun initAdapter() {
        //this.nfcAdapter = NfcAdapter.getDefaultAdapter(this)?.let { it }
        if (nfcAdapter == null) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this)
            Log.d("NFC", "NFC Adapter initialized")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        Log.d("MAIN", "Received intent!")
        setIntent(intent)
        if (getIntent() != null && intent != null) {
            if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action || NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
                nfcMessages =
                    transformSparseArrayToArrayList(nfcReadUtility?.readFromTagWithSparseArray(intent))
            }
        }

        var tagFromIntent: Tag? = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)

        //Method 1: https://blog.csdn.net/u011082160/article/details/89146192
        var uid: String? = tagFromIntent?.let { bytesToHex(it.id) }

        //Method 2: https://itnext.io/how-to-use-nfc-tags-with-android-studio-detect-read-and-write-nfcs-42f1d60b033
        //val payload: ByteArray? = nfcReadUtility?.detectTagData(tagFromIntent)?.toByteArray() //.getBytes()
        /*val payload: ByteArray? =
            tagFromIntent?.let { nfcReadUtility?.detectTagData(it)?.toByteArray() } //.getBytes()*/
        val payload: String? =
            tagFromIntent?.let { nfcReadUtility?.detectTagData(it) }

        AlertDialog.Builder(this@MainActivity)
            .setTitle("NFC")
            .setMessage(uid + "  |   " + payload)
            .setNegativeButton("cancel",null).create()
            .show()

        val nfc = NfcA.get(tagFromIntent)
        val atqa: ByteArray = nfc.getAtqa()
        val sak: Short = nfc.getSak()
        //nfc.connect()
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val i = byte.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }


    protected fun transformSparseArrayToArrayList(sparseArray: SparseArray<String?>?) =
        ArrayList<String?>(sparseArray?.size() ?: 0).apply {
            sparseArray?.forEach { _, value -> add(value) }
        }


    override fun onPause() {
        super.onPause()
        if (nfcAdapter != null) {
            nfcAdapter?.disableForegroundDispatch(this)
            Log.d("NFC", "FGD disabled")
        }
    }

    public override fun onResume() {
        super.onResume()
        initAdapter()
        if (nfcAdapter != null) {
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, mIntentFilters, mTechLists)
            Log.d("NFC", "FGD enabled")
        }
    }
}