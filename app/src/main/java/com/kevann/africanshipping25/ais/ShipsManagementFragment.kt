package com.kevann.africanshipping25.ais

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kevann.africanshipping25.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShipsManagementFragment : Fragment() {

    private lateinit var rvShips: RecyclerView
    private lateinit var shipsAdapter: ShipsAdapter
    private val shipsList = mutableListOf<Ship>()
    private val shipsRepository = ShipsRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ships_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvShips = view.findViewById(R.id.rv_ships)
        val fabAddShip = view.findViewById<FloatingActionButton>(R.id.fab_add_ship)

        setupRecyclerView()
        loadShips()

        fabAddShip.setOnClickListener {
            showAddShipDialog()
        }
    }

    private fun setupRecyclerView() {
        shipsAdapter = ShipsAdapter(shipsList) { ship, action ->
            handleShipAction(ship, action)
        }
        rvShips.layoutManager = LinearLayoutManager(requireContext())
        rvShips.adapter = shipsAdapter
    }

    private fun loadShips() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val ships = shipsRepository.getAllShips()
                shipsList.clear()
                shipsList.addAll(ships)
                shipsAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading ships: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddShipDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_ship, null)

        val etShipName = dialogView.findViewById<EditText>(R.id.et_ship_name)
        val etShipNumber = dialogView.findViewById<EditText>(R.id.et_ship_number)
        val etIMONumber = dialogView.findViewById<EditText>(R.id.et_imo_number)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add New Ship")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val shipName = etShipName.text.toString().trim()
                val shipNumber = etShipNumber.text.toString().trim()
                val imoNumber = etIMONumber.text.toString().trim()

                if (shipName.isEmpty() || shipNumber.isEmpty() || imoNumber.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val ship = Ship(
                            name = shipName,
                            number = shipNumber,
                            imoNumber = imoNumber,
                            status = "Active"
                        )
                        shipsRepository.addShip(ship)
                        Toast.makeText(requireContext(), "Ship added successfully!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadShips()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error adding ship: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun handleShipAction(ship: Ship, action: String) {
        when (action) {
            "edit" -> showEditShipDialog(ship)
            "delete" -> deleteShip(ship)
            "refresh" -> refreshShipLocation(ship)
        }
    }

    private fun showEditShipDialog(ship: Ship) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_ship, null)

        val etShipName = dialogView.findViewById<EditText>(R.id.et_ship_name)
        val etShipNumber = dialogView.findViewById<EditText>(R.id.et_ship_number)
        val etIMONumber = dialogView.findViewById<EditText>(R.id.et_imo_number)

        etShipName.setText(ship.name)
        etShipNumber.setText(ship.number)
        etIMONumber.setText(ship.imoNumber)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Ship")
            .setView(dialogView)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val shipName = etShipName.text.toString().trim()
                val shipNumber = etShipNumber.text.toString().trim()
                val imoNumber = etIMONumber.text.toString().trim()

                if (shipName.isEmpty() || shipNumber.isEmpty() || imoNumber.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        shipsRepository.updateShipDetails(ship.id, shipName, shipNumber, imoNumber)
                        Toast.makeText(requireContext(), "Ship updated successfully!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadShips()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error updating ship: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun deleteShip(ship: Ship) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Ship")
            .setMessage("Are you sure you want to delete ${ship.name}?")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        shipsRepository.deleteShip(ship.id)
                        Toast.makeText(requireContext(), "Ship deleted successfully!", Toast.LENGTH_SHORT).show()
                        loadShips()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error deleting ship: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshShipLocation(ship: Ship) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Toast.makeText(requireContext(), "Fetching live location...", Toast.LENGTH_SHORT).show()
                
                val aisRepository = AisHubRepository()
                val vesselLocation = aisRepository.getVesselLocationByIMO(ship.imoNumber)

                if (vesselLocation != null) {
                    shipsRepository.updateShipLocation(ship.id, vesselLocation)
                    Toast.makeText(
                        requireContext(),
                        "Location updated: ${vesselLocation.Latitude}, ${vesselLocation.Longitude}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadShips()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Vessel not found in AIS Hub database. It may be offline or in territorial waters.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error refreshing location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
