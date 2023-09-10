/*

	Author: Alvaro Martinez Quiroga
	Contact: alvaroo2302@gmail.com
	Github: github.com/AlvaroMartinezQ

*/

package ukea;

public class Producto {
	private int id;
	private double precio;
	
	public Producto(int id, double precio) {
		super();
		this.id = id;
		this.precio = precio;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public double getPrecio() {
		return precio;
	}

	public void setPrecio(double precio) {
		this.precio = precio;
	}

	@Override
	public String toString() {
		return "Producto [id=" + id + ", precio=" + precio + "]";
	}
	
}
