package com.example.caresync

import adapter.PatientAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caresync.databinding.ActivityDashboardBinding
import com.google.android.material.snackbar.Snackbar

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var viewModel: PatientViewModel
    private lateinit var adapter: PatientAdapter
    private var currentPatientList: List<Patient> = emptyList()
    private var currentQuery: String = ""

    private var currentObservedLiveData: LiveData<List<Patient>>? = null

    private val patientObserver = Observer<List<Patient>> { patients ->
        currentPatientList = patients ?: emptyList()
        adapter.submitList(currentPatientList)
        binding.tvPatientCount.text = "${currentPatientList.size} patients registered"

        // show empty search state only when searching and nothing found
        if (currentPatientList.isEmpty() && currentQuery.isNotEmpty()) {
            binding.tvEmpty.text = "No patients found for \"$currentQuery\""
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerPatients.visibility = View.GONE
            binding.fabShare.visibility = View.GONE
        } else {
            updateEmptyState(currentPatientList.isEmpty())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this)[PatientViewModel::class.java]

        // greeting with name and token
        val prefs = getSharedPreferences("caresync_prefs", MODE_PRIVATE)
        val name  = prefs.getString("logged_in_name", "Doctor") ?: "Doctor"
        val token = prefs.getString("logged_in_token", "") ?: ""
        binding.tvGreeting.text = "Hello, Dr. $name • $token 👋"

        // tap greeting to copy token to clipboard
        binding.tvGreeting.setOnClickListener {
            if (token.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("CareSync Token", token)
                clipboard.setPrimaryClip(clip)
                Snackbar.make(binding.root, "Token $token copied!", Snackbar.LENGTH_SHORT).show()
            }
        }

        setupRecyclerView()
        setupChips()         // ✅ wire up age filter chips

        // start with full list, All chip selected
        switchObserver(viewModel.allPatients)

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddPatientActivity::class.java))
        }

        binding.fabShare.setOnClickListener {
            sharePatientList()
        }
    }

    // ─── Chips ────────────────────────────────────────────────────────────────

    private fun setupChips() {
        // "All" chip — show full list
        binding.chipAll.setOnClickListener {
            currentQuery = ""
            switchObserver(viewModel.allPatients)
        }

        // "Children" chip — age 0 to 12
        binding.chipChildren.setOnClickListener {
            currentQuery = ""
            switchObserver(viewModel.getPatientsByAge(0, 12))
        }

        // "Adults" chip — age 13 to 59
        binding.chipAdults.setOnClickListener {
            currentQuery = ""
            switchObserver(viewModel.getPatientsByAge(13, 59))
        }

        // "Senior" chip — age 60 and above
        binding.chipSenior.setOnClickListener {
            currentQuery = ""
            switchObserver(viewModel.getPatientsByAge(60, 120))
        }
    }

    // ─── RecyclerView ─────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = PatientAdapter(
            onPatientClick = { patient ->
                val intent = Intent(this, PatientDetailActivity::class.java)
                intent.putExtra("patient_id", patient.id)
                startActivity(intent)
            },
            onDeleteClick = { patient ->
                // delete immediately
                viewModel.delete(patient)
                // undo snackbar
                Snackbar.make(binding.root, "${patient.name} removed", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        viewModel.insert(patient)
                    }
                    .setActionTextColor(getColor(R.color.primary))
                    .show()
            }
        )
        binding.recyclerPatients.layoutManager = LinearLayoutManager(this)
        binding.recyclerPatients.adapter = adapter
    }

    // ─── Observer switching ───────────────────────────────────────────────────

    private fun switchObserver(newLiveData: LiveData<List<Patient>>) {
        currentObservedLiveData?.removeObserver(patientObserver)
        currentObservedLiveData = newLiveData
        newLiveData.observe(this, patientObserver)
    }

    // ─── Empty state ──────────────────────────────────────────────────────────

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.tvEmpty.text = "No patients added yet"
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerPatients.visibility = View.GONE
            binding.fabShare.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerPatients.visibility = View.VISIBLE
            binding.fabShare.visibility = View.VISIBLE
        }
    }

    // ─── Toolbar menu ─────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = "Search by name, diagnosis, phone..."

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText?.trim() ?: ""

                if (currentQuery.isEmpty()) {
                    // reset chip to All when search is cleared
                    binding.chipAll.isChecked = true
                    switchObserver(viewModel.allPatients)
                } else {
                    // when searching, deselect all chips visually
                    binding.chipGroup.clearCheck()
                    switchObserver(viewModel.searchPatients(currentQuery))
                }
                return true
            }
        })

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem) = true
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                // reset to All when search bar closed
                currentQuery = ""
                binding.chipAll.isChecked = true
                switchObserver(viewModel.allPatients)
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                getSharedPreferences("caresync_prefs", MODE_PRIVATE)
                    .edit().clear().apply()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─── Share ────────────────────────────────────────────────────────────────

    private fun sharePatientList() {
        if (currentPatientList.isEmpty()) return

        val sb = StringBuilder()
        sb.appendLine("📋 CareSync — Patient List")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("Total Patients: ${currentPatientList.size}")
        sb.appendLine()

        currentPatientList.forEachIndexed { index, patient ->
            sb.appendLine("${index + 1}. ${patient.name}")
            sb.appendLine("   Age: ${patient.age} | ${patient.gender} | ${patient.bloodGroup}")
            sb.appendLine("   Diagnosis: ${patient.diagnosis}")
            sb.appendLine("   📞 ${patient.phone}")
            sb.appendLine()
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("Shared via CareSync")

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CareSync Patient List")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(shareIntent, "Share patient list via..."))
    }
}