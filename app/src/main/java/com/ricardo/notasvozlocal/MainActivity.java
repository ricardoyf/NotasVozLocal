package com.ricardo.notasvozlocal;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends android.app.Activity {
    private AppDatabase db;
    private LinearLayout notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.WHITE);
        db = AppDatabase.get(this);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (notesList != null) {
            loadNotes();
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(12));
        root.setBackgroundColor(Color.rgb(250, 250, 250));

        TextView title = new TextView(this);
        title.setText("Ideas");
        title.setTextSize(25);
        title.setTextColor(Color.rgb(20, 20, 20));
        title.setPadding(0, 0, 0, dp(10));
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        Button newNote = new Button(this);
        newNote.setText("Nueva nota de voz");
        newNote.setAllCaps(false);
        newNote.setTextSize(20);
        newNote.setOnClickListener(v -> startActivity(new Intent(this, CaptureActivity.class)));
        root.addView(newNote, new LinearLayout.LayoutParams(-1, dp(64)));

        TextView notesTitle = new TextView(this);
        notesTitle.setText("Notas guardadas");
        notesTitle.setTextSize(18);
        notesTitle.setTextColor(Color.rgb(20, 20, 20));
        notesTitle.setPadding(0, dp(18), 0, dp(6));
        root.addView(notesTitle, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        notesList = new LinearLayout(this);
        notesList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(notesList);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void loadNotes() {
        notesList.removeAllViews();
        for (NoteEntity note : db.notes().list()) {
            notesList.addView(noteRow(note));
        }
    }

    private View noteRow(NoteEntity note) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(4), dp(10));
        row.setBackgroundColor(Color.WHITE);

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);

        TextView date = new TextView(this);
        date.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                .format(new Date(note.createdAt)));
        date.setTextSize(12);
        date.setTextColor(Color.rgb(105, 105, 105));
        textBox.addView(date);

        TextView body = new TextView(this);
        body.setText(note.text);
        body.setTextSize(16);
        body.setTextColor(Color.rgb(22, 22, 22));
        body.setMaxLines(4);
        textBox.addView(body);

        row.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1));

        Button menuButton = new Button(this);
        menuButton.setText("⋮");
        menuButton.setTextSize(22);
        menuButton.setAllCaps(false);
        menuButton.setOnClickListener(v -> showMenu(v, note));
        row.addView(menuButton, new LinearLayout.LayoutParams(dp(54), dp(54)));
        addSwipeActions(row, note);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(params);
        return row;
    }

    private void addSwipeActions(View row, NoteEntity note) {
        final float[] downX = new float[1];
        final float[] downY = new float[1];
        row.setClickable(true);
        row.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX[0] = event.getRawX();
                    downY[0] = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dxMove = event.getRawX() - downX[0];
                    float dyMove = event.getRawY() - downY[0];
                    if (Math.abs(dxMove) > Math.abs(dyMove)) {
                        view.setTranslationX(Math.max(-dp(96), Math.min(dp(96), dxMove * 0.35f)));
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_CANCEL:
                    view.animate().translationX(0).setDuration(120).start();
                    return true;
                case MotionEvent.ACTION_UP:
                    float dx = event.getRawX() - downX[0];
                    float dy = event.getRawY() - downY[0];
                    view.animate().translationX(0).setDuration(120).start();
                    if (Math.abs(dx) > dp(90) && Math.abs(dx) > Math.abs(dy) * 1.4f) {
                        if (dx > 0) {
                            deleteNote(note);
                        } else {
                            shareNote(note);
                        }
                    } else if (Math.abs(dx) < dp(12) && Math.abs(dy) < dp(12)) {
                        editNote(note);
                    }
                    return true;
                default:
                    return false;
            }
        });
    }

    private void showMenu(View anchor, NoteEntity note) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Copiar texto");
        menu.getMenu().add("Compartir");
        menu.getMenu().add("Editar");
        menu.getMenu().add("Eliminar");
        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Copiar texto".equals(title)) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("Nota de voz", note.text));
                Toast.makeText(this, "Nota copiada.", Toast.LENGTH_SHORT).show();
            } else if ("Editar".equals(title)) {
                editNote(note);
            } else if ("Eliminar".equals(title)) {
                confirmDelete(note);
            } else if ("Compartir".equals(title)) {
                shareNote(note);
            }
            return true;
        });
        menu.show();
    }

    private void shareNote(NoteEntity note) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, note.text);
        startActivity(Intent.createChooser(send, "Compartir nota"));
    }

    private void editNote(NoteEntity note) {
        EditText input = new EditText(this);
        input.setText(note.text);
        input.setMinLines(4);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setGravity(Gravity.TOP);
        new AlertDialog.Builder(this)
                .setTitle("Editar nota")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    note.text = input.getText().toString().trim();
                    note.updatedAt = System.currentTimeMillis();
                    db.notes().update(note);
                    loadNotes();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmDelete(NoteEntity note) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar nota")
                .setMessage("¿Eliminar esta nota?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    db.notes().delete(note);
                    loadNotes();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteNote(NoteEntity note) {
        db.notes().delete(note);
        loadNotes();
        Toast.makeText(this, "Nota eliminada.", Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
