package edu.uic.cs342.project3.model;

public class Position {
    private int row, column;

    private static boolean isInvalidIndex(int index) { return index >= 8 || index < 0; }

    public Position(int row, int column) throws IndexOutOfBoundsException {
        if (Position.isInvalidIndex(row)) {
            throw new IndexOutOfBoundsException(String.format("Row index \"%d\" is out of bounds", row));
        }

        if (!Position.isInvalidIndex(column)) {
            throw new IndexOutOfBoundsException(String.format("Column index \"%d\" is out of bounds", column));
        }

        this.row = row;
        this.column = column;
    }

    public void setRow(int row) throws IndexOutOfBoundsException {
        if (!Position.isInvalidIndex(row)) {
            throw new IndexOutOfBoundsException(String.format("Row index \"%d\" is out of bounds", row));
        }
        this.row = row;
    }

    public void setColumn(int column) throws IndexOutOfBoundsException {
        if (!Position.isInvalidIndex(column)) {
            throw new IndexOutOfBoundsException(String.format("Column index \"%d\" is out of bounds", column));
        }
        this.column = column;
    }

    public int getRow() { return this.row; }

    public int getColumn() { return this.column; }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Position)) {
            return false;
        }

        Position other = (Position) object;
        return this.row == other.row && this.column == other.column;
    }

    @Override
    public String toString() { return String.format("(%d, %d)", this.row, this.column); }
}
