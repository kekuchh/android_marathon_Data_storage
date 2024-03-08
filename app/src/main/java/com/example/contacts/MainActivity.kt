package com.example.contacts

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.contacts.data.ContactsRepo
import com.example.contacts.data.NotesRepo
import com.example.contacts.model.Contact
import com.example.contacts.model.Note
import com.example.contacts.ui.theme.ContactsTheme
import kotlinx.coroutines.launch

enum class Codes(val code: Int) {
    BaseCode(100),
    ReadPermissionCode(101),
    WritePermissionCode(102)
}

class MainActivity : ComponentActivity() {
    private lateinit var contactsRepo: ContactsRepo
    private lateinit var notesRepo: NotesRepo

    private var contactsList by mutableStateOf(emptyList<Contact>())
    private var notesList by mutableStateOf(emptyList<Note>())

    private var selectedContact by mutableStateOf(0L)
    private var isEditDialog by mutableStateOf(false)

    private var isContactsImported by mutableStateOf(false)
    private var isNotesImported by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        importData()

        setContent {
            ContactsTheme(content = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
            )
        }
        checkAndRequestContactsPermission()
    }

    private fun importData() {
        contactsRepo = ContactsRepo(this)
        notesRepo = NotesRepo(this)

        importContacts()
        importNotes()
    }

    private fun importContacts() {
        if (!isContactsImported) {
            lifecycleScope.launch {
                contactsRepo.importContacts()
                contactsList = contactsRepo.getAllContacts()
            }
            isContactsImported = true
        }
    }

    private fun importNotes() {
        if (!isNotesImported) {
            lifecycleScope.launch {
                notesList = notesRepo.getAllNotes()
            }
            isNotesImported = true
        }
    }

    private fun checkAndRequestContactsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                Codes.ReadPermissionCode.code
            )
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_CONTACTS),
                    Codes.WritePermissionCode.code
                )
            } else {
                importContacts()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            Codes.ReadPermissionCode.code,
            Codes.WritePermissionCode.code -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    importContacts()
                }
            }

            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainScreen() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(50.dp))
                    }
                    items(contactsList.size) { index ->
                        val contact = contactsList[index]
                        val currNote = notesList.find { note -> note.contactId == contact.id }
                        ContactItem(
                            contact = contact,
                            note = currNote,
                            onEditClick = {
                                selectedContact = contact.id
                                isEditDialog = true
                            },
                            onDeleteClick = {
                                lifecycleScope.launch {
                                    val note = notesRepo.getNoteByContactId(contact.id)
                                    if (note != null) {
                                        notesRepo.deleteNote(note)
                                    }
                                    notesList = notesRepo.getAllNotes()
                                }
                            }
                        )
                    }
                }

                if (isEditDialog) {
                    val currNote = notesList.find { note -> note.contactId == selectedContact }
                    EditContactDialog(
                        oldNote = currNote?.description ?: stringResource(R.string.empty_note),
                        onDismiss = {
                            isEditDialog = false
                        },
                        onSave = { newNote: String ->
                            lifecycleScope.launch {
                                val noteId = notesRepo.getNoteIdByContactId(selectedContact)
                                if (noteId != null) {
                                    val editedNote = Note(
                                        id = noteId,
                                        description = newNote,
                                        contactId = selectedContact
                                    )
                                    notesRepo.updateNote(editedNote)
                                } else {
                                    val note = Note(
                                        description = newNote,
                                        contactId = selectedContact
                                    )
                                    notesRepo.insertNote(note)
                                }
                                notesList = notesRepo.getAllNotes()
                            }
                            isEditDialog = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun ContactItem(
        contact: Contact,
        note: Note?,
        onEditClick: () -> Unit,
        onDeleteClick: () -> Unit
    ) {
        val noteText: String = note?.description ?: stringResource(R.string.empty_note)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "${stringResource(R.string.contact_name)}: ${contact.name}",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "${stringResource(R.string.contact_phone)}: ${contact.phone}",
                style = MaterialTheme.typography.headlineMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${stringResource(R.string.contact_note)}: ${noteText}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (noteText != stringResource(R.string.empty_note)) Color.Black else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                ContactActionButton(Icons.Default.Edit) {
                    onEditClick()
                }
                ContactActionButton(Icons.Default.Delete) {
                    onDeleteClick()
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }

    @Composable
    private fun ContactActionButton(icon: ImageVector, onClick: () -> Unit) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }

    @Composable
    fun EditContactDialog(
        oldNote: String,
        onDismiss: () -> Unit,
        onSave: (newNote: String) -> Unit
    ) {
        var newNote by remember { mutableStateOf(TextFieldValue(oldNote)) }

        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(stringResource(R.string.edit_note_dialog_label)) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = newNote,
                        onValueChange = { newNote = it },
                        label = { Text(stringResource(R.string.edit_note_dialog)) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSave(newNote.text)
                        onDismiss()
                    },
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.save_button),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { onDismiss() },
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        stringResource(R.string.cancel_button),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        )
    }
}