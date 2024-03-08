package com.example.contacts.data

import android.content.Context
import com.example.contacts.dao.NoteDao
import com.example.contacts.model.Note

class NotesRepo(context: Context) {

    private val noteDao: NoteDao = AppDatabase.getDatabase(context).noteDao()


    suspend fun getAllNotes(): List<Note> {
        return noteDao.getAllNotes();
    }

    suspend fun insertAll(notes: List<Note>) {
        return noteDao.insertAll(notes);
    }

    suspend fun insertNote(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note){
        return noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note){
        return noteDao.deleteNote(note);
    }

    suspend fun getNoteByContactId(contactId: Long) : Note?{
        return noteDao.getNoteByContactId(contactId)
    }

    suspend fun getNoteIdByContactId(contactId: Long): Long? {
        return noteDao.getNoteIdByContactId(contactId)
    }
}