package com.ricardo.notasvozlocal;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public final class NoteEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String text;
    public long createdAt;
    public long updatedAt;

    public NoteEntity(String text, long createdAt, long updatedAt) {
        this.text = text;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
