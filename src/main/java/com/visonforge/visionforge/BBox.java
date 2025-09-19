package com.visonforge.visionforge;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class BBox {

    public String filename = null;
    public int type_int = 0;
    public String type_string = "";
    public int x1 = 0;
    public int y1 = 0;
    public int x2 = 0;
    public int y2 = 0;
    public Rectangle displayRect = new Rectangle();

    public BBox(String filename, int type_int, String type_str, int x1, int y1, int x2, int y2) {
        this.filename = filename;
        this.type_int = type_int;
        this.type_string = type_str;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        displayRect.setFill(Color.TRANSPARENT);
        displayRect.setStroke(Color.RED);
    }

    @Override
    public boolean equals(Object obj){
        if (obj == null || getClass() != obj.getClass()) return false;
        if (this.filename.equals(((BBox)obj).filename) &&
                this.x1 == ((BBox) obj).getX1() &&
                this.y1 == ((BBox) obj).getY1() &&
                this.x2 == ((BBox) obj).getX2() &&
                this.y2 == ((BBox) obj).getY2() &&
                this.type_int == ((BBox) obj).getIntType()) {
            return true;
        }

        return false;
    }

    public String getFilename() {
        return filename;
    }

    public int getIntType() {
        return this.type_int;
    }

    public String getStringType() {
        return this.type_string;
    }

    public int getX1() {
        return this.x1;
    }

    public int getY1() {
        return this.y1;
    }
    public int getX2() {
        return  this.x2;
    }
    public int getY2() {
        return this.y2;
    }
}
