/*
 *  This file is part of Life Hacking
 *
 *  Life Hacking is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Life Hacking is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Life Hacking.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.notes

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.dto.DisplayNote
import com.samco.trackandgraph.database.dto.NoteType
import com.samco.trackandgraph.databinding.ListItemGlobalNoteBinding
import com.samco.trackandgraph.util.formatDayMonthYearHourMinute

internal class NoteListAdapter(
    private val clickListener: NoteClickListener
) : ListAdapter<DisplayNote, NoteViewHolder>(NoteDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        return NoteViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }
}

internal class NoteDiffCallback : DiffUtil.ItemCallback<DisplayNote>() {
    override fun areItemsTheSame(oldItem: DisplayNote, newItem: DisplayNote): Boolean {
        return oldItem.timestamp == newItem.timestamp
    }

    override fun areContentsTheSame(oldItem: DisplayNote, newItem: DisplayNote): Boolean {
        return oldItem == newItem
    }
}

internal class NoteViewHolder private constructor(private val binding: ListItemGlobalNoteBinding) :
    RecyclerView.ViewHolder(binding.root), PopupMenu.OnMenuItemClickListener {

    private var clickListener: NoteClickListener? = null
    private var note: DisplayNote? = null

    fun bind(note: DisplayNote, clickListener: NoteClickListener) {
        this.note = note
        this.clickListener = clickListener
        binding.timestampText.text =
            formatDayMonthYearHourMinute(binding.root.context, note.timestamp)
        binding.noteText.text = note.note
        binding.featureAndTrackGroupText.text = when (note.noteType) {
            NoteType.DATA_POINT -> "${note.trackGroupName} -> ${note.featureName}"
            NoteType.GLOBAL_NOTE -> ""
        }
        binding.editButton.setOnClickListener { createContextMenu(binding.editButton) }
        binding.cardView.setOnClickListener { clickListener.onClicked(note) }
    }

    private fun createContextMenu(view: View) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.edit_note_context_menu, popup.menu)
        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    companion object {
        fun from(parent: ViewGroup): NoteViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ListItemGlobalNoteBinding.inflate(layoutInflater, parent, false)
            return NoteViewHolder(binding)
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        note?.let { note ->
            when (item?.itemId) {
                R.id.edit -> clickListener?.onEditClicked(note)
                R.id.delete -> clickListener?.onDeleteClicked(note)
                else -> run { }
            }
        }
        return false
    }
}

internal class NoteClickListener(
    private val onClick: (DisplayNote) -> Unit,
    private val onEdit: (DisplayNote) -> Unit,
    private val onDelete: (DisplayNote) -> Unit
) {
    fun onClicked(note: DisplayNote) = onClick(note)
    fun onEditClicked(note: DisplayNote) = onEdit(note)
    fun onDeleteClicked(note: DisplayNote) = onDelete(note)
}
