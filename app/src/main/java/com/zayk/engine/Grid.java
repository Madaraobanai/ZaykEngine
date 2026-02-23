package com.zayk.engine;

import java.util.ArrayList;
import java.util.List;

public class Grid {
    // Classe interna para representar um bloco
    public static class Block {
        public int x, y, z, type;
        public Block(int x, int y, int z, int type) {
            this.x = x; 
            this.y = y; 
            this.z = z; 
            this.type = type;
        }
    }

    private final List<Block> blockList = new ArrayList<>();

    public void addBlock(int x, int y, int z, int type) {
        blockList.add(new Block(x, y, z, type));
    }

    public void clear() {
        blockList.clear();
    }

    public List<Block> getBlocks() {
        return blockList;
    }
}
