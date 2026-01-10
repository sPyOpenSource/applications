package cr0s.javara.util;

public class Pos {

    private double x, y, z;
    
    public Pos(int x, int y){
        this.x = x * 24;
        this.y = y * 24;
    }
    
    public Pos(double x, double y) {
	this.x = x;
        this.y = y;
	this.z = 0;
    }

    public Pos(double x, double y, double z) {
	this(x, y);
	
	this.z = z;
    }
    
    public Pos Clone() {
	return new Pos(x, y, z);
    }
    
    public double getZ() {
	return this.z;
    }
    
    public void setZ(double z) {
	this.z = z;
    }
    
    public double distanceToSq(Pos other) {
	double dx = this.x - other.getX();
	double dy = this.y - other.getY();
	double dz = this.z - other.getZ();

	return Math.ceil(dx * dx + dy * dy + dz * dz);	
    }
    
    public double distanceTo(Pos other) {
	return Math.sqrt(this.distanceToSq(other));
    }
    
    @Override
    public int hashCode() {
	final int prime = 3;
	int result = 1;
	result = prime * result + (int) this.getX() + (prime * 2 + (int) this.getY()) + (prime * 3 + (int) this.getZ());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Pos other = (Pos) obj;
	return !((int) x != (int) other.x || (int)y != (int) other.y || (int) z != (int) other.z);
    }

    public Pos add(Pos other) {
	Pos p = new Pos(0, 0);
	p.setX(this.getX() + other.getX());
	p.setY(this.getY() + other.getY());
	p.setZ(this.getZ() + other.getZ());
	
	return p;
    }
    
    @Override
    public String toString() {
	return "Pos (" + this.getX() + "; " + this.getY() + "; " + this.getZ() + ")";
    }

    public Pos sub(Pos other) {
	Pos p = new Pos(0, 0);
	p.setX(this.getX() - other.getX());
	p.setY(this.getY() - other.getY());
	p.setZ(this.getZ() - other.getZ());
	
	return p;
    }

    public Pos rotate2D(float angle) {
	return new Pos(this.getX() * (float) Math.sin(angle), this.getY() * (float) Math.cos(angle), this.getZ());
    }
    
    public Pos mul(float c) {
	Pos p = new Pos(0, 0);
	p.setX(this.getX() * c);
	p.setY(this.getY() * c);
	p.setZ(this.getZ() * c);
	
	return p;
    }

    public float getHorizontalLength() {
	return (float) Math.sqrt(this.getX() * this.getX() + this.getY() * this.getY());
    }

    public Pos mul(Pos other) {
	Pos p = new Pos(0, 0);
	p.setX(this.getX() * other.getX());
	p.setY(this.getY() * other.getY());
	p.setZ(this.getZ() * other.getZ());
	
	return p;
    }

    public Pos add(float a) {
	return this.add(new Pos(a, a));
    }    
    
    public double lengthSquared() {
	return this.getX() * this.getX() + this.getY() * this.getY();
    }
    
    public float length() {
	return (float) Math.sqrt(this.lengthSquared());
    }
    
    public double dot(Pos other) {
	return this.getX() * other.getX() + this.getY() + other.getY() + this.getZ() + other.getZ();	
    }

    public double getX() {
        return x;
    }
    
    public int getCellX(){
        return (int)x / 24;
    }
    
    public int getCellY(){
        return (int)y / 24;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }
    
    public void setY(double y){
        this.y = y;
    }

    public void setLocation(double x, double y) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
