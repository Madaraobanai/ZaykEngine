package com.zayk.engine;

import java.io.File;

public class FileItem {
    private File file;
    private int level; // 0 para raiz, 1 para subpasta, etc.
    private boolean isExpanded;

    public FileItem(File file, int level) {
        this.file = file;
        this.level = level;
        this.isExpanded = false;
    }

    public File getFile() { return file; }
    public int getLevel() { return level; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}
