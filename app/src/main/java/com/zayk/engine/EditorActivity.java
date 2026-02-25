package com.zayk.engine;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class EditorActivity extends Activity {

    private File scriptFile;
    private EditText editorField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        editorField = findViewById(R.id.editor_field);
        TextView txtName = findViewById(R.id.txt_script_name);
        Button btnSave = findViewById(R.id.btn_save_script);
        Button btnClose = findViewById(R.id.btn_close_editor);

        String path = getIntent().getStringExtra("filePath");
        if (path != null) {
            scriptFile = new File(path);
            txtName.setText(scriptFile.getName());
            loadFileContent();
        } else {
            Toast.makeText(this, "Erro: Ficheiro não encontrado", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSave.setOnClickListener(v -> saveFileContent());
        btnClose.setOnClickListener(v -> finish());
    }

    private void loadFileContent() {
        try {
            FileInputStream fis = new FileInputStream(scriptFile);
            byte[] data = new byte[(int) scriptFile.length()];
            fis.read(data);
            fis.close();
            editorField.setText(new String(data, "UTF-8"));
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao ler script", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFileContent() {
        try {
            String content = editorField.getText().toString();
            FileOutputStream fos = new FileOutputStream(scriptFile);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
            Toast.makeText(this, "Guardado!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao guardar", Toast.LENGTH_SHORT).show();
        }
    }
}
