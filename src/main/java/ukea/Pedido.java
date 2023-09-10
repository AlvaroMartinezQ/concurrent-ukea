/*

	Author: Alvaro Martinez Quiroga
	Contact: alvaroo2302@gmail.com
	Github: github.com/AlvaroMartinezQ

*/

package ukea;

import java.util.ArrayList;

public class Pedido {
	/*
	 * 1 pedido tiene maximo 5 productos
	 */
	private int id;
	private int numeroProductos;
	private int productosPedidos;
	private ArrayList<Producto> listaPedido;
	private ArrayList<ProductoEspecial> listaEspecial;
	private double precio;
	private boolean lleno;
	// Variables de gestion para la clase Ukea
	private int idcli;
	private int posicionCola;
	private boolean realizado;
	
	public Pedido(int productos, boolean especial) {
		super();
		this.numeroProductos = 0;
		this.productosPedidos = productos;
		this.listaPedido = new ArrayList<Producto>();
		this.listaEspecial = new ArrayList<ProductoEspecial>();
		this.precio = 0;
		this.lleno = false;
		this.idcli = 0;
		this.posicionCola = 0;
		this.realizado = false;
	}
	
	public int addProducto(Producto p) {
		if(lleno) {
			System.out.println("Pedido " + this.id +" lleno de productos.");
			return -1;
		}
		this.listaPedido.add(p);
		this.numeroProductos++;
		if(this.numeroProductos == 5) {
			this.lleno = true;
		}
		calculaPrecio();
		return 0;
	}
	
	public int addEspecial(ProductoEspecial p) {
		if(lleno) {
			System.out.println("Pedido " + this.id +" lleno de productos.");
			return -1;
		}
		this.listaEspecial.add(p);
		this.numeroProductos++;
		if(this.numeroProductos == 5) {
			this.lleno = true;
		}
		calculaPrecio();
		return 0;
	}
	
	public double calculaPrecio() {
		for(Producto p: this.listaPedido) {
			this.precio = this.precio + p.getPrecio();
		}
		for(ProductoEspecial p: this.listaEspecial) {
			this.precio = this.precio + p.getPrecio();
		}
		return this.precio;
	}
	
	public boolean esPedidoEspecial() {
		if(this.listaEspecial.size() > 0) {
			return true;
		}
		return false;
	}
	
	public ArrayList<Producto> getListaPedido() {
		return listaPedido;
	}

	public ArrayList<ProductoEspecial> getListaEspecial() {
		return listaEspecial;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public int getIdcli() {
		return idcli;
	}

	public void setIdcli(int idcli) {
		this.idcli = idcli;
	}

	public int getPosicionCola() {
		return posicionCola;
	}

	public void setPosicionCola(int posicionCola) {
		this.posicionCola = posicionCola;
	}
	
	public boolean isRealizado() {
		return realizado;
	}

	public void setRealizado(boolean realizado) {
		this.realizado = realizado;
	}

	public int getProductosPedidos() {
		return productosPedidos;
	}

	public void setProductosPedidos(int productosPedidos) {
		this.productosPedidos = productosPedidos;
	}

	@Override
	public String toString() {
		return "[id=" + id + ", numeroProductos=" + numeroProductos + ", precio=" + precio + "]";
	}
	
}
