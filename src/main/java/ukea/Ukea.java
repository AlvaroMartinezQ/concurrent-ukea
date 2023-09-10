/*

	Author: Alvaro Martinez Quiroga
	Contact: alvaroo2302@gmail.com
	Github: github.com/AlvaroMartinezQ

*/

package ukea;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Ukea {
	
	// Almacen y su acceso
	private Almacen a;
	private Semaphore accesoAlmacen;
	// Mapa inicial de pedidos de cliente
	private ConcurrentHashMap<Integer, Pedido> listaPedidos;
	// Contador de pedidos
	private AtomicInteger contadorPedidos;
	// Contador de pedidos en preparacion
	private AtomicInteger contadorPreparacion;
	// Dispensadores
	private LinkedBlockingDeque<Pedido> listaPedidosCliente;
	private LinkedBlockingDeque<Pedido> listaPedidosEspeciales;
	// Colas de preparacion
	private LinkedBlockingDeque<Pedido> normales;
	private LinkedBlockingDeque<Pedido> especiales;
	// Numero de clientes maximo en tienda
	private final int MAX_CLIENTES_TIENDA = 5;
	private Semaphore[] lineaDeCaja;
	private boolean[] linea;
	
	private static final int PROD_MENOR = 0;
	private static final int PROD_MAYOR = 9;
	
	public Ukea(int dispensadores) {
		a = new Almacen();
		accesoAlmacen = new Semaphore(1);
		listaPedidos = new ConcurrentHashMap<>();
		contadorPedidos = new AtomicInteger(0);
		contadorPreparacion = new AtomicInteger(0);
		iniciaLineaCajas();
		iniciaDispensadores(dispensadores);
		iniciaColas();
	}
	
	private void iniciaLineaCajas() {
		lineaDeCaja = new Semaphore[MAX_CLIENTES_TIENDA];
		linea = new boolean[MAX_CLIENTES_TIENDA];
		for(int i = 0; i < MAX_CLIENTES_TIENDA; i++) {
			lineaDeCaja[i] = new Semaphore(0);
			linea[i] = false;
		}
	}
	
	private void iniciaDispensadores(int dispensadores) {
		listaPedidosCliente = new LinkedBlockingDeque<Pedido>(dispensadores);
		listaPedidosEspeciales = new LinkedBlockingDeque<Pedido>(dispensadores);
	}
	
	private void iniciaColas() {
		normales = new LinkedBlockingDeque<Pedido>();
		especiales = new LinkedBlockingDeque<Pedido>();
	}
	
	private int posicionLibreCola() {
		for(int i = 0; i < MAX_CLIENTES_TIENDA; i++) {
			if(linea[i] == false) {
				return i;
			}
		}
		return -1;
	}
	
	//Acciones del cliente en la tienda
	
	public Producto dameNormal(int idprod) {
		return a.dameNormal(idprod);
	}
	
	public ProductoEspecial dameEspecial(int idprod1, int idprod2) {
		return a.dameEspecial(idprod1, idprod2);
	}
	
	public double relizarPedido(Pedido p, int idcli, double dinero) throws InterruptedException {
		System.out.println("Cliente " + idcli + ", quiere realizar un pedido.");
		int rand, rand2;
		if(p.esPedidoEspecial()) {
			for(int i = 0; i < p.getProductosPedidos(); i++) {
				rand = ThreadLocalRandom.current().nextInt(PROD_MENOR, PROD_MAYOR + 1);
				rand2 = ThreadLocalRandom.current().nextInt(PROD_MENOR, PROD_MAYOR + 1);
				p.addEspecial(dameEspecial(rand, rand2));
			}
		} else {
			for(int i = 0; i < p.getProductosPedidos(); i++) {
				rand = ThreadLocalRandom.current().nextInt(PROD_MENOR, PROD_MAYOR + 1);
				p.addProducto(dameNormal(rand));
			}
		}
		Thread.sleep((long)(Math.random() * 500));
		// Realizar pedido
		int pos = -1;
		while(pos == -1) {
			pos = posicionLibreCola();
			System.out.println("Cliente " + idcli + ", buscando una posicion libre en la cola.");
			Thread.sleep(1000); // Simulamos espera y volvemos a intentar
		}
		linea[pos] = true;
		p.setId(contadorPedidos.incrementAndGet());
		p.setIdcli(idcli);
		p.setPosicionCola(pos);
		listaPedidos.put(contadorPedidos.get(), p);
		System.out.println("Cliente: " + idcli + ", ha hecho el pedido: " + p.toString() + ". Esperando...");
		lineaDeCaja[pos].acquire();
		// En este momento ya esta preparado el pedido, pagar
		if(!p.isRealizado()) {
			return -2;
		}
		double precio = p.calculaPrecio();
		if(dinero - precio < 0) {
			// No puedo pagar
			System.out.println("El cliente " + idcli + " no dispone de fondos para pagar el pedido " + p.toString() + ". ABORTANDO OPERACION...");
			return -1;
		} else {
			// Puedo pagar
			System.out.println("El cliente " + idcli + " dispone de fondos para pagar el pedido " + p.toString() + ". REALIZANDO OPERACION...");
			return (dinero-precio);
		}
	}
	
	public void devolverPedido(Pedido p) throws InterruptedException {
		// Devolver los productos
		if(p == null) {
			// No deberia de llegar aqui
			System.out.println("Se ha intentado devolver un pedido inexistente.");
			return;
		}
		accesoAlmacen.acquire();
		System.out.println("Devuelto el pedido: " + p.toString());
		a.devolverPedido(p);
		accesoAlmacen.release();
	}
	
	// Acciones de EmpCogePedidos en la tienda
	
	public void entregaPedido(int idemp) throws InterruptedException {
		Thread.sleep((long)(Math.random() * 1000));
		Pedido p = listaPedidosCliente.poll();
		if(p == null) {
			return;
		}
		System.out.println("EmpCogePedidos: " + idemp + ", entregando pedido: " + p.toString());
		Thread.sleep((long)(Math.random() * 1000)); // Simulacion de entrega
		int pos = p.getPosicionCola();
		linea[pos] = false;
		lineaDeCaja[pos].release();
	}
	
	public void pedidoAProduccion(int idemp) throws InterruptedException {
		Thread.sleep((long)(Math.random() * 1000));
		int pedidos = contadorPedidos.get();
		int preparacion = contadorPreparacion.get();
		if(pedidos <= preparacion) { // El numero de preparacion no deberia ser mayor NUNCA que el numero de pedidos
			// No hay pedidos que poner en cola de preparacion
			return;
		}
		preparacion = contadorPreparacion.incrementAndGet();
		Pedido p = listaPedidos.get(preparacion);
		if(p == null) {
			// No deberia de ejecutar este if (por si acaso lo controlamos)
			System.out.println("EmpCogePedidos: " + idemp + ". ha ocurrido un error en la lista de pedidos. ABORTANDO OPERACION...");
			return;
		}
		System.out.println("EmpCogePedidos: " + idemp + ", pedido: " + p.toString() + " a produccion.");
		if(p.esPedidoEspecial()) {
			// Poner el pedido en lista de preparacion espeaciales
			addEspecial(p);
		} else {
			// Poner el pedido en lista de preparacion normales
			addNormal(p);
		}
	}
	
	private void addNormal(Pedido p) throws InterruptedException {
		normales.add(p);
		Thread.sleep((long)(Math.random() * 500)); // Simulacion de cola de preparacion
	}
	
	private void addEspecial(Pedido p) throws InterruptedException {
		especiales.add(p);
		Thread.sleep((long)(Math.random() * 500)); // Simulacion de cola de preparacion		
	}
	
	// Acciones de EmpElaboraPedido (se encarga de pedidios especiales) en la tienda
	
	public void preparaEspecial(int idemp) throws InterruptedException {
		Thread.sleep((long)(Math.random() * 1000));
		Pedido p = especiales.poll();
		if(p == null) {
			return;
		}
		System.out.println("EmpElaboraPedido: "+ idemp + ", pedido: " + p.toString() + " en preparacion...");
		// Entrar al almacen a por los objetos, SECCION CRITICA
		accesoAlmacen.acquire();
		for(ProductoEspecial pe: p.getListaEspecial()) {
			ProductoEspecial sacar = a.sacarProductoEspecial(pe.getPrimero().getId(), pe.getSegundo().getId());
			if(sacar == null) {
				System.out.println("EmpElaboraPedido: " + idemp + ", sin existencias de productos. ABORTANDO OPERACION...");
				int pos = p.getPosicionCola();
				linea[pos] = false;
				lineaDeCaja[pos].release();
				return;
			}
		}
		if(p.getListaPedido().size() != 0) {
			// Tambien hay productos simples en este pedido, sacarlos del almacen
			for(Producto ps: p.getListaPedido()) {
				a.sacarProductoSimple(ps.getId());
			}
		}
		Thread.sleep((long)(Math.random() * 3000)); // Simulacion de preparacion
		accesoAlmacen.release();
		// FIN SECCION CRITICA
		// Insertar producto preparado en dispensador
		p.setRealizado(true);
		boolean insertado = false;
		while(!insertado) {
			insertado = listaPedidosEspeciales.add(p);
		}
		System.out.println("EmpElaboraPedido: " + idemp + ", pedido: " + p.toString() + " incluido en linea de entrega pedidos especiales.");
	}
	
	// Acciones de EmpPreparaProductos (se encarga de pedidios normales y transportar de linea pedidos especiales finalizados) en la tienda
	
	public void cambiaLineaEspecial(int idemp) throws InterruptedException {
		Thread.sleep((long)(Math.random() * 1000));
		Pedido p = listaPedidosEspeciales.poll();
		if(p == null) {
			// Sin pedidos que cambiar de dispensador
			return;
		}
		boolean insertado = false;
		while(!insertado) {
			insertado = listaPedidosCliente.add(p);
		}
		Thread.sleep((long)(Math.random() * 1000)); // Simulacion del cambio de dispensador
		System.out.println("EmpPreparaPedidos: " + idemp + ", pedido " + p.toString() + " incluido en linea de entrega a cliente");
	}
	
	public void preparaNormal(int idemp) throws InterruptedException {
		Thread.sleep((long)(Math.random() * 1000));
		Pedido p = normales.poll();
		if(p == null) {
			// Sin pedidos normales que preparar
			return;
		}
		System.out.println("EmpPreparaPedidos " + idemp + ", pedido: " + p.toString() + " en preparacion...");
		if(p.getListaEspecial().size() != 0) {
			// No deberia de llegar aqui
			System.out.println("EmpPreparaPedidos: " + idemp +", pedido: " + p.toString() + " contiene productos especiales, se encuentra en linea de produccion erronea. ABORTANDO OPERACION...");
			// Despertar al cliente en espera
			return;
		}
		// Entrar al almacen a por los objetos, SECCION CRITICA
		accesoAlmacen.acquire();
		for(Producto ps: p.getListaPedido()) {
			Producto sacar = a.sacarProductoSimple(ps.getId());
			if(sacar == null) {
				System.out.println("EmpPreparaPedidos: " + idemp + ", sin existencias de: " + ps.getId() + ". ABORTANDO OPERACION...");
				int pos = p.getPosicionCola();
				linea[pos] = false;
				lineaDeCaja[pos].release();
				return;
			}
		}
		Thread.sleep((long)(Math.random() * 3000)); // Simulacion de preparacion
		accesoAlmacen.release();
		// FIN SECCION CRITICA
		// Insertar pedido listo en el dispensador
		p.setRealizado(true);
		boolean insertado = false;
		while(!insertado) {
			insertado = listaPedidosCliente.add(p);
		}
		System.out.println("EmpPreparaPedidos: " + idemp + ", pedido " + p.toString() + " incluido en linea de entrega a cliente");
	}
	
	// Estado de la tienda
	
	public void infoTienda() throws InterruptedException {
		// No es necesario tomar el semaforo de entrada al almacen ya que no se cambiaran variables
		System.out.println("\nESTADO TIENDA :");
		System.out.println("Pedidos: " + contadorPedidos.get());
		System.out.println("FIN ESTADO TIENDA.\n");
		System.out.println("\nESTADO ALMACEN :");
		a.estadoAlmacen();
		System.out.println("FIN ESTADO ALMACEN.\n");
	}
	
}
