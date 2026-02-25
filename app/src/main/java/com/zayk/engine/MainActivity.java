package com.zayk.engine;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.provider.Settings;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
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
    private String selectedPath = ""; 
    private String searchQuery = ""; 
    private static final int PICK_FILE_REQUEST = 1;
    private File currentImportDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mGLView = findViewById(R.id.zayk_surface_view);
        
        ListView listProject = findViewById(R.id.list_project);
        treeAdapter = new FileTreeAdapter();
        listProject.setAdapter(treeAdapter);

        EditText searchBar = findViewById(R.id.search_bar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                refreshTree();
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        // Clique simples: Abrir pastas ou scripts
        listProject.setOnItemClickListener((parent, view, position, id) -> {
            FileNode node = displayList.get(position);
            selectedPath = node.file.getAbsolutePath();
            if (node.file.isDirectory()) {
                node.isExpanded = !node.isExpanded;
                refreshTree();
            } else if (node.file.getName().toLowerCase().endsWith(".lua")) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                intent.putExtra("filePath", node.file.getAbsolutePath());
                startActivity(intent);
            }
            treeAdapter.notifyDataSetChanged();
        });

        // Clique longo: Menu de opções (O que estava faltando)
        listProject.setOnItemLongClickListener((parent, view, position, id) -> {
            FileNode node = displayList.get(position);
            selectedPath = node.file.getAbsolutePath();
            treeAdapter.notifyDataSetChanged();
            showGodotMenu(view, node.file);
            return true;
        });

        setupResizers();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGLView != null) mGLView.onResume();
        rootDir = new File(Environment.getExternalStorageDirectory(), "ZaykProjects");
        if (hasStoragePermission()) {
            if (!rootDir.exists()) rootDir.mkdirs();
            refreshTree();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return Environment.isExternalStorageManager();
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 200);
            } catch (Exception e) {}
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == 200 || requestCode == 100) && hasStoragePermission()) {
            recreate();
        }
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            handleImport(data.getData());
        }
    }

    private void setupResizers() {
        final View leftP = findViewById(R.id.left_panel);
        final View rightP = findViewById(R.id.right_panel);
        final View sceneC = findViewById(R.id.scene_container);
        final int sw = getResources().getDisplayMetrics().widthPixels;

        findViewById(R.id.left_resizer).setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                int nw = (int) e.getRawX();
                if (nw > 100 && nw < sw * 0.6) {
                    leftP.getLayoutParams().width = nw;
                    leftP.requestLayout();
                }
            }
            return true;
        });

        findViewById(R.id.right_resizer).setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                int nw = sw - (int) e.getRawX();
                if (nw > 100 && nw < sw * 0.6) {
                    rightP.getLayoutParams().width = nw;
                    rightP.requestLayout();
                }
            }
            return true;
        });

        findViewById(R.id.horizontal_resizer).setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                int[] loc = new int[2];
                leftP.getLocationOnScreen(loc);
                int newH = (int) e.getRawY() - loc[1];
                if (newH > 150 && newH < leftP.getHeight() - 150) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) sceneC.getLayoutParams();
                    lp.height = newH;
                    lp.weight = 0;
                    sceneC.setLayoutParams(lp);
                }
            }
            return true;
        });
    }

    private void refreshTree() {
        if (!hasStoragePermission()) return;
        List<String> expandedPaths = new ArrayList<>();
        for(FileNode n : displayList) if(n.isExpanded) expandedPaths.add(n.file.getAbsolutePath());
        displayList.clear();
        if (rootDir != null && rootDir.exists()) {
            FileNode rootNode = new FileNode(rootDir, 0);
            rootNode.isExpanded = true;
            displayList.add(rootNode);
            buildTree(rootDir, 1, expandedPaths);
        }
        treeAdapter.notifyDataSetChanged();
    }

    private void buildTree(File dir, int level, List<String> expandedPaths) {
        File[] files = dir.listFiles();
        if (files == null) return;
        List<File> fileList = new ArrayList<>();
        Collections.addAll(fileList, files);
        Collections.sort(fileList, (a, b) -> (a.isDirectory() == b.isDirectory()) ? 
            a.getName().compareToIgnoreCase(b.getName()) : a.isDirectory() ? -1 : 1);
        for (File f : fileList) {
            FileNode node = new FileNode(f, level);
            if (!searchQuery.isEmpty()) {
                if (f.getName().toLowerCase().contains(searchQuery)) displayList.add(node);
                if (f.isDirectory()) buildTree(f, level + 1, expandedPaths);
            } else {
                if (expandedPaths.contains(f.getAbsolutePath())) node.isExpanded = true;
                displayList.add(node);
                if (f.isDirectory() && node.isExpanded) buildTree(f, level + 1, expandedPaths);
            }
        }
    }

    private void showGodotMenu(View anchor, final File target) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_InputMethod);
        PopupMenu popup = new PopupMenu(wrapper, anchor);
        Menu menu = popup.getMenu();
        Menu createSub = menu.addSubMenu("＋ Criar Novo");
        createSub.add(0, 101, 0, "pasta");
        createSub.add(0, 102, 1, "script.lua");
        menu.add(0, 103, 2, "Importar");
        if (!target.equals(rootDir)) {
            menu.add(0, 5, 3, "Duplicar");
            menu.add(0, 6, 4, "Copiar res://");
            menu.add(0, 4, 5, "Excluir");
        }
        popup.setOnMenuItemClickListener(item -> {
            File activeDir = target.isDirectory() ? target : target.getParentFile();
            switch (item.getItemId()) {
                case 101: showInputDialog("Nova Pasta", "", activeDir, true); break;
                case 102: showInputDialog("Novo Script", ".lua", activeDir, false); break;
                case 103: currentImportDir = activeDir; openFilePicker(); break;
                case 5: duplicateFile(target); break;
                case 6: copyResPath(target); break;
                case 4: target.delete(); refreshTree(); break;
            }
            return true;
        });
        popup.show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    private void handleImport(Uri uri) {
        try {
            String fileName = "imported_" + System.currentTimeMillis();
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) fileName = cursor.getString(nameIndex);
                cursor.close();
            }
            InputStream in = getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(new File(currentImportDir, fileName));
            byte[] buf = new byte[1024]; int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close(); out.close();
            refreshTree();
        } catch (Exception e) {}
    }

    private void duplicateFile(File source) {
        try {
            File dest = new File(source.getParentFile(), "Copy_" + source.getName());
            FileChannel inC = new FileInputStream(source).getChannel();
            FileChannel outC = new FileOutputStream(dest).getChannel();
            inC.transferTo(0, inC.size(), outC);
            inC.close(); outC.close();
            refreshTree();
        } catch (Exception e) {}
    }

    private void copyResPath(File file) {
        String resPath = "res:/" + file.getAbsolutePath().replace(rootDir.getAbsolutePath(), "");
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText("ResPath", resPath));
        Toast.makeText(this, "Caminho res:// copiado!", Toast.LENGTH_SHORT).show();
    }

    private void showInputDialog(String title, String ext, File dir, boolean isFolder) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_input, null);
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(v);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        ((TextView)v.findViewById(R.id.dialog_title)).setText(title);
        v.findViewById(R.id.btn_confirm).setOnClickListener(view -> {
            String n = ((EditText)v.findViewById(R.id.dialog_input)).getText().toString().trim();
            if(!n.isEmpty()){
                File f = new File(dir, n + ext);
                try { if(isFolder) f.mkdir(); else f.createNewFile(); refreshTree(); } catch(Exception e){}
                dialog.dismiss();
            }
        });
        v.findViewById(R.id.btn_cancel).setOnClickListener(view -> dialog.dismiss());
        dialog.show();
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
            v.setBackgroundColor(node.file.getAbsolutePath().equals(selectedPath) ? Color.parseColor("#444488FF") : Color.TRANSPARENT);
            indent.getLayoutParams().width = (node.level * 40);
            typeIcon.setColorFilter(null);
            if (node.file.isDirectory()) {
                arrowIcon.setVisibility(View.VISIBLE);
                arrowIcon.setImageResource(node.isExpanded ? android.R.drawable.arrow_down_float : android.R.drawable.ic_media_play);
                typeIcon.setImageResource(android.R.drawable.ic_menu_today);
                typeIcon.setColorFilter(Color.parseColor("#4CCBFF"), PorterDuff.Mode.SRC_IN);
                name.setText(node.file.equals(rootDir) ? "res://" : node.file.getName());
            } else {
                arrowIcon.setVisibility(View.INVISIBLE);
                name.setText(node.file.getName());
                String fn = node.file.getName().toLowerCase();
                if (fn.endsWith(".png") || fn.endsWith(".jpg") || fn.endsWith(".webp")) {
                    try {
                        BitmapFactory.Options opt = new BitmapFactory.Options();
                        opt.inSampleSize = 8;
                        Bitmap thumb = BitmapFactory.decodeFile(node.file.getAbsolutePath(), opt);
                        if (thumb != null) typeIcon.setImageBitmap(thumb);
                        else typeIcon.setImageResource(android.R.drawable.ic_menu_gallery);
                    } catch (Exception e) { typeIcon.setImageResource(android.R.drawable.ic_menu_gallery); }
                } else {
                    typeIcon.setImageResource(fn.endsWith(".lua") ? android.R.drawable.ic_menu_edit : android.R.drawable.ic_menu_report_image);
                    typeIcon.setColorFilter(fn.endsWith(".lua") ? Color.YELLOW : Color.LTGRAY, PorterDuff.Mode.SRC_IN);
                }
            }
            return v;
        }
    }

    @Override protected void onPause() { super.onPause(); if (mGLView != null) mGLView.onPause(); }
}
