package com.zayk.engine;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    class FileNode {
        File file;
        int level;
        boolean isExpanded = false;
        public FileNode(File f, int l) { this.file = f; this.level = l; }
    }

    private List<FileNode> displayList = new ArrayList<>();
    private FileTreeAdapter treeAdapter;
    private File rootDir;
    private ZaykSurfaceView mGLView;
    private static final int PICK_FILE_REQUEST = 1;
    private File currentImportDir;
    private String selectedPath = ""; 
    private String searchQuery = ""; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mGLView = findViewById(R.id.zayk_surface_view);
        rootDir = getExternalFilesDir(null);
        
        ListView listProject = findViewById(R.id.list_project);
        LinearLayout projectContainer = findViewById(R.id.project_container);
        EditText searchBar = findViewById(R.id.search_bar);

        listProject.setSelector(android.R.color.transparent);
        treeAdapter = new FileTreeAdapter();
        listProject.setAdapter(treeAdapter);

        // Barra de Busca
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                refreshTree();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listProject.setOnItemClickListener((parent, view, position, id) -> {
            FileNode node = displayList.get(position);
            selectedPath = node.file.getAbsolutePath();
            if (node.file.isDirectory()) node.isExpanded = !node.isExpanded;
            refreshTree();
        });

        listProject.setOnItemLongClickListener((parent, view, position, id) -> {
            selectedPath = displayList.get(position).file.getAbsolutePath();
            refreshTree();
            showGodotMenu(view, displayList.get(position).file);
            return true;
        });

        projectContainer.setOnLongClickListener(v -> {
            showGodotMenu(v, rootDir);
            return true;
        });

        setupTabs();
        setupResizers();
        
        // --- TRAVA DE LARGURA INICIAL (180dp) ---
        float density = getResources().getDisplayMetrics().density;
        int initialWidth = (int) (180 * density);
        findViewById(R.id.left_panel).getLayoutParams().width = initialWidth;
        findViewById(R.id.right_panel).getLayoutParams().width = initialWidth;

        refreshTree();
    }

    private void setupResizers() {
        final View leftP = findViewById(R.id.left_panel);
        final View rightP = findViewById(R.id.right_panel);
        final int screenWidth = getResources().getDisplayMetrics().widthPixels;

        findViewById(R.id.left_resizer).setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                int newWidth = (int) e.getRawX();
                if (newWidth > 150 && newWidth < screenWidth * 0.45) {
                    leftP.getLayoutParams().width = newWidth;
                    leftP.requestLayout();
                }
            }
            return true;
        });

        findViewById(R.id.right_resizer).setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                int newWidth = screenWidth - (int) e.getRawX();
                if (newWidth > 150 && newWidth < screenWidth * 0.45) {
                    rightP.getLayoutParams().width = newWidth;
                    rightP.requestLayout();
                }
            }
            return true;
        });
    }

    // --- MÉTODOS DE ÁRVORE E INTERFACE MANTIDOS ---
    private void refreshTree() {
        if (rootDir == null) rootDir = getExternalFilesDir(null);
        List<String> expandedPaths = new ArrayList<>();
        for(FileNode n : displayList) if(n.isExpanded) expandedPaths.add(n.file.getAbsolutePath());
        displayList.clear();
        FileNode rootNode = new FileNode(rootDir, 0); rootNode.isExpanded = true; 
        displayList.add(rootNode);
        buildTree(rootDir, 1, expandedPaths);
        treeAdapter.notifyDataSetChanged();
    }

    private void buildTree(File dir, int level, List<String> expandedPaths) {
        File[] files = dir.listFiles(); if (files == null) return;
        List<File> fileList = new ArrayList<>(); Collections.addAll(fileList, files);
        Collections.sort(fileList, (a, b) -> (a.isDirectory() == b.isDirectory()) ? 
            a.getName().compareToIgnoreCase(b.getName()) : a.isDirectory() ? -1 : 1);
        for (File f : fileList) {
            if (!searchQuery.isEmpty()) {
                if (f.getName().toLowerCase().contains(searchQuery)) displayList.add(new FileNode(f, level));
                if (f.isDirectory()) buildTree(f, level + 1, expandedPaths);
            } else {
                FileNode node = new FileNode(f, level);
                if (expandedPaths.contains(f.getAbsolutePath())) node.isExpanded = true;
                displayList.add(node);
                if (f.isDirectory() && node.isExpanded) buildTree(f, level + 1, expandedPaths);
            }
        }
    }

    private class FileTreeAdapter extends BaseAdapter {
        @Override public int getCount() { return displayList.size(); }
        @Override public FileNode getItem(int p) { return displayList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup parent) {
            if (v == null) v = LayoutInflater.from(MainActivity.this).inflate(R.layout.list_item_file, parent, false);
            FileNode node = getItem(p);
            TextView name = v.findViewById(R.id.file_name);
            ImageView typeIcon = v.findViewById(R.id.file_icon);
            ImageView arrowIcon = v.findViewById(R.id.folder_arrow);
            View indent = v.findViewById(R.id.file_indent);
            v.setBackgroundColor(node.file.getAbsolutePath().equals(selectedPath) ? Color.parseColor("#334488FF") : Color.TRANSPARENT);
            indent.getLayoutParams().width = searchQuery.isEmpty() ? (node.level * 35) : 10;
            String fileName = node.file.getName().toLowerCase();
            if (node.file.isDirectory()) {
                arrowIcon.setVisibility(searchQuery.isEmpty() ? View.VISIBLE : View.GONE);
                arrowIcon.setImageResource(node.isExpanded ? android.R.drawable.arrow_down_float : android.R.drawable.ic_media_play);
                typeIcon.setImageResource(android.R.drawable.ic_menu_today); 
                typeIcon.setColorFilter(Color.parseColor("#4CCBFF"), PorterDuff.Mode.SRC_IN);
                name.setText(node.file.equals(rootDir) ? "res://" : node.file.getName());
            } else {
                arrowIcon.setVisibility(View.INVISIBLE);
                name.setText(node.file.getName());
                if (fileName.endsWith(".lua")) {
                    typeIcon.setImageResource(android.R.drawable.ic_menu_edit);
                    typeIcon.setColorFilter(Color.parseColor("#F0DA50"), PorterDuff.Mode.SRC_IN);
                } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg")) {
                    Bitmap b = BitmapFactory.decodeFile(node.file.getAbsolutePath());
                    if (b != null) { typeIcon.setImageBitmap(b); typeIcon.setColorFilter(null); }
                    else { typeIcon.setImageResource(android.R.drawable.ic_menu_gallery); typeIcon.setColorFilter(Color.parseColor("#90EE90"), PorterDuff.Mode.SRC_IN); }
                } else {
                    typeIcon.setImageResource(android.R.drawable.ic_menu_report_image);
                    typeIcon.setColorFilter(Color.parseColor("#AAAAAA"), PorterDuff.Mode.SRC_IN);
                }
            }
            return v;
        }
    }

    private void showGodotMenu(View anchor, final File target) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_InputMethod);
        PopupMenu popup = new PopupMenu(wrapper, anchor);
        Menu menu = popup.getMenu();
        Menu createSub = menu.addSubMenu("＋ Criar Novo");
        createSub.add(0, 101, 0, "📁 Pasta...");
        createSub.add(0, 102, 1, "📜 Script (.lua)...");
        menu.add(0, 103, 2, "📥 Importar...");
        if (!target.equals(rootDir)) {
            menu.add(0, 3, 3, "✎ Renomear");
            menu.add(0, 4, 4, "🗑 Excluir");
        }
        final File activeDir = target.isDirectory() ? target : target.getParentFile();
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 101: showInputDialog("New Folder", "", activeDir, true); break;
                case 102: showInputDialog("New Script", ".lua", activeDir, false); break;
                case 103: openFilePicker(activeDir); break;
                case 3:   showRenameDialog(target); break;
                case 4:   target.delete(); refreshTree(); break;
            }
            return true;
        });
        popup.show();
    }

    private void showInputDialog(String title, String ext, File dir, boolean isFolder) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_input, null);
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(v);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        TextView txtTitle = v.findViewById(R.id.dialog_title);
        EditText input = v.findViewById(R.id.dialog_input);
        Button btnConfirm = v.findViewById(R.id.btn_confirm);
        txtTitle.setText(title);
        btnConfirm.setText(isFolder ? "CREATE" : "SAVE");
        btnConfirm.setOnClickListener(view -> {
            String name = input.getText().toString().trim();
            if(!name.isEmpty()){
                File f = new File(dir, name + ext);
                try { if(isFolder) f.mkdir(); else f.createNewFile(); refreshTree(); } catch(Exception e){}
                dialog.dismiss();
            }
        });
        v.findViewById(R.id.btn_cancel).setOnClickListener(view -> dialog.dismiss());
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
    }

    private void showRenameDialog(File target) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_input, null);
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(v);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        TextView txtTitle = v.findViewById(R.id.dialog_title);
        EditText input = v.findViewById(R.id.dialog_input);
        Button btnConfirm = v.findViewById(R.id.btn_confirm);
        txtTitle.setText("Rename Item");
        input.setText(target.getName());
        btnConfirm.setOnClickListener(view -> {
            String name = input.getText().toString().trim();
            if(!name.isEmpty()){
                target.renameTo(new File(target.getParentFile(), name));
                refreshTree();
                dialog.dismiss();
            }
        });
        v.findViewById(R.id.btn_cancel).setOnClickListener(view -> dialog.dismiss());
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
    }

    private void openFilePicker(File targetDir) {
        currentImportDir = targetDir;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) importFileToEngine(uri);
        }
    }

    private void importFileToEngine(Uri uri) {
        try {
            String fileName = "file";
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIdx != -1) fileName = cursor.getString(nameIdx);
                cursor.close();
            }
            InputStream in = getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(new File(currentImportDir, fileName));
            byte[] buf = new byte[1024]; int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close(); out.close(); refreshTree();
        } catch (Exception e) {}
    }

    private void setupTabs() {
        final Button btnH = findViewById(R.id.btn_tab_hierarchy), btnP = findViewById(R.id.btn_tab_project);
        final View vH = findViewById(R.id.list_hierarchy), vP = findViewById(R.id.project_container);
        btnH.setOnClickListener(v -> { vH.setVisibility(View.VISIBLE); vP.setVisibility(View.GONE); btnH.setBackgroundColor(0xFF1A1A1A); btnP.setBackgroundColor(0xFF252525); btnH.setTextColor(Color.WHITE); btnP.setTextColor(0xFF888888); });
        btnP.setOnClickListener(v -> { vH.setVisibility(View.GONE); vP.setVisibility(View.VISIBLE); btnP.setBackgroundColor(0xFF1A1A1A); btnH.setBackgroundColor(0xFF252525); btnP.setTextColor(Color.WHITE); btnH.setTextColor(0xFF888888); refreshTree(); });
    }

    @Override protected void onPause() { super.onPause(); if (mGLView != null) mGLView.onPause(); }
    @Override protected void onResume() { super.onResume(); if (mGLView != null) mGLView.onResume(); }
}
