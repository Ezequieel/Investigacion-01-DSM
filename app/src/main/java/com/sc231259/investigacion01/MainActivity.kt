package com.sc231259.investigacion01

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.DatePicker
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.sc231259.investigacion01.databinding.ActivityMainBinding
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adaptadorTareas: AdaptadorTareas
    private val listaTareas = mutableListOf<Tarea>()

    private val db = FirebaseFirestore.getInstance()
    private val coleccionTareas = db.collection("tareas")

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adaptadorTareas = AdaptadorTareas(
            listaTareas,
            alCambiarEstado = { tarea ->
                coleccionTareas.document(tarea.id)
                    .update("completada", tarea.completada)
            },
            alEditarTarea = { tarea ->
                // guarda descripción y fechaVencimiento
                val datos = mutableMapOf<String, Any>(
                    "descripcion" to tarea.descripcion
                )
                if (tarea.fechaVencimiento != null) {
                    datos["fechaVencimiento"] = tarea.fechaVencimiento!!
                } else {
                    datos["fechaVencimiento"] = com.google.firebase.firestore.FieldValue.delete()
                }
                coleccionTareas.document(tarea.id)
                    .update(datos)
            }
        )

        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = adaptadorTareas
        }

        cargarTareasDesdeFirestore()

        binding.buttonAddTask.setOnClickListener {
            val descripcion = binding.editTextTask.text.toString().trim()
            if (descripcion.isNotEmpty()) {
                mostrarSelectorFecha(descripcion)
            }
        }

        binding.buttonDeleteCompleted.setOnClickListener {
            confirmarEliminacionTareasCompletadas()
        }
    }

    private fun cargarTareasDesdeFirestore() {
        coleccionTareas.get()
            .addOnSuccessListener { resultados ->
                listaTareas.clear()
                for (documento in resultados) {
                    val fechaVenc = documento.getLong("fechaVencimiento")
                    val tarea = Tarea(
                        id = documento.id,
                        descripcion = documento.getString("descripcion") ?: "",
                        completada = documento.getBoolean("completada") ?: false,
                        fechaVencimiento = fechaVenc
                    )
                    listaTareas.add(tarea)
                }
                adaptadorTareas.notifyDataSetChanged()
            }
    }

    private fun agregarTareaAFirestore(descripcion: String, fechaVencimiento: Long?) {
        val nuevaTarea = mutableMapOf<String, Any>(
            "descripcion" to descripcion,
            "completada" to false
        )
        if (fechaVencimiento != null) {
            nuevaTarea["fechaVencimiento"] = fechaVencimiento
        }

        coleccionTareas.add(nuevaTarea)
            .addOnSuccessListener { docRef ->
                val tareaConId = Tarea(
                    id = docRef.id,
                    descripcion = descripcion,
                    completada = false,
                    fechaVencimiento = fechaVencimiento
                )
                listaTareas.add(tareaConId)
                adaptadorTareas.notifyItemInserted(listaTareas.size - 1)
            }
    }

    private fun mostrarSelectorFecha(descripcion: String) {
        val calendario = Calendar.getInstance()
        val datePicker = DatePicker(this)

        AlertDialog.Builder(this)
            .setTitle("Selecciona la fecha de vencimiento")
            .setView(datePicker)
            .setPositiveButton("Guardar") { dialog, _ ->
                calendario.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
                val fechaSeleccionada = calendario.timeInMillis
                agregarTareaAFirestore(descripcion, fechaSeleccionada)
                binding.editTextTask.text?.clear()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun confirmarEliminacionTareasCompletadas() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Deseas eliminar las tareas completadas?")
            .setPositiveButton("Sí") { dialog, _ ->
                val tareasAEliminar = adaptadorTareas.obtenerTareasCompletadas()
                for (tarea in tareasAEliminar) {
                    coleccionTareas.document(tarea.id).delete()
                }
                listaTareas.removeAll { it.completada }
                adaptadorTareas.notifyDataSetChanged()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
