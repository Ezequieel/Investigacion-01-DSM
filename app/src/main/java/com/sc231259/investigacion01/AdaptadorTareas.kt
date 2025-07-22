package com.sc231259.investigacion01

import android.app.DatePickerDialog
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.sc231259.investigacion01.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

data class Tarea(
    val id: String = "",
    var descripcion: String,
    var completada: Boolean = false,
    var fechaVencimiento: Long? = null
)

class AdaptadorTareas(
    private val listaTareas: MutableList<Tarea>,
    private val alCambiarEstado: (tarea: Tarea) -> Unit,
    private val alEditarTarea: (tarea: Tarea) -> Unit  //  ojo callback
) : RecyclerView.Adapter<AdaptadorTareas.VistaTarea>() {

    inner class VistaTarea(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VistaTarea {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VistaTarea(binding)
    }

    override fun onBindViewHolder(holder: VistaTarea, position: Int) {
        val tarea = listaTareas[position]
        holder.binding.textViewTask.text = tarea.descripcion

        // Mostrar fecha de vencimiento si existe
        if (tarea.fechaVencimiento != null) {
            val fechaStr = formatoFecha.format(Date(tarea.fechaVencimiento!!))
            holder.binding.textViewFechaVencimiento.text = "Vence: $fechaStr"
            holder.binding.textViewFechaVencimiento.visibility = android.view.View.VISIBLE

            // Pintar en rojo si está vencida y no completada
            if (!tarea.completada && tarea.fechaVencimiento!! < System.currentTimeMillis()) {
                holder.binding.textViewFechaVencimiento.setTextColor(android.graphics.Color.RED)
            } else {
                holder.binding.textViewFechaVencimiento.setTextColor(android.graphics.Color.DKGRAY)
            }
        } else {
            holder.binding.textViewFechaVencimiento.visibility = android.view.View.GONE
        }

        holder.binding.checkBoxCompleted.setOnCheckedChangeListener(null)
        holder.binding.checkBoxCompleted.isChecked = tarea.completada

        if (tarea.completada) {
            holder.binding.textViewTask.paintFlags =
                holder.binding.textViewTask.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.binding.textViewTask.paintFlags = 0
        }

        holder.binding.checkBoxCompleted.setOnCheckedChangeListener { _, marcado ->
            tarea.completada = marcado
            alCambiarEstado(tarea)
            notifyItemChanged(position)
        }

        holder.binding.buttonEditar.setOnClickListener {
            mostrarDialogoEditarTarea(tarea, position, holder)
        }
    }

    override fun getItemCount(): Int = listaTareas.size

    fun agregarTarea(tarea: Tarea) {
        listaTareas.add(tarea)
        notifyItemInserted(listaTareas.size - 1)
    }

    fun eliminarTareasCompletadas() {
        listaTareas.removeAll { it.completada }
        notifyDataSetChanged()
    }

    fun obtenerTareasCompletadas(): List<Tarea> {
        return listaTareas.filter { it.completada }
    }

    private fun mostrarDialogoEditarTarea(tarea: Tarea, posicion: Int, holder: VistaTarea) {
        val contexto = holder.binding.root.context

        // EditText para descripción
        val editTextDescripcion = EditText(contexto).apply {
            setText(tarea.descripcion)
        }

        // Fecha inicial para el DatePicker
        val calendario = Calendar.getInstance()
        if (tarea.fechaVencimiento != null) {
            calendario.timeInMillis = tarea.fechaVencimiento!!
        }

        AlertDialog.Builder(contexto)
            .setTitle("Editar tarea")
            .setView(editTextDescripcion)
            .setPositiveButton("Seleccionar fecha") { dialog, _ ->
                // Abrir DatePicker para elegir fecha
                val datePicker = DatePickerDialog(
                    contexto,
                    { _, year, month, dayOfMonth ->
                        calendario.set(year, month, dayOfMonth)
                        // Actualizar tarea con descripción y fecha nueva
                        val nuevaDescripcion = editTextDescripcion.text.toString()
                        if (nuevaDescripcion.isNotBlank()) {
                            tarea.descripcion = nuevaDescripcion
                            tarea.fechaVencimiento = calendario.timeInMillis
                            alEditarTarea(tarea)
                            notifyItemChanged(posicion)
                        }
                    },
                    calendario.get(Calendar.YEAR),
                    calendario.get(Calendar.MONTH),
                    calendario.get(Calendar.DAY_OF_MONTH)
                )
                datePicker.show()
                dialog.dismiss()
            }
            .setNeutralButton("Guardar sin fecha") { dialog, _ ->
                val nuevaDescripcion = editTextDescripcion.text.toString()
                if (nuevaDescripcion.isNotBlank()) {
                    tarea.descripcion = nuevaDescripcion
                    tarea.fechaVencimiento = null
                    alEditarTarea(tarea)
                    notifyItemChanged(posicion)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
