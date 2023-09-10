/*

	Author: Alvaro Martinez Quiroga
	Contact: alvaroo2302@gmail.com
	Github: github.com/AlvaroMartinezQ

*/

package ukea;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Almacen {
	/*
	 * Almacenamiento de objetos
	 * Hay 10 productos en el sistema, identificados del 0 al 9
	 * 15 unidades en existencia por producto
	 */
	
	private final int N_PRODUCTOS = 10; // diferentes productos
	private final int N_EXISTENCIAS_PRODUCTO = 15; // existencias por producto
	
	private final int min = 10;
	private final int max = 50;
	
	private ArrayList<Producto> listaProductos;
	private int[] existencias;

	public Almacen() {
		super();
		this.listaProductos = new ArrayList<Producto>();
		// Generar productos
		for(int i = 0; i < this.N_PRODUCTOS; i++) {
			double precio = ThreadLocalRandom.current().nextDouble(min, max + 1);;
			Producto p = new Producto(i, precio);
			this.listaProductos.add(p);
		}
		this.existencias = new int[this.N_PRODUCTOS];
		for(int i = 0; i < this.N_PRODUCTOS; i++) {
			this.existencias[i] = this.N_EXISTENCIAS_PRODUCTO;
		}
	}
	
	// metodos que devuelven productos, no cambian existencias
	public Producto dameNormal(int idprod) {
		return this.listaProductos.get(idprod);
	}
	
	public ProductoEspecial dameEspecial(int idprod1, int idprod2) {
		ProductoEspecial pe = new ProductoEspecial(this.listaProductos.get(idprod1), this.listaProductos.get(idprod2));
		return pe;
	}
	
	// operacion donde solo 1 hilo puede actuar
	public Producto sacarProductoSimple(int id) {
		Producto p = this.listaProductos.get(id);
		// Comprobar exitencias
		if(!compruebaExistencias(id)) {
			return null;
		}
		this.existencias[id]--;
		return p;
	}
	
	// operacion donde solo 1 hilo puede actuar
	public ProductoEspecial sacarProductoEspecial(int id1, int id2) {
		Producto p1 =  this.listaProductos.get(id1);
		Producto p2 =  this.listaProductos.get(id2);
		ProductoEspecial pe = new ProductoEspecial(p1, p2);
		// Comprobar existencias
		if(!compruebaExistencias(id1) || !compruebaExistencias(id2)) {
			return null;
		}
		this.existencias[id1]--;
		this.existencias[id2]--;
		return pe;
	}
	
	public boolean compruebaExistencias(int id) {
		if(this.existencias[id] == 0) { // no quedan existencias
			System.out.println("Sin existencias del producto solicitado con id: " + id);
			return false;
		}
		return true;
	}
	
	public void devolverPedido(Pedido p) {
		if(!p.getListaPedido().isEmpty()) {
			for(Producto ps: p.getListaPedido()) {
				this.existencias[ps.getId()]++;
			}
		}
		if(!p.getListaEspecial().isEmpty()) {
			for(ProductoEspecial pe: p.getListaEspecial()) {
				this.existencias[pe.getPrimero().getId()]++;
				this.existencias[pe.getSegundo().getId()]++;
			}
		}
	}
	
	public void estadoAlmacen() {
		for(int i = 0; i < this.N_PRODUCTOS; i++) {
			System.out.println("-> Producto: " + this.listaProductos.get(i).toString() + ", existencias: " + this.existencias[i]);
		}
	}
	
}
