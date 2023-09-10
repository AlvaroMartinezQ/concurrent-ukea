/*

	Author: Alvaro Martinez Quiroga
	Contact: alvaroo2302@gmail.com
	Github: github.com/AlvaroMartinezQ

*/

package ukea;

public class ProductoEspecial {
	/*
	 * Compuesto de 2 productos sencilos
	 */
	
	private Producto primero;
	private Producto segundo;
	
	public ProductoEspecial(Producto primero, Producto segundo) {
		super();
		this.primero = primero;
		this.segundo = segundo;
	}

	public double getPrecio() {
		return this.primero.getPrecio() + this.segundo.getPrecio();
	}
	
	public Producto getPrimero() {
		return primero;
	}

	public Producto getSegundo() {
		return segundo;
	}

	@Override
	public String toString() {
		return "ProductoEspecial [primero=" + primero.toString() + ", segundo=" + segundo.toString() + "]";
	}
	
}
